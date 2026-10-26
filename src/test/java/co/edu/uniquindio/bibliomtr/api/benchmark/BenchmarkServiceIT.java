package co.edu.uniquindio.bibliomtr.api.benchmark;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import co.edu.uniquindio.bibliomtr.TestcontainersConfiguration;
import co.edu.uniquindio.bibliomtr.algorithms.ranking.TermFrequency;
import co.edu.uniquindio.bibliomtr.api.benchmark.FakeBenchmarkLauncher.Mode;
import co.edu.uniquindio.bibliomtr.api.ranking.FakeCorpusTokenSource;
import co.edu.uniquindio.bibliomtr.persistence.benchmark.BenchmarkRunRepository;

/**
 * Tests de {@code POST /api/benchmarks} y {@code GET /api/benchmarks} con Postgres real y un launcher falso
 * (REQ-17 a REQ-22, REQ-27, REQ-28; AC-13, AC-14, AC-15, AC-20, AC-27).
 *
 * <p><b>Corpus.</b> {@code model} ×42, {@code data} ×30, {@code graph} ×30 y {@code tree} ×7: n = 4,
 * k = 42, d = 2 (dígitos de 42), b = 10 y m = ⌊(42 − 7) / 5⌋ + 1 = 8 con {@code bucketSize} = 5.
 *
 * <p><b>Espera de la medición.</b> El executor de mediciones tiene un solo hilo; enviarle una tarea vacía y
 * esperarla garantiza que la medición anterior ya terminó, sin sondeos ni pausas.
 */
@SpringBootTest(properties = "bibliomtr.corpus.dir=build/tmp/empty-corpus")
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, FakeCorpusTokenSource.Config.class, FakeBenchmarkLauncher.Config.class})
class BenchmarkServiceIT {

	private static final String[] SLUGS = {"counting", "radix", "bucket"};

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private FakeCorpusTokenSource corpus;

	@Autowired
	private FakeBenchmarkLauncher launcher;

	@Autowired
	private BenchmarkRunRepository repository;

	@Autowired
	@Qualifier("benchmarkExecutor")
	private ExecutorService executor;

	@BeforeEach
	void reset() throws Exception {
		launcher.reset();
		awaitIdle();
		repository.deleteAll();
		launcher.reset();
		List<String> tokens = new ArrayList<>();
		tokens.addAll(Collections.nCopies(42, "model"));
		tokens.addAll(Collections.nCopies(30, "data"));
		tokens.addAll(Collections.nCopies(30, "graph"));
		tokens.addAll(Collections.nCopies(7, "tree"));
		corpus.setAbstracts(List.of(tokens));
	}

	@Test
	@DisplayName("AC-13: POST da 202 EN_CURSO con los parámetros fijos y al terminar queda COMPLETADA con ms ± error, n, k / d y b / m y complejidad")
	void runGoesFromInProgressToCompleted() throws Exception {
		launcher.setMode(Mode.BLOCK);

		mockMvc.perform(post("/api/benchmarks"))
				.andExpect(status().isAccepted())
				.andExpect(jsonPath("$.id", notNullValue()))
				.andExpect(jsonPath("$.startedAt", notNullValue()))
				.andExpect(jsonPath("$.finishedAt", nullValue()))
				.andExpect(jsonPath("$.status").value("EN_CURSO"))
				.andExpect(jsonPath("$.termCount").value(4))
				.andExpect(jsonPath("$.results", empty()))
				// REQ-18: parámetros fijos de JMH guardados con la medición.
				.andExpect(jsonPath("$.parameters.forks").value(1))
				.andExpect(jsonPath("$.parameters.warmupIterations").value(5))
				.andExpect(jsonPath("$.parameters.warmupSeconds").value(1))
				.andExpect(jsonPath("$.parameters.measurementIterations").value(10))
				.andExpect(jsonPath("$.parameters.measurementSeconds").value(1))
				.andExpect(jsonPath("$.parameters.mode").value("avgt"))
				.andExpect(jsonPath("$.parameters.timeUnit").value("ms"))
				.andExpect(jsonPath("$.parameters.bucketSize").value(5))
				.andExpect(jsonPath("$.parameters.traceEnabled").value(false));
		// Mientras JMH corre, el historial la muestra EN_CURSO.
		mockMvc.perform(get("/api/benchmarks"))
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].status").value("EN_CURSO"));

