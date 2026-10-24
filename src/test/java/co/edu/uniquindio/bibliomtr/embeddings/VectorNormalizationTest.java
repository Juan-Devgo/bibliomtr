package co.edu.uniquindio.bibliomtr.embeddings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import java.util.Random;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests de {@link VectorNormalization} (spec 001, REQ-9 y AC-8a): un caso a mano, norma 1 y el vector cero.
 */
class VectorNormalizationTest {

	@Test
	@DisplayName("(3, 4) tiene norma 5 y queda (0.6, 0.8) sin modificar la entrada")
	void normalizesHandComputedVector() {
		float[] v = {3f, 4f};

		float[] normalized = VectorNormalization.l2Normalize(v);

		assertThat(normalized[0]).isCloseTo(0.6f, within(1e-6f));
		assertThat(normalized[1]).isCloseTo(0.8f, within(1e-6f));
		assertThat(v).containsExactly(3f, 4f);
	}

	@Test
	@DisplayName("AC-8a: un vector de 768 componentes queda con norma L2 igual a 1")
	void resultHasUnitNorm() {
		Random random = new Random(42);
		float[] v = new float[768];
		for (int i = 0; i < v.length; i++) {
			v[i] = (float) (random.nextGaussian() * 10);
		}

		assertThat(norm(VectorNormalization.l2Normalize(v))).isCloseTo(1.0, within(1e-5));
	}

	@Test
	@DisplayName("Un vector ya unitario no cambia")
	void unitVectorIsUnchanged() {
		assertThat(VectorNormalization.l2Normalize(new float[] {0f, 1f, 0f})).containsExactly(0f, 1f, 0f);
	}

	@Test
	@DisplayName("El vector cero y el vector vacío lanzan IllegalArgumentException")
	void zeroVectorIsRejected() {
		assertThatThrownBy(() -> VectorNormalization.l2Normalize(new float[] {0f, 0f}))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("norma 0");
		assertThatThrownBy(() -> VectorNormalization.l2Normalize(new float[0]))
				.isInstanceOf(IllegalArgumentException.class);
	}

	private static double norm(float[] v) {
		double sum = 0;
		for (float x : v) {
			sum += (double) x * x;
		}
		return Math.sqrt(sum);
	}
}
