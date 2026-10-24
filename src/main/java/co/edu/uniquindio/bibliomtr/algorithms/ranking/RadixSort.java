package co.edu.uniquindio.bibliomtr.algorithms.ranking;

import java.util.ArrayList;
import java.util.List;

import co.edu.uniquindio.bibliomtr.trace.ranking.RadixSortTrace;

/**
 * Radix Sort LSD en base 10 sobre pares (término, frecuencia), adaptado de
 * {@code docs/referencias/ordenamiento/RadixSort.java} (REQ-7, REQ-14).
 *
 * <p><b>Definición formal.</b> Sea {@code max} la frecuencia máxima. Para {@code exp = 1, 10, 100, …}
 * mientras {@code ⌊max / exp⌋ > 0}, se aplica una pasada de Counting Sort estable sobre el dígito
 * {@code δ(v) = ⌊v / exp⌋ mod 10}:
 * <ol>
 *   <li>Conteo: {@code C[δ]} = número de elementos con dígito {@code δ}, con {@code C} de tamaño b = 10.</li>
 *   <li>Acumulado: para {@code δ = 1..9}, {@code C[δ] += C[δ − 1]}.</li>
 *   <li>Salida: para {@code i = n−1 .. 0}, {@code B[C[δ(A[i])] − 1] = A[i]} y {@code C[δ]−−}; luego
 *       {@code A = B}.</li>
 * </ol>
 * Como cada pasada es estable, tras la pasada del dígito j el arreglo queda ordenado por los j dígitos menos
 * significativos, y tras d pasadas (d = dígitos de {@code max}) queda ordenado por la frecuencia completa,
 * conservando el orden de entrada en los empates.
 *
 * <p><b>Cambios respecto a la referencia.</b> Ordena pares por su frecuencia, trabaja sobre una copia de la
 * entrada, registra la traza si {@link SortOptions#recordTrace()} es {@code true} y agrega la guarda de
 * lista vacía. La lógica no cambia. {@link SortOptions#bucketSize()} se ignora.
 *
 * <p><b>Límite.</b> Como la referencia, usa {@code int exp} y {@code exp *= 10}: con un máximo mayor o igual
 * a 10⁹ el exponente desbordaría {@code int}. Las frecuencias del corpus están varios órdenes por debajo.
 *
 * <p>Complejidad: tiempo O(d · (n + b)) y espacio O(n + b), con n = número de términos, d = dígitos del
 * máximo y b = 10.
 */
public final class RadixSort implements TermSorter<RadixSortTrace> {

	/** Base de numeración de los dígitos (REQ-7). */
	private static final int BASE = SortAlgorithm.RADIX_BASE;

	/**
	 * {@inheritDoc}
	 *
	 * <p>Complejidad: O(1) en tiempo y espacio.
	 */
	@Override
	public SortAlgorithm algorithm() {
		return SortAlgorithm.RADIX;
	}

	/**
	 * Ordena ascendente y de forma estable por frecuencia con Radix Sort LSD en base 10; no modifica
	 * {@code input}.
	 *
	 * <p>Complejidad: tiempo O(d · (n + b)) y espacio O(n + b); con traza, el espacio es O(d · (n + b))
	 * porque se guarda cada pasada.
	 *
	 * @param input   pares a ordenar (puede ser vacía)
	 * @param options solo se usa {@code recordTrace}
	 * @return lista ordenada y traza ({@code null} si no se pidió)
	 */
	@Override
	public SortResult<RadixSortTrace> sort(List<TermFrequency> input, SortOptions options) {
		boolean recordTrace = options.recordTrace();
		// Guarda agregada: la referencia accede a array[0] sin comprobar que exista.
		if (input.isEmpty()) {
			RadixSortTrace trace = recordTrace ? new RadixSortTrace(List.of(), 0, List.of(), List.of()) : null;
			return new SortResult<>(List.of(), trace);
		}
		// Copia de trabajo: las pasadas reescriben el arreglo y la entrada no debe cambiar.
		TermFrequency[] array = input.toArray(new TermFrequency[0]);

		int maxValue = array[0].frequency();
		for (TermFrequency element : array) {
			if (element.frequency() > maxValue) {
				maxValue = element.frequency();
			}
		}
		List<RadixSortTrace.Pass> passes = recordTrace ? new ArrayList<>() : null;
		// Una pasada por dígito, del menos al más significativo, mientras ⌊max / exp⌋ > 0.
		for (int exp = 1; maxValue / exp > 0; exp *= BASE) {
			RadixSortTrace.Pass pass = countingSortByDigit(array, exp, recordTrace);
			if (recordTrace) {
				passes.add(pass);
			}
		}

		List<TermFrequency> outputList = List.of(array);
		RadixSortTrace trace = recordTrace ? new RadixSortTrace(input, maxValue, passes, outputList) : null;
		return new SortResult<>(outputList, trace);
	}

	/**
	 * Pasada de Counting Sort estable sobre el dígito {@code ⌊v / exp⌋ mod 10}; reescribe {@code array}.
	 *
	 * <p>Complejidad: tiempo O(n + b) y espacio O(n + b).
	 *
	 * @return la pasada registrada, o {@code null} si no se registra la traza
	 */
	private static RadixSortTrace.Pass countingSortByDigit(TermFrequency[] array, int exp, boolean recordTrace) {
		int n = array.length;
		TermFrequency[] output = new TermFrequency[n];
		int[] count = new int[BASE];
		int[] digits = recordTrace ? new int[n] : null;

		// 1. Conteo de cada dígito δ = ⌊v / exp⌋ mod 10.
		for (int i = 0; i < n; i++) {
			int digit = (array[i].frequency() / exp) % BASE;
			count[digit]++;
			if (recordTrace) {
				digits[i] = digit;
			}
		}
		int[] countSnapshot = recordTrace ? count.clone() : null;

		// 2. Acumulado: C[δ] += C[δ − 1].
		for (int i = 1; i < BASE; i++) {
			count[i] += count[i - 1];
		}
		int[] cumulativeSnapshot = recordTrace ? count.clone() : null;

		// 3. Salida de derecha a izquierda (estable): B[C[δ] − 1] = A[i]; C[δ]−−.
		for (int i = n - 1; i >= 0; i--) {
			int digit = (array[i].frequency() / exp) % BASE;
			output[count[digit] - 1] = array[i];
			count[digit]--;
		}

		// 4. Copia de la salida al arreglo para la siguiente pasada.
		for (int i = 0; i < n; i++) {
			array[i] = output[i];
		}

		return recordTrace
				? new RadixSortTrace.Pass(exp, digits, countSnapshot, cumulativeSnapshot, List.of(array))
				: null;
	}

}
