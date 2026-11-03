package co.edu.uniquindio.bibliomtr.algorithms.clustering;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests de {@link DendrogramValidator} (spec 003, REQ-7, AC-10): acepta un dendrograma bien formado, recorta las
 * alturas dentro de la tolerancia y rechaza con mensaje en español cada regla incumplida.
 *
 * <p>Complejidad: O(N) por caso.
 */
class DendrogramValidatorTest {

	private static final String PREFIX = "El algoritmo single-linkage devolvió un dendrograma inválido: ";

	/** Fusiones de Single Linkage sobre D₄ (guía, § 10). */
	private static final List<Merge> SINGLE_D4 = List.of(
			new Merge(0, 1, 0.2, 2), new Merge(2, 4, 0.3, 3), new Merge(3, 5, 0.45, 4));

	@Test
	@DisplayName("Un dendrograma bien formado se devuelve igual")
	void validDendrogramIsReturned() {
		assertThat(DendrogramValidator.validate("single-linkage", 4, SINGLE_D4)).isEqualTo(SINGLE_D4);
	}

	@Test
	@DisplayName("Una altura 1 + 1e-12 o −1e-12 se recorta a [0,1]")
	void heightWithinToleranceIsClamped() {
		List<Merge> merges = List.of(new Merge(0, 1, -1e-12, 2), new Merge(2, 3, 1 + 1e-12, 3));

		assertThat(DendrogramValidator.validate("single-linkage", 3, merges))
				.containsExactly(new Merge(0, 1, 0.0, 2), new Merge(2, 3, 1.0, 3));
	}

	@Test
	@DisplayName("AC-10: 3 fusiones para 3 artículos se rechazan")
	void wrongMergeCountIsRejected() {
		List<Merge> merges = List.of(new Merge(0, 1, 0.1, 2), new Merge(2, 3, 0.2, 3), new Merge(0, 4, 0.3, 4));

		assertThatThrownBy(() -> DendrogramValidator.validate("single-linkage", 3, merges))
				.isInstanceOf(InvalidDendrogramException.class)
				.hasMessage(PREFIX + "3 fusiones para 3 artículos; se esperaban 2");
	}

	@Test
	@DisplayName("AC-10: una lista nula se rechaza")
	void nullMergesAreRejected() {
		assertThatThrownBy(() -> DendrogramValidator.validate("single-linkage", 2, null))
				.isInstanceOf(InvalidDendrogramException.class)
				.hasMessage(PREFIX + "0 fusiones para 2 artículos; se esperaban 1");
	}

	@Test
	@DisplayName("AC-10: un grupo fusionado dos veces se rechaza")
	void groupMergedTwiceIsRejected() {
		List<Merge> merges = List.of(new Merge(0, 1, 0.1, 2), new Merge(0, 3, 0.2, 3), new Merge(2, 4, 0.3, 4));

		assertThatThrownBy(() -> DendrogramValidator.validate("single-linkage", 4, merges))
				.isInstanceOf(InvalidDendrogramException.class)
				.hasMessage(PREFIX + "el grupo 0 se fusiona dos veces");
	}

	@Test
	@DisplayName("AC-10: left ≥ right o un grupo aún no creado se rechazan")
	void groupOutOfOrderIsRejected() {
		assertThatThrownBy(() -> DendrogramValidator.validate("single-linkage", 3,
				List.of(new Merge(1, 0, 0.1, 2), new Merge(2, 3, 0.2, 3))))
				.isInstanceOf(InvalidDendrogramException.class)
				.hasMessage(PREFIX + "la fusión 0 usa un grupo inexistente o fuera de orden (1, 0)");
		assertThatThrownBy(() -> DendrogramValidator.validate("single-linkage", 3,
				List.of(new Merge(0, 3, 0.1, 2), new Merge(1, 2, 0.2, 3))))
				.isInstanceOf(InvalidDendrogramException.class)
				.hasMessage(PREFIX + "la fusión 0 usa un grupo inexistente o fuera de orden (0, 3)");
	}

	@Test
	@DisplayName("AC-10: un tamaño que no es la suma de los hijos se rechaza")
	void wrongSizeIsRejected() {
		List<Merge> merges = List.of(new Merge(0, 1, 0.1, 2), new Merge(2, 3, 0.2, 2));

		assertThatThrownBy(() -> DendrogramValidator.validate("single-linkage", 3, merges))
				.isInstanceOf(InvalidDendrogramException.class)
				.hasMessage(PREFIX + "la fusión 1 tiene tamaño 2; se esperaba 3");
	}

	@Test
	@DisplayName("AC-10: una altura de 1.5 o NaN se rechaza")
	void heightOutOfRangeIsRejected() {
		assertThatThrownBy(() -> DendrogramValidator.validate("single-linkage", 2, List.of(new Merge(0, 1, 1.5, 2))))
				.isInstanceOf(InvalidDendrogramException.class)
				.hasMessage(PREFIX + "la fusión 0 tiene altura 1.5 fuera de [0,1]");
		assertThatThrownBy(() -> DendrogramValidator.validate("single-linkage", 2,
				List.of(new Merge(0, 1, Double.NaN, 2))))
				.isInstanceOf(InvalidDendrogramException.class)
				.hasMessage(PREFIX + "la fusión 0 tiene altura NaN fuera de [0,1]");
	}

}
