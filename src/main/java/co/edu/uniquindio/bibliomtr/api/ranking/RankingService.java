package co.edu.uniquindio.bibliomtr.api.ranking;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import co.edu.uniquindio.bibliomtr.algorithms.ranking.BucketSort;
import co.edu.uniquindio.bibliomtr.algorithms.ranking.CountingSort;
import co.edu.uniquindio.bibliomtr.algorithms.ranking.RadixSort;
import co.edu.uniquindio.bibliomtr.algorithms.ranking.RankingReader;
import co.edu.uniquindio.bibliomtr.algorithms.ranking.SortAlgorithm;
import co.edu.uniquindio.bibliomtr.algorithms.ranking.SortOptions;
import co.edu.uniquindio.bibliomtr.algorithms.ranking.SortResult;
import co.edu.uniquindio.bibliomtr.algorithms.ranking.TermFrequency;
import co.edu.uniquindio.bibliomtr.algorithms.ranking.TermFrequencyCounter;
import co.edu.uniquindio.bibliomtr.algorithms.ranking.TermSorter;
import co.edu.uniquindio.bibliomtr.api.ranking.RankingDtos.AlgorithmRanking;
import co.edu.uniquindio.bibliomtr.api.ranking.RankingDtos.RankingResponse;
import co.edu.uniquindio.bibliomtr.api.ranking.RankingDtos.SortTraceResponse;
import co.edu.uniquindio.bibliomtr.evaluation.Stopwatch;
import co.edu.uniquindio.bibliomtr.evaluation.Stopwatch.Timed;

/**
 * Calcula el ranking de términos con los tres algoritmos y la traza de uno de ellos (REQ-1, REQ-3, REQ-10,
 * REQ-11, REQ-16).
 *
 * <p><b>Flujo del ranking.</b>
 * <ol>
 *   <li>Preprocesamiento, cronometrado como {@code preprocessingMs}: tokens de los abstracts seleccionables
 *       ({@link CorpusTokenSource}) y conteo de frecuencias en orden alfabético descendente
 *       ({@link TermFrequencyCounter}).</li>
 *   <li>Cada algoritmo ordena la misma lista completa con {@code recordTrace = false}, cronometrado por
 *       separado como {@code elapsedMs}, igual que en JMH.</li>
 *   <li>Cada salida se lee de derecha a izquierda ({@link RankingReader#read}); {@code allMatch} compara los
 *       rankings completos y la respuesta lleva solo el top N de cada uno.</li>
 * </ol>
 *
 * <p><b>Flujo de la traza.</b> Los mismos pasos 1 y 2 con un solo algoritmo y {@code recordTrace = true}.
 */
@Service
public class RankingService {

	private final CorpusTokenSource tokenSource;
	private final TermFrequencyCounter counter = new TermFrequencyCounter();
	private final List<TermSorter<?>> sorters;

	/**
	 * Servicio con los tres algoritmos del Requerimiento 2 en el orden Counting, Radix, Bucket.
	 *
	 * <p>Complejidad: O(1) en tiempo y espacio.
	 *
	 * @param tokenSource fuente de tokens preprocesados del corpus
	 */
	@Autowired
	public RankingService(CorpusTokenSource tokenSource) {
		this(tokenSource, List.of(new CountingSort(), new RadixSort(), new BucketSort()));
	}

	/**
	 * Servicio con una lista explícita de algoritmos; permite a los tests inyectar un {@link TermSorter}
	 * espía para comprobar que el ranking se mide sin traza (AC-7).
	 *
	 * <p>Complejidad: O(a) en tiempo y espacio, con a = número de algoritmos.
	 *
	 * @param tokenSource fuente de tokens preprocesados del corpus
	 * @param sorters     algoritmos a ejecutar, en el orden de la respuesta
	 */
	public RankingService(CorpusTokenSource tokenSource, List<TermSorter<?>> sorters) {
		this.tokenSource = Objects.requireNonNull(tokenSource, "La fuente de tokens no puede ser nula");
		this.sorters = List.copyOf(sorters);
	}

