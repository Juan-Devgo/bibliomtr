package co.edu.uniquindio.bibliomtr.algorithms.similarity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import co.edu.uniquindio.bibliomtr.trace.similarity.NeedlemanWunschTrace;

/**
 * Tests de estudiante de {@link NeedlemanWunsch} con {@link NeedlemanWunschScoring#DEFAULT} = (1, −1, −1)
 * (spec 001, REQ-5, AC-6 y AC-7; constitución P10).
 *
 * <p>Los valores esperados están calculados a mano con la recurrencia y la normalización sugerida del Javadoc
 * de {@link NeedlemanWunsch}: {@code maxScore = 2 · min(m, n) − max(m, n)}, {@code minScore = −max(m, n)} y
 * {@code similarity = (score − minScore) / (maxScore − minScore)}; coinciden con la tabla de
 * {@code docs/guia-implementacion.md}. Fallan hasta que se implemente el algoritmo:
 * {@code ./gradlew studentTest --tests NeedlemanWunschTest}.
 */
@Tag("student")
class NeedlemanWunschTest {

	private static final double EPSILON = 1e-9;

	private final NeedlemanWunsch needlemanWunsch = new NeedlemanWunsch(NeedlemanWunschScoring.DEFAULT);

	@Test
	@DisplayName("AC-7: g,a,t,o vs p,a,t,o: score 2, maxScore 4, minScore −4 y similitud 0.75")
	void gatoPato() {
		SimilarityResult<NeedlemanWunschTrace> result =
				needlemanWunsch.compare(List.of("g", "a", "t", "o"), List.of("p", "a", "t", "o"));

		assertThat(result.value()).isEqualTo(2.0);
		assertThat(result.similarity()).isCloseTo(0.75, within(EPSILON));
		NeedlemanWunschTrace trace = result.trace();
		assertThat(trace.score()).isEqualTo(2);
		assertThat(trace.maxScore()).isEqualTo(4);
		assertThat(trace.minScore()).isEqualTo(-4);
		assertThat(trace.scoring()).isEqualTo(NeedlemanWunschScoring.DEFAULT);
		// F y la matriz de movimientos son de (m + 1) × (n + 1), y F[m][n] es el puntaje.
		assertThat(trace.scores()).hasDimensions(5, 5);
		assertThat(trace.scores()[4][4]).isEqualTo(2);
		assertThat(trace.moves()).hasDimensions(5, 5);
	}

	@Test
	@DisplayName("Borde: disjuntas [a,b,c] vs [x,y,z] dan score −3 y similitud 0")
	void disjoint() {
		SimilarityResult<NeedlemanWunschTrace> result =
				needlemanWunsch.compare(List.of("a", "b", "c"), List.of("x", "y", "z"));

		assertThat(result.value()).isEqualTo(-3.0);
		assertThat(result.similarity()).isEqualTo(0.0);
		assertThat(result.trace().maxScore()).isEqualTo(3);
		assertThat(result.trace().minScore()).isEqualTo(-3);
	}

	@Test
	@DisplayName("Borde: ambas vacías dan score 0 y similitud 1")
	void bothEmpty() {
		SimilarityResult<NeedlemanWunschTrace> result = needlemanWunsch.compare(List.of(), List.of());

		assertThat(result.value()).isEqualTo(0.0);
		assertThat(result.similarity()).isEqualTo(1.0);
	}

	@Test
	@DisplayName("Borde: solo una vacía da score = longitud · gap y similitud 0, en ambos órdenes")
	void oneEmpty() {
		SimilarityResult<NeedlemanWunschTrace> result = needlemanWunsch.compare(List.of(), List.of("a", "b", "c"));
		SimilarityResult<NeedlemanWunschTrace> reversed = needlemanWunsch.compare(List.of("a", "b", "c"), List.of());

		assertThat(result.value()).isEqualTo(-3.0);
		assertThat(result.similarity()).isEqualTo(0.0);
		assertThat(reversed.value()).isEqualTo(-3.0);
		assertThat(reversed.similarity()).isEqualTo(0.0);
	}

	@Test
	@DisplayName("Borde: secuencias idénticas dan score = m y similitud 1")
	void identical() {
		SimilarityResult<NeedlemanWunschTrace> result =
				needlemanWunsch.compare(List.of("a", "b", "c"), List.of("a", "b", "c"));

		assertThat(result.value()).isEqualTo(3.0);
		assertThat(result.similarity()).isEqualTo(1.0);
	}

	@Test
	@DisplayName("Borde: [a,b] vs [a, 9×x]: score −8, maxScore −6, minScore −10 y similitud 0.5")
	void veryDifferentLengths() {
		List<String> b = new ArrayList<>();
		b.add("a");
		b.addAll(Collections.nCopies(9, "x"));
		SimilarityResult<NeedlemanWunschTrace> result = needlemanWunsch.compare(List.of("a", "b"), b);

		assertThat(result.value()).isEqualTo(-8.0);
		assertThat(result.similarity()).isCloseTo(0.5, within(EPSILON));
		assertThat(result.trace().score()).isEqualTo(-8);
		assertThat(result.trace().maxScore()).isEqualTo(-6);
		assertThat(result.trace().minScore()).isEqualTo(-10);
	}

}
