package co.edu.uniquindio.bibliomtr.api.similarity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import co.edu.uniquindio.bibliomtr.TestcontainersConfiguration;
import co.edu.uniquindio.bibliomtr.api.ranking.FakeCorpusTokenSource;
import co.edu.uniquindio.bibliomtr.api.similarity.FakeSimilarityCatalog.Mode;
import co.edu.uniquindio.bibliomtr.api.similarity.FakeSimilarityCatalog.Strategy;
import co.edu.uniquindio.bibliomtr.persistence.article.Article;
import co.edu.uniquindio.bibliomtr.persistence.article.ArticleRepository;
import co.edu.uniquindio.bibliomtr.preprocessing.TextPreprocessor;

/**
 * Tests de {@code POST /api/similarity/comparisons} y {@code GET /api/similarity/trace} con Postgres real y
 * algoritmos falsos (spec 001, REQ-13, REQ-14, REQ-15, REQ-16, REQ-20, REQ-22; AC-13, AC-14, AC-15, AC-17,
 * AC-19).
 *
 * <p><b>Datos.</b> Cada test vacía {@code article} e inserta por el repositorio 4 artículos seleccionables
 * ({@code a.pdf} … {@code d.pdf}, con abstracts y vectores distintos) y uno sin abstract. Los algoritmos los da
 * {@link FakeSimilarityCatalog}, que por defecto los tiene todos implementados con valores fijos: así los casos
 * CALCULADO no dependen de los stubs de los estudiantes.
 *
 * <p><b>Servidor.</b> Casi todo se prueba con {@link MockMvc}. El contexto levanta además el servidor en un puerto
 * aleatorio porque MockMvc no traduce una excepción sin manejar a HTTP 500; ese caso se pide por HTTP real.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, properties = "bibliomtr.corpus.dir=build/tmp/empty-corpus")
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, FakeCorpusTokenSource.Config.class, FakeSimilarityCatalog.Config.class})
class ComparisonApiIT {

	private static final String[] SLUGS = {"levenshtein", "needleman-wunsch", "tfidf-cosine", "jaccard",
			"specter2-cosine", "specter2-euclidean", "nomic-cosine", "nomic-euclidean"};

	private static final String[] STUB_CLASSES = {"Levenshtein", "NeedlemanWunsch", "TfIdfCosineSimilarity",
			"Jaccard", "CosineSimilarity", "EuclideanDistance", "CosineSimilarity", "EuclideanDistance"};

	private static final String STUB_DIRECTORY = "src/main/java/co/edu/uniquindio/bibliomtr/algorithms/similarity/";

	private static final String[] ABSTRACTS = {
			"Generative models reshape assessment in higher education and the design of tasks.",
			"Learning analytics dashboards help instructors identify students at risk.",
			"Graph neural networks classify citation networks of scientific articles.",
			"Text embeddings compare abstracts of articles with cosine similarity."};

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ArticleRepository repository;

	@Autowired
	private FakeSimilarityCatalog catalog;

	@LocalServerPort
	private int port;

	/** Ids de los 4 seleccionables, en el orden a, b, c, d. */
	private final List<Long> ids = new ArrayList<>();

	/** Id del artículo sin abstract. */
	private long withoutAbstractId;

	@BeforeEach
	void insertArticles() {
		catalog.reset();
		repository.deleteAll();
		ids.clear();
		for (int i = 0; i < ABSTRACTS.length; i++) {
			String name = (char) ('a' + i) + ".pdf";
			Article article = Article.withAbstract(name, sha(i), "Artículo " + name, List.of("Ana Gómez"),
					ABSTRACTS[i], unitVector(i), false, 20, unitVector(i + 10), Instant.now());
			ids.add(repository.save(article).getId());
		}
		withoutAbstractId = repository.save(Article.withoutAbstract("e.pdf", sha(9), "Sin abstract", List.of(),
				Instant.now())).getId();
	}

	@Test
	@DisplayName("AC-13, AC-19: 3 artículos dan 8 matrices 3×3 simétricas en [0,1] con tiempos en ms")
	void threeArticlesGiveEightSymmetricMatrices() throws Exception {
		List<Long> selection = List.of(ids.get(2), ids.get(0), ids.get(1));

		ResultActions result = compare(selection)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.articles", hasSize(3)))
				.andExpect(jsonPath("$.articles[0].id").value(ids.get(2)))
				.andExpect(jsonPath("$.articles[1].id").value(ids.get(0)))
				.andExpect(jsonPath("$.articles[2].fileName").value("b.pdf"))
				.andExpect(jsonPath("$.editGranularity").value("WORDS"))
				.andExpect(jsonPath("$.preprocessingMs").isNumber())
				.andExpect(jsonPath("$.preprocessingMs", greaterThanOrEqualTo(0.0)))
				.andExpect(jsonPath("$.results", hasSize(8)));
		for (int r = 0; r < SLUGS.length; r++) {
			String path = "$.results[" + r + "]";
			result.andExpect(jsonPath(path + ".algorithm").value(SLUGS[r]))
					.andExpect(jsonPath(path + ".label").isString())
					.andExpect(jsonPath(path + ".status").value("CALCULADO"))
					.andExpect(jsonPath(path + ".matrix", hasSize(3)))
					.andExpect(jsonPath(path + ".elapsedMs").isNumber())
					.andExpect(jsonPath(path + ".elapsedMs", greaterThanOrEqualTo(0.0)))
					.andExpect(jsonPath(path + ".pendingLocation").value(nullValue()));
		}

		String body = result.andReturn().getResponse().getContentAsString();
		for (int r = 0; r < SLUGS.length; r++) {
			List<List<Number>> matrix = JsonPath.read(body, "$.results[" + r + "].matrix");
			assertThat(matrix).as(SLUGS[r]).hasSize(3);
			for (int i = 0; i < 3; i++) {
				assertThat(matrix.get(i)).as(SLUGS[r] + " fila " + i).hasSize(3);
				for (int j = 0; j < 3; j++) {
					double value = matrix.get(i).get(j).doubleValue();
					assertThat(value).as(SLUGS[r] + " [" + i + "][" + j + "]").isBetween(0.0, 1.0)
							.isEqualTo(matrix.get(j).get(i).doubleValue());
				}
			}
		}
		// El fake da 1 entre entradas iguales y su valor fijo entre distintas: la diagonal se calcula.
		List<List<Number>> levenshtein = JsonPath.read(body, "$.results[0].matrix");
		assertThat(levenshtein.get(0).get(0).doubleValue()).isEqualTo(1.0);
		assertThat(levenshtein.get(0).get(1).doubleValue()).isEqualTo(Strategy.LEVENSHTEIN.similarity());
	}

	@Test
	@DisplayName("El IDF usa todos los seleccionables y la granularidad WORDS pasa los tokens a Levenshtein")
	void tfIdfReceivesAllSelectableAndEditSequencesAreTokens() throws Exception {
		compare(List.of(ids.get(0), ids.get(1))).andExpect(status().isOk());

		assertThat(catalog.lastCorpusSize()).isEqualTo(ABSTRACTS.length);
		// El último par recorrido es (b, b): su secuencia de edición son los tokens de su abstract.
		assertThat(catalog.lastEditSequence()).isEqualTo(new TextPreprocessor().tokenize(ABSTRACTS[1]));
	}

	@Test
	@DisplayName("AC-14: 1 artículo da 400 con mensaje en español")
	void oneArticleIsRejected() throws Exception {
		compare(List.of(ids.get(0)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value("Debe seleccionar entre 2 y 20 artículos; se recibieron 1"));
	}

	@Test
	@DisplayName("AC-14: 21 artículos dan 400 con mensaje en español")
	void twentyOneArticlesAreRejected() throws Exception {
		List<Long> many = new ArrayList<>();
		for (long id = 1; id <= 21; id++) {
			many.add(id);
		}

		compare(many)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value("Debe seleccionar entre 2 y 20 artículos; se recibieron 21"));
	}

	@Test
	@DisplayName("AC-14: ids repetidos dan 400 con los ids repetidos")
	void repeatedIdsAreRejected() throws Exception {
		compare(List.of(ids.get(0), ids.get(1), ids.get(0)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail")
						.value("Los identificadores de artículo no deben repetirse: " + ids.get(0)));
	}

	@Test
	@DisplayName("AC-14: un id inexistente o sin abstract da 400 con esos ids")
	void missingOrUnselectableIdsAreRejected() throws Exception {
		long missing = ids.get(3) + 1000;

		compare(List.of(ids.get(0), missing, withoutAbstractId))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value(
						"Artículos inexistentes o no seleccionables: " + missing + ", " + withoutAbstractId));
	}

	@ParameterizedTest(name = "cuerpo \"{0}\"")
	@ValueSource(strings = {"", "{\"articleIds\": [\"x\", 2]}", "no es json"})
	@DisplayName("AC-14: sin cuerpo o con un cuerpo que no es una lista de enteros da 400")
	void unreadableBodyIsRejected(String body) throws Exception {
		mockMvc.perform(post("/api/similarity/comparisons").contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value("El cuerpo debe ser un JSON con articleIds: lista de enteros"));
	}

	@Test
	@DisplayName("AC-14: un cuerpo sin articleIds cuenta como 0 artículos")
	void bodyWithoutIdsCountsAsZero() throws Exception {
		mockMvc.perform(post("/api/similarity/comparisons").contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value("Debe seleccionar entre 2 y 20 artículos; se recibieron 0"));
	}

	@Test
	@DisplayName("AC-15: la traza de un par con un algoritmo implementado trae valor, similitud y traza")
	void pairTraceOfImplementedAlgorithm() throws Exception {
		mockMvc.perform(get("/api/similarity/trace")
						.param("algorithm", "levenshtein")
						.param("a", ids.get(0).toString())
						.param("b", ids.get(1).toString()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.algorithm").value("levenshtein"))
				.andExpect(jsonPath("$.label").value("Levenshtein"))
				.andExpect(jsonPath("$.articleA.id").value(ids.get(0)))
				.andExpect(jsonPath("$.articleB.id").value(ids.get(1)))
				.andExpect(jsonPath("$.editGranularity").value("WORDS"))
				.andExpect(jsonPath("$.status").value("CALCULADO"))
				.andExpect(jsonPath("$.value").value(Strategy.LEVENSHTEIN.similarity()))
				.andExpect(jsonPath("$.similarity").value(Strategy.LEVENSHTEIN.similarity()))
				.andExpect(jsonPath("$.trace", notNullValue()))
				.andExpect(jsonPath("$.trace.strategy").value("LEVENSHTEIN"))
				.andExpect(jsonPath("$.trace.sizeA").isNumber())
				.andExpect(jsonPath("$.elapsedMs", greaterThanOrEqualTo(0.0)))
				.andExpect(jsonPath("$.pendingLocation").value(nullValue()));
	}

	@Test
	@DisplayName("AC-15: la traza de un artículo consigo mismo está permitida")
	void pairTraceWithSameArticle() throws Exception {
		mockMvc.perform(get("/api/similarity/trace")
						.param("algorithm", "nomic-cosine")
						.param("a", ids.get(2).toString())
						.param("b", ids.get(2).toString()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("CALCULADO"))
				.andExpect(jsonPath("$.similarity").value(1.0))
				.andExpect(jsonPath("$.trace.sizeA").value(768));
	}

	@Test
	@DisplayName("REQ-20: la traza de un algoritmo pendiente no trae valores e indica el stub")
	void pairTraceOfPendingAlgorithm() throws Exception {
		catalog.setMode(Strategy.EUCLIDEAN, Mode.PENDING);

		mockMvc.perform(get("/api/similarity/trace")
						.param("algorithm", "specter2-euclidean")
						.param("a", ids.get(0).toString())
						.param("b", ids.get(1).toString()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("PENDIENTE"))
				.andExpect(jsonPath("$.value").value(nullValue()))
				.andExpect(jsonPath("$.similarity").value(nullValue()))
				.andExpect(jsonPath("$.trace").value(nullValue()))
				.andExpect(jsonPath("$.elapsedMs").value(nullValue()))
				.andExpect(jsonPath("$.pendingLocation").value(STUB_DIRECTORY + "EuclideanDistance.java#compare"));
	}

	@Test
	@DisplayName("AC-14: la traza sin parámetro, con id inexistente o algoritmo desconocido da 400 en español")
	void invalidTraceRequestsAreRejected() throws Exception {
		mockMvc.perform(get("/api/similarity/trace").param("algorithm", "jaccard").param("a", ids.get(0).toString()))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value("Falta el parámetro b"));
		mockMvc.perform(get("/api/similarity/trace")
						.param("algorithm", "jaccard")
						.param("a", ids.get(0).toString())
						.param("b", String.valueOf(withoutAbstractId)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value("Artículos inexistentes o no seleccionables: " + withoutAbstractId));
		mockMvc.perform(get("/api/similarity/trace")
						.param("algorithm", "quick")
						.param("a", ids.get(0).toString())
						.param("b", ids.get(1).toString()))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value("Algoritmo desconocido: quick. Valores válidos: "
						+ String.join(", ", SLUGS)));
		mockMvc.perform(get("/api/similarity/trace")
						.param("algorithm", "jaccard")
						.param("a", "abc")
						.param("b", ids.get(1).toString()))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value("El parámetro a debe ser un entero mayor o igual a 1"));
	}

	@Test
	@DisplayName("AC-17: con solo Jaccard implementado, Jaccard trae su matriz y los otros 7 resultados quedan PENDIENTE")
	void onlyJaccardImplemented() throws Exception {
		catalog.onlyImplemented(Strategy.JACCARD);

		ResultActions result = compare(List.of(ids.get(0), ids.get(1), ids.get(3)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.preprocessingMs", greaterThanOrEqualTo(0.0)))
				.andExpect(jsonPath("$.results", hasSize(8)));
		for (int r = 0; r < SLUGS.length; r++) {
			String path = "$.results[" + r + "]";
			result.andExpect(jsonPath(path + ".algorithm").value(SLUGS[r]));
			if (SLUGS[r].equals("jaccard")) {
				result.andExpect(jsonPath(path + ".status").value("CALCULADO"))
						.andExpect(jsonPath(path + ".matrix", hasSize(3)))
						.andExpect(jsonPath(path + ".matrix[0][1]").value(Strategy.JACCARD.similarity()))
						.andExpect(jsonPath(path + ".elapsedMs", greaterThanOrEqualTo(0.0)))
						.andExpect(jsonPath(path + ".pendingLocation").value(nullValue()));
			} else {
				result.andExpect(jsonPath(path + ".status").value("PENDIENTE"))
						.andExpect(jsonPath(path + ".matrix").value(nullValue()))
						.andExpect(jsonPath(path + ".elapsedMs").value(nullValue()))
						.andExpect(jsonPath(path + ".pendingLocation")
								.value(STUB_DIRECTORY + STUB_CLASSES[r] + ".java#compare"));
			}
		}
	}

	@Test
	@DisplayName("Una similitud de 1 + 1e-12 queda dentro de la tolerancia: se recorta a 1.0 y el resultado es CALCULADO")
	void roundingAboveOneIsClamped() throws Exception {
		catalog.setMode(Strategy.COSINE, Mode.ROUNDING_ABOVE_ONE);

		compare(List.of(ids.get(0), ids.get(1)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.results[4].algorithm").value("specter2-cosine"))
				.andExpect(jsonPath("$.results[4].status").value("CALCULADO"))
				.andExpect(jsonPath("$.results[4].matrix[0][0]").value(1.0))
				.andExpect(jsonPath("$.results[4].matrix[0][1]").value(1.0))
				.andExpect(jsonPath("$.results[6].matrix[1][1]").value(1.0));
		mockMvc.perform(get("/api/similarity/trace")
						.param("algorithm", "nomic-cosine")
						.param("a", ids.get(0).toString())
						.param("b", ids.get(1).toString()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("CALCULADO"))
				.andExpect(jsonPath("$.similarity").value(1.0));
	}

	@Test
	@DisplayName("Una similitud de 1.5 está fuera de la tolerancia: la comparación y la traza responden 500")
	void outOfRangeSimilarityGivesServerError() throws Exception {
		catalog.setMode(Strategy.COSINE, Mode.OUT_OF_RANGE);
		HttpClient client = HttpClient.newHttpClient();

		HttpResponse<String> comparison = client.send(HttpRequest.newBuilder(uri("/api/similarity/comparisons"))
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(
						"{\"articleIds\": [" + ids.get(0) + ", " + ids.get(1) + "]}"))
				.build(), HttpResponse.BodyHandlers.ofString());
		HttpResponse<String> trace = client.send(HttpRequest.newBuilder(uri("/api/similarity/trace?algorithm="
				+ "specter2-cosine&a=" + ids.get(0) + "&b=" + ids.get(1))).GET().build(),
				HttpResponse.BodyHandlers.ofString());

		assertThat(comparison.statusCode()).isEqualTo(500);
		assertThat(trace.statusCode()).isEqualTo(500);
	}

	@ParameterizedTest(name = "articleIds {0}")
	@ValueSource(strings = {"[1, null]", "[null]"})
	@DisplayName("AC-14: un id null da 400 de cuerpo inválido, revisado antes que la cantidad")
	void nullIdIsRejected(String articleIds) throws Exception {
		mockMvc.perform(post("/api/similarity/comparisons")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"articleIds\": " + articleIds + "}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value("El cuerpo debe ser un JSON con articleIds: lista de enteros"));
	}

	/** URL del servidor del test. */
	private URI uri(String path) {
		return URI.create("http://localhost:" + port + path);
	}

	/** {@code POST /api/similarity/comparisons} con los ids dados. */
	private ResultActions compare(List<Long> articleIds) throws Exception {
		StringJoiner json = new StringJoiner(", ", "{\"articleIds\": [", "]}");
		for (Long id : articleIds) {
			json.add(String.valueOf(id));
		}
		return mockMvc.perform(post("/api/similarity/comparisons")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json.toString()));
	}

	/** SHA-256 ficticio de 64 caracteres, distinto por índice. */
	private static String sha(int index) {
		return String.format("%064x", index + 1);
	}

	/** Vector canónico eᵢ de 768 dimensiones. */
	private static float[] unitVector(int index) {
		float[] vector = new float[Article.EMBEDDING_DIMENSION];
		vector[index] = 1f;
		return vector;
	}

}
