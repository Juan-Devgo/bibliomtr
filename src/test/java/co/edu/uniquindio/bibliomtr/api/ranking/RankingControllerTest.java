package co.edu.uniquindio.bibliomtr.api.ranking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import co.edu.uniquindio.bibliomtr.algorithms.ranking.BucketSort;
import co.edu.uniquindio.bibliomtr.algorithms.ranking.CountingSort;
import co.edu.uniquindio.bibliomtr.algorithms.ranking.RadixSort;
import co.edu.uniquindio.bibliomtr.algorithms.ranking.SortAlgorithm;
import co.edu.uniquindio.bibliomtr.algorithms.ranking.SortOptions;
import co.edu.uniquindio.bibliomtr.algorithms.ranking.SortResult;
import co.edu.uniquindio.bibliomtr.algorithms.ranking.TermFrequency;
import co.edu.uniquindio.bibliomtr.algorithms.ranking.TermSorter;
import co.edu.uniquindio.bibliomtr.api.ranking.RankingDtos.RankingResponse;

/**
 * Tests de {@code GET /api/ranking} y {@code GET /api/ranking/trace/{algorithm}} con un corpus falso (REQ-3,
 * REQ-4, REQ-9, REQ-11, REQ-16; AC-2, AC-3, AC-4, AC-6, AC-7, AC-22, AC-23, AC-25).
 */
@WebMvcTest(RankingController.class)
@Import({RankingService.class, FakeCorpusTokenSource.Config.class})
class RankingControllerTest {

	private static final String[] SLUGS = {"counting", "radix", "bucket"};

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private FakeCorpusTokenSource corpus;

	@BeforeEach
	void resetCorpus() {
		corpus.clear();
	}

