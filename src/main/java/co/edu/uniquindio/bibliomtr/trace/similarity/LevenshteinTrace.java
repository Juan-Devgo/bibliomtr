package co.edu.uniquindio.bibliomtr.trace.similarity;

import java.util.List;

/**
 * Traza de la distancia de Levenshtein: secuencias, matriz de programación dinámica y distancia.
 *
 * <p><b>Qué guarda.</b> Con m = |a| y n = |b|, {@code matrix} es la matriz D de (m + 1) × (n + 1) en la que
 * {@code D[i][j]} es la distancia entre los prefijos {@code a[0..i)} y {@code b[0..j)}; {@code distance} es
 * {@code D[m][n]} y {@code maxLength} es {@code max(m, n)}, el denominador de la normalización sugerida.
 * Ejemplo (AC-7): g,a,t,o y p,a,t,o dan una matriz 5 × 5 con {@code D[4][4] = 1} y {@code maxLength = 4}.
 *
 * <p>Complejidad: construcción O(m · n) en tiempo y espacio por las copias defensivas.
 *
 * @param a         primera secuencia
 * @param b         segunda secuencia
 * @param matrix    matriz D de (m + 1) × (n + 1)
 * @param distance  distancia de Levenshtein D[m][n]
 * @param maxLength max(m, n)
 */
public record LevenshteinTrace(List<String> a, List<String> b, int[][] matrix, int distance, int maxLength) {

	/**
	 * Copia defensiva de listas y matriz para que la traza sea inmutable.
	 *
	 * <p>Complejidad: O(m · n) en tiempo y espacio.
	 */
	public LevenshteinTrace {
		a = List.copyOf(a);
		b = List.copyOf(b);
		matrix = copy(matrix);
	}

	/**
	 * Matriz D de programación dinámica.
	 *
	 * <p>Complejidad: O(m · n) en tiempo y espacio (copia).
	 *
	 * @return copia de la matriz
	 */
	@Override
	public int[][] matrix() {
		return copy(matrix);
	}

	/** Copia profunda de una matriz; O(m · n) en tiempo y espacio. */
	private static int[][] copy(int[][] source) {
		int[][] target = new int[source.length][];
		for (int i = 0; i < source.length; i++) {
			target[i] = source[i].clone();
		}
		return target;
	}

}
