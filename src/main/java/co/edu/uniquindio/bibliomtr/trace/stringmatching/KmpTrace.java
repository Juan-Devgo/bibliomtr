package co.edu.uniquindio.bibliomtr.trace.stringmatching;

import java.util.List;

/**
 * Traza de la búsqueda de Knuth-Morris-Pratt: una entrada por comparación de un carácter del texto con uno del
 * patrón (REQ-23).
 *
 * <p><b>Ejemplo.</b> T = "abababca", P = "abab" da 9 comparaciones con (i, j, s): (0,0,0) (1,1,0) (2,2,0)
 * (3,3,0) (4,2,2) (5,3,2) (6,2,4) (6,0,6) (7,0,7); coinciden todas salvo la 7.ª y la 8.ª; la 4.ª y la 6.ª
 * completan las ocurrencias en s = 0 y s = 2 y retroceden a j = 2; la 7.ª retrocede a j = 0;
 * {@code occurrences = [0, 2]}. El índice i nunca disminuye (REQ-4, REQ-11).
 *
 * <p>Complejidad: construcción O(c + k) en tiempo y espacio por las copias, con c = comparaciones y
 * k = ocurrencias.
 *
 * @param comparisons comparaciones, en el orden en que se hacen
 * @param occurrences posiciones de las ocurrencias, ascendentes
 */
public record KmpTrace(List<KmpComparison> comparisons, List<Integer> occurrences) {

	/**
	 * Copia defensiva de las listas.
	 *
	 * <p>Complejidad: O(c + k) en tiempo y espacio.
	 */
	public KmpTrace {
		comparisons = List.copyOf(comparisons);
		occurrences = List.copyOf(occurrences);
	}

	/**
	 * Una comparación T[i] con P[j].
	 *
	 * <p>Complejidad: construcción O(1) en tiempo y espacio.
	 *
	 * @param i           índice del texto
	 * @param j           índice del patrón
	 * @param shift       desplazamiento s = i − j del patrón respecto al texto, calculado por el backend
	 * @param textChar    T[i]
	 * @param patternChar P[j]
	 * @param matched     si T[i] = P[j]
	 * @param fallbackTo  j nuevo tras el retroceso j ← π[j − 1], aplicado por un desajuste con j &gt; 0 o tras
	 *                    completar una ocurrencia; {@code null} si no hubo retroceso
	 * @param occurrence  s de la ocurrencia que completa esta comparación; {@code null} si no completa ninguna
	 */
	public record KmpComparison(int i, int j, int shift, char textChar, char patternChar, boolean matched,
			Integer fallbackTo, Integer occurrence) {
	}

}