		launcher.release();
		awaitIdle();

		mockMvc.perform(get("/api/benchmarks"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].status").value("COMPLETADA"))
				.andExpect(jsonPath("$[0].finishedAt", notNullValue()))
				.andExpect(jsonPath("$[0].message", nullValue()))
				.andExpect(jsonPath("$[0].termCount").value(4))
				.andExpect(jsonPath("$[0].results", hasSize(3)))
				.andExpect(jsonPath("$[0].results[0].algorithm").value("counting"))
				.andExpect(jsonPath("$[0].results[0].scoreMs").value(0.0271))
				.andExpect(jsonPath("$[0].results[0].scoreErrorMs").value(0.0012))
				.andExpect(jsonPath("$[0].results[0].parameters.k").value(42))
				.andExpect(jsonPath("$[0].results[0].parameters.d", nullValue()))
				.andExpect(jsonPath("$[0].results[0].complexity").value("O(n+k)"))
				.andExpect(jsonPath("$[0].results[1].algorithm").value("radix"))
				.andExpect(jsonPath("$[0].results[1].scoreMs").value(0.1004))
				.andExpect(jsonPath("$[0].results[1].scoreErrorMs").value(0.0047))
				.andExpect(jsonPath("$[0].results[1].parameters.d").value(2))
				.andExpect(jsonPath("$[0].results[1].parameters.b").value(10))
				.andExpect(jsonPath("$[0].results[1].parameters.k", nullValue()))
				.andExpect(jsonPath("$[0].results[1].complexity").value("O(d·(n+b))"))
				.andExpect(jsonPath("$[0].results[2].algorithm").value("bucket"))
				.andExpect(jsonPath("$[0].results[2].scoreMs").value(0.0752))
				.andExpect(jsonPath("$[0].results[2].scoreErrorMs").value(0.0031))
				.andExpect(jsonPath("$[0].results[2].parameters.m").value(8))
				.andExpect(jsonPath("$[0].results[2].complexity").value("O(n+m) promedio, O(n²) peor caso"));

		// JMH recibió la lista completa del corpus, en orden alfabético descendente, con el límite de 10 minutos.
		assertThat(launcher.invocations()).singleElement().satisfies(invocation -> {
			assertThat(invocation.input()).containsExactly(new TermFrequency("tree", 7),
					new TermFrequency("model", 42), new TermFrequency("graph", 30), new TermFrequency("data", 30));
			assertThat(invocation.timeout()).isEqualTo(Duration.ofMinutes(10));
		});
	}

