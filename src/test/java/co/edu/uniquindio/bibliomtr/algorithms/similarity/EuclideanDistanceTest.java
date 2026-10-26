package co.edu.uniquindio.bibliomtr.algorithms.similarity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import co.edu.uniquindio.bibliomtr.trace.similarity.EuclideanTrace;

/**
 * Tests de estudiante de {@link EuclideanDistance} (spec 001, REQ-5, AC-6 y AC-7; constitución P10).
 *
 * <p>Los valores esperados están calculados a mano con {@code d = √Σ(aᵢ − bᵢ)²} y la normalización sugerida
 * {@code similarity = 1 / (1 + d)}; coinciden con la tabla de {@code docs/guia-implementacion.md}. Fallan
 * hasta que se implemente el algoritmo: {@code ./gradlew studentTest --tests EuclideanDistanceTest}.
 */
@Tag("student")
class EuclideanDistanceTest {

	private static final double EPSILON = 1e-9;
	private static final String DIMENSION_MESSAGE = "Los vectores deben tener la misma dimensión mayor que 0";

	private final EuclideanDistance euclidean = new EuclideanDistance();

	@Test
	@DisplayName("AC-7: (0,0) vs (3,4): d = 5, similitud 1/6 y traza [9, 16] con suma 25")
	void handCase() {
		SimilarityResult<EuclideanTrace> result = euclidean.compare(new float[] {0, 0}, new float[] {3, 4});

		assertThat(result.value()).isCloseTo(5.0, within(EPSILON));
		assertThat(result.similarity()).isCloseTo(1.0 / 6.0, within(EPSILON));
		EuclideanTrace trace = result.trace();
		assertThat(trace.dimension()).isEqualTo(2);
		assertThat(trace.squaredDifferences()).containsExactly(new double[] {9.0, 16.0}, within(EPSILON));
		assertThat(trace.sumOfSquares()).isCloseTo(25.0, within(EPSILON));
	}

	@Test
	@DisplayName("Borde: vectores idénticos dan d = 0 y similitud 1")
	void identical() {
		SimilarityResult<EuclideanTrace> result = euclidean.compare(new float[] {1, 2, 3}, new float[] {1, 2, 3});

		assertThat(result.value()).isEqualTo(0.0);
		assertThat(result.similarity()).isEqualTo(1.0);
	}

	@Test
	@DisplayName("Borde: dimensiones distintas o 0 lanzan IllegalArgumentException")
	void differentDimensions() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> euclidean.compare(new float[] {0, 0}, new float[] {3}))
				.withMessage(DIMENSION_MESSAGE);
		assertThatIllegalArgumentException()
				.isThrownBy(() -> euclidean.compare(new float[0], new float[0]))
				.withMessage(DIMENSION_MESSAGE);
	}

}
