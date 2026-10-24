package co.edu.uniquindio.bibliomtr.algorithms.ranking;

import java.util.ArrayList;
import java.util.List;

import co.edu.uniquindio.bibliomtr.trace.ranking.CountingSortTrace;

/**
 * Counting Sort estable sobre pares (término, frecuencia), adaptado de
 * {@code docs/referencias/ordenamiento/CountingSort.java} (REQ-6, REQ-13).
 *
 * <p><b>Definición formal.</b> Sea {@code A[0..n−1]} la entrada con claves {@code v = A[i].frequency} en
 * {@code [0, k]}, k = máximo:
 * <ol>
 *   <li>Conteo: {@code C[v] = |{ i : A[i].frequency = v }|}, con {@code C} de tamaño {@code k + 1}.</li>
 *   <li>Acumulado: para {@code v = 1..k}, {@code C[v] += C[v − 1]}; ahora {@code C[v]} es el número de
 *       elementos con clave {@code ≤ v}.</li>
 *   <li>Salida: para {@code i = n−1 .. 0}, {@code B[C[v] − 1] = A[i]} y {@code C[v]−−}. Recorrer de derecha a
 *       izquierda coloca el último de cada clave en la última posición libre de su grupo, lo que conserva
 *       el orden relativo de los empates (estabilidad).</li>
 * </ol>
 *
 * <p><b>Cambios respecto a la referencia.</b> Ordena pares por su frecuencia en lugar de {@code int},
 * registra la traza si {@link SortOptions#recordTrace()} es {@code true} y agrega la guarda de lista vacía
 * (la referencia lee {@code array[0]} sin comprobar). La lógica de los tres pasos no cambia.
 * {@link SortOptions#bucketSize()} se ignora.
 *
 * <p>Complejidad: tiempo O(n + k) y espacio O(n + k), con n = número de términos y k = frecuencia máxima.
 */
public final class CountingSort implements TermSorter<CountingSortTrace> {

	/**
	 * {@inheritDoc}
	 *
	 * <p>Complejidad: O(1) en tiempo y espacio.
	 */
	@Override
	public SortAlgorithm algorithm() {
		return SortAlgorithm.COUNTING;
	}

	/**
	 * Ordena ascendente y de forma estable por frecuencia con Counting Sort; no modifica {@code input}.
	 *
	 * <p>Complejidad: tiempo O(n + k) y espacio O(n + k), con n = número de términos y k = frecuencia
	 * máxima; con traza, el espacio adicional sigue siendo O(n + k).
	 *
	 * @param input   pares a ordenar (puede ser vacía)
	 * @param options solo se usa {@code recordTrace}
	 * @return lista ordenada y traza ({@code null} si no se pidió)
	 */
	@Override
	public SortResult<CountingSortTrace> sort(List<TermFrequency> input, SortOptions options) {
		boolean recordTrace = options.recordTrace();
		int n = input.size();
		// Guarda agregada: la referencia accede a array[0] sin comprobar que exista.
		if (n == 0) {
			CountingSortTrace trace = recordTrace
					? new CountingSortTrace(List.of(), 0, new int[0], new int[0], List.of(), List.of())
					: null;
			return new SortResult<>(List.of(), trace);
		}
		TermFrequency[] array = input.toArray(new TermFrequency[0]);

		// Máximo k: fija el tamaño del arreglo de conteo.
		int maxValue = array[0].frequency();
		for (TermFrequency element : array) {
			if (element.frequency() > maxValue) {
				maxValue = element.frequency();
			}
		}
		int[] count = new int[maxValue + 1];
		TermFrequency[] output = new TermFrequency[n];

		// 1. Conteo de ocurrencias de cada frecuencia: C[v]++.
		for (int i = 0; i < n; i++) {
			count[array[i].frequency()]++;
		}
		int[] countSnapshot = recordTrace ? count.clone() : null;

		// 2. Conteo acumulado: C[v] += C[v − 1].
		for (int i = 1; i <= maxValue; i++) {
			count[i] += count[i - 1];
		}
		int[] cumulativeSnapshot = recordTrace ? count.clone() : null;

		// 3. Construcción de la salida de derecha a izquierda (estable): B[C[v] − 1] = A[i]; C[v]−−.
		List<CountingSortTrace.Placement> placements = recordTrace ? new ArrayList<>(n) : null;
		for (int i = n - 1; i >= 0; i--) {
			int outputIndex = count[array[i].frequency()] - 1;
			output[outputIndex] = array[i];
			count[array[i].frequency()]--;
			if (recordTrace) {
				placements.add(new CountingSortTrace.Placement(i, array[i], outputIndex));
			}
		}

		List<TermFrequency> outputList = List.of(output);
		CountingSortTrace trace = recordTrace
				? new CountingSortTrace(input, maxValue, countSnapshot, cumulativeSnapshot, placements, outputList)
				: null;
		return new SortResult<>(outputList, trace);
	}

}
