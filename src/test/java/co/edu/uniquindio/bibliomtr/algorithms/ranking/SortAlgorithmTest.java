package co.edu.uniquindio.bibliomtr.algorithms.ranking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests de {@link SortAlgorithm}: slugs, complejidades y parámetros propios k; d y b; m (REQ-22, AC-13).
 */
class SortAlgorithmTest {

	/** Construye pares con términos t0, t1, … para las frecuencias dadas. */
	static List<TermFrequency> frequencies(int... values) {
		List<TermFrequency> list = new ArrayList<>();
		for (int i = 0; i < values.length; i++) {
			list.add(new TermFrequency("t" + i, values[i]));
		}
		return list;
	}

	@Test
	@DisplayName("Counting: k es la frecuencia máxima; el resto no aplica")
	void countingParameters() {
		assertThat(SortAlgorithm.COUNTING.parameters(frequencies(4, 1, 3, 1), 5))
				.isEqualTo(new AlgorithmParameters(4, null, null, null));
	}

	@Test
	@DisplayName("Radix: d son los dígitos del máximo y b = 10")
	void radixParameters() {
		assertThat(SortAlgorithm.RADIX.parameters(frequencies(170, 45, 75, 90, 802, 24, 2, 66), 5))
				.isEqualTo(new AlgorithmParameters(null, 3, 10, null));
		assertThat(SortAlgorithm.RADIX.parameters(frequencies(9), 5).d()).isEqualTo(1);
		assertThat(SortAlgorithm.RADIX.parameters(frequencies(10), 5).d()).isEqualTo(2);
	}

	@Test
	@DisplayName("Bucket: m = ⌊(max − min)/bucketSize⌋ + 1")
	void bucketParameters() {
		// [5, 3, 8, 3] con bucketSize 2: ⌊(8 − 3)/2⌋ + 1 = 3 (AC-9).
		assertThat(SortAlgorithm.BUCKET.parameters(frequencies(5, 3, 8, 3), 2))
				.isEqualTo(new AlgorithmParameters(null, null, null, 3));
		// Con bucketSize 5: ⌊5/5⌋ + 1 = 2.
		assertThat(SortAlgorithm.BUCKET.parameters(frequencies(5, 3, 8, 3), 5).m()).isEqualTo(2);
	}

	@Test
	@DisplayName("Lista vacía: k, d y m valen 0")
	void emptyListParameters() {
		assertThat(SortAlgorithm.COUNTING.parameters(List.of(), 5)).isEqualTo(new AlgorithmParameters(0, null, null, null));
		assertThat(SortAlgorithm.RADIX.parameters(List.of(), 5)).isEqualTo(new AlgorithmParameters(null, 0, 10, null));
		assertThat(SortAlgorithm.BUCKET.parameters(List.of(), 5)).isEqualTo(new AlgorithmParameters(null, null, null, 0));
	}

	@Test
	@DisplayName("bucketSize menor que 1 se rechaza")
	void rejectsInvalidBucketSize() {
		assertThatThrownBy(() -> SortAlgorithm.BUCKET.parameters(frequencies(1), 0))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	@DisplayName("Slugs y complejidades teóricas")
	void slugsAndComplexities() {
		assertThat(SortAlgorithm.fromSlug("counting")).isEqualTo(SortAlgorithm.COUNTING);
		assertThat(SortAlgorithm.fromSlug("radix")).isEqualTo(SortAlgorithm.RADIX);
		assertThat(SortAlgorithm.fromSlug("bucket")).isEqualTo(SortAlgorithm.BUCKET);
		assertThat(SortAlgorithm.COUNTING.complexity()).isEqualTo("O(n+k)");
		assertThat(SortAlgorithm.RADIX.complexity()).isEqualTo("O(d·(n+b))");
		assertThat(SortAlgorithm.BUCKET.complexity()).isEqualTo("O(n+m) promedio, O(n²) peor caso");
	}

	@Test
	@DisplayName("Un slug desconocido lanza IllegalArgumentException con mensaje en español")
	void rejectsUnknownSlug() {
		assertThatThrownBy(() -> SortAlgorithm.fromSlug("quick"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Algoritmo desconocido: quick. Valores válidos: counting, radix, bucket");
	}

}
