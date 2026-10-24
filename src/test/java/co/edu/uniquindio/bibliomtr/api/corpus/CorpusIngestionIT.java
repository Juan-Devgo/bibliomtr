package co.edu.uniquindio.bibliomtr.api.corpus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.TypeExcludeFilter;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.postgresql.PostgreSQLContainer;

import co.edu.uniquindio.bibliomtr.BibliomtrApplication;
import co.edu.uniquindio.bibliomtr.TestcontainersConfiguration;
import co.edu.uniquindio.bibliomtr.api.corpus.FakeEmbedders.FakeNomic;
import co.edu.uniquindio.bibliomtr.api.corpus.FakeEmbedders.FakeSpecter2;
import co.edu.uniquindio.bibliomtr.api.corpus.FakeEmbedders.Specter2Call;
import co.edu.uniquindio.bibliomtr.api.ranking.FakeCorpusTokenSource;
import co.edu.uniquindio.bibliomtr.pdf.SyntheticPdf;
import co.edu.uniquindio.bibliomtr.persistence.article.Article;
import co.edu.uniquindio.bibliomtr.persistence.article.ArticleRepository;

/**
 * Tests de la ingesta del corpus al arrancar (spec 001, REQ-8, REQ-9, REQ-9a, REQ-10; AC-8, AC-8a, AC-9, AC-10,
 * AC-11).
 *
 * <p><b>Montaje.</b> Como en {@code BenchmarkRestartIT}: un {@link PostgreSQLContainer} estático vive durante
 * toda la clase y cada arranque es un {@link SpringApplicationBuilder} nuevo que apunta a él, con
 * {@code --bibliomtr.ingestion.enabled=true} y un directorio temporal de PDF sintéticos ({@link SyntheticPdf}).
 * Los embeddings los dan los fakes de {@link FakeEmbedders}, sin modelo ONNX ni Ollama. Se excluyen del escaneo
 * las {@code @TestConfiguration} del classpath de test y las necesarias se pasan al builder.
 *
 * <p>La ingesta corre en {@code ApplicationReadyEvent} en el hilo de arranque, así que al volver de
 * {@code run} el corpus ya está en la base. Cada test vacía la tabla {@code article} antes de empezar, porque la
 * base es compartida.
 */
@ExtendWith(OutputCaptureExtension.class)
class CorpusIngestionIT {

	private static final PostgreSQLContainer POSTGRES =
			new PostgreSQLContainer(TestcontainersConfiguration.PGVECTOR_IMAGE);

	private static final String LINE_1 = "Generative artificial intelligence is reshaping assessment in higher";
	private static final String LINE_2 = "education. This review maps the evidence published between 2019 and";
	private static final String LINE_3 = "2024 and proposes principles for the redesign of assessment tasks.";
	private static final String ABSTRACT_1 = LINE_1 + " " + LINE_2 + " " + LINE_3;

	private static final String LINE_4 = "Learning analytics dashboards help instructors identify students at";
	private static final String LINE_5 = "risk of failing. We evaluate three dashboards in two universities and";
	private static final String LINE_6 = "report their effect on retention and on the workload of instructors.";
	private static final String ABSTRACT_2 = LINE_4 + " " + LINE_5 + " " + LINE_6;

	private static final String TITLE_1 = "Generative AI in Higher Education";
	private static final String TITLE_2 = "Learning Analytics for Student Success";
	private static final String FILE_1 = "1. Generative AI.pdf";
	private static final String FILE_2 = "2. Learning analytics.pdf";
	private static final String FILE_WITHOUT_ABSTRACT = "3. Sin abstract.pdf";

	@TempDir
	Path corpus;

	@BeforeAll
	static void startDatabase() {
		POSTGRES.start();
	}

	@AfterAll
	static void stopDatabase() {
		POSTGRES.stop();
	}

