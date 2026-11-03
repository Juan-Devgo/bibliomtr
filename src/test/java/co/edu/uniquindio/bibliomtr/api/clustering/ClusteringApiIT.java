package co.edu.uniquindio.bibliomtr.api.clustering;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
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
import co.edu.uniquindio.bibliomtr.api.clustering.FakeClusteringCatalog.Mode;
import co.edu.uniquindio.bibliomtr.api.ranking.FakeCorpusTokenSource;
import co.edu.uniquindio.bibliomtr.api.similarity.FakeSimilarityCatalog;
import co.edu.uniquindio.bibliomtr.api.similarity.FakeSimilarityCatalog.Strategy;
import co.edu.uniquindio.bibliomtr.persistence.article.Article;
import co.edu.uniquindio.bibliomtr.persistence.article.ArticleRepository;

/**
 * Tests de {@code POST /api/clustering} y {@code GET /api/clustering/trace} con Postgres real y algoritmos
 * falsos (spec 003, REQ-7, REQ-10, REQ-11, REQ-12, REQ-16, REQ-18, REQ-19, REQ-22; AC-10, AC-11, AC-12, AC-15,
 * AC-16, AC-17, AC-19).
 *
 * <p><b>Datos.</b> Cada test vacía {@code article} e inserta por el repositorio 5 artículos seleccionables
 * ({@code a.pdf} … {@code e.pdf}, con abstracts y vectores distintos) y uno sin abstract. La similitud la da
 * {@link FakeSimilarityCatalog} (coseno 0.75 entre artículos distintos, así que d = 0.25) y el clustering
 * {@link FakeClusteringCatalog}: así los casos CALCULADO no dependen de los stubs de los estudiantes.
 *
 * <p><b>Servidor.</b> Casi todo se prueba con {@link MockMvc}; el 500 de un dendrograma inválido se pide además
 * por HTTP real, como en {@code ComparisonApiIT}.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, properties = "bibliomtr.corpus.dir=build/tmp/empty-corpus")
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, FakeCorpusTokenSource.Config.class, FakeSimilarityCatalog.Config.class,
		FakeClusteringCatalog.Config.class})
class ClusteringApiIT {

	private static final String[] SLUGS = {"single-linkage", "complete-linkage", "average-linkage", "diana"};

	private static final String[] STUB_CLASSES = {"SingleLinkage", "CompleteLinkage", "AverageLinkage", "Diana"};

	private static final String STUB_DIRECTORY = "src/main/java/co/edu/uniquindio/bibliomtr/algorithms/clustering/";

	private static final String[] ABSTRACTS = {
			"Generative models reshape assessment in higher education and the design of tasks.",
			"Learning analytics dashboards help instructors identify students at risk.",
			"Graph neural networks classify citation networks of scientific articles.",
			"Text embeddings compare abstracts of articles with cosine similarity.",
			"Hierarchical clustering groups related abstracts into a dendrogram."};

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ArticleRepository repository;

	@Autowired
	private FakeSimilarityCatalog similarityCatalog;

	@Autowired
	private FakeClusteringCatalog clusteringCatalog;

	@LocalServerPort
	private int port;

	/** Ids de los 5 seleccionables, en el orden a … e. */
	private final List<Long> ids = new ArrayList<>();

	/** Id del artículo sin abstract. */
	private long withoutAbstractId;

	@BeforeEach
	void insertArticles() {
		similarityCatalog.reset();
		clusteringCatalog.reset();
		repository.deleteAll();
		ids.clear();
		for (int i = 0; i < ABSTRACTS.length; i++) {
			String name = (char) ('a' + i) + ".pdf";
			Article article = Article.withAbstract(name, sha(i), "Artículo " + name, List.of("Ana Gómez"),
					ABSTRACTS[i], unitVector(i), false, 20, unitVector(i + 10), Instant.now());
			ids.add(repository.save(article).getId());
		}
		withoutAbstractId = repository.save(Article.withoutAbstract("f.pdf", sha(9), "Sin abstract", List.of(),
				Instant.now())).getId();
	}

	@Test
	@DisplayName("AC-11, AC-19: 5 artículos con SPECTER2 (coseno) dan 4 dendrogramas de 4 fusiones y tiempos en ms")
	void fiveArticlesGiveFourDendrograms() throws Exception {
		List<Long> selection = List.of(ids.get(3), ids.get(0), ids.get(1), ids.get(4), ids.get(2));

		ResultActions result = cluster(selection, "specter2-cosine")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.articles", hasSize(5)))
				.andExpect(jsonPath("$.articles[0].id").value(ids.get(3)))
				.andExpect(jsonPath("$.articles[1].fileName").value("a.pdf"))
				.andExpect(jsonPath("$.source.source").value("specter2-cosine"))
				.andExpect(jsonPath("$.source.label").value("SPECTER2 (coseno)"))
				.andExpect(jsonPath("$.source.status").value("CALCULADO"))
				.andExpect(jsonPath("$.source.pendingLocation").value(nullValue()))
				.andExpect(jsonPath("$.preparationMs", greaterThanOrEqualTo(0.0)))
				.andExpect(jsonPath("$.distances", hasSize(5)))
				.andExpect(jsonPath("$.results", hasSize(4)));
		for (int r = 0; r < SLUGS.length; r++) {
			String path = "$.results[" + r + "]";
			result.andExpect(jsonPath(path + ".algorithm").value(SLUGS[r]))
					.andExpect(jsonPath(path + ".label").isString())
					.andExpect(jsonPath(path + ".status").value("CALCULADO"))
					.andExpect(jsonPath(path + ".merges", hasSize(4)))
					.andExpect(jsonPath(path + ".merges[3].size").value(5))
					.andExpect(jsonPath(path + ".elapsedMs", greaterThanOrEqualTo(0.0)))
					.andExpect(jsonPath(path + ".pendingLocation").value(nullValue()))
					.andExpect(jsonPath(path + ".trace").doesNotExist());
		}

		String body = result.andReturn().getResponse().getContentAsString();
		List<List<Number>> distances = JsonPath.read(body, "$.distances");
		for (int i = 0; i < 5; i++) {
			for (int j = 0; j < 5; j++) {
				assertThat(distances.get(i).get(j).doubleValue()).as("d[%d][%d]", i, j)
						.isEqualTo(i == j ? 0.0 : 0.25);
			}
		}
		double[][] received = clusteringCatalog.lastDistances();
		assertThat(received).hasDimensions(5, 5);
		for (int i = 0; i < 5; i++) {
			for (int j = 0; j < 5; j++) {
				assertThat(received[i][j]).isEqualTo(distances.get(i).get(j).doubleValue());
			}
		}
	}

	@Test
	@DisplayName("REQ-10: la distancia es 1 − s de la fuente elegida (Jaccard 0.375 → 0.625)")
	void distancesComeFromTheChosenSource() throws Exception {
		cluster(List.of(ids.get(0), ids.get(1)), "jaccard")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.source.label").value("Jaccard"))
				.andExpect(jsonPath("$.distances[0][1]").value(0.625))
				.andExpect(jsonPath("$.distances[1][0]").value(0.625))
				.andExpect(jsonPath("$.distances[1][1]").value(0.0))
				.andExpect(jsonPath("$.results[0].merges", hasSize(1)))
				.andExpect(jsonPath("$.results[0].merges[0].height").value(0.625));
	}

	@Test
	@DisplayName("AC-11, AC-15: la traza de average-linkage trae fusiones, traza y tiempos")
	void traceOfImplementedAlgorithm() throws Exception {
		trace("average-linkage", "specter2-cosine", ids.subList(0, 3))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.algorithm").value("average-linkage"))
				.andExpect(jsonPath("$.label").value("Average Linkage (UPGMA)"))
				.andExpect(jsonPath("$.articles", hasSize(3)))
				.andExpect(jsonPath("$.articles[2].id").value(ids.get(2)))
				.andExpect(jsonPath("$.source.status").value("CALCULADO"))
				.andExpect(jsonPath("$.preparationMs", greaterThanOrEqualTo(0.0)))
				.andExpect(jsonPath("$.distances", hasSize(3)))
				.andExpect(jsonPath("$.status").value("CALCULADO"))
				.andExpect(jsonPath("$.merges", hasSize(2)))
				.andExpect(jsonPath("$.trace.algorithm").value("average-linkage"))
				.andExpect(jsonPath("$.trace.leaves").value(3))
				.andExpect(jsonPath("$.elapsedMs", greaterThanOrEqualTo(0.0)))
				.andExpect(jsonPath("$.pendingLocation").value(nullValue()));
	}

	@Test
	@DisplayName("AC-16: solo Average Linkage implementado da 1 CALCULADO y 3 PENDIENTE con su ubicación")
	void onlyAverageLinkageImplemented() throws Exception {
		clusteringCatalog.onlyImplemented(ClusteringAlgorithm.AVERAGE_LINKAGE);

		ResultActions result = cluster(ids.subList(0, 4), "specter2-cosine")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.results", hasSize(4)));
		for (int r = 0; r < SLUGS.length; r++) {
			String path = "$.results[" + r + "]";
			if (r == 2) {
				result.andExpect(jsonPath(path + ".status").value("CALCULADO"))
						.andExpect(jsonPath(path + ".merges", hasSize(3)));
			} else {
				result.andExpect(jsonPath(path + ".status").value("PENDIENTE"))
						.andExpect(jsonPath(path + ".merges").value(nullValue()))
						.andExpect(jsonPath(path + ".elapsedMs").value(nullValue()))
						.andExpect(jsonPath(path + ".pendingLocation")
								.value(STUB_DIRECTORY + STUB_CLASSES[r] + ".java#cluster"));
			}
		}
	}

	@Test
	@DisplayName("AC-16: la traza de un algoritmo pendiente trae su ubicación y ni fusiones ni traza")
	void traceOfPendingAlgorithm() throws Exception {
		clusteringCatalog.setMode(ClusteringAlgorithm.DIANA, Mode.PENDING);

		trace("diana", "specter2-cosine", ids.subList(0, 2))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("PENDIENTE"))
				.andExpect(jsonPath("$.distances", hasSize(2)))
				.andExpect(jsonPath("$.merges").value(nullValue()))
				.andExpect(jsonPath("$.trace").value(nullValue()))
				.andExpect(jsonPath("$.elapsedMs").value(nullValue()))
				.andExpect(jsonPath("$.pendingLocation").value(STUB_DIRECTORY + "Diana.java#cluster"));
	}

	@Test
	@DisplayName("AC-17: con la fuente pendiente no hay distancias ni dendrogramas y se informa su ubicación")
	void pendingSourceGivesNoDendrograms() throws Exception {
		similarityCatalog.setMode(Strategy.COSINE, FakeSimilarityCatalog.Mode.PENDING);
		String cosineLocation = "src/main/java/co/edu/uniquindio/bibliomtr/algorithms/similarity/"
				+ "CosineSimilarity.java#compare";

		cluster(ids.subList(0, 3), "specter2-cosine")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.articles", hasSize(3)))
				.andExpect(jsonPath("$.source.status").value("PENDIENTE"))
				.andExpect(jsonPath("$.source.pendingLocation").value(cosineLocation))
				.andExpect(jsonPath("$.preparationMs").value(nullValue()))
				.andExpect(jsonPath("$.distances").value(nullValue()))
				.andExpect(jsonPath("$.results", hasSize(0)));
		trace("single-linkage", "nomic-cosine", ids.subList(0, 3))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.algorithm").value("single-linkage"))
				.andExpect(jsonPath("$.source.status").value("PENDIENTE"))
				.andExpect(jsonPath("$.source.pendingLocation").value(cosineLocation))
				.andExpect(jsonPath("$.status").value(nullValue()))
				.andExpect(jsonPath("$.merges").value(nullValue()))
				.andExpect(jsonPath("$.trace").value(nullValue()));
		assertThat(clusteringCatalog.lastDistances()).isNull();
	}

	@Test
	@DisplayName("AC-12: 1 artículo, 21 artículos, ids repetidos o inexistentes dan 400 con los mensajes de la 001")
	void invalidSelectionsAreRejected() throws Exception {
		List<Long> many = new ArrayList<>();
		for (long id = 1; id <= 21; id++) {
			many.add(id);
		}
		long missing = ids.get(4) + 1000;

		cluster(List.of(ids.get(0)), "specter2-cosine")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value("Debe seleccionar entre 2 y 20 artículos; se recibieron 1"));
		cluster(many, "specter2-cosine")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value("Debe seleccionar entre 2 y 20 artículos; se recibieron 21"));
		cluster(List.of(ids.get(0), ids.get(1), ids.get(0)), "specter2-cosine")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail")
						.value("Los identificadores de artículo no deben repetirse: " + ids.get(0)));
		cluster(List.of(ids.get(0), missing, withoutAbstractId), "specter2-cosine")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value(
						"Artículos inexistentes o no seleccionables: " + missing + ", " + withoutAbstractId));
		trace("single-linkage", "specter2-cosine", List.of(ids.get(0)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value("Debe seleccionar entre 2 y 20 artículos; se recibieron 1"));
	}

	@Test
	@DisplayName("AC-12: fuente desconocida o ausente da 400, y se revisa antes que los artículos")
	void invalidSourcesAreRejected() throws Exception {
		cluster(ids.subList(0, 2), "lcs")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value("Fuente de similitud desconocida: lcs. Valores válidos: "
						+ "levenshtein, needleman-wunsch, tfidf-cosine, jaccard, specter2-cosine, specter2-euclidean, "
						+ "nomic-cosine, nomic-euclidean"));
		cluster(List.of(ids.get(0)), "lcs")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value(startsWith("Fuente de similitud desconocida")));
		mockMvc.perform(post("/api/clustering")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"articleIds\": [" + ids.get(0) + ", " + ids.get(1) + "]}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value("Debe indicar la fuente de similitud (source)"));
		cluster(ids.subList(0, 2), " ")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value("Debe indicar la fuente de similitud (source)"));
		trace("single-linkage", "lcs", ids.subList(0, 2))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value(startsWith("Fuente de similitud desconocida")));
	}

	@Test
	@DisplayName("AC-12: la traza rechaza un algoritmo desconocido, parámetros ausentes y un cuerpo ilegible")
	void invalidTraceRequestsAreRejected() throws Exception {
		trace("ward", "specter2-cosine", ids.subList(0, 2))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value("Algoritmo de clustering desconocido: ward. Valores válidos: "
						+ "single-linkage, complete-linkage, average-linkage, diana"));
		mockMvc.perform(get("/api/clustering/trace").param("algorithm", "diana").param("source", "jaccard"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value("Falta el parámetro articleIds"));
		mockMvc.perform(get("/api/clustering/trace").param("algorithm", "diana")
						.param("articleIds", ids.get(0) + "," + ids.get(1)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value("Falta el parámetro source"));
		mockMvc.perform(post("/api/clustering").contentType(MediaType.APPLICATION_JSON).content("no es json"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value("El cuerpo debe ser un JSON con articleIds: lista de enteros"));
	}

	@Test
	@DisplayName("AC-10: un dendrograma inválido da HTTP 500 con el mensaje en español, en el clustering y la traza")
	void invalidDendrogramGivesServerError() throws Exception {
		clusteringCatalog.setMode(ClusteringAlgorithm.SINGLE_LINKAGE, Mode.INVALID);
		HttpClient client = HttpClient.newHttpClient();
		String expected = "El algoritmo single-linkage devolvió un dendrograma inválido: "
				+ "3 fusiones para 3 artículos; se esperaban 2";

		HttpResponse<String> clustering = client.send(HttpRequest.newBuilder(uri("/api/clustering"))
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString("{\"articleIds\": [" + ids.get(0) + ", " + ids.get(1)
						+ ", " + ids.get(2) + "], \"source\": \"specter2-cosine\"}"))
				.build(), HttpResponse.BodyHandlers.ofString());
		HttpResponse<String> trace = client.send(HttpRequest.newBuilder(uri("/api/clustering/trace?algorithm="
				+ "single-linkage&source=specter2-cosine&articleIds=" + ids.get(0) + "," + ids.get(1) + ","
				+ ids.get(2))).GET().build(), HttpResponse.BodyHandlers.ofString());

		assertThat(clustering.statusCode()).isEqualTo(500);
		assertThat((String) JsonPath.read(clustering.body(), "$.detail")).isEqualTo(expected);
		assertThat((String) JsonPath.read(clustering.body(), "$.title")).isEqualTo("Error interno");
		assertThat(trace.statusCode()).isEqualTo(500);
		assertThat((String) JsonPath.read(trace.body(), "$.detail")).isEqualTo(expected);
	}

	/** URL del servidor del test. */
	private URI uri(String path) {
		return URI.create("http://localhost:" + port + path);
	}

	/** {@code POST /api/clustering} con los ids y la fuente dados. */
	private ResultActions cluster(List<Long> articleIds, String source) throws Exception {
		StringJoiner json = new StringJoiner(", ", "{\"articleIds\": [", "], \"source\": \"" + source + "\"}");
		for (Long id : articleIds) {
			json.add(String.valueOf(id));
		}
		return mockMvc.perform(post("/api/clustering")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json.toString()));
	}

	/** {@code GET /api/clustering/trace} con el algoritmo, la fuente y los ids dados. */
	private ResultActions trace(String algorithm, String source, List<Long> articleIds) throws Exception {
		StringJoiner joined = new StringJoiner(",");
		for (Long id : articleIds) {
			joined.add(String.valueOf(id));
		}
		return mockMvc.perform(get("/api/clustering/trace")
				.param("algorithm", algorithm)
				.param("source", source)
				.param("articleIds", joined.toString()));
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
