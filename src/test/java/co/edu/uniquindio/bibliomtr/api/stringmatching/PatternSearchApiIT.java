package co.edu.uniquindio.bibliomtr.api.stringmatching;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.jayway.jsonpath.JsonPath;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import co.edu.uniquindio.bibliomtr.TestcontainersConfiguration;
import co.edu.uniquindio.bibliomtr.algorithms.stringmatching.GuideFragment;
import co.edu.uniquindio.bibliomtr.api.ranking.FakeCorpusTokenSource;
import co.edu.uniquindio.bibliomtr.api.stringmatching.FakeStringMatchingCatalog.Call;
import co.edu.uniquindio.bibliomtr.api.stringmatching.FakeStringMatchingCatalog.Mode;
import co.edu.uniquindio.bibliomtr.persistence.article.Article;
import co.edu.uniquindio.bibliomtr.persistence.article.ArticleRepository;

/**
 * Tests de {@code GET /api/pattern-search} y {@code GET /api/pattern-search/trace} con Postgres real y algoritmos
 * falsos (spec 004, REQ-1, REQ-8, REQ-15, REQ-16, REQ-17, REQ-18, REQ-22, REQ-28, REQ-29; AC-3, AC-12, AC-13,
 * AC-14, AC-19, AC-20, AC-21, AC-25, AC-26, AC-27).
 *
 * <p><b>Datos.</b> Cada test vacía {@code article} e inserta por el repositorio 4 artículos seleccionables y uno sin
 * abstract. Con el patrón "systems" (m = 7), calculado a mano:
 * <ul>
 *   <li>{@code a.pdf} y {@code b.pdf}: {@link GuideFragment#FRAGMENT} (abstracts idénticos), posiciones [214, 271];
 *       24 colisiones simuladas (las 's' en s ≤ 272 que no son ocurrencias).</li>
 *   <li>{@code c.pdf}: "Expert systems and tutoring systems." (n = 36), posiciones [7, 28]; colisiones simuladas
 *       [9, 13] (las 's' internas de "systems" en s = 7; las de s = 30 y 34 pasan de n − m = 29).</li>
 *   <li>{@code d.pdf}: "Brief" (n = 5 &lt; m): 0 ocurrencias sin error (REQ-17).</li>
 * </ul>
 * Los algoritmos los da {@link FakeStringMatchingCatalog}; así los casos CALCULADO no dependen de los stubs.
 *
 * <p><b>Servidor.</b> Casi todo se prueba con {@link MockMvc}; el patrón " " (AC-25) y el 500 de posiciones
 * inválidas (AC-3) se piden además por HTTP real, para cubrir la decodificación de la URL y el manejo de errores.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, properties = "bibliomtr.corpus.dir=build/tmp/empty-corpus")
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, FakeCorpusTokenSource.Config.class, FakeStringMatchingCatalog.Config.class})
class PatternSearchApiIT {

	private static final String[] SLUGS = {"kmp", "boyer-moore", "rabin-karp"};

	private static final String[] LABELS = {"Knuth-Morris-Pratt (KMP)", "Boyer-Moore", "Rabin-Karp"};

	private static final String STUB_DIRECTORY = "src/main/java/co/edu/uniquindio/bibliomtr/algorithms/stringmatching/";

	private static final String PATTERN = GuideFragment.PATTERN;

	private static final String SHORT_TEXT = "Expert systems and tutoring systems.";

	private static final String TINY_TEXT = "Brief";

	private static final String[] ABSTRACTS = {GuideFragment.FRAGMENT, GuideFragment.FRAGMENT, SHORT_TEXT, TINY_TEXT};

	private static final String Q_MESSAGE = "El parámetro q debe ser un número primo entre 2 y 1000000007; se recibió: ";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ArticleRepository repository;

	@Autowired
	private FakeStringMatchingCatalog catalog;

	@LocalServerPort
	private int port;

	/** Ids de los 4 seleccionables, en el orden a … d. */
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
	@DisplayName("AC-12, AC-21: el patrón de la guía da las posiciones, totales, colisiones de Rabin-Karp y allMatch")
	void guidePatternGivesPositionsWithTheThreeAlgorithms() throws Exception {
		ResultActions result = search(PATTERN, null)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.pattern").value(PATTERN))
				.andExpect(jsonPath("$.q").value(101))
				.andExpect(jsonPath("$.d").value(65_536))
				.andExpect(jsonPath("$.articleCount").value(4))
				.andExpect(jsonPath("$.allMatch").value(true))
				.andExpect(jsonPath("$.algorithms", hasSize(3)))
				.andExpect(jsonPath("$.articles", hasSize(4)))
				.andExpect(jsonPath("$.articles[0].articleId").value(ids.get(0)))
				.andExpect(jsonPath("$.articles[0].title").value("Artículo a.pdf"))
				.andExpect(jsonPath("$.articles[0].abstractText").value(GuideFragment.FRAGMENT))
				.andExpect(jsonPath("$.articles[2].abstractText").value(SHORT_TEXT))
				.andExpect(jsonPath("$.articles[3].articleId").value(ids.get(3)));
		for (int r = 0; r < SLUGS.length; r++) {
			String path = "$.algorithms[" + r + "]";
			boolean rabinKarp = r == 2;
			result.andExpect(jsonPath(path + ".algorithm").value(SLUGS[r]))
					.andExpect(jsonPath(path + ".label").value(LABELS[r]))
					.andExpect(jsonPath(path + ".status").value("CALCULADO"))
					.andExpect(jsonPath(path + ".pendingLocation").value(nullValue()))
					.andExpect(jsonPath(path + ".preprocessingMs", nonNegative()))
					.andExpect(jsonPath(path + ".searchMs", nonNegative()))
					.andExpect(jsonPath(path + ".totalOccurrences").value(6))
					.andExpect(jsonPath(path + ".articles", hasSize(4)))
					.andExpect(jsonPath(path + ".articles[0].articleId").value(ids.get(0)))
					.andExpect(jsonPath(path + ".articles[0].title").value("Artículo a.pdf"))
					.andExpect(jsonPath(path + ".articles[0].positions", exactly(GuideFragment.POSITIONS)))
					.andExpect(jsonPath(path + ".articles[0].count").value(2))
					.andExpect(jsonPath(path + ".articles[1].positions", exactly(GuideFragment.POSITIONS)))
					.andExpect(jsonPath(path + ".articles[2].positions", exactly(List.of(7, 28))))
					.andExpect(jsonPath(path + ".articles[2].count").value(2))
					.andExpect(jsonPath(path + ".articles[3].positions", hasSize(0)))
					.andExpect(jsonPath(path + ".articles[3].count").value(0));
			if (rabinKarp) {
				result.andExpect(jsonPath(path + ".articles[0].collisions").value(24))
						.andExpect(jsonPath(path + ".articles[1].collisions").value(24))
						.andExpect(jsonPath(path + ".articles[2].collisions").value(2))
						.andExpect(jsonPath(path + ".articles[3].collisions").value(0));
			} else {
				result.andExpect(jsonPath(path + ".articles[0].collisions").value(nullValue()))
						.andExpect(jsonPath(path + ".articles[2].collisions").value(nullValue()));
			}
		}
	}

	@Test
	@DisplayName("AC-27: un solo preprocess y un search por abstract con la misma instancia y sin traza")
	void preprocessesOnceAndReusesTheTables() throws Exception {
		search(PATTERN, "13").andExpect(status().isOk());

		for (StringMatchingAlgorithm algorithm : StringMatchingAlgorithm.values()) {
			List<Call> calls = catalog.calls(algorithm);
			assertThat(calls).as("llamadas a %s", algorithm.slug()).hasSize(1 + ABSTRACTS.length);
			Call preprocess = calls.getFirst();
			assertThat(preprocess.method()).isEqualTo("preprocess");
			assertThat(preprocess.recordTrace()).isFalse();
			assertThat(preprocess.q()).isEqualTo(13L);
			for (Call call : calls.subList(1, calls.size())) {
				assertThat(call.method()).isEqualTo("search");
				assertThat(call.preprocessed()).isSameAs(preprocess.preprocessed());
				assertThat(call.recordTrace()).isFalse();
				assertThat(call.q()).isEqualTo(13L);
			}
		}
	}

	@Test
	@DisplayName("AC-20, AC-21: solo KMP implementado da 2 PENDIENTE en preprocess, sin tiempos y sin allMatch")
	void onlyKmpImplemented() throws Exception {
		catalog.onlyImplemented(StringMatchingAlgorithm.KMP);

		ResultActions result = search(PATTERN, null)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.allMatch").value(nullValue()))
				.andExpect(jsonPath("$.algorithms[0].status").value("CALCULADO"))
				.andExpect(jsonPath("$.algorithms[0].preprocessingMs", nonNegative()))
				.andExpect(jsonPath("$.algorithms[0].articles[0].positions", exactly(GuideFragment.POSITIONS)));
		String[] classes = {"KnuthMorrisPratt", "BoyerMoore", "RabinKarp"};
		for (int r = 1; r < SLUGS.length; r++) {
			String path = "$.algorithms[" + r + "]";
			result.andExpect(jsonPath(path + ".algorithm").value(SLUGS[r]))
					.andExpect(jsonPath(path + ".status").value("PENDIENTE"))
					.andExpect(jsonPath(path + ".pendingLocation")
							.value(STUB_DIRECTORY + classes[r] + ".java#preprocess"))
					.andExpect(jsonPath(path + ".preprocessingMs").value(nullValue()))
					.andExpect(jsonPath(path + ".searchMs").value(nullValue()))
					.andExpect(jsonPath(path + ".totalOccurrences").value(nullValue()))
					.andExpect(jsonPath(path + ".articles", hasSize(0)));
		}
	}

	@Test
	@DisplayName("AC-20: un pendiente en search informa #search y los otros 2 algoritmos siguen comparándose")
	void pendingInSearchReportsTheSearchMethod() throws Exception {
		catalog.setMode(StringMatchingAlgorithm.RABIN_KARP, Mode.PENDING_SEARCH);

		search(PATTERN, null)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.allMatch").value(true))
				.andExpect(jsonPath("$.algorithms[2].status").value("PENDIENTE"))
				.andExpect(jsonPath("$.algorithms[2].pendingLocation").value(STUB_DIRECTORY + "RabinKarp.java#search"))
				.andExpect(jsonPath("$.algorithms[2].preprocessingMs").value(nullValue()))
				.andExpect(jsonPath("$.algorithms[2].searchMs").value(nullValue()))
				.andExpect(jsonPath("$.algorithms[2].totalOccurrences").value(nullValue()))
				.andExpect(jsonPath("$.algorithms[2].articles", hasSize(0)));
	}

	@Test
	@DisplayName("REQ-16: si un algoritmo da otras posiciones válidas, allMatch es false")
	void differentPositionsGiveAllMatchFalse() throws Exception {
		catalog.setMode(StringMatchingAlgorithm.BOYER_MOORE, Mode.PARTIAL);

		search(PATTERN, null)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.allMatch").value(false))
				.andExpect(jsonPath("$.algorithms[1].articles[0].positions", exactly(List.of(214))))
				.andExpect(jsonPath("$.algorithms[1].totalOccurrences").value(3));
	}

	@Test
	@DisplayName("AC-14: sin abstracts seleccionables responde 200 con listas vacías")
	void emptyCorpusGivesEmptyResults() throws Exception {
		repository.deleteAll();

		ResultActions result = search(PATTERN, null)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.articleCount").value(0))
				.andExpect(jsonPath("$.articles", hasSize(0)))
				.andExpect(jsonPath("$.allMatch").value(true))
				.andExpect(jsonPath("$.algorithms", hasSize(3)));
		for (int r = 0; r < SLUGS.length; r++) {
			result.andExpect(jsonPath("$.algorithms[" + r + "].status").value("CALCULADO"))
					.andExpect(jsonPath("$.algorithms[" + r + "].articles", hasSize(0)))
					.andExpect(jsonPath("$.algorithms[" + r + "].totalOccurrences").value(0));
		}
	}

	@Test
	@DisplayName("AC-13: patrón vacío, ausente o de 201 char, y q = 1, 100, abc o mayor que el máximo dan 400")
	void invalidPatternOrQIsRejected() throws Exception {
		search("", null)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.title").value("Solicitud inválida"))
				.andExpect(jsonPath("$.detail").value("El patrón no puede estar vacío"));
		mockMvc.perform(get("/api/pattern-search"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value("El patrón no puede estar vacío"));
		search("x".repeat(201), null)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value("El patrón tiene 201 caracteres; el máximo es 200"));
		for (String q : List.of("1", "100", "abc", "1000000009", "-7", "4.5")) {
			search(PATTERN, q)
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.detail").value(Q_MESSAGE + q));
		}
		assertThat(catalog.calls(StringMatchingAlgorithm.KMP)).as("no se busca con parámetros inválidos").isEmpty();
	}

	@Test
	@DisplayName("REQ-17: un patrón de 200 char y q = 2 o 1000000007 se aceptan")
	void boundaryPatternAndQAreAccepted() throws Exception {
		search("x".repeat(200), "2")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.q").value(2))
				.andExpect(jsonPath("$.algorithms[0].totalOccurrences").value(0));
		search(PATTERN, "1000000007")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.q").value(1_000_000_007L));
	}

	@Test
	@DisplayName("AC-25: el patrón \" \" llega sin recortar por HTTP real y da las posiciones de los espacios")
	void spacePatternIsNotTrimmed() throws Exception {
		HttpResponse<String> response = HttpClient.newHttpClient().send(
				HttpRequest.newBuilder(uri("/api/pattern-search?pattern=%20")).GET().build(),
				HttpResponse.BodyHandlers.ofString());

		assertThat(response.statusCode()).isEqualTo(200);
		String body = response.body();
		assertThat((String) JsonPath.read(body, "$.pattern")).isEqualTo(" ");
		for (int r = 0; r < SLUGS.length; r++) {
			String path = "$.algorithms[" + r + "]";
			assertThat((List<Integer>) JsonPath.read(body, path + ".articles[2].positions"))
					.containsExactly(6, 14, 18, 27);
			assertThat((List<Integer>) JsonPath.read(body, path + ".articles[3].positions")).isEmpty();
			assertThat((Integer) JsonPath.read(body, path + ".articles[0].count")).isEqualTo(36);
		}
	}

	@Test
	@DisplayName("AC-12: la traza de Rabin-Karp con q = 3 trae preprocesamiento, traza, posiciones y colisiones")
	void traceOfImplementedAlgorithm() throws Exception {
		trace("rabin-karp", ids.get(0), PATTERN, "3")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.algorithm").value("rabin-karp"))
				.andExpect(jsonPath("$.label").value("Rabin-Karp"))
				.andExpect(jsonPath("$.articleId").value(ids.get(0)))
				.andExpect(jsonPath("$.title").value("Artículo a.pdf"))
				.andExpect(jsonPath("$.abstractText").value(GuideFragment.FRAGMENT))
				.andExpect(jsonPath("$.pattern").value(PATTERN))
				.andExpect(jsonPath("$.q").value(3))
				.andExpect(jsonPath("$.status").value("CALCULADO"))
				.andExpect(jsonPath("$.pendingLocation").value(nullValue()))
				.andExpect(jsonPath("$.positions", exactly(GuideFragment.POSITIONS)))
				.andExpect(jsonPath("$.collisions", hasSize(24)))
				.andExpect(jsonPath("$.collisions[0]").value(4))
				.andExpect(jsonPath("$.preprocessing.algorithm").value("rabin-karp"))
				.andExpect(jsonPath("$.preprocessing.q").value(3))
				.andExpect(jsonPath("$.trace.textLength").value(GuideFragment.FRAGMENT_LENGTH))
				.andExpect(jsonPath("$.trace.windows").value(GuideFragment.WINDOW_COUNT));

		List<Call> calls = catalog.calls(StringMatchingAlgorithm.RABIN_KARP);
		assertThat(calls).hasSize(2);
		assertThat(calls.get(0).method()).isEqualTo("preprocess");
		assertThat(calls.get(0).recordTrace()).isTrue();
		assertThat(calls.get(1).method()).isEqualTo("search");
		assertThat(calls.get(1).recordTrace()).isTrue();
		assertThat(calls.get(1).preprocessed()).isSameAs(calls.get(0).preprocessed());
		assertThat(catalog.calls(StringMatchingAlgorithm.KMP)).isEmpty();
	}

	@Test
	@DisplayName("REQ-22: la traza de KMP con q por defecto no trae colisiones y acepta el patrón \" \"")
	void traceOfKmpHasNoCollisions() throws Exception {
		trace("kmp", ids.get(2), PATTERN, null)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.q").value(101))
				.andExpect(jsonPath("$.positions", exactly(List.of(7, 28))))
				.andExpect(jsonPath("$.collisions").value(nullValue()))
				.andExpect(jsonPath("$.trace.algorithm").value("kmp"));
		trace("boyer-moore", ids.get(2), " ", null)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.pattern").value(" "))
				.andExpect(jsonPath("$.positions", exactly(List.of(6, 14, 18, 27))));
	}

	@Test
	@DisplayName("AC-26: la traza de un algoritmo pendiente responde 200 con PENDIENTE y su ubicación")
	void traceOfPendingAlgorithm() throws Exception {
		catalog.setMode(StringMatchingAlgorithm.BOYER_MOORE, Mode.PENDING_PREPROCESS);
		catalog.setMode(StringMatchingAlgorithm.KMP, Mode.PENDING_SEARCH);

		trace("boyer-moore", ids.get(0), PATTERN, null)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.algorithm").value("boyer-moore"))
				.andExpect(jsonPath("$.articleId").value(ids.get(0)))
				.andExpect(jsonPath("$.abstractText").value(GuideFragment.FRAGMENT))
				.andExpect(jsonPath("$.status").value("PENDIENTE"))
				.andExpect(jsonPath("$.pendingLocation").value(STUB_DIRECTORY + "BoyerMoore.java#preprocess"))
				.andExpect(jsonPath("$.positions").value(nullValue()))
				.andExpect(jsonPath("$.collisions").value(nullValue()))
				.andExpect(jsonPath("$.preprocessing").value(nullValue()))
				.andExpect(jsonPath("$.trace").value(nullValue()));
		trace("kmp", ids.get(0), PATTERN, null)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("PENDIENTE"))
				.andExpect(jsonPath("$.pendingLocation").value(STUB_DIRECTORY + "KnuthMorrisPratt.java#search"))
				.andExpect(jsonPath("$.trace").value(nullValue()));
	}

	@Test
	@DisplayName("AC-19: la traza rechaza artículo inexistente o sin abstract, algoritmo desconocido, patrón y q")
	void invalidTraceRequestsAreRejected() throws Exception {
		long missing = ids.get(3) + 1000;

		trace("kmp", missing, PATTERN, null)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value("El artículo " + missing + " no existe o no tiene abstract"));
		trace("kmp", withoutAbstractId, PATTERN, null)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail")
						.value("El artículo " + withoutAbstractId + " no existe o no tiene abstract"));
		trace("x", ids.get(0), PATTERN, null)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value("Algoritmo de búsqueda de patrones desconocido: x. "
						+ "Valores válidos: kmp, boyer-moore, rabin-karp"));
		trace("kmp", ids.get(0), "", null)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value("El patrón no puede estar vacío"));
		trace("kmp", ids.get(0), "x".repeat(201), null)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value("El patrón tiene 201 caracteres; el máximo es 200"));
		for (String q : List.of("1", "100", "abc")) {
			trace("rabin-karp", ids.get(0), PATTERN, q)
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.detail").value(Q_MESSAGE + q));
		}
		mockMvc.perform(get("/api/pattern-search/trace").param("algorithm", "kmp").param("articleId", "abc")
						.param("pattern", PATTERN))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value("El parámetro articleId debe ser un entero mayor o igual a 1"));
		mockMvc.perform(get("/api/pattern-search/trace").param("articleId", String.valueOf(ids.get(0)))
						.param("pattern", PATTERN))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value("Falta el parámetro algorithm"));
		assertThat(catalog.calls(StringMatchingAlgorithm.KMP)).as("no se busca con parámetros inválidos").isEmpty();
	}

	@Test
	@DisplayName("REQ-22: se valida patrón, luego q, luego algoritmo y al final el artículo")
	void traceValidationOrder() throws Exception {
		long missing = ids.get(3) + 1000;

		trace("x", missing, "", "100")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value("El patrón no puede estar vacío"));
		trace("x", missing, PATTERN, "100")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value(Q_MESSAGE + "100"));
		trace("x", missing, PATTERN, null)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value(startsWith(
						"Algoritmo de búsqueda de patrones desconocido: x")));
	}

	@Test
	@DisplayName("AC-3: posiciones inválidas dan HTTP 500 con el mensaje en español, en la búsqueda y la traza")
	void invalidPositionsGiveServerError() throws Exception {
		catalog.setMode(StringMatchingAlgorithm.KMP, Mode.INVALID);
		HttpClient client = HttpClient.newHttpClient();
		String repeated = "El algoritmo kmp devolvió posiciones inválidas en el artículo " + ids.get(0)
				+ ": la posición 214 no es mayor que la anterior 271";

		HttpResponse<String> searchResponse = client.send(
				HttpRequest.newBuilder(uri("/api/pattern-search?pattern=" + PATTERN)).GET().build(),
				HttpResponse.BodyHandlers.ofString());
		HttpResponse<String> traceResponse = client.send(HttpRequest.newBuilder(uri("/api/pattern-search/trace"
				+ "?algorithm=kmp&articleId=" + ids.get(0) + "&pattern=" + PATTERN)).GET().build(),
				HttpResponse.BodyHandlers.ofString());
		HttpResponse<String> outOfRange = client.send(HttpRequest.newBuilder(uri("/api/pattern-search/trace"
				+ "?algorithm=kmp&articleId=" + ids.get(3) + "&pattern=" + PATTERN)).GET().build(),
				HttpResponse.BodyHandlers.ofString());

		assertThat(searchResponse.statusCode()).isEqualTo(500);
		assertThat((String) JsonPath.read(searchResponse.body(), "$.title")).isEqualTo("Error interno");
		assertThat((String) JsonPath.read(searchResponse.body(), "$.detail")).isEqualTo(repeated);
		assertThat(traceResponse.statusCode()).isEqualTo(500);
		assertThat((String) JsonPath.read(traceResponse.body(), "$.detail")).isEqualTo(repeated);
		assertThat(outOfRange.statusCode()).isEqualTo(500);
		assertThat((String) JsonPath.read(outOfRange.body(), "$.detail")).isEqualTo("El algoritmo kmp devolvió "
				+ "posiciones inválidas en el artículo " + ids.get(3) + ": la posición -1 está fuera de [0, -2]");
	}

	/** Lista JSON con exactamente estos enteros, en orden. */
	private static Matcher<Iterable<? extends Integer>> exactly(List<Integer> expected) {
		return contains(expected.toArray(Integer[]::new));
	}

	/**
	 * Número JSON mayor o igual a 0, sea cual sea su tipo al leerlo ({@code Double} o {@code BigDecimal}, según los
	 * dígitos del tiempo medido).
	 */
	private static Matcher<Object> nonNegative() {
		return new TypeSafeMatcher<>(Object.class) {
			@Override
			protected boolean matchesSafely(Object value) {
				return value instanceof Number number && number.doubleValue() >= 0.0;
			}

			@Override
			public void describeTo(Description description) {
				description.appendText("un número mayor o igual a 0");
			}
		};
	}

	/** URL del servidor del test. */
	private URI uri(String path) {
		return URI.create("http://localhost:" + port + path);
	}

	/** {@code GET /api/pattern-search} con el patrón y q dados; q nulo no se envía. */
	private ResultActions search(String pattern, String q) throws Exception {
		MockHttpServletRequestBuilder request = get("/api/pattern-search").param("pattern", pattern);
		if (q != null) {
			request.param("q", q);
		}
		return mockMvc.perform(request);
	}

	/** {@code GET /api/pattern-search/trace} con los parámetros dados; q nulo no se envía. */
	private ResultActions trace(String algorithm, long articleId, String pattern, String q) throws Exception {
		MockHttpServletRequestBuilder request = get("/api/pattern-search/trace")
				.param("algorithm", algorithm)
				.param("articleId", String.valueOf(articleId))
				.param("pattern", pattern);
		if (q != null) {
			request.param("q", q);
		}
		return mockMvc.perform(request);
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
