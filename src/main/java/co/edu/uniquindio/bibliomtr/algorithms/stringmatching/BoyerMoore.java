package co.edu.uniquindio.bibliomtr.algorithms.stringmatching;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import co.edu.uniquindio.bibliomtr.trace.stringmatching.BoyerMoorePreprocessing;
import co.edu.uniquindio.bibliomtr.trace.stringmatching.BoyerMooreTrace;
import co.edu.uniquindio.bibliomtr.trace.stringmatching.BoyerMooreTrace.Alignment;
import co.edu.uniquindio.bibliomtr.trace.stringmatching.BoyerMooreTrace.CharComparison;

/**
 * Boyer-Moore (Requerimiento 4). STUB: los cuerpos de {@link #preprocess} y {@link #search} los escribe el
 * estudiante (constitución P1; ver {@code docs/guia-implementacion.md}, § 11).
 *
 * <p><b>Qué calcula.</b> Las posiciones iniciales de todas las ocurrencias exactas de un patrón P (|P| = m) en un
 * texto T (|T| = n), incluidas las solapadas. En cada alineación s compara P con T[s..s + m − 1] de derecha a
 * izquierda y, ante un desajuste, salta usando dos reglas.
 *
 * <p><b>Definición formal.</b>
 * <pre>
 *   Regla del carácter malo:  last(c) = máx { k : P[k] = c }, o −1 si c no está en P
 *                             salto_cm(j) = j − last(T[s + j])           (puede ser ≤ 0)
 *   Regla del sufijo bueno (fuerte): con P[j+1..m−1] ya coincidente y P[j] ≠ T[s + j],
 *                             salto_sb(j) = menor t &gt; 0 tal que P[j+1..m−1] vuelve a coincidir desplazado t
 *                             posiciones y el carácter que queda bajo T[s + j] es distinto de P[j]
 *                             (o el borde más largo de P que encaja como prefijo)
 *   Desplazamiento:           s ← s + máx(salto_cm(j), salto_sb(j), 1)
 *   Tras una ocurrencia:      s ← s + (m − borde(P)), borde(P) = longitud del borde propio más largo de P
 * </pre>
 * Por el máximo, el salto nunca baja de 1.
 *
 * <p><b>Preprocesamiento.</b> {@code preprocess(P)} lanza {@code IllegalArgumentException} si P es nulo o vacío y,
 * en O(m):
 * <ul>
 *   <li>arma la tabla del carácter malo como un mapa solo con los caracteres de P y su última posición, en orden de
 *       primera aparición (un carácter ausente vale −1 y no se guarda);</li>
 *   <li>arma la tabla del sufijo bueno fuerte indexada por la posición j del desajuste, con el preprocesamiento de
 *       bordes: el arreglo f de m + 1 posiciones (f[i] = inicio del borde más ancho de P[i..m − 1], f[m] = m + 1)
 *       y luego la tabla para los sufijos sin reaparición, rellenada con el borde de P;</li>
 *   <li>calcula borde(P) y el salto tras ocurrencia m − borde(P).</li>
 * </ul>
 * Devuelve {@link BoyerMoorePreprocessing}.
 *
 * <p><b>Búsqueda.</b> {@code search(T, preprocesado)} prueba las alineaciones s = 0, 1, … mientras s ≤ n − m,
 * reutilizando las tablas. Devuelve las posiciones ascendentes y {@code collisions} vacía.
 *
 * <p><b>Traza.</b> {@link BoyerMooreTrace}: un {@code Alignment} por alineación con s, los caracteres comparados de
 * derecha a izquierda, el índice j del desajuste (o la ocurrencia), el salto del carácter malo, el del sufijo bueno
 * y el salto aplicado; más la lista de ocurrencias. Con {@code recordTrace = false} la traza es {@code null}.
 *
 * <p><b>Caso a mano.</b> P = "abab" → last = {a: 2, b: 3}; f = [2, 3, 4, 4, 5]; sufijo bueno [2, 2, 4, 1];
 * borde(P) = 2; salto tras ocurrencia 4 − 2 = 2. En T = "abababca": s = 0 (ocurrencia, salto 2), s = 2
 * (ocurrencia, salto 2), s = 4 (desajuste en j = 3 con 'a': carácter malo 3 − 2 = 1, sufijo bueno 1, salto 1);
 * posiciones [0, 2].
 *
 * <p><b>Casos de borde.</b> Patrón vacío → {@code IllegalArgumentException}. Texto vacío o n &lt; m → [].
 * "abc"/"abc" → [0]; "aaaa"/"aa" → [0, 1, 2] (solapadas); "Algoritmo algoritmo"/"algoritmo" → [10] (distingue
 * mayúsculas); "canción cancion"/"cancion" → [8] (distingue tildes); "a b c"/" " → [1, 3]. Dos abstracts
 * idénticos con el mismo preprocesado dan las mismas posiciones.
 *
 * <p><b>Complejidad del preprocesamiento: TODO estudiante</b> (tiempo y espacio en función de m y del número de
 * caracteres distintos de P).
 *
 * <p><b>Complejidad de la búsqueda: TODO estudiante</b> (mejor caso, peor caso y espacio en función de n y m;
 * compare el texto aleatorio con el repetitivo de JMH).
 */
public final class BoyerMoore implements StringMatcher<BoyerMoorePreprocessing, BoyerMooreTrace> {

