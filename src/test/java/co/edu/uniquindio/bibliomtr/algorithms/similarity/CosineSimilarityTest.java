package co.edu.uniquindio.bibliomtr.algorithms.similarity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import co.edu.uniquindio.bibliomtr.trace.similarity.CosineTrace;

/**
 * Tests de estudiante de {@link CosineSimilarity} (spec 001, REQ-5, AC-6 y AC-7; constitución P10).
 *
 * <p>Los valores esperados están calculados a mano con {@code cos = Σ aᵢbᵢ / (‖a‖·‖b‖)} y la normalización
 * sugerida {@code similarity = (1 + cos) / 2}; coinciden con la tabla de {@code docs/guia-implementacion.md}.
 * Fallan hasta que se implemente el algoritmo: {@code ./gradlew studentTest --tests CosineSimilarityTest}.
 */
@Tag("student")
class CosineSimilarityTest {

	private static final double EPSILON = 1e-6;
	private static final String DIMENSION_MESSAGE = "Los vectores deben tener la misma dimensión mayor que 0";

	private final CosineSimilarity cosine = new CosineSimilarity();

	@Test
	@DisplayName("AC-7: (1,0) vs (1,1): cos = 1/√2 ≈ 0.707107 y similitud ≈ 0.853553")
	void handCase() {
		SimilarityResult<CosineTrace> result = cosine.compare(new float[] {1, 0}, new float[] {1, 1});

		assertThat(result.value()).isCloseTo(0.707107, within(EPSILON));
		assertThat(result.similarity()).isCloseTo(0.853553, within(EPSILON));
		CosineTrace trace = result.trace();
		assertThat(trace.dimension()).isEqualTo(2);
		assertThat(trace.products()).containsExactly(new double[] {1.0, 0.0}, within(EPSILON));
		assertThat(trace.dotProduct()).isCloseTo(1.0, within(EPSILON));
		assertThat(trace.normA()).isCloseTo(1.0, within(EPSILON));
		assertThat(trace.normB()).isCloseTo(Math.sqrt(2.0), within(EPSILON));
	}

	@Test
	@DisplayName("Borde: vectores idénticos dan cos 1 y similitud 1")
	void identical() {
		SimilarityResult<CosineTrace> result = cosine.compare(new float[] {1, 2, 3}, new float[] {1, 2, 3});

		assertThat(result.value()).isCloseTo(1.0, within(EPSILON));
		assertThat(result.similarity()).isCloseTo(1.0, within(EPSILON));
	}

	@Test
	@DisplayName("Borde: opuestos (1,0) vs (−1,0) dan cos −1 y similitud 0")
	void opposite() {
		SimilarityResult<CosineTrace> result = cosine.compare(new float[] {1, 0}, new float[] {-1, 0});

		assertThat(result.value()).isCloseTo(-1.0, within(EPSILON));
		assertThat(result.similarity()).isCloseTo(0.0, within(EPSILON));
	}

	@Test
	@DisplayName("Borde: ortogonales (1,0) vs (0,1) dan cos 0 y similitud 0.5")
	void orthogonal() {
		SimilarityResult<CosineTrace> result = cosine.compare(new float[] {1, 0}, new float[] {0, 1});

		assertThat(result.value()).isCloseTo(0.0, within(EPSILON));
		assertThat(result.similarity()).isCloseTo(0.5, within(EPSILON));
	}

	@Test
	@DisplayName("Borde: dimensiones distintas o 0 lanzan IllegalArgumentException")
	void differentDimensions() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> cosine.compare(new float[] {1, 0}, new float[] {1, 0, 0}))
				.withMessage(DIMENSION_MESSAGE);
		assertThatIllegalArgumentException()
				.isThrownBy(() -> cosine.compare(new float[0], new float[0]))
				.withMessage(DIMENSION_MESSAGE);
	}

}
