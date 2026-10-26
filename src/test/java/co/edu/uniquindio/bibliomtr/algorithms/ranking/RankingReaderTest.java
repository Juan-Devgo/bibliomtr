package co.edu.uniquindio.bibliomtr.algorithms.ranking;

import static co.edu.uniquindio.bibliomtr.algorithms.ranking.SortEdgeCases.tf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests de {@link RankingReader}: ranking leído de derecha a izquierda, top N y coincidencia de los tres
 * algoritmos sobre la misma lista (REQ-2, REQ-3, REQ-10, AC-1, AC-6).
 */
class RankingReaderTest {

	private static final SortOptions OPTIONS = new SortOptions(SortOptions.DEFAULT_BUCKET_SIZE, false);

	private static final List<TermSorter<?>> SORTERS = List.of(new CountingSort(), new RadixSort(), new BucketSort());

	/** Tokens de AC-1: model ×42, data ×30, graph ×30. */
	private static List<List<String>> ac1Tokens() {
		List<String> tokens = new ArrayList<>();
		tokens.addAll(Collections.nCopies(30, "graph"));
		tokens.addAll(Collections.nCopies(42, "model"));
		tokens.addAll(Collections.nCopies(30, "data"));
		return List.of(tokens);
	}

	@Test
	@DisplayName("AC-1: el ranking de cada algoritmo es model (42), data (30), graph (30)")
	void rankingBreaksTiesAlphabetically() {
		List<TermFrequency> input = new TermFrequencyCounter().count(ac1Tokens());

		for (TermSorter<?> sorter : SORTERS) {
			List<TermFrequency> sorted = sorter.sort(input, OPTIONS).output();
			// Ascendente y estable: graph antes que data porque así venían en la entrada.
			assertThat(sorted).as(sorter.algorithm().slug())
					.containsExactly(tf("graph", 30), tf("data", 30), tf("model", 42));
			assertThat(RankingReader.read(sorted)).as(sorter.algorithm().slug())
					.containsExactly(tf("model", 42), tf("data", 30), tf("graph", 30));
		}
	}

	@Test
	@DisplayName("AC-6: los tres rankings completos sobre la misma lista son idénticos")
	void threeAlgorithmsProduceSameRanking() {
		List<String> tokens = new ArrayList<>();
		String[] terms = {"algorithm", "data", "graph", "model", "network", "text", "vector", "zeta"};
		int[] counts = {17, 3, 17, 42, 1, 3, 26, 1};
		for (int i = 0; i < terms.length; i++) {
			tokens.addAll(Collections.nCopies(counts[i], terms[i]));
		}
		List<TermFrequency> input = new TermFrequencyCounter().count(List.of(tokens));

		List<List<TermFrequency>> rankings = new ArrayList<>();
		for (TermSorter<?> sorter : SORTERS) {
			rankings.add(RankingReader.read(sorter.sort(input, OPTIONS).output()));
		}

		assertThat(RankingReader.sameRanking(rankings)).isTrue();
		assertThat(rankings.getFirst()).containsExactly(
				tf("model", 42), tf("vector", 26), tf("algorithm", 17), tf("graph", 17),
				tf("data", 3), tf("text", 3), tf("network", 1), tf("zeta", 1));
	}

	@Test
	@DisplayName("sameRanking detecta diferencias de orden, de frecuencia y de tamaño")
	void sameRankingDetectsDifferences() {
		List<TermFrequency> base = List.of(tf("a", 2), tf("b", 1));

		assertThat(RankingReader.sameRanking(List.of())).isTrue();
		assertThat(RankingReader.sameRanking(List.of(base))).isTrue();
		assertThat(RankingReader.sameRanking(List.of(base, List.of(tf("a", 2), tf("b", 1))))).isTrue();
		assertThat(RankingReader.sameRanking(List.of(base, List.of(tf("b", 1), tf("a", 2))))).isFalse();
		assertThat(RankingReader.sameRanking(List.of(base, List.of(tf("a", 2), tf("b", 3))))).isFalse();
		assertThat(RankingReader.sameRanking(List.of(base, base, List.of(tf("a", 2))))).isFalse();
	}

	@Test
	@DisplayName("read invierte la lista; con lista vacía devuelve vacía")
	void readReversesList() {
		assertThat(RankingReader.read(List.of(tf("c", 1), tf("b", 2), tf("a", 3))))
				.containsExactly(tf("a", 3), tf("b", 2), tf("c", 1));
		assertThat(RankingReader.read(List.of())).isEmpty();
	}

	@Test
	@DisplayName("top corta los N primeros y, si N supera el tamaño, devuelve todos")
	void topCutsFirstN() {
		List<TermFrequency> ranking = List.of(tf("a", 3), tf("b", 2), tf("c", 1));

		assertThat(RankingReader.top(ranking, 2)).containsExactly(tf("a", 3), tf("b", 2));
		assertThat(RankingReader.top(ranking, 3)).isEqualTo(ranking);
		assertThat(RankingReader.top(ranking, 20)).isEqualTo(ranking);
		assertThat(RankingReader.top(List.of(), 20)).isEmpty();
		assertThatThrownBy(() -> RankingReader.top(ranking, 0)).isInstanceOf(IllegalArgumentException.class);
	}

}