	/**
	 * Ejecuta los tres algoritmos sobre la lista completa de frecuencias y devuelve el top N de cada uno, el
	 * indicador de coincidencia y los tiempos por separado, medidos sin traza (REQ-3, REQ-11, AC-6, AC-7).
	 *
	 * <p>Complejidad: tiempo O(T · log V) del conteo más la suma de los algoritmos (Counting O(V + k), Radix
	 * O(d · (V + b)), Bucket O(V + m) promedio y O(V²) peor caso) y O(a · V) de la lectura y la comparación;
	 * espacio O(a · V). T = total de tokens, V = número de términos distintos, a = número de algoritmos.
	 *
	 * @param n          número de términos del top, mayor o igual a 1
	 * @param bucketSize ancho de cubeta de Bucket Sort, mayor o igual a 1
	 * @return ranking con top N, {@code allMatch} y tiempos en ms
	 */
	public RankingResponse rank(int n, int bucketSize) {
		SortOptions options = new SortOptions(bucketSize, false);
		Timed<List<TermFrequency>> preprocessing = Stopwatch.time(this::frequencyList);
		List<TermFrequency> input = preprocessing.value();

		List<List<TermFrequency>> fullRankings = new ArrayList<>(sorters.size());
		List<AlgorithmRanking> algorithms = new ArrayList<>(sorters.size());
		for (TermSorter<?> sorter : sorters) {
			// Solo el ordenamiento entra en el tiempo del algoritmo; la lectura del ranking queda fuera.
			Timed<? extends SortResult<?>> sorting = Stopwatch.time(() -> sorter.sort(input, options));
			List<TermFrequency> ranking = RankingReader.read(sorting.value().output());
			fullRankings.add(ranking);
			algorithms.add(new AlgorithmRanking(sorter.algorithm().slug(), RankingReader.top(ranking, n),
					sorting.millis()));
		}
		// La coincidencia se calcula sobre las listas completas, no sobre el top N (REQ-11).
		boolean allMatch = RankingReader.sameRanking(fullRankings);
		return new RankingResponse(input.size(), n, bucketSize, preprocessing.millis(), allMatch, algorithms);
	}

	/**
	 * Ejecuta un algoritmo con la grabación de traza activada sobre la lista completa de frecuencias del
	 * corpus (REQ-16, AC-23). {@code bucketSize} solo afecta a Bucket Sort.
	 *
	 * <p>Complejidad: tiempo O(T · log V) del conteo más el del algoritmo; espacio el de su traza (Counting
	 * O(V + k), Radix O(d · (V + b)), Bucket O(V + m)).
	 *
	 * @param algorithm  algoritmo a trazar
	 * @param bucketSize ancho de cubeta de Bucket Sort, mayor o igual a 1
	 * @return entrada, salida, ranking y traza completos
	 */
	public SortTraceResponse trace(SortAlgorithm algorithm, int bucketSize) {
		SortOptions options = new SortOptions(bucketSize, true);
		TermSorter<?> sorter = sorterFor(algorithm);
		List<TermFrequency> input = frequencyList();
		SortResult<?> result = sorter.sort(input, options);
		List<TermFrequency> output = result.output();
		return new SortTraceResponse(algorithm.slug(), bucketSize, input, output, RankingReader.read(output),
				result.trace());
	}

	/**
	 * Pasos 1 y 2 de ambos flujos: tokens del corpus y lista de frecuencias en orden alfabético descendente.
	 *
	 * <p>Complejidad: O(T · log V) en tiempo y O(V) en espacio adicional.
	 */
	private List<TermFrequency> frequencyList() {
		return counter.count(tokenSource.selectableAbstractTokens());
	}

	/**
	 * Busca la estrategia del algoritmo pedido.
	 *
	 * <p>Complejidad: O(a) en tiempo y O(1) en espacio.
	 */
	private TermSorter<?> sorterFor(SortAlgorithm algorithm) {
		for (TermSorter<?> sorter : sorters) {
			if (sorter.algorithm() == algorithm) {
				return sorter;
			}
		}
		throw new IllegalStateException("No hay implementación para el algoritmo " + algorithm.slug());
	}

}
