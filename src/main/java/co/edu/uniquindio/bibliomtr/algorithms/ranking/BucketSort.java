package co.edu.uniquindio.bibliomtr.algorithms.ranking;

import java.util.ArrayList;
import java.util.List;

import co.edu.uniquindio.bibliomtr.trace.ranking.BucketSortTrace;

/**
 * Bucket Sort sobre pares (término, frecuencia) con insertion sort estable en cada cubeta, adaptado de
 * {@code docs/referencias/ordenamiento/BucketSort.java} (REQ-8, REQ-15).
 *
 * <p><b>Definición formal.</b> Sea {@code s = bucketSize ≥ 1}:
 * <ol>
 *   <li>Mínimo y máximo de las frecuencias.</li>
 *   <li>Número de cubetas: {@code m = ⌊(max − min) / s⌋ + 1}.</li>
 *   <li>Distribución, en el orden de la entrada: el elemento con frecuencia {@code v} va a la cubeta
 *       {@code ⌊(v − min) / s⌋}. Toda frecuencia de la cubeta j está en {@code [min + j·s, min + (j+1)·s)},
 *       así que las cubetas quedan ordenadas entre sí.</li>
 *   <li>Cada cubeta se ordena con insertion sort y las cubetas se concatenan en orden de índice.</li>
 * </ol>
 *
 * <p><b>Cambios respecto a la referencia.</b> Por el conflicto con la constitución P1, {@code Collections.sort}
 * de cada cubeta se reemplaza por un insertion sort escrito a mano que desplaza solo mientras
 * {@code previo.frequency > clave.frequency} (desigualdad estricta), de modo que es estable; como la
 * distribución también respeta el orden de entrada, el algoritmo completo es estable. Además ordena pares,
 * trabaja sobre copias y registra la traza si {@link SortOptions#recordTrace()} es {@code true}. La guarda
 * de lista vacía ya estaba en la referencia.
 *
 * <p>Complejidad: tiempo O(n + m) en promedio (frecuencias repartidas de manera uniforme entre las cubetas)
 * y O(n²) en el peor caso (todas en una cubeta, insertion sort cuadrático); espacio O(n + m), con
 * n = número de términos y m = número de cubetas.
 */
public final class BucketSort implements TermSorter<BucketSortTrace> {

	/**
	 * {@inheritDoc}
	 *
	 * <p>Complejidad: O(1) en tiempo y espacio.
	 */
	@Override
	public SortAlgorithm algorithm() {
		return SortAlgorithm.BUCKET;
	}

	/**
	 * Ordena ascendente y de forma estable por frecuencia con Bucket Sort; no modifica {@code input}.
	 *
	 * <p>Complejidad: tiempo O(n + m) promedio y O(n²) peor caso; espacio O(n + m), también con traza.
	 *
	 * @param input   pares a ordenar (puede ser vacía)
	 * @param options {@code bucketSize} (ancho de cubeta) y {@code recordTrace}
	 * @return lista ordenada y traza ({@code null} si no se pidió)
	 */
	@Override
	public SortResult<BucketSortTrace> sort(List<TermFrequency> input, SortOptions options) {
		boolean recordTrace = options.recordTrace();
		int bucketSize = options.bucketSize();
		if (input.isEmpty()) {
			BucketSortTrace trace = recordTrace
					? new BucketSortTrace(List.of(), 0, 0, bucketSize, 0, List.of(), List.of())
					: null;
			return new SortResult<>(List.of(), trace);
		}
		TermFrequency[] array = input.toArray(new TermFrequency[0]);

		// 1. Mínimo y máximo.
		int minValue = array[0].frequency();
		int maxValue = array[0].frequency();
		for (int i = 1; i < array.length; i++) {
			if (array[i].frequency() < minValue) {
				minValue = array[i].frequency();
			} else if (array[i].frequency() > maxValue) {
				maxValue = array[i].frequency();
			}
		}

		// 2. m = ⌊(max − min) / bucketSize⌋ + 1 cubetas vacías.
		int bucketCount = (maxValue - minValue) / bucketSize + 1;
		List<List<TermFrequency>> buckets = new ArrayList<>(bucketCount);
		for (int i = 0; i < bucketCount; i++) {
			buckets.add(new ArrayList<>());
		}

		// 3. Distribución en el orden de la entrada: índice ⌊(v − min) / bucketSize⌋.
		for (int i = 0; i < array.length; i++) {
			int bucketIndex = (array[i].frequency() - minValue) / bucketSize;
			buckets.get(bucketIndex).add(array[i]);
		}

		// 4. Insertion sort de cada cubeta y concatenación.
		List<BucketSortTrace.Bucket> bucketTraces = recordTrace ? new ArrayList<>(bucketCount) : null;
		int idx = 0;
		for (int i = 0; i < buckets.size(); i++) {
			List<TermFrequency> bucket = buckets.get(i);
			List<TermFrequency> before = recordTrace ? List.copyOf(bucket) : null;
			insertionSort(bucket);
			if (recordTrace) {
				bucketTraces.add(new BucketSortTrace.Bucket(i, before, bucket));
			}
			for (int j = 0; j < bucket.size(); j++) {
				array[idx++] = bucket.get(j);
			}
		}

		List<TermFrequency> outputList = List.of(array);
		BucketSortTrace trace = recordTrace
				? new BucketSortTrace(input, minValue, maxValue, bucketSize, bucketCount, bucketTraces, outputList)
				: null;
		return new SortResult<>(outputList, trace);
	}

	/**
	 * Insertion sort estable por frecuencia, en el lugar: para cada {@code i}, toma la clave
	 * {@code bucket[i]} y desplaza una posición a la derecha cada elemento previo con frecuencia
	 * <em>estrictamente</em> mayor; luego inserta la clave en el hueco. Los iguales no se desplazan, por eso
	 * conservan su orden relativo.
	 *
	 * <p>Complejidad: tiempo O(t²) en el peor caso (cubeta en orden inverso) y O(t) en el mejor (ya
	 * ordenada), con t = tamaño de la cubeta; espacio O(1) adicional.
	 */
	private static void insertionSort(List<TermFrequency> bucket) {
		for (int i = 1; i < bucket.size(); i++) {
			TermFrequency key = bucket.get(i);
			int j = i - 1;
			while (j >= 0 && bucket.get(j).frequency() > key.frequency()) {
				bucket.set(j + 1, bucket.get(j));
				j--;
			}
			bucket.set(j + 1, key);
		}
	}

}
