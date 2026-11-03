package co.edu.uniquindio.bibliomtr.trace.stringmatching;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Preprocesamiento de Boyer-Moore: tabla del carácter malo, tabla del sufijo bueno (regla fuerte) y salto tras una
 * ocurrencia completa (REQ-5, REQ-24).
 *
 * <p><b>Ejemplo.</b> P = "abab" → {@code lastOccurrence = {a: 2, b: 3}}, {@code borderPositions = [2, 3, 4, 4,
 * 5]}, {@code goodSuffixShift = [2, 2, 4, 1]}, {@code patternBorder = 2} y {@code matchShift = 4 − 2 = 2}.
 *
 * <p>Complejidad: construcción O(m) en tiempo y espacio por las copias defensivas.
 *
 * @param pattern         patrón preprocesado, tal como llegó
 * @param lastOccurrence  última posición de cada carácter del patrón, solo con los caracteres del patrón y en orden
 *                        de primera aparición; un carácter ausente vale −1 y no está en el mapa
 * @param borderPositions arreglo f de m + 1 posiciones del preprocesamiento de bordes: f[i] es la posición donde
 *                        empieza el borde más ancho del sufijo P[i..m − 1] (f[m] = m + 1)
 * @param goodSuffixShift salto por desajuste en la posición j del patrón (regla fuerte), de longitud m
 * @param patternBorder   borde(P): longitud del borde propio más largo de P
 * @param matchShift      salto tras una ocurrencia completa, m − borde(P)
 */
public record BoyerMoorePreprocessing(String pattern, Map<Character, Integer> lastOccurrence, int[] borderPositions,
		int[] goodSuffixShift, int patternBorder, int matchShift) {

	/**
	 * Copia defensiva del mapa (inmodificable, en el mismo orden) y de los arreglos.
	 *
	 * <p>Complejidad: O(m) en tiempo y espacio.
	 */
	public BoyerMoorePreprocessing {
		lastOccurrence = Collections.unmodifiableMap(new LinkedHashMap<>(lastOccurrence));
		borderPositions = borderPositions.clone();
		goodSuffixShift = goodSuffixShift.clone();
	}

	/**
	 * Arreglo f del preprocesamiento de bordes.
	 *
	 * <p>Complejidad: O(m) en tiempo y espacio (copia).
	 *
	 * @return copia de f
	 */
	@Override
	public int[] borderPositions() {
		return borderPositions.clone();
	}

	/**
	 * Tabla del sufijo bueno indexada por la posición j del desajuste.
	 *
	 * <p>Complejidad: O(m) en tiempo y espacio (copia).
	 *
	 * @return copia de la tabla
	 */
	@Override
	public int[] goodSuffixShift() {
		return goodSuffixShift.clone();
	}

}