	@Test
	@DisplayName("AC-14: con una medición EN_CURSO, otro POST da 409 con \"Ya hay una medición en curso\"")
	void secondRequestWhileInProgressIsRejected() throws Exception {
		launcher.setMode(Mode.BLOCK);
		mockMvc.perform(post("/api/benchmarks")).andExpect(status().isAccepted());

		mockMvc.perform(post("/api/benchmarks"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.detail").value("Ya hay una medición en curso"));

		launcher.release();
		awaitIdle();
		// La rechazada no se guardó ni lanzó JMH; al terminar la primera se puede lanzar otra.
		assertThat(launcher.invocations()).hasSize(1);
		mockMvc.perform(get("/api/benchmarks")).andExpect(jsonPath("$", hasSize(1)));
		mockMvc.perform(post("/api/benchmarks")).andExpect(status().isAccepted());
		awaitIdle();
	}

	@Test
	@DisplayName("AC-15: una medición que falla queda FALLIDA con mensaje en español y la COMPLETADA anterior se conserva")
	void failedRunKeepsPreviousCompleted() throws Exception {
		assertFailedAfterCompleted(Mode.FAIL, "La medición falló: " + FakeBenchmarkLauncher.FAILURE_CAUSE);
	}

	@Test
	@DisplayName("AC-15: una medición que supera 10 minutos queda FALLIDA con mensaje en español y la COMPLETADA anterior se conserva")
	void timedOutRunKeepsPreviousCompleted() throws Exception {
		assertFailedAfterCompleted(Mode.TIMEOUT, "La medición superó el límite de 10 minutos");
	}

	@Test
	@DisplayName("AC-20: tres mediciones aparecen de la más reciente a la más antigua con fecha, estado y resultados")
	void historyOfThreeIsNewestFirst() throws Exception {
		List<Long> ids = new ArrayList<>();
		ids.add(runToEnd(Mode.COMPLETE));
		ids.add(runToEnd(Mode.FAIL));
		ids.add(runToEnd(Mode.COMPLETE));

		ResultActions history = mockMvc.perform(get("/api/benchmarks"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(3)))
				.andExpect(jsonPath("$[0].id").value(ids.get(2)))
				.andExpect(jsonPath("$[0].status").value("COMPLETADA"))
				.andExpect(jsonPath("$[0].results", hasSize(3)))
				.andExpect(jsonPath("$[1].id").value(ids.get(1)))
				.andExpect(jsonPath("$[1].status").value("FALLIDA"))
				.andExpect(jsonPath("$[1].results", empty()))
				.andExpect(jsonPath("$[2].id").value(ids.get(0)))
				.andExpect(jsonPath("$[2].status").value("COMPLETADA"))
				.andExpect(jsonPath("$[2].results", hasSize(3)));
		assertNewestFirst(history, 3);
	}

	@Test
	@DisplayName("AC-27: treinta mediciones aparecen todas en una sola lista, de la más reciente a la más antigua")
	void historyOfThirtyIsCompleteAndNewestFirst() throws Exception {
		List<Long> ids = new ArrayList<>();
		for (int i = 0; i < 30; i++) {
			ids.add(runToEnd(Mode.COMPLETE));
		}

		ResultActions history = mockMvc.perform(get("/api/benchmarks"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(30)));
		List<Number> returnedIds = JsonPath.read(history.andReturn().getResponse().getContentAsString(), "$[*].id");
		List<Long> expected = new ArrayList<>(ids);
		Collections.reverse(expected);
		assertThat(returnedIds).extracting(Number::longValue).containsExactlyElementsOf(expected);
		assertNewestFirst(history, 30);
	}

	/** Una COMPLETADA y luego una medición en {@code failingMode}: la nueva es FALLIDA y la anterior sigue. */
	private void assertFailedAfterCompleted(Mode failingMode, String expectedMessage) throws Exception {
		long completedId = runToEnd(Mode.COMPLETE);
		long failedId = runToEnd(failingMode);

		ResultActions history = mockMvc.perform(get("/api/benchmarks"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(2)))
				.andExpect(jsonPath("$[0].id").value(failedId))
				.andExpect(jsonPath("$[0].status").value("FALLIDA"))
				.andExpect(jsonPath("$[0].message").value(expectedMessage))
				.andExpect(jsonPath("$[0].finishedAt", notNullValue()))
				.andExpect(jsonPath("$[0].results", empty()))
				.andExpect(jsonPath("$[1].id").value(completedId))
				.andExpect(jsonPath("$[1].status").value("COMPLETADA"))
				.andExpect(jsonPath("$[1].message", nullValue()))
				.andExpect(jsonPath("$[1].results", hasSize(3)));
		for (int a = 0; a < SLUGS.length; a++) {
			history.andExpect(jsonPath("$[1].results[" + a + "].algorithm").value(SLUGS[a]));
		}
	}

	/** Lanza una medición en el modo dado, espera a que termine y devuelve su id. */
	private long runToEnd(Mode mode) throws Exception {
		launcher.setMode(mode);
		String body = mockMvc.perform(post("/api/benchmarks"))
				.andExpect(status().isAccepted())
				.andReturn().getResponse().getContentAsString();
		awaitIdle();
		return ((Number) JsonPath.read(body, "$.id")).longValue();
	}

	/** Comprueba que {@code startedAt} no aumenta a lo largo de la lista. */
	private static void assertNewestFirst(ResultActions history, int size) throws Exception {
		List<String> startedAt = JsonPath.read(history.andReturn().getResponse().getContentAsString(),
				"$[*].startedAt");
		assertThat(startedAt).hasSize(size);
		for (int i = 1; i < startedAt.size(); i++) {
			assertThat(Instant.parse(startedAt.get(i - 1))).isAfterOrEqualTo(Instant.parse(startedAt.get(i)));
		}
	}

	/** Espera a que el executor de un hilo termine la medición en curso. */
	private void awaitIdle() throws Exception {
		executor.submit(() -> {
		}).get(30, TimeUnit.SECONDS);
	}

}
