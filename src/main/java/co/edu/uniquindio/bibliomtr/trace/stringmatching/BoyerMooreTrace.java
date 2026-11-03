package co.edu.uniquindio.bibliomtr.trace.stringmatching;

import java.util.List;

/**
 * Traza de la búsqueda de Boyer-Moore: una entrada por alineación del patrón con el texto (REQ-24).
 *
 * <p><b>Ejemplo.</b> T = "abababca", P = "abab": s = 0 (ocurrencia, salto 2), s = 2 (ocurrencia, salto 2) y s = 4
 * (desajuste en j = 3 con 'a': carácter malo 3 − 2 = 1, sufijo bueno 1, salto max(1, 1, 1) = 1);
 * {@code occurrences = [0, 2]}.
 *
 * <p>Complejidad: construcción O(a + c + k) en tiempo y espacio por las copias, con a = alineaciones,
 * c = comparaciones y k = ocurrencias.
 *
 * @param alignments  alineaciones, en orden de s creciente
 * @param occurrences posiciones de las ocurrencias, ascendentes
 */
public record BoyerMooreTrace(List<Alignment> alignments, List<Integer> occurrences) {

	/**
	 * Copia defensiva de las listas.
	 *
	 * <p>Complejidad: O(a + k) en tiempo y espacio.
	 */
	public BoyerMooreTrace {
		alignments = List.copyOf(alignments);
		occurrences = List.copyOf(occurrences);
	}

	/**
	 * Una alineación del patrón en el desplazamiento s.
	 *
	 * <p><b>Con desajuste</b> en j: {@code mismatchIndex = j}, {@code badCharacterShift = j − last(T[s + j])} (puede
	 * ser ≤ 0), {@code goodSuffixShift = goodSuffixShift[j]} y {@code appliedShift = max(las dos, 1)}. <b>Con
	 * ocurrencia</b>: {@code mismatchIndex} y {@code badCharacterShift} son {@code null} y
	 * {@code goodSuffixShift = appliedShift = matchShift}.
	 *
	 * <p>Complejidad: construcción O(c) en tiempo y espacio, con c = comparaciones de la alineación.
	 *
	 * @param shift             desplazamiento s
	 * @param comparisons       caracteres comparados, de derecha a izquierda
	 * @param mismatchIndex     posición j del desajuste, o {@code null} si hubo ocurrencia
	 * @param occurrence        si el patrón coincide completo en s
	 * @param badCharacterShift salto de la regla del carácter malo, o {@code null} si hubo ocurrencia
	 * @param goodSuffixShift   salto de la regla del sufijo bueno (o {@code matchShift} tras una ocurrencia)
	 * @param appliedShift      salto aplicado: s siguiente = s + {@code appliedShift}
	 */
	public record Alignment(int shift, List<CharComparison> comparisons, Integer mismatchIndex, boolean occurrence,
			Integer badCharacterShift, Integer goodSuffixShift, int appliedShift) {

		/**
		 * Copia defensiva de las comparaciones.
		 *
		 * <p>Complejidad: O(c) en tiempo y espacio.
		 */
		public Alignment {
			comparisons = List.copyOf(comparisons);
		}

	}

	/**
	 * Una comparación P[j] con T[s + j].
	 *
	 * <p>Complejidad: construcción O(1) en tiempo y espacio.
	 *
	 * @param j           índice del patrón
	 * @param textIndex   índice del texto, s + j
	 * @param textChar    T[s + j]
	 * @param patternChar P[j]
	 * @param matched     si coinciden
	 */
	public record CharComparison(int j, int textIndex, char textChar, char patternChar, boolean matched) {
	}

}
