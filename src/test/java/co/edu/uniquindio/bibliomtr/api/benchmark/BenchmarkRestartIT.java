package co.edu.uniquindio.bibliomtr.api.benchmark;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.TypeExcludeFilter;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.postgresql.PostgreSQLContainer;

import co.edu.uniquindio.bibliomtr.BibliomtrApplication;
import co.edu.uniquindio.bibliomtr.TestcontainersConfiguration;
import co.edu.uniquindio.bibliomtr.api.ranking.FakeCorpusTokenSource;
import co.edu.uniquindio.bibliomtr.evaluation.JmhParameters;
import co.edu.uniquindio.bibliomtr.persistence.benchmark.BenchmarkRun;
import co.edu.uniquindio.bibliomtr.persistence.benchmark.BenchmarkRunRepository;

/**
 * Tests de dos arranques sucesivos de la aplicación sobre la misma base (REQ-21, REQ-27; AC-21, AC-24).
 *
 * <p><b>Montaje.</b> Un {@link PostgreSQLContainer} estático vive durante toda la clase y cada arranque es un
 * {@link SpringApplicationBuilder} nuevo que apunta a él, de modo que el segundo arranque ve lo que dejó el
 * primero, como un reinicio real. Se usan el corpus y el launcher falsos. Como no es un {@code @SpringBootTest},
 * se registra un {@link TypeExcludeFilter} que deja fuera del escaneo las {@code @TestConfiguration} del
 * classpath de test (entre ellas {@link TestcontainersConfiguration}, que crearía otra base).
 *
 * <p>Cada test vacía el historial en su primer arranque, porque la base es compartida.
 *
 * <p><b>Medición EN_CURSO interrumpida.</b> Se guarda directamente una fila EN_CURSO en el primer arranque, que
 * es lo que queda en la base cuando la aplicación muere a mitad de una medición.
 */
class BenchmarkRestartIT {

	private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(TestcontainersConfiguration.PGVECTOR_IMAGE);

	@BeforeAll
	static void startDatabase() {
		POSTGRES.start();
	}

	@AfterAll
	static void stopDatabase() {
		POSTGRES.stop();
	}

	@Test
	@DisplayName("AC-21: las mediciones guardadas se conservan tras reiniciar la aplicación")
	void historySurvivesRestart() throws Exception {
		long completedId;
		try (ConfigurableApplicationContext first = startApplication()) {
			first.getBean(BenchmarkRunRepository.class).deleteAll();
			MockMvc mockMvc = mockMvc(first);
			String body = mockMvc.perform(post("/api/benchmarks"))
					.andExpect(status().isAccepted())
					.andReturn().getResponse().getContentAsString();
			completedId = ((Number) JsonPath.read(body, "$.id")).longValue();
			awaitIdle(first);
			mockMvc.perform(get("/api/benchmarks")).andExpect(jsonPath("$[0].status").value("COMPLETADA"));
		}

		try (ConfigurableApplicationContext second = startApplication()) {
			mockMvc(second).perform(get("/api/benchmarks"))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$", hasSize(1)))
					.andExpect(jsonPath("$[0].id").value(completedId))
					.andExpect(jsonPath("$[0].status").value("COMPLETADA"))
					.andExpect(jsonPath("$[0].termCount").value(3))
					.andExpect(jsonPath("$[0].parameters.measurementIterations").value(10))
					.andExpect(jsonPath("$[0].results", hasSize(3)))
					.andExpect(jsonPath("$[0].results[0].algorithm").value("counting"))
					.andExpect(jsonPath("$[0].results[0].scoreMs").value(0.0271))
					.andExpect(jsonPath("$[0].results[0].parameters.k").value(5))
					.andExpect(jsonPath("$[0].results[1].parameters.d").value(1))
					.andExpect(jsonPath("$[0].results[1].parameters.b").value(10))
					.andExpect(jsonPath("$[0].results[2].parameters.m").value(1));
		}
	}

