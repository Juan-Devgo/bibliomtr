package co.edu.uniquindio.bibliomtr.algorithms.ranking;

import static co.edu.uniquindio.bibliomtr.algorithms.ranking.SortEdgeCases.tf;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import co.edu.uniquindio.bibliomtr.trace.ranking.RadixSortTrace;
import co.edu.uniquindio.bibliomtr.trace.ranking.RadixSortTrace.Pass;

/**
 * Tests de {@link RadixSort}: caso a mano de AC-11, casos de borde y lista vacía
 * (REQ-7, REQ-14, REQ-24, AC-11, AC-17, AC-18).
 */
class RadixSortTest {

	private static final SortOptions WITH_TRACE = new SortOptions(SortOptions.DEFAULT_BUCKET_SIZE, true);
	private static final SortOptions WITHOUT_TRACE = new SortOptions(SortOptions.DEFAULT_BUCKET_SIZE, false);

	private final RadixSort sorter = new RadixSort();

	/** Pares t0, t1, … con las frecuencias dadas. */
	private static List<TermFrequency> frequencies(int... values) {
		List<TermFrequency> list = new ArrayList<>();
		for (int i = 0; i < values.length; i++) {
			list.add(tf("t" + i, values[i]));
		}
		return list;
	}

	/** Frecuencias de una lista, en orden. */
	private static List<Integer> keys(List<TermFrequency> list) {
		return list.stream().map(TermFrequency::frequency).toList();
	}

	@Test
	@DisplayName("AC-11: [170,45,75,90,802,24,2,66] da 3 pasadas y tras exp = 1 [170,90,802,2,24,45,75,66]")
	void handCase() {
		List<TermFrequency> input = frequencies(170, 45, 75, 90, 802, 24, 2, 66);

		SortResult<RadixSortTrace> result = sorter.sort(input, WITH_TRACE);
		RadixSortTrace trace = result.trace();

		assertThat(trace.input()).isEqualTo(input);
		assertThat(trace.max()).isEqualTo(802);
		assertThat(trace.passes()).extracting(Pass::exp).containsExactly(1, 10, 100);

		// Pasada exp = 1: dígito ⌊v/1⌋ mod 10.
		Pass units = trace.passes().get(0);
		assertThat(units.digits()).containsExactly(0, 5, 5, 0, 2, 4, 2, 6);
		assertThat(units.count()).containsExactly(2, 0, 2, 0, 1, 2, 1, 0, 0, 0);
		assertThat(units.cumulative()).containsExactly(2, 2, 4, 4, 5, 7, 8, 8, 8, 8);
		assertThat(keys(units.result())).containsExactly(170, 90, 802, 2, 24, 45, 75, 66);

		// Pasada exp = 10 sobre el resultado anterior.
		Pass tens = trace.passes().get(1);
		assertThat(tens.digits()).containsExactly(7, 9, 0, 0, 2, 4, 7, 6);
		assertThat(keys(tens.result())).containsExactly(802, 2, 24, 45, 66, 170, 75, 90);

		// Pasada exp = 100.
		Pass hundreds = trace.passes().get(2);
		assertThat(hundreds.digits()).containsExactly(8, 0, 0, 0, 0, 1, 0, 0);
		assertThat(keys(hundreds.result())).containsExactly(2, 24, 45, 66, 75, 90, 170, 802);

		assertThat(keys(result.output())).containsExactly(2, 24, 45, 66, 75, 90, 170, 802);
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
	@DisplayName("k ≫ n: con máximo 1000 hace 4 pasadas (d = 4)")
	void passesFollowDigitsOfMax() {
		RadixSortTrace trace = sorter.sort(List.of(tf("a", 1), tf("b", 1000)), WITH_TRACE).trace();

		assertThat(trace.passes()).extracting(Pass::exp).containsExactly(1, 10, 100, 1000);
	}

	@Test
	@DisplayName("AC-18: lista vacía devuelve lista vacía sin excepción")
	void emptyList() {
		SortResult<RadixSortTrace> result = sorter.sort(List.of(), WITH_TRACE);

		assertThat(result.output()).isEmpty();
		assertThat(result.trace().input()).isEmpty();
		assertThat(result.trace().max()).isZero();
		assertThat(result.trace().passes()).isEmpty();
		assertThat(result.trace().output()).isEmpty();
		assertThat(sorter.sort(List.of(), WITHOUT_TRACE).output()).isEmpty();
	}

	@Test
	@DisplayName("Sin traza devuelve null en la traza y el mismo resultado; no modifica la entrada")
	void withoutTraceAndInputUnchanged() {
		List<TermFrequency> input = new ArrayList<>(frequencies(21, 3, 12, 3));
		List<TermFrequency> copy = List.copyOf(input);

		SortResult<RadixSortTrace> withoutTrace = sorter.sort(input, WITHOUT_TRACE);

		assertThat(withoutTrace.trace()).isNull();
		assertThat(withoutTrace.output()).isEqualTo(sorter.sort(input, WITH_TRACE).output());
		assertThat(input).isEqualTo(copy);
		assertThat(sorter.algorithm()).isEqualTo(SortAlgorithm.RADIX);
	}

}