	@Test
	@DisplayName("AC-2: sin n responde los 20 primeros términos de cada algoritmo con su frecuencia")
	void defaultTopIsTwenty() throws Exception {
		corpus.setDistinctTerms(25);

		var result = mockMvc.perform(get("/api/ranking"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.termCount").value(25))
				.andExpect(jsonPath("$.n").value(20))
				.andExpect(jsonPath("$.bucketSize").value(5))
				.andExpect(jsonPath("$.algorithms", hasSize(3)));
		for (int a = 0; a < SLUGS.length; a++) {
			String algorithm = "$.algorithms[" + a + "]";
			result.andExpect(jsonPath(algorithm + ".algorithm").value(SLUGS[a]))
					.andExpect(jsonPath(algorithm + ".top", hasSize(20)))
					.andExpect(jsonPath(algorithm + ".top[0].term").value("term25"))
					.andExpect(jsonPath(algorithm + ".top[0].frequency").value(25))
					.andExpect(jsonPath(algorithm + ".top[19].term").value("term06"))
					.andExpect(jsonPath(algorithm + ".top[19].frequency").value(6));
		}
	}

	@ParameterizedTest(name = "consulta \"{0}\"")
	@ValueSource(strings = {"/api/ranking", "/api/ranking?n=11"})
	@DisplayName("AC-22: con 10 términos, sin n o con n = 11, responde los 10 términos sin error")
	void topLargerThanTermCountReturnsAllTerms(String uri) throws Exception {
		corpus.setDistinctTerms(10);

		var result = mockMvc.perform(get(uri))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.termCount").value(10));
		for (int a = 0; a < SLUGS.length; a++) {
			result.andExpect(jsonPath("$.algorithms[" + a + "].top", hasSize(10)))
					.andExpect(jsonPath("$.algorithms[" + a + "].top[9].term").value("term01"));
		}
	}

	@ParameterizedTest(name = "{0}={1}")
	@CsvSource({"n, 0", "n, abc", "bucketSize, 0", "bucketSize, abc", "n, -3", "bucketSize, 2.5"})
	@DisplayName("AC-3: n o bucketSize no entero o menor que 1 da 400 con mensaje en español")
	void invalidRankingParametersAreRejected(String parameter, String value) throws Exception {
		corpus.setDistinctTerms(10);

		mockMvc.perform(get("/api/ranking").param(parameter, value))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.detail")
						.value("El parámetro " + parameter + " debe ser un entero mayor o igual a 1"));
	}

	@Test
	@DisplayName("AC-4: corpus sin abstracts seleccionables responde un ranking vacío sin error")
	void emptyCorpusGivesEmptyRanking() throws Exception {
		var result = mockMvc.perform(get("/api/ranking"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.termCount").value(0))
				.andExpect(jsonPath("$.allMatch").value(true))
				.andExpect(jsonPath("$.algorithms", hasSize(3)));
		for (int a = 0; a < SLUGS.length; a++) {
			result.andExpect(jsonPath("$.algorithms[" + a + "].top", hasSize(0)));
		}
	}

	@Test
	@DisplayName("AC-4: la traza de cada algoritmo sobre un corpus vacío responde listas vacías sin error")
	void emptyCorpusGivesEmptyTrace() throws Exception {
		for (String slug : SLUGS) {
			mockMvc.perform(get("/api/ranking/trace/" + slug))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.input", hasSize(0)))
					.andExpect(jsonPath("$.sorted", hasSize(0)))
					.andExpect(jsonPath("$.ranking", hasSize(0)));
		}
	}

	@Test
	@DisplayName("AC-6: los tres algoritmos coinciden y allMatch es verdadero, con desempate alfabético")
	void allAlgorithmsMatch() throws Exception {
		corpus.setAbstracts(List.of(
				tokens("model", 30, "graph", 30, "data", 30),
				tokens("model", 12, "vector", 17, "text", 3, "zeta", 1),
				tokens("algorithm", 17, "network", 1)));

		var result = mockMvc.perform(get("/api/ranking").param("n", "8").param("bucketSize", "3"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.termCount").value(8))
				.andExpect(jsonPath("$.bucketSize").value(3))
				.andExpect(jsonPath("$.allMatch").value(true));
		String[] expectedTerms = {"model", "data", "graph", "algorithm", "vector", "text", "network", "zeta"};
		int[] expectedFrequencies = {42, 30, 30, 17, 17, 3, 1, 1};
		for (int a = 0; a < SLUGS.length; a++) {
			for (int i = 0; i < expectedTerms.length; i++) {
				String element = "$.algorithms[" + a + "].top[" + i + "]";
				result.andExpect(jsonPath(element + ".term").value(expectedTerms[i]))
						.andExpect(jsonPath(element + ".frequency").value(expectedFrequencies[i]));
			}
		}
	}

	@Test
	@DisplayName("AC-7: la respuesta trae top y tiempos en ms por separado, sin trazas")
	void rankingHasSeparateTimesAndNoTrace() throws Exception {
		corpus.setDistinctTerms(15);

		var result = mockMvc.perform(get("/api/ranking"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.preprocessingMs").isNumber())
				.andExpect(jsonPath("$.preprocessingMs", greaterThanOrEqualTo(0.0)))
				.andExpect(jsonPath("$.trace").doesNotExist());
		for (int a = 0; a < SLUGS.length; a++) {
			String algorithm = "$.algorithms[" + a + "]";
			result.andExpect(jsonPath(algorithm + ".elapsedMs").isNumber())
					.andExpect(jsonPath(algorithm + ".elapsedMs", greaterThanOrEqualTo(0.0)))
					.andExpect(jsonPath(algorithm + ".trace").doesNotExist());
		}
	}

	@Test
	@DisplayName("AC-7: el ranking ejecuta cada algoritmo con la grabación de traza desactivada")
	void rankingSortsWithoutRecordingTrace() {
		FakeCorpusTokenSource source = new FakeCorpusTokenSource();
		source.setDistinctTerms(12);
		List<SpySorter> spies = List.of(new SpySorter(new CountingSort()), new SpySorter(new RadixSort()),
				new SpySorter(new BucketSort()));
		RankingService service = new RankingService(source, List.copyOf(spies));

		RankingResponse response = service.rank(5, 4);

		assertThat(response.allMatch()).isTrue();
		for (SpySorter spy : spies) {
			assertThat(spy.received).as(spy.algorithm().slug()).containsExactly(new SortOptions(4, false));
		}
	}

	@Test
	@DisplayName("AC-23: la traza de Counting Sort cubre la lista completa del corpus")
	void countingTraceIsComplete() throws Exception {
		corpus.setDistinctTerms(25);

		mockMvc.perform(get("/api/ranking/trace/counting"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.algorithm").value("counting"))
				.andExpect(jsonPath("$.input", hasSize(25)))
				.andExpect(jsonPath("$.input[0].term").value("term25"))
				.andExpect(jsonPath("$.sorted", hasSize(25)))
				.andExpect(jsonPath("$.sorted[0].frequency").value(1))
				.andExpect(jsonPath("$.ranking", hasSize(25)))
				.andExpect(jsonPath("$.ranking[0].term").value("term25"))
				.andExpect(jsonPath("$.trace.input", hasSize(25)))
				.andExpect(jsonPath("$.trace.max").value(25))
				.andExpect(jsonPath("$.trace.count", hasSize(26)))
				.andExpect(jsonPath("$.trace.cumulative", hasSize(26)))
				.andExpect(jsonPath("$.trace.cumulative[25]").value(25))
				.andExpect(jsonPath("$.trace.placements", hasSize(25)))
				.andExpect(jsonPath("$.trace.output", hasSize(25)));
	}

	@Test
	@DisplayName("AC-23: la traza de Radix Sort tiene una pasada por dígito del máximo sobre la lista completa")
	void radixTraceIsComplete() throws Exception {
		corpus.setDistinctTerms(25);

		mockMvc.perform(get("/api/ranking/trace/radix"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.algorithm").value("radix"))
				.andExpect(jsonPath("$.ranking", hasSize(25)))
				.andExpect(jsonPath("$.trace.max").value(25))
				.andExpect(jsonPath("$.trace.passes", hasSize(2)))
				.andExpect(jsonPath("$.trace.passes[0].exp").value(1))
				.andExpect(jsonPath("$.trace.passes[1].exp").value(10))
				.andExpect(jsonPath("$.trace.passes[0].digits", hasSize(25)))
				.andExpect(jsonPath("$.trace.passes[0].count", hasSize(10)))
				.andExpect(jsonPath("$.trace.passes[0].cumulative", hasSize(10)))
				.andExpect(jsonPath("$.trace.passes[1].result", hasSize(25)))
				.andExpect(jsonPath("$.trace.output", hasSize(25)));
	}

	@Test
	@DisplayName("AC-23, AC-25: la traza de Bucket Sort sin bucketSize usa 5 y cubre la lista completa")
	void bucketTraceUsesDefaultBucketSize() throws Exception {
		corpus.setDistinctTerms(25);

		// min = 1, max = 25: ⌊(25 − 1) / 5⌋ + 1 = 5 cubetas.
		mockMvc.perform(get("/api/ranking/trace/bucket"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.algorithm").value("bucket"))
				.andExpect(jsonPath("$.bucketSize").value(5))
				.andExpect(jsonPath("$.ranking", hasSize(25)))
				.andExpect(jsonPath("$.trace.min").value(1))
				.andExpect(jsonPath("$.trace.max").value(25))
				.andExpect(jsonPath("$.trace.bucketSize").value(5))
				.andExpect(jsonPath("$.trace.bucketCount").value(5))
				.andExpect(jsonPath("$.trace.buckets", hasSize(5)))
				.andExpect(jsonPath("$.trace.concatenated", hasSize(25)));
	}

	@Test
	@DisplayName("AC-25: la traza de Bucket Sort con bucketSize = 10 tiene ⌊(max − min)/10⌋ + 1 cubetas")
	void bucketTraceUsesRequestedBucketSize() throws Exception {
		corpus.setDistinctTerms(25);

		// ⌊(25 − 1) / 10⌋ + 1 = 3 cubetas: [1..10], [11..20], [21..25].
		mockMvc.perform(get("/api/ranking/trace/bucket").param("bucketSize", "10"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.bucketSize").value(10))
				.andExpect(jsonPath("$.trace.bucketSize").value(10))
				.andExpect(jsonPath("$.trace.bucketCount").value(3))
				.andExpect(jsonPath("$.trace.buckets", hasSize(3)))
				.andExpect(jsonPath("$.trace.buckets[0].after", hasSize(10)))
				.andExpect(jsonPath("$.trace.buckets[1].after", hasSize(10)))
				.andExpect(jsonPath("$.trace.buckets[2].after", hasSize(5)));
	}

	@ParameterizedTest(name = "{0} con bucketSize={1}")
	@CsvSource({"counting, 0", "counting, abc", "radix, 0", "radix, abc", "bucket, 0", "bucket, abc"})
	@DisplayName("AC-25: la traza con bucketSize = 0 o abc da 400 con mensaje en español")
	void invalidTraceBucketSizeIsRejected(String algorithm, String value) throws Exception {
		corpus.setDistinctTerms(10);

		mockMvc.perform(get("/api/ranking/trace/" + algorithm).param("bucketSize", value))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value("El parámetro bucketSize debe ser un entero mayor o igual a 1"));
	}

	@ParameterizedTest(name = "{0}")
	@ValueSource(strings = {"counting", "radix"})
	@DisplayName("AC-25: en Counting y Radix bucketSize no cambia la traza")
	void bucketSizeDoesNotChangeCountingOrRadixTrace(String algorithm) throws Exception {
		corpus.setDistinctTerms(25);

		Object withDefault = JsonPath.read(traceBody(algorithm, "5"), "$.trace");
		Object withTen = JsonPath.read(traceBody(algorithm, "10"), "$.trace");

		assertThat(withTen).isEqualTo(withDefault);
	}

	@Test
	@DisplayName("Un algoritmo desconocido da 400 con los valores válidos en español")
	void unknownAlgorithmIsRejected() throws Exception {
		mockMvc.perform(get("/api/ranking/trace/quick"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail")
						.value("Algoritmo desconocido: quick. Valores válidos: counting, radix, bucket"));
	}

	/** Cuerpo JSON de la traza de un algoritmo con el {@code bucketSize} dado. */
	private String traceBody(String algorithm, String bucketSize) throws Exception {
		return mockMvc.perform(get("/api/ranking/trace/" + algorithm).param("bucketSize", bucketSize))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
	}

	/** Tokens de un abstract a partir de pares (término, repeticiones). */
	private static List<String> tokens(Object... termsAndCounts) {
		List<String> tokens = new ArrayList<>();
		for (int i = 0; i < termsAndCounts.length; i += 2) {
			tokens.addAll(Collections.nCopies((Integer) termsAndCounts[i + 1], (String) termsAndCounts[i]));
		}
		return tokens;
	}

	/** {@link TermSorter} espía que registra las opciones recibidas y delega en el algoritmo real. */
	private static final class SpySorter implements TermSorter<Object> {

		private final TermSorter<?> delegate;
		private final List<SortOptions> received = new ArrayList<>();

		SpySorter(TermSorter<?> delegate) {
			this.delegate = delegate;
		}

		@Override
		public SortAlgorithm algorithm() {
			return delegate.algorithm();
		}

		@Override
		public SortResult<Object> sort(List<TermFrequency> input, SortOptions options) {
			received.add(options);
			SortResult<?> result = delegate.sort(input, options);
			return new SortResult<>(result.output(), result.trace());
		}

	}

}