	/** Vacía la tabla si ya existe (Flyway la crea en el primer arranque de la clase). */
	@BeforeEach
	void emptyArticles() throws SQLException {
		try (Connection connection = DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(),
				POSTGRES.getPassword()); Statement statement = connection.createStatement()) {
			statement.execute("DO $$ BEGIN IF to_regclass('article') IS NOT NULL THEN TRUNCATE article; END IF; END $$");
		}
	}

	@Test
	@DisplayName("AC-8: 2 PDF con abstract quedan guardados con título, autores, abstract y los dos vectores")
	void ingestsArticlesWithBothEmbeddings() throws Exception {
		writeArticle1();
		writeArticle2();

		try (ConfigurableApplicationContext application = startApplication(corpus)) {
			List<Article> articles = articlesByFileName(application);
			assertThat(articles).hasSize(2);

			Article first = articles.get(0);
			assertThat(first.getFileName()).isEqualTo(FILE_1);
			assertThat(first.getFileSha256()).hasSize(64);
			assertThat(first.getTitle()).isEqualTo(TITLE_1);
			assertThat(first.getAuthors()).containsExactly("Ana Gómez", "Luis Pérez");
			assertThat(first.getAbstractText()).isEqualTo(ABSTRACT_1);
			assertThat(first.isAbstractDetected()).isTrue();
			assertThat(first.getSpecter2Embedding()).hasSize(768).containsExactly(FakeEmbedders.SPECTER2_VECTOR);
			assertThat(first.getNomicEmbedding()).hasSize(768).containsExactly(FakeEmbedders.NOMIC_VECTOR);
			assertThat(first.isSpecter2Truncated()).isFalse();
			assertThat(first.getSpecter2TokenCount()).isEqualTo(FakeEmbedders.NORMAL_TOKEN_COUNT);
			assertThat(first.getIngestedAt()).isNotNull();

			Article second = articles.get(1);
			assertThat(second.getTitle()).isEqualTo(TITLE_2);
			assertThat(second.getAuthors()).containsExactly("Eva Ruiz");
			assertThat(second.getAbstractText()).isEqualTo(ABSTRACT_2);

			// SPECTER2 recibe título y abstract; nomic-embed-text, solo el abstract; el modelo se pide una vez.
			assertThat(application.getBean(FakeSpecter2.class).calls())
					.containsExactlyInAnyOrder(new Specter2Call(TITLE_1, ABSTRACT_1), new Specter2Call(TITLE_2, ABSTRACT_2));
			FakeNomic nomic = application.getBean(FakeNomic.class);
			assertThat(nomic.inputs()).containsExactlyInAnyOrder(ABSTRACT_1, ABSTRACT_2);
			assertThat(nomic.ensureModelCalls()).isEqualTo(1);

			mockMvc(application).perform(get("/api/articles"))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$", hasSize(2)))
					.andExpect(jsonPath("$[0].id").value(first.getId()))
					.andExpect(jsonPath("$[0].title").value(TITLE_1))
					.andExpect(jsonPath("$[0].authors[1]").value("Luis Pérez"))
					.andExpect(jsonPath("$[0].fileName").value(FILE_1))
					.andExpect(jsonPath("$[0].specter2Truncated").value(false))
					.andExpect(jsonPath("$[1].fileName").value(FILE_2));
		}
	}

	@Test
	@DisplayName("AC-9: un PDF sin abstract queda marcado y no aparece en GET /api/articles")
	void articleWithoutAbstractIsNotSelectable(CapturedOutput output) throws Exception {
		writeArticle1();
		SyntheticPdf.builder()
				.metadataTitle("A Document Without Heading")
				.lines("This document has a body of text but no section that is called the summary of the paper.",
						"It should be stored as not selectable and should not be offered for comparison.")
				.writeTo(corpus, FILE_WITHOUT_ABSTRACT);

		try (ConfigurableApplicationContext application = startApplication(corpus)) {
			List<Article> articles = articlesByFileName(application);
			assertThat(articles).hasSize(2);
			Article withoutAbstract = articles.get(1);
			assertThat(withoutAbstract.getFileName()).isEqualTo(FILE_WITHOUT_ABSTRACT);
			assertThat(withoutAbstract.isAbstractDetected()).isFalse();
			assertThat(withoutAbstract.getAbstractText()).isNull();
			assertThat(withoutAbstract.getSpecter2Embedding()).isNull();
			assertThat(withoutAbstract.getNomicEmbedding()).isNull();
			assertThat(output).contains("Sin abstract detectado: " + FILE_WITHOUT_ABSTRACT);

			mockMvc(application).perform(get("/api/articles"))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$", hasSize(1)))
					.andExpect(jsonPath("$[0].fileName").value(FILE_1));
		}
	}

	@Test
	@DisplayName("AC-10: con el corpus vacío la aplicación arranca y no hay seleccionables")
	void emptyCorpusStartsWithNoArticles(CapturedOutput output) throws Exception {
		try (ConfigurableApplicationContext application = startApplication(corpus)) {
			mockMvc(application).perform(get("/api/articles"))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$", hasSize(0)));
			assertThat(output).contains("Corpus vacío o inexistente: " + corpus.toAbsolutePath());
		}
	}

	@Test
	@DisplayName("AC-10: con el corpus inexistente la aplicación arranca y no hay seleccionables")
	void missingCorpusStartsWithNoArticles(CapturedOutput output) throws Exception {
		Path missing = corpus.resolve("no-existe");
		assertThat(Files.exists(missing)).isFalse();

		try (ConfigurableApplicationContext application = startApplication(missing)) {
			mockMvc(application).perform(get("/api/articles"))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$", hasSize(0)));
			assertThat(output).contains("Corpus vacío o inexistente: " + missing.toAbsolutePath());
		}
	}

	@Test
	@DisplayName("AC-11: dos arranques sobre la misma base no duplican artículos")
	void restartDoesNotDuplicate(CapturedOutput output) throws Exception {
		writeArticle1();
		writeArticle2();

		List<Long> firstIds;
		try (ConfigurableApplicationContext first = startApplication(corpus)) {
			firstIds = ids(articlesByFileName(first));
			assertThat(firstIds).hasSize(2);
		}

		try (ConfigurableApplicationContext second = startApplication(corpus)) {
			assertThat(ids(articlesByFileName(second))).isEqualTo(firstIds);
			assertThat(second.getBean(FakeSpecter2.class).calls()).isEmpty();
			assertThat(second.getBean(FakeNomic.class).ensureModelCalls()).isZero();
			assertThat(output).contains("ingested=0, alreadyPresent=2, withoutAbstract=0, failed=0");
		}
	}

	@Test
	@DisplayName("AC-11: si nomic-embed-text falla, el artículo no se guarda, queda un ERROR y se reintenta al reiniciar")
	void embeddingFailureIsRetriedOnRestart(CapturedOutput output) throws Exception {
		writeArticle1();

		try (ConfigurableApplicationContext first = startApplication(corpus,
				"--" + FakeEmbedders.NOMIC_FAILS_PROPERTY + "=true")) {
			assertThat(articlesByFileName(first)).isEmpty();
			assertThat(output).contains("No se pudo ingerir " + FILE_1 + ": " + FakeEmbedders.NOMIC_FAILURE)
					.contains("ERROR");
			mockMvc(first).perform(get("/api/articles")).andExpect(jsonPath("$", hasSize(0)));
		}

		try (ConfigurableApplicationContext second = startApplication(corpus)) {
			List<Article> articles = articlesByFileName(second);
			assertThat(articles).hasSize(1);
			assertThat(articles.getFirst().getFileName()).isEqualTo(FILE_1);
			assertThat(articles.getFirst().getNomicEmbedding()).containsExactly(FakeEmbedders.NOMIC_VECTOR);
		}
	}

	@Test
	@DisplayName("AC-8, AC-11: si el pull de Ollama falla, ese PDF queda con ERROR, el siguiente lo reintenta y el"
			+ " fallido se guarda al reiniciar")
	void pullFailureIsRetriedByNextPdfAndOnRestart(CapturedOutput output) throws Exception {
		writeArticle1();
		writeArticle2();

		String failedFile;
		try (ConfigurableApplicationContext first = startApplication(corpus,
				"--" + FakeEmbedders.NOMIC_PULL_FAILURES_PROPERTY + "=1")) {
			// El primer PDF con abstract falla en ensureModel(); el segundo vuelve a llamarlo y se guarda.
			assertThat(first.getBean(FakeNomic.class).ensureModelCalls()).isEqualTo(2);
			List<Article> articles = articlesByFileName(first);
			assertThat(articles).hasSize(1);

			// El orden de los PDF depende del sistema de archivos: el fallido es el que no quedó en la base.
			String savedFile = articles.getFirst().getFileName();
			assertThat(savedFile).isIn(FILE_1, FILE_2);
			failedFile = savedFile.equals(FILE_1) ? FILE_2 : FILE_1;

			String log = output.getAll();
			assertThat(occurrences(log, "No se pudo ingerir ")).isEqualTo(1);
			assertThat(log).contains("No se pudo ingerir " + failedFile + ": " + FakeEmbedders.NOMIC_PULL_FAILURE)
					.contains("ERROR")
					.contains("ingested=1, alreadyPresent=0, withoutAbstract=0, failed=1");
		}

		try (ConfigurableApplicationContext second = startApplication(corpus)) {
			List<Article> articles = articlesByFileName(second);
			assertThat(articles).hasSize(2);
			assertThat(articles.get(0).getFileName()).isEqualTo(FILE_1);
			assertThat(articles.get(1).getFileName()).isEqualTo(FILE_2);
			assertThat(second.getBean(FakeNomic.class).ensureModelCalls()).isEqualTo(1);
			assertThat(second.getBean(FakeSpecter2.class).calls()).hasSize(1);
			assertThat(second.getBean(FakeNomic.class).inputs())
					.containsExactly(failedFile.equals(FILE_1) ? ABSTRACT_1 : ABSTRACT_2);
			assertThat(output.getAll()).contains("ingested=1, alreadyPresent=1, withoutAbstract=0, failed=0");
		}
	}

	@Test
	@DisplayName("AC-8a: un texto de SPECTER2 truncado queda marcado en la base y con un WARN en el log")
	void truncatedTextIsMarked(CapturedOutput output) throws Exception {
		writeArticle1();

		try (ConfigurableApplicationContext application = startApplication(corpus,
				"--" + FakeEmbedders.SPECTER2_TRUNCATED_PROPERTY + "=true")) {
			Article article = articlesByFileName(application).getFirst();
			assertThat(article.isSpecter2Truncated()).isTrue();
			assertThat(article.getSpecter2TokenCount()).isEqualTo(FakeEmbedders.TRUNCATED_TOKEN_COUNT);
			assertThat(output).contains(FILE_1 + ": texto de SPECTER2 truncado a 512 tokens ("
					+ FakeEmbedders.TRUNCATED_TOKEN_COUNT + " tokens)").contains("WARN");

			mockMvc(application).perform(get("/api/articles"))
					.andExpect(jsonPath("$[0].specter2Truncated").value(true));
		}
	}

	/** Escribe el artículo 1, con dos autores separados por {@code and}. */
	private void writeArticle1() throws IOException {
		SyntheticPdf.builder()
				.metadataTitle(TITLE_1)
				.metadataAuthor("Ana Gómez and Luis Pérez")
				.visibleTitle(TITLE_1)
				.abstractSection(LINE_1, LINE_2, LINE_3)
				.writeTo(corpus, FILE_1);
	}

	/** Escribe el artículo 2, con un autor. */
	private void writeArticle2() throws IOException {
		SyntheticPdf.builder()
				.metadataTitle(TITLE_2)
				.metadataAuthor("Eva Ruiz")
				.visibleTitle(TITLE_2)
				.abstractSection(LINE_4, LINE_5, LINE_6)
				.writeTo(corpus, FILE_2);
	}

	/**
	 * Arranca la aplicación completa sobre el contenedor compartido, con la ingesta activada sobre
	 * {@code corpusDir}, los embedders falsos y el corpus de tokens falso de la 002.
	 */
	private static ConfigurableApplicationContext startApplication(Path corpusDir, String... extraArguments) {
		List<String> arguments = new ArrayList<>(List.of(
				"--spring.datasource.url=" + POSTGRES.getJdbcUrl(),
				"--spring.datasource.username=" + POSTGRES.getUsername(),
				"--spring.datasource.password=" + POSTGRES.getPassword(),
				"--server.port=0",
				"--bibliomtr.ingestion.enabled=true",
				"--bibliomtr.corpus.dir=" + corpusDir,
				"--spring.main.banner-mode=off"));
		arguments.addAll(List.of(extraArguments));
		return new SpringApplicationBuilder(BibliomtrApplication.class, FakeEmbedders.Config.class,
				FakeCorpusTokenSource.Config.class)
				.initializers(context -> context.getBeanFactory()
						.registerSingleton("excludeTestConfigurations", new ExcludeTestConfigurations()))
				.run(arguments.toArray(String[]::new));
	}

	/** Artículos guardados, seleccionables o no, en orden de nombre de archivo (todos empiezan por un dígito). */
	private static List<Article> articlesByFileName(ConfigurableApplicationContext application) {
		List<Article> all = application.getBean(ArticleRepository.class).findAll();
		// Selección del menor nombre en cada paso: n ≤ 3 en estos tests.
		List<Article> ordered = new ArrayList<>();
		List<Article> remaining = new ArrayList<>(all);
		while (!remaining.isEmpty()) {
			Article smallest = remaining.getFirst();
			for (Article candidate : remaining) {
				if (candidate.getFileName().compareTo(smallest.getFileName()) < 0) {
					smallest = candidate;
				}
			}
			ordered.add(smallest);
			remaining.remove(smallest);
		}
		return ordered;
	}

	/** Veces que {@code fragment} aparece en {@code text}, sin solaparse. Complejidad: O(|text| · |fragment|). */
	private static int occurrences(String text, String fragment) {
		int count = 0;
		int from = text.indexOf(fragment);
		while (from >= 0) {
			count++;
			from = text.indexOf(fragment, from + fragment.length());
		}
		return count;
	}

	/** Ids de los artículos, en el mismo orden. */
	private static List<Long> ids(List<Article> articles) {
		List<Long> ids = new ArrayList<>();
		for (Article article : articles) {
			ids.add(article.getId());
		}
		return ids;
	}

	/** MockMvc sobre el contexto web de un arranque. */
	private static MockMvc mockMvc(ConfigurableApplicationContext application) {
		return MockMvcBuilders.webAppContextSetup((WebApplicationContext) application).build();
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
