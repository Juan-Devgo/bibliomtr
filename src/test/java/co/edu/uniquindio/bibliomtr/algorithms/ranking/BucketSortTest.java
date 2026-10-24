package co.edu.uniquindio.bibliomtr.algorithms.ranking;

import static co.edu.uniquindio.bibliomtr.algorithms.ranking.SortEdgeCases.tf;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import co.edu.uniquindio.bibliomtr.trace.ranking.BucketSortTrace;
import co.edu.uniquindio.bibliomtr.trace.ranking.BucketSortTrace.Bucket;

/**
 * Tests de {@link BucketSort}: caso a mano de AC-9, cubetas antes y después del insertion sort, casos de
 * borde y lista vacía (REQ-8, REQ-15, REQ-24, AC-9, AC-17, AC-18).
 */
class BucketSortTest {

	private static final SortOptions WITH_TRACE = new SortOptions(SortOptions.DEFAULT_BUCKET_SIZE, true);
	private static final SortOptions WITHOUT_TRACE = new SortOptions(SortOptions.DEFAULT_BUCKET_SIZE, false);

	private final BucketSort sorter = new BucketSort();

	@Test
	@DisplayName("AC-9: [5, 3, 8, 3] con bucketSize 2 crea 3 cubetas: [3, 3], [5] y [8]")
	void handCase() {
		List<TermFrequency> input = List.of(tf("t0", 5), tf("t1", 3), tf("t2", 8), tf("t3", 3));

		SortResult<BucketSortTrace> result = sorter.sort(input, new SortOptions(2, true));
		BucketSortTrace trace = result.trace();

		assertThat(trace.input()).isEqualTo(input);
		assertThat(trace.min()).isEqualTo(3);
		assertThat(trace.max()).isEqualTo(8);
		assertThat(trace.bucketSize()).isEqualTo(2);
		// ⌊(8 − 3)/2⌋ + 1 = 3; índices ⌊(5−3)/2⌋ = 1, ⌊(3−3)/2⌋ = 0, ⌊(8−3)/2⌋ = 2.
		assertThat(trace.bucketCount()).isEqualTo(3);
		assertThat(trace.buckets()).containsExactly(
				new Bucket(0, List.of(tf("t1", 3), tf("t3", 3)), List.of(tf("t1", 3), tf("t3", 3))),
				new Bucket(1, List.of(tf("t0", 5)), List.of(tf("t0", 5))),
				new Bucket(2, List.of(tf("t2", 8)), List.of(tf("t2", 8))));
		assertThat(trace.concatenated()).containsExactly(tf("t1", 3), tf("t3", 3), tf("t0", 5), tf("t2", 8));
		assertThat(result.output()).isEqualTo(trace.concatenated());
	}

	@Test
	@DisplayName("Cada cubeta registra su contenido antes y después del insertion sort estable")
	void bucketBeforeAndAfterInsertionSort() {
		// bucketSize 5, min 1, max 4: una sola cubeta con todo, en el orden de la entrada.
		List<TermFrequency> input = List.of(tf("a", 4), tf("b", 2), tf("c", 1), tf("d", 2));

		BucketSortTrace trace = sorter.sort(input, WITH_TRACE).trace();

		assertThat(trace.bucketCount()).isEqualTo(1);
		Bucket bucket = trace.buckets().getFirst();
		assertThat(bucket.before()).containsExactly(tf("a", 4), tf("b", 2), tf("c", 1), tf("d", 2));
		// b y d (frecuencia 2) conservan su orden relativo.
		assertThat(bucket.after()).containsExactly(tf("c", 1), tf("b", 2), tf("d", 2), tf("a", 4));
	}

	@Test
	@DisplayName("Las cubetas vacías también aparecen en la traza")
	void emptyBucketsAreRecorded() {
		// bucketSize 5, min 1, max 20: ⌊19/5⌋ + 1 = 4 cubetas; 1 va a la 0 y 20 a la ⌊19/5⌋ = 3.
		BucketSortTrace trace = sorter.sort(List.of(tf("a", 20), tf("b", 1)), WITH_TRACE).trace();

		assertThat(trace.bucketCount()).isEqualTo(4);
		assertThat(trace.buckets()).extracting(Bucket::index).containsExactly(0, 1, 2, 3);
		assertThat(trace.buckets().get(1).before()).isEmpty();
		assertThat(trace.buckets().get(2).after()).isEmpty();
		assertThat(trace.concatenated()).containsExactly(tf("b", 1), tf("a", 20));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("co.edu.uniquindio.bibliomtr.algorithms.ranking.SortEdgeCases#cases")
	@DisplayName("AC-17: casos de borde con varios bucketSize")
	void edgeCases(String name, List<TermFrequency> input, List<TermFrequency> expected) {
		for (int bucketSize : new int[] {1, 2, 5, 1000}) {
			assertThat(sorter.sort(input, new SortOptions(bucketSize, false)).output())
					.as("bucketSize = %d", bucketSize)
					.containsExactlyElementsOf(expected);
			assertThat(sorter.sort(input, new SortOptions(bucketSize, true)).output())
					.as("bucketSize = %d con traza", bucketSize)
					.containsExactlyElementsOf(expected);
		}
	}

	@Test
	@DisplayName("AC-18: lista vacía devuelve lista vacía sin excepción")
	void emptyList() {
		SortResult<BucketSortTrace> result = sorter.sort(List.of(), WITH_TRACE);

		assertThat(result.output()).isEmpty();
		assertThat(result.trace().input()).isEmpty();
		assertThat(result.trace().min()).isZero();
		assertThat(result.trace().max()).isZero();
		assertThat(result.trace().bucketCount()).isZero();
		assertThat(result.trace().buckets()).isEmpty();
		assertThat(result.trace().concatenated()).isEmpty();
		assertThat(sorter.sort(List.of(), WITHOUT_TRACE).output()).isEmpty();
	}

	@Test
	@DisplayName("Sin traza devuelve null en la traza y el mismo resultado; no modifica la entrada")
	void withoutTraceAndInputUnchanged() {
		List<TermFrequency> input = new ArrayList<>(List.of(tf("a", 9), tf("b", 1), tf("c", 9), tf("d", 4)));
		List<TermFrequency> copy = List.copyOf(input);

		SortResult<BucketSortTrace> withoutTrace = sorter.sort(input, WITHOUT_TRACE);

		assertThat(withoutTrace.trace()).isNull();
		assertThat(withoutTrace.output()).isEqualTo(sorter.sort(input, WITH_TRACE).output());
		assertThat(input).isEqualTo(copy);
		assertThat(sorter.algorithm()).isEqualTo(SortAlgorithm.BUCKET);
	}

}
