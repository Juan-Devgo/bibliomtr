package co.edu.uniquindio.bibliomtr.algorithms.ranking;

import static co.edu.uniquindio.bibliomtr.algorithms.ranking.SortEdgeCases.tf;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import co.edu.uniquindio.bibliomtr.trace.ranking.CountingSortTrace;
import co.edu.uniquindio.bibliomtr.trace.ranking.CountingSortTrace.Placement;

/**
 * Tests de {@link CountingSort}: caso a mano de AC-10, estabilidad, casos de borde y lista vacía
 * (REQ-6, REQ-13, REQ-24, AC-10, AC-17, AC-18).
 */
class CountingSortTest {

	private static final SortOptions WITH_TRACE = new SortOptions(SortOptions.DEFAULT_BUCKET_SIZE, true);
	private static final SortOptions WITHOUT_TRACE = new SortOptions(SortOptions.DEFAULT_BUCKET_SIZE, false);

	private final CountingSort sorter = new CountingSort();

	@Test
	@DisplayName("AC-10: [4, 1, 3, 1] da conteo [0,2,0,1,1] y acumulado [0,2,2,3,4]")
	void handCase() {
		List<TermFrequency> input = List.of(tf("t0", 4), tf("t1", 1), tf("t2", 3), tf("t3", 1));

		SortResult<CountingSortTrace> result = sorter.sort(input, WITH_TRACE);
		CountingSortTrace trace = result.trace();

		assertThat(trace.input()).isEqualTo(input);
		assertThat(trace.max()).isEqualTo(4);
		assertThat(trace.count()).containsExactly(0, 2, 0, 1, 1);
		assertThat(trace.cumulative()).containsExactly(0, 2, 2, 3, 4);
		// Recorrido de derecha a izquierda: índice de salida C[v] − 1 y luego C[v]−−.
		assertThat(trace.placements()).containsExactly(
				new Placement(3, tf("t3", 1), 1),
				new Placement(2, tf("t2", 3), 2),
				new Placement(1, tf("t1", 1), 0),
				new Placement(0, tf("t0", 4), 3));
		// Estable: t1 y t3 (frecuencia 1) conservan su orden de entrada.
		assertThat(result.output()).containsExactly(tf("t1", 1), tf("t3", 1), tf("t2", 3), tf("t0", 4));
		assertThat(trace.output()).isEqualTo(result.output());
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("co.edu.uniquindio.bibliomtr.algorithms.ranking.SortEdgeCases#cases")
	@DisplayName("AC-17: casos de borde")
	void edgeCases(String name, List<TermFrequency> input, List<TermFrequency> expected) {
		assertThat(sorter.sort(input, WITHOUT_TRACE).output()).containsExactlyElementsOf(expected);
		assertThat(sorter.sort(input, WITH_TRACE).output()).containsExactlyElementsOf(expected);
	}

	@Test
	@DisplayName("k ≫ n: el arreglo de conteo tiene tamaño k + 1")
	void countArraySizeIsMaxPlusOne() {
		CountingSortTrace trace = sorter.sort(List.of(tf("a", 1), tf("b", 1000)), WITH_TRACE).trace();

		assertThat(trace.count()).hasSize(1001);
		assertThat(trace.cumulative()[1000]).isEqualTo(2);
	}

	@Test
	@DisplayName("AC-18: lista vacía devuelve lista vacía sin excepción")
	void emptyList() {
		SortResult<CountingSortTrace> result = sorter.sort(List.of(), WITH_TRACE);

		assertThat(result.output()).isEmpty();
		assertThat(result.trace().input()).isEmpty();
		assertThat(result.trace().max()).isZero();
		assertThat(result.trace().count()).isEmpty();
		assertThat(result.trace().cumulative()).isEmpty();
		assertThat(result.trace().placements()).isEmpty();
		assertThat(result.trace().output()).isEmpty();
		assertThat(sorter.sort(List.of(), WITHOUT_TRACE).output()).isEmpty();
	}

	@Test
	@DisplayName("Sin traza devuelve null en la traza y el mismo resultado; no modifica la entrada")
	void withoutTraceAndInputUnchanged() {
		List<TermFrequency> input = new ArrayList<>(List.of(tf("a", 2), tf("b", 1), tf("c", 2)));
		List<TermFrequency> copy = List.copyOf(input);

		SortResult<CountingSortTrace> withoutTrace = sorter.sort(input, WITHOUT_TRACE);

		assertThat(withoutTrace.trace()).isNull();
		assertThat(withoutTrace.output()).isEqualTo(sorter.sort(input, WITH_TRACE).output());
		assertThat(input).isEqualTo(copy);
		assertThat(sorter.algorithm()).isEqualTo(SortAlgorithm.COUNTING);
	}

}
