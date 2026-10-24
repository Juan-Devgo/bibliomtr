package co.edu.uniquindio.bibliomtr.algorithms.similarity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import co.edu.uniquindio.bibliomtr.trace.similarity.LevenshteinTrace;

/**
 * Tests de estudiante de {@link Levenshtein} (spec 001, REQ-5, AC-6 y AC-7; constitución P10).
 *
 * <p>Los valores esperados están calculados a mano con la recurrencia y la normalización sugerida del Javadoc
 * de {@link Levenshtein} ({@code similarity = 1 − D[m][n] / max(m, n)}) y coinciden con la tabla de
 * {@code docs/guia-implementacion.md}. Los casos valen a nivel de secuencia de símbolos, sin importar la
 * granularidad. Fallan hasta que se implemente el algoritmo: {@code ./gradlew studentTest --tests LevenshteinTest}.
 */
@Tag("student")
class LevenshteinTest {

	private static final double EPSILON = 1e-9;

	private final Levenshtein levenshtein = new Levenshtein();

	@Test
	@DisplayName("AC-7: g,a,t,o vs p,a,t,o: distancia 1, similitud 0.75 y matriz D de 5×5")
	void gatoPato() {
		SimilarityResult<LevenshteinTrace> result =
				levenshtein.compare(List.of("g", "a", "t", "o"), List.of("p", "a", "t", "o"));

		assertThat(result.value()).isEqualTo(1.0);
		assertThat(result.similarity()).isCloseTo(0.75, within(EPSILON));
		LevenshteinTrace trace = result.trace();
		assertThat(trace.a()).containsExactly("g", "a", "t", "o");
		assertThat(trace.b()).containsExactly("p", "a", "t", "o");
		assertThat(trace.distance()).isEqualTo(1);
		assertThat(trace.maxLength()).isEqualTo(4);
		// D[i][j] = distancia entre los prefijos de longitud i y j.
		assertThat(trace.matrix()).isDeepEqualTo(new int[][] {
				{0, 1, 2, 3, 4},
				{1, 1, 2, 3, 4},
				{2, 2, 1, 2, 3},
				{3, 3, 2, 1, 2},
				{4, 4, 3, 2, 1}});
	}

	@Test
	@DisplayName("Borde: ambas vacías dan distancia 0, similitud 1 y matriz 1×1")
	void bothEmpty() {
		SimilarityResult<LevenshteinTrace> result = levenshtein.compare(List.of(), List.of());

		assertThat(result.value()).isEqualTo(0.0);
		assertThat(result.similarity()).isEqualTo(1.0);
		assertThat(result.trace().matrix()).isDeepEqualTo(new int[][] {{0}});
	}

	@Test
	@DisplayName("Borde: [] vs [a,b,c] da distancia 3 y similitud 0, en ambos órdenes")
	void oneEmpty() {
		SimilarityResult<LevenshteinTrace> result = levenshtein.compare(List.of(), List.of("a", "b", "c"));
		SimilarityResult<LevenshteinTrace> reversed = levenshtein.compare(List.of("a", "b", "c"), List.of());

		assertThat(result.value()).isEqualTo(3.0);
		assertThat(result.similarity()).isEqualTo(0.0);
		assertThat(result.trace().matrix()).isDeepEqualTo(new int[][] {{0, 1, 2, 3}});
		assertThat(reversed.value()).isEqualTo(3.0);
		assertThat(reversed.similarity()).isEqualTo(0.0);
	}

	@Test
	@DisplayName("Borde: secuencias idénticas dan distancia 0 y similitud 1")
	void identical() {
		List<String> tokens = List.of("machine", "learning", "education");
		SimilarityResult<LevenshteinTrace> result = levenshtein.compare(tokens, tokens);

		assertThat(result.value()).isEqualTo(0.0);
		assertThat(result.similarity()).isEqualTo(1.0);
	}

	@Test
	@DisplayName("Borde: disjuntas [a,b,c] vs [x,y,z] dan distancia 3 y similitud 0")
	void disjoint() {
		SimilarityResult<LevenshteinTrace> result = levenshtein.compare(List.of("a", "b", "c"), List.of("x", "y", "z"));

		assertThat(result.value()).isEqualTo(3.0);
		assertThat(result.similarity()).isEqualTo(0.0);
	}

	@Test
	@DisplayName("Borde: longitudes muy distintas [a] vs 50×a dan distancia 49 y similitud 0.02")
	void veryDifferentLengths() {
		SimilarityResult<LevenshteinTrace> result = levenshtein.compare(List.of("a"), Collections.nCopies(50, "a"));

		assertThat(result.value()).isEqualTo(49.0);
		assertThat(result.similarity()).isCloseTo(0.02, within(EPSILON));
		assertThat(result.trace().maxLength()).isEqualTo(50);
	}

}
