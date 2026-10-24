package co.edu.uniquindio.bibliomtr.oracle;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import co.edu.uniquindio.bibliomtr.algorithms.ranking.BucketSort;
import co.edu.uniquindio.bibliomtr.algorithms.ranking.CountingSort;
import co.edu.uniquindio.bibliomtr.algorithms.ranking.RadixSort;
import co.edu.uniquindio.bibliomtr.algorithms.ranking.RankingReader;
import co.edu.uniquindio.bibliomtr.algorithms.ranking.SortOptions;
import co.edu.uniquindio.bibliomtr.algorithms.ranking.TermFrequency;
import co.edu.uniquindio.bibliomtr.algorithms.ranking.TermFrequencyCounter;
import co.edu.uniquindio.bibliomtr.algorithms.ranking.TermSorter;

/**
 * Test oráculo del Requerimiento 2 (REQ-25, AC-19): el ranking de cada algoritmo escrito a mano debe ser
 * igual al que produce {@link List#sort} de la librería estándar con el mismo criterio (frecuencia
 * descendente y, en los empates, término ascendente por {@link String#compareTo}).
 *
 * <p>{@code List.sort} está prohibido en producción (REQ-12); aquí solo sirve de referencia. Las listas
 * aleatorias usan una semilla fija para que el test sea reproducible (constitución P12).
 */
class SortingOracleTest {

	/** Semilla fija de las listas aleatorias. */
	private static final long SEED = 20261023L;

	/** Criterio del ranking (REQ-2) con la librería estándar. */
	private static final Comparator<TermFrequency> RANKING_ORDER =
			Comparator.comparingInt(TermFrequency::frequency).reversed().thenComparing(TermFrequency::term);

	private static final List<TermSorter<?>> SORTERS = List.of(new CountingSort(), new RadixSort(), new BucketSort());

	private static final int[] BUCKET_SIZES = {1, 2, SortOptions.DEFAULT_BUCKET_SIZE, 7, 100};

	private final TermFrequencyCounter counter = new TermFrequencyCounter();

	/** Ranking esperado: copia de la entrada ordenada con {@code List.sort}. */
	private static List<TermFrequency> oracleRanking(List<TermFrequency> input) {
		List<TermFrequency> expected = new ArrayList<>(input);
		expected.sort(RANKING_ORDER);
		return expected;
	}

	/** Comprueba los tres algoritmos, con y sin traza y con varios bucketSize, contra el oráculo. */
	private static void assertMatchesOracle(List<TermFrequency> input) {
		List<TermFrequency> expected = oracleRanking(input);
		for (TermSorter<?> sorter : SORTERS) {
			for (int bucketSize : BUCKET_SIZES) {
				for (boolean recordTrace : new boolean[] {false, true}) {
					List<TermFrequency> sorted = sorter.sort(input, new SortOptions(bucketSize, recordTrace)).output();
					assertThat(RankingReader.read(sorted))
							.as("%s, bucketSize = %d, traza = %b, n = %d",
									sorter.algorithm().slug(), bucketSize, recordTrace, input.size())
							.containsExactlyElementsOf(expected);
				}
			}
		}
	}

	@Test
	@DisplayName("AC-19: listas pequeñas iguales al ordenamiento de la librería estándar")
	void smallListsMatchStandardLibrary() {
		List<List<String>> cases = List.of(
				List.of(),
				List.of("model"),
				List.of("b", "a", "b", "c", "a", "b"),
				List.of("graph", "data", "model", "model", "data", "graph"),
				List.of("z", "y", "x", "w", "v"));
		for (List<String> tokens : cases) {
			assertMatchesOracle(counter.count(List.of(tokens)));
		}
	}

	@Test
	@DisplayName("AC-19: listas aleatorias con semilla fija iguales al ordenamiento de la librería estándar")
	void randomListsMatchStandardLibrary() {
		Random random = new Random(SEED);
		int[] vocabularySizes = {1, 5, 20, 88, 300};
		int[] maxFrequencies = {1, 3, 50, 1500};
		for (int vocabulary : vocabularySizes) {
			for (int maxFrequency : maxFrequencies) {
				List<List<String>> abstracts = new ArrayList<>();
				for (int a = 0; a < 3; a++) {
					List<String> tokens = new ArrayList<>();
					for (int t = 0; t < vocabulary; t++) {
						// Términos de longitud variable para ejercitar el orden natural de String.
						String term = "t" + Integer.toString(t, 36) + (t % 3 == 0 ? "x" : "");
						tokens.addAll(Collections.nCopies(random.nextInt(maxFrequency + 1), term));
					}
					Collections.shuffle(tokens, random);
					abstracts.add(tokens);
				}
				assertMatchesOracle(counter.count(abstracts));
			}
		}
	}

}
