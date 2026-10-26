package co.edu.uniquindio.bibliomtr.trace.ranking;

import java.util.List;

import co.edu.uniquindio.bibliomtr.algorithms.ranking.TermFrequency;

/**
 * Traza de Radix Sort LSD en base 10 (REQ-14): una entrada por pasada con su exponente, el dígito de cada
 * elemento, el conteo, el acumulado y el arreglo resultante.
 *
 * <p><b>Qué guarda.</b> Las pasadas usan {@code exp} = 1, 10, 100, … mientras {@code ⌊max / exp⌋ > 0}; en
 * cada una el dígito es {@code ⌊v / exp⌋ mod 10} (AC-11: [170, 45, 75, 90, 802, 24, 2, 66] da 3 pasadas y
 * tras {@code exp = 1} el arreglo [170, 90, 802, 2, 24, 45, 75, 66]). Lista vacía: listas vacías,
 * {@code max = 0} y ninguna pasada.
 *
 * <p>Complejidad: construcción O(d · (n + b)) en tiempo y espacio por las copias defensivas, con
 * d = dígitos del máximo y b = 10.
 *
 * @param input  entrada recibida por el algoritmo
 * @param max    frecuencia máxima, que fija el número de pasadas
 * @param passes pasadas en orden de exponente creciente
 * @param output salida ordenada ascendente y estable
 */
public record RadixSortTrace(List<TermFrequency> input, int max, List<Pass> passes, List<TermFrequency> output) {

	/**
	 * Copia defensiva de las listas para que la traza sea inmutable.
	 *
	 * <p>Complejidad: O(n + d) en tiempo y espacio.
	 */
	public RadixSortTrace {
		input = List.copyOf(input);
		passes = List.copyOf(passes);
		output = List.copyOf(output);
	}

	/**
	 * Una pasada de Counting Sort estable sobre el dígito {@code ⌊v / exp⌋ mod 10}.
	 *
	 * <p>Complejidad: construcción O(n + b) en tiempo y espacio por las copias defensivas.
	 *
	 * @param exp        exponente de la pasada (1, 10, 100, …)
	 * @param digits     dígito de cada elemento, en el orden del arreglo al iniciar la pasada
	 * @param count      conteo de cada dígito 0–9
	 * @param cumulative conteo acumulado de cada dígito 0–9
	 * @param result     arreglo al terminar la pasada
	 */
	public record Pass(int exp, int[] digits, int[] count, int[] cumulative, List<TermFrequency> result) {

		/**
		 * Copia defensiva de arreglos y lista.
		 *
		 * <p>Complejidad: O(n + b) en tiempo y espacio.
		 */
		public Pass {
			digits = digits.clone();
			count = count.clone();
			cumulative = cumulative.clone();
			result = List.copyOf(result);
		}

		/**
		 * Dígitos de la pasada.
		 *
		 * <p>Complejidad: O(n) en tiempo y espacio (copia).
		 *
		 * @return copia de los dígitos
		 */
		@Override
		public int[] digits() {
			return digits.clone();
		}

		/**
		 * Conteo de dígitos.
		 *
		 * <p>Complejidad: O(b) en tiempo y espacio (copia).
		 *
		 * @return copia del conteo
		 */
		@Override
		public int[] count() {
			return count.clone();
		}

		/**
		 * Conteo acumulado de dígitos.
		 *
		 * <p>Complejidad: O(b) en tiempo y espacio (copia).
		 *
		 * @return copia del acumulado
		 */
		@Override
		public int[] cumulative() {
			return cumulative.clone();
		}

	}

}
