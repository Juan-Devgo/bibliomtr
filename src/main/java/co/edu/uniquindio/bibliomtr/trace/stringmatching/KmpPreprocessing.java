package co.edu.uniquindio.bibliomtr.trace.stringmatching;

import java.util.List;

/**
 * Preprocesamiento de Knuth-Morris-Pratt: la función de prefijo π del patrón y los pasos de su construcción
 * (REQ-23).
 *
 * <p><b>Definición.</b> π[i] es la longitud del prefijo propio más largo de P[0..i] que también es sufijo de
 * P[0..i]; π[0] = 0.
 *
 * <p><b>Ejemplo.</b> P = "abab" → {@code pi = [0, 0, 1, 2]}; los pasos son i = 1 ('b', candidatos [0], valor 0),
 * i = 2 ('a', candidatos [0], valor 1) e i = 3 ('b', candidatos [1], valor 2).
 *
 * <p>Complejidad: construcción O(m + Σ candidatos) en tiempo y espacio por las copias defensivas.
 *
 * @param pattern patrón preprocesado, tal como llegó
 * @param pi      tabla π, de longitud m
 * @param steps   un paso por i = 1 … m − 1; vacía si {@code recordTrace = false}
 */
public record KmpPreprocessing(String pattern, int[] pi, List<PrefixStep> steps) {

	/**
	 * Copia defensiva de la tabla y de los pasos.
	 *
	 * <p>Complejidad: O(m) en tiempo y espacio.
	 */
	public KmpPreprocessing {
		pi = pi.clone();
		steps = List.copyOf(steps);
	}

	/**
	 * Tabla π.
	 *
	 * <p>Complejidad: O(m) en tiempo y espacio (copia).
	 *
	 * @return copia de π
	 */
	@Override
	public int[] pi() {
		return pi.clone();
	}

	/**
	 * Un paso de la construcción de π: el cálculo de π[i].
	 *
	 * <p><b>Candidatos.</b> Los valores de k probados en orden: el k inicial es π[i − 1] y, mientras k &gt; 0 y
	 * P[k] ≠ P[i], el siguiente es π[k − 1]. El último candidato es el k con el que termina el ciclo; si
	 * P[k] = P[i], π[i] = k + 1, si no π[i] = 0.
	 *
	 * <p>Complejidad: construcción O(c) en tiempo y espacio, con c = número de candidatos.
	 *
	 * @param i          posición del patrón, 1 ≤ i &lt; m
	 * @param character  P[i]
	 * @param candidates valores de k probados, en orden
	 * @param value      π[i]
	 */
	public record PrefixStep(int i, char character, List<Integer> candidates, int value) {

		/**
		 * Copia defensiva de los candidatos.
		 *
		 * <p>Complejidad: O(c) en tiempo y espacio.
		 */
		public PrefixStep {
			candidates = List.copyOf(candidates);
		}

	}

}