	/**
	 * Construye las tablas del carácter malo y del sufijo bueno, y el salto tras ocurrencia.
	 *
	 * <p>Complejidad: TODO estudiante.
	 *
	 * @param pattern patrón no vacío
	 * @param options grabación de traza (q no se usa)
	 * @return las tablas del patrón
	 * @throws IllegalArgumentException si el patrón es nulo o vacío
	 */
	@Override
	public BoyerMoorePreprocessing preprocess(String pattern, MatchOptions options) {

  	if (pattern == null || pattern.isEmpty()) {
  		throw new IllegalArgumentException("El patrón no puede ser nulo ni vacío");
  	}
  	int m = pattern.length();

  	// Carácter malo: última posición de cada carácter de P. Re-insertar una clave existente conserva su lugar
  	// en el LinkedHashMap, así que el orden queda por primera aparición. Un carácter ausente vale −1 y no se guarda.
  	Map<Character, Integer> lastOccurrence = new LinkedHashMap<>();
  	for (int k = 0; k < m; k++) {
  		lastOccurrence.put(pattern.charAt(k), k);
  	}

  	// Bordes: f[i] = inicio del borde más ancho de P[i..m−1] (f[m] = m + 1 por convención).
  	// shift[i] = desplazamiento de la regla del sufijo bueno cuando el desajuste ocurre en la posición i − 1.
  	int[] borderPositions = new int[m + 1];
  	int[] shift = new int[m + 1];
  	int i = m;
  	int j = m + 1;
  	borderPositions[i] = j;
  	while (i > 0) {
  		// Mientras el carácter anterior al sufijo no coincida con el anterior al borde, el borde no se extiende:
  		// ese sufijo reaparece desplazado j − i (caso 1: el sufijo coincidente reaparece en otro lugar de P).
  		while (j <= m && pattern.charAt(i - 1) != pattern.charAt(j - 1)) {
  			if (shift[j] == 0) {
  				shift[j] = j - i;
  			}
  			j = borderPositions[j];
  		}
  		i--;
  		j--;
  		borderPositions[i] = j;
  	}

  	// Caso 2: sufijos que no reaparecen. Se rellenan con el borde más ancho de todo P que encaje como prefijo.
  	j = borderPositions[0];
  	for (i = 0; i <= m; i++) {
  		if (shift[i] == 0) {
  			shift[i] = j;
  		}
  		if (i == j) {
  			j = borderPositions[j];
  		}
  	}

  	// La tabla del sufijo bueno se indexa por la posición j del desajuste: shift[j + 1].
  	int[] goodSuffixShift = new int[m];
  	for (int k = 0; k < m; k++) {
  		goodSuffixShift[k] = shift[k + 1];
  	}

  	int patternBorder = m - borderPositions[0]; // longitud del borde propio más largo de P
  	int matchShift = m - patternBorder; // salto tras una ocurrencia completa

  	return new BoyerMoorePreprocessing(pattern, lastOccurrence, borderPositions, goodSuffixShift, patternBorder, matchShift);
	}

	/**
	 * Busca el patrón preprocesado comparando de derecha a izquierda en cada alineación.
	 *
	 * <p>Complejidad: TODO estudiante.
	 *
	 * @param text         texto donde se busca
	 * @param preprocessed resultado de {@link #preprocess}
	 * @param options      grabación de traza (q no se usa)
	 * @return posiciones ascendentes, colisiones vacías y la traza {@link BoyerMooreTrace} (o {@code null})
	 */
	@Override
	public MatchResult<BoyerMooreTrace> search(String text, BoyerMoorePreprocessing preprocessed, MatchOptions options) {
	
    String pattern = preprocessed.pattern();
		Map<Character, Integer> lastOccurrence = preprocessed.lastOccurrence();
		int[] goodSuffixShift = preprocessed.goodSuffixShift();
		int matchShift = preprocessed.matchShift();
		int n = text.length();
		int m = pattern.length();
		List<Integer> positions = new ArrayList<>();
		List<Alignment> alignments = new ArrayList<>();

		int s = 0; // alineación: P[0] sobre T[s]
		while (s <= n - m) {
			List<CharComparison> comparisons = new ArrayList<>();
			int j = m - 1;
			// Se compara de derecha a izquierda hasta el primer desajuste o hasta agotar el patrón.
			while (j >= 0) {
				char textChar = text.charAt(s + j);
				char patternChar = pattern.charAt(j);
				boolean matched = textChar == patternChar;
				if (options.recordTrace()) {
					comparisons.add(new CharComparison(j, s + j, textChar, patternChar, matched));
				}
				if (!matched) {
					break;
				}
				j--;
			}

			if (j < 0) {
				// Ocurrencia: se salta m − borde(P) para no perder las solapadas.
				positions.add(s);
				if (options.recordTrace()) {
					alignments.add(new Alignment(s, comparisons, null, true, null, matchShift, matchShift));
				}
				s += matchShift;
			} else {
				// Desajuste en j: carácter malo j − last(T[s + j]) (last = −1 si no está en P) y sufijo bueno de la tabla.
				Integer last = lastOccurrence.get(text.charAt(s + j));
				int badCharacterShift = j - (last == null ? -1 : last);
				int suffixShift = goodSuffixShift[j];
				int appliedShift = Math.max(Math.max(badCharacterShift, suffixShift), 1); // siempre avanza
				if (options.recordTrace()) {
					alignments.add(new Alignment(s, comparisons, j, false, badCharacterShift, suffixShift, appliedShift));
				}
				s += appliedShift;
			}
		}
		
		BoyerMooreTrace trace = options.recordTrace() ? new BoyerMooreTrace(alignments, positions) : null;
		return new MatchResult<>(positions, List.of(), trace);
	}

}