	@Test
	@DisplayName("AC-24: una medición EN_CURSO pasa a FALLIDA al reiniciar y luego se puede lanzar otra sin 409")
	void inProgressRunFailsOnRestart() throws Exception {
		long interruptedId;
		try (ConfigurableApplicationContext first = startApplication()) {
			BenchmarkRunRepository repository = first.getBean(BenchmarkRunRepository.class);
			repository.deleteAll();
			BenchmarkRun run = repository.save(BenchmarkRun.start(Instant.now(), JmhParameters.FIXED, 3));
			interruptedId = run.getId();
			mockMvc(first).perform(post("/api/benchmarks")).andExpect(status().isConflict());
		}

		try (ConfigurableApplicationContext second = startApplication()) {
			MockMvc mockMvc = mockMvc(second);
			mockMvc.perform(get("/api/benchmarks"))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$", hasSize(1)))
					.andExpect(jsonPath("$[0].id").value(interruptedId))
					.andExpect(jsonPath("$[0].status").value("FALLIDA"))
					.andExpect(jsonPath("$[0].message").value("Medición interrumpida por reinicio de la aplicación"))
					.andExpect(jsonPath("$[0].finishedAt", notNullValue()))
					.andExpect(jsonPath("$[0].results", empty()));

			mockMvc.perform(post("/api/benchmarks"))
					.andExpect(status().isAccepted())
					.andExpect(jsonPath("$.status").value("EN_CURSO"));
			awaitIdle(second);
		}
	}

	/**
	 * Arranca la aplicación completa sobre el contenedor compartido, con servidor en un puerto libre, el corpus
	 * falso con {@code model} ×5, {@code data} ×2 y {@code graph} ×1 (n = 3, k = 5, d = 1, m = 1) y el launcher
	 * falso en modo completa.
	 */
	private static ConfigurableApplicationContext startApplication() {
		ConfigurableApplicationContext application = new SpringApplicationBuilder(BibliomtrApplication.class,
				FakeCorpusTokenSource.Config.class, FakeBenchmarkLauncher.Config.class)
				.initializers(context -> context.getBeanFactory()
						.registerSingleton("excludeTestConfigurations", new ExcludeTestConfigurations()))
				.run("--spring.datasource.url=" + POSTGRES.getJdbcUrl(),
						"--spring.datasource.username=" + POSTGRES.getUsername(),
						"--spring.datasource.password=" + POSTGRES.getPassword(),
						"--server.port=0",
						"--bibliomtr.corpus.dir=build/tmp/empty-corpus",
						"--spring.main.banner-mode=off");
		List<String> tokens = new ArrayList<>();
		tokens.addAll(Collections.nCopies(5, "model"));
		tokens.addAll(Collections.nCopies(2, "data"));
		tokens.add("graph");
		application.getBean(FakeCorpusTokenSource.class).setAbstracts(List.of(tokens));
		return application;
	}

	/** MockMvc sobre el contexto web de un arranque. */
	private static MockMvc mockMvc(ConfigurableApplicationContext application) {
		return MockMvcBuilders.webAppContextSetup((WebApplicationContext) application).build();
	}

	/** Espera a que el executor de un hilo termine la medición en curso. */
	private static void awaitIdle(ConfigurableApplicationContext application) throws Exception {
		application.getBean("benchmarkExecutor", ExecutorService.class).submit(() -> {
		}).get(30, TimeUnit.SECONDS);
	}

	/**
	 * Excluye del escaneo de componentes las clases anotadas con {@link TestConfiguration}, como hace
	 * {@code @SpringBootTest}; las que el test necesita se pasan explícitamente al builder.
	 */
	static final class ExcludeTestConfigurations extends TypeExcludeFilter {

		@Override
		public boolean match(MetadataReader metadataReader, MetadataReaderFactory metadataReaderFactory)
				throws IOException {
			return metadataReader.getAnnotationMetadata().hasAnnotation(TestConfiguration.class.getName());
		}

		@Override
		public boolean equals(Object other) {
			return other != null && other.getClass() == getClass();
		}

		@Override
		public int hashCode() {
			return getClass().hashCode();
		}

	}

}
