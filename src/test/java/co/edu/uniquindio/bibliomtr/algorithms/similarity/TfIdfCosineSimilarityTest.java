package co.edu.uniquindio.bibliomtr.algorithms.similarity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import co.edu.uniquindio.bibliomtr.trace.similarity.TfIdfCosineTrace;

/**
 * Tests de estudiante de {@link TfIdfCosineSimilarity} (spec 001, REQ-5, AC-6 y AC-7; constitución P10).
 *
 * <p><b>Caso a mano.</b> Corpus d1 = [a,b], d2 = [b,c], d3 = [c,d]; N = 3, df(a) = 1, df(b) = 2, df(c) = 2.
 * Entre d1 y d2 el vocabulario es {a, b, c}, con idf = (ln 3, ln 1.5, ln 1.5), tf₁ = (0.5, 0.5, 0) y
 * tf₂ = (0, 0.5, 0.5). Así w₁ = (0.549306, 0.202733, 0), w₂ = (0, 0.202733, 0.202733), producto punto
 * 0.041100, ‖w₁‖ = 0.585523, ‖w₂‖ = 0.286707 y cos ≈ 0.244830. Coincide con la tabla de
 * {@code docs/guia-implementacion.md}.
 *
 * <p>El orden del vocabulario no está fijado por el contrato: la traza se revisa buscando la posición de cada
 * término. Fallan hasta que se implemente el algoritmo:
 * {@code ./gradlew studentTest --tests TfIdfCosineSimilarityTest}.
 */
@Tag("student")
class TfIdfCosineSimilarityTest {

	private static final double EPSILON = 1e-6;

	private static final List<String> D1 = List.of("a", "b");
	private static final List<String> D2 = List.of("b", "c");
	private static final List<String> D3 = List.of("c", "d");
	private static final List<List<String>> CORPUS = List.of(D1, D2, D3);

	private final TfIdfCosineSimilarity tfIdf = new TfIdfCosineSimilarity();

	@Test
	@DisplayName("AC-7: d1 vs d2 con corpus [[a,b],[b,c],[c,d]] ≈ 0.244830")
	void handCase() {
		SimilarityResult<TfIdfCosineTrace> result = tfIdf.compare(D1, D2, CORPUS);

		assertThat(result.value()).isCloseTo(0.244830, within(EPSILON));
		assertThat(result.similarity()).isCloseTo(0.244830, within(EPSILON));
	}

	@Test
	@DisplayName("AC-7: la traza de d1 vs d2 trae N, df, idf, tf, pesos, producto punto y normas")
	void handCaseTrace() {
		TfIdfCosineTrace trace = tfIdf.compare(D1, D2, CORPUS).trace();

		assertThat(trace.vocabulary()).containsExactlyInAnyOrder("a", "b", "c");
		assertThat(trace.documentCount()).isEqualTo(3);
		int a = trace.vocabulary().indexOf("a");
		int b = trace.vocabulary().indexOf("b");
		int c = trace.vocabulary().indexOf("c");
		assertThat(trace.documentFrequency()[a]).isEqualTo(1);
		assertThat(trace.documentFrequency()[b]).isEqualTo(2);
		assertThat(trace.documentFrequency()[c]).isEqualTo(2);
		assertThat(trace.idf()[a]).isCloseTo(Math.log(3.0), within(EPSILON));
		assertThat(trace.idf()[b]).isCloseTo(Math.log(1.5), within(EPSILON));
		assertThat(trace.tfA()[a]).isCloseTo(0.5, within(EPSILON));
		assertThat(trace.tfA()[c]).isCloseTo(0.0, within(EPSILON));
		assertThat(trace.tfB()[c]).isCloseTo(0.5, within(EPSILON));
		assertThat(trace.weightsA()[a]).isCloseTo(0.549306, within(EPSILON));
		assertThat(trace.weightsB()[b]).isCloseTo(0.202733, within(EPSILON));
		assertThat(trace.dotProduct()).isCloseTo(0.041100, within(EPSILON));
		assertThat(trace.normA()).isCloseTo(0.585523, within(EPSILON));
		assertThat(trace.normB()).isCloseTo(0.286707, within(EPSILON));
	}

	@Test
	@DisplayName("Borde: idénticas, d1 vs d1 = 1")
	void identical() {
		assertThat(tfIdf.compare(D1, D1, CORPUS).similarity()).isCloseTo(1.0, within(EPSILON));
	}

	@Test
	@DisplayName("Borde: disjuntas, d1 vs d3 = 0")
	void disjoint() {
		assertThat(tfIdf.compare(D1, D3, CORPUS).similarity()).isCloseTo(0.0, within(EPSILON));
	}

	@Test
	@DisplayName("Borde: ambas vacías dan 1")
	void bothEmpty() {
		List<List<String>> corpus = List.of(List.of(), List.of("a", "b"));
		SimilarityResult<TfIdfCosineTrace> result = tfIdf.compare(List.of(), List.of(), corpus);

		assertThat(result.value()).isEqualTo(1.0);
		assertThat(result.similarity()).isEqualTo(1.0);
	}

	@Test
	@DisplayName("Borde: una vacía tiene norma 0 y da 0")
	void oneEmpty() {
		List<List<String>> corpus = List.of(List.of(), List.of("a", "b"));
		assertThat(tfIdf.compare(List.of(), List.of("a", "b"), corpus).similarity()).isEqualTo(0.0);
	}

	@Test
	@DisplayName("Borde: longitudes muy distintas con las mismas proporciones, [a,b] vs 5×[a,b], dan 1")
	void veryDifferentLengths() {
		List<String> longer = List.of("a", "b", "a", "b", "a", "b", "a", "b", "a", "b");
		List<List<String>> corpus = List.of(D1, longer, D3);
		// tf es igual en los dos documentos (0.5 y 0.5), así que los pesos son proporcionales.
		assertThat(tfIdf.compare(D1, longer, corpus).similarity()).isCloseTo(1.0, within(EPSILON));
	}

}
