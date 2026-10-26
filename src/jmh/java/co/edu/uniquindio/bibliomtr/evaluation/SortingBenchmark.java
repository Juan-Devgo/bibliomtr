package co.edu.uniquindio.bibliomtr.evaluation;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import co.edu.uniquindio.bibliomtr.algorithms.ranking.BucketSort;
import co.edu.uniquindio.bibliomtr.algorithms.ranking.CountingSort;
import co.edu.uniquindio.bibliomtr.algorithms.ranking.RadixSort;
import co.edu.uniquindio.bibliomtr.algorithms.ranking.SortOptions;
import co.edu.uniquindio.bibliomtr.algorithms.ranking.SortResult;
import co.edu.uniquindio.bibliomtr.algorithms.ranking.TermFrequency;
import co.edu.uniquindio.bibliomtr.trace.ranking.BucketSortTrace;
import co.edu.uniquindio.bibliomtr.trace.ranking.CountingSortTrace;
import co.edu.uniquindio.bibliomtr.trace.ranking.RadixSortTrace;

/**
 * Medición JMH de Counting Sort, Radix Sort y Bucket Sort sobre la lista de frecuencias del corpus completo
 * (REQ-17, REQ-18; constitución P11).
 *
 * <p><b>Qué mide.</b> El tiempo promedio por operación, en milisegundos, de ordenar la lista completa con cada
 * algoritmo, con {@code bucketSize} por defecto (5) y la grabación de traza desactivada, igual que los tiempos
 * de {@code GET /api/ranking}. La lista se lee una vez por trial ({@link Level#Trial}) del TSV que escribe la
 * API ({@link FrequencyListFile}), de modo que la lectura del archivo no entra en la medición.
 *
 * <p><b>Parámetros fijos (REQ-18).</b> 1 fork, 5 iteraciones de calentamiento de 1 s, 10 iteraciones de
 * medición de 1 s, modo {@code avgt}, unidad ms. Coinciden con {@link JmhParameters#FIXED}, con los argumentos
 * de {@link JmhProcessLauncher} y con el bloque {@code jmh} de {@code build.gradle.kts}.
 *
 * <p><b>Por qué devolver el resultado.</b> JMH consume el valor devuelto por cada {@code @Benchmark}; así el
 * JIT no puede eliminar el ordenamiento como código muerto.
 *
 * <p>Ejecución: la API lo lanza como proceso hijo
 * ({@code java -jar benchmarks.jar SortingBenchmark … -p inputFile=<tsv>}).
 */
@State(Scope.Benchmark)
@Fork(1)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class SortingBenchmark {

	/** Opciones de la medición: {@code bucketSize} por defecto y sin traza (REQ-18). */
	private static final SortOptions OPTIONS = new SortOptions(SortOptions.DEFAULT_BUCKET_SIZE, false);

	/** Ruta del TSV con la lista de frecuencias; la fija la API con {@code -p inputFile=…}. */
	@Param("")
	public String inputFile;

	private final CountingSort countingSort = new CountingSort();
	private final RadixSort radixSort = new RadixSort();
	private final BucketSort bucketSort = new BucketSort();
	private List<TermFrequency> input;

	/**
	 * Lee la lista de frecuencias una vez por trial.
	 *
	 * <p>Complejidad: O(L) en tiempo y O(n) en espacio, con L = caracteres del archivo y n = número de pares.
	 *
	 * @throws IllegalStateException si falta el parámetro {@code inputFile}
	 */
	@Setup(Level.Trial)
	public void loadInput() {
		if (inputFile == null || inputFile.isBlank()) {
			throw new IllegalStateException("Falta el parámetro inputFile");
		}
		input = FrequencyListFile.read(Path.of(inputFile));
	}

	/**
	 * Counting Sort sobre la lista completa: O(n + k), k = frecuencia máxima.
	 *
	 * @return el resultado, consumido por JMH
	 */
	@Benchmark
	public SortResult<CountingSortTrace> countingSort() {
		return countingSort.sort(input, OPTIONS);
	}

	/**
	 * Radix Sort LSD en base 10 sobre la lista completa: O(d · (n + b)), d = dígitos del máximo, b = 10.
	 *
	 * @return el resultado, consumido por JMH
	 */
	@Benchmark
	public SortResult<RadixSortTrace> radixSort() {
		return radixSort.sort(input, OPTIONS);
	}

	/**
	 * Bucket Sort con {@code bucketSize} = 5 sobre la lista completa: O(n + m) promedio y O(n²) peor caso,
	 * m = número de cubetas.
	 *
	 * @return el resultado, consumido por JMH
	 */
	@Benchmark
	public SortResult<BucketSortTrace> bucketSort() {
		return bucketSort.sort(input, OPTIONS);
	}

}
