package co.edu.uniquindio.bibliomtr.algorithms.stringmatching;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Ejemplo reproducible de {@code docs/guia-implementacion.md}, § 11: un fragmento real de un abstract del corpus,
 * un patrón que aparece 2 veces en él y las tablas de los 3 algoritmos calculadas a mano (spec 004, REQ-32).
 *
 * <p><b>Origen.</b> El fragmento se extrajo una vez con {@code PdfArticleExtractor.extract} del abstract de
 * {@value #PDF_FILE} y se copió literal ({@value #FRAGMENT_LENGTH} {@code char}, solo ASCII). Lo usan los tests de
 * estudiante de cada algoritmo, el oráculo y, en la fase de API, los abstracts de prueba.
 * {@code oracle/GuideFragmentCorpusTest} comprueba que sigue siendo parte del abstract extraído (se omite sin el
 * PDF, que no se versiona).
 *
 * <p><b>Patrón.</b> P = {@value #PATTERN} (m = 7): s0 y1 s2 t3 e4 m5 s6. Aparece en s = 214 ("adaptive
 * systems") y s = 271 ("tutoring systems"); el prefijo "sy" de "synthesis" (s = 4) es una coincidencia parcial.
 *
 * <p><b>KMP, función de prefijo π</b> (π[i] = prefijo propio más largo de P[0..i] que también es sufijo):
 * <pre>
 *   i = 1 'y': k = π[0] = 0; P[0] = 's' ≠ 'y'                          → π[1] = 0   candidatos [0]
 *   i = 2 's': k = π[1] = 0; P[0] = 's' = 's'                          → π[2] = 1   candidatos [0]
 *   i = 3 't': k = π[2] = 1; P[1] = 'y' ≠ 't', k ← π[0] = 0; 's' ≠ 't'   → π[3] = 0   candidatos [1, 0]
 *   i = 4 'e': k = 0; 's' ≠ 'e'                                         → π[4] = 0   candidatos [0]
 *   i = 5 'm': k = 0; 's' ≠ 'm'                                         → π[5] = 0   candidatos [0]
 *   i = 6 's': k = 0; 's' = 's'                                         → π[6] = 1   candidatos [0]
 *   π = [0, 0, 1, 0, 0, 0, 1]
 * </pre>
 *
 * <p><b>Boyer-Moore.</b>
 * <ul>
 *   <li>Carácter malo (última posición, en orden de primera aparición): {s: 6, y: 1, t: 3, e: 4, m: 5}; cualquier
 *       otro carácter vale −1.</li>
 *   <li>Bordes: f[i] = m − borde(P[i..6]) para i &lt; m y f[7] = m + 1 = 8: "systems" tiene borde "s" → f[0] = 6;
 *       "ystems" → 7; "stems" tiene borde "s" → f[2] = 6; "tems", "ems", "ms", "s" → 7. f = [6, 7, 6, 7, 7, 7, 7,
 *       8].</li>
 *   <li>Sufijo bueno fuerte (desajuste en j, sufijo P[j+1..6] ya coincidente): j = 6 (sufijo vacío): t = 1, porque
 *       P[5] = 'm' ≠ 's'; j = 5 (sufijo "s"): "s" reaparece en 2 con P[1] = 'y' ≠ 'm' → t = 6 − 2 = 4;
 *       j = 0 … 4 (sufijos "ms" … "ystems"): no reaparecen y el borde más largo que encaja como prefijo es "s" →
 *       t = 7 − 1 = 6. Tabla [6, 6, 6, 6, 6, 4, 1].</li>
 *   <li>borde(P) = 1 ("s"); salto tras ocurrencia m − borde(P) = 7 − 1 = 6.</li>
 * </ul>
 *
 * <p><b>Rabin-Karp</b> con d = 65 536 y q = 101 ('s' = 115 ≡ 14, 'y' = 121 ≡ 20, 't' = 116 ≡ 15, 'e' = 101 ≡ 0,
 * 'm' = 109 ≡ 8):
 * <pre>
 *   d mod q = 65 536 − 648·101 = 88 ≡ −13
 *   h = d^6 mod q: (−13)² = 169 ≡ 68; (−13)³ ≡ 68·(−13) = −884 ≡ 25; (−13)⁶ ≡ 25² = 625 ≡ 19   → h = 19
 *   Horner, t ← (d·t + P[i]) mod q: 14, 40, 100, 28, 40, 94, 4                            → hash(P) = 4
 * </pre>
 * Las ventanas en s = 214 y s = 271 tienen hash 4 y se verifican; hay 273 ventanas (s = 0 … n − m).
 *
 * <p>Los valores se verificaron además contra las definiciones por fuerza bruta (fuera del repositorio).
 *
 * <p>Complejidad: constantes; los métodos que devuelven arreglos copian en O(m).
 */
public final class GuideFragment {

	/** PDF del corpus del que sale el abstract. */
	public static final String PDF_FILE =
			"20. Systematic review of research on artificial intelligence applications in higher education where are the educators.pdf";

	/** Fragmento literal del abstract de {@link #PDF_FILE}. */
	public static final String FRAGMENT = "The synthesis of results presents four areas of AIEd applications in "
			+ "academic support services, and institutional and administrative services: 1. profiling and prediction, "
			+ "2. assessment and evaluation, 3. adaptive systems and personalisation, and 4. intelligent tutoring "
			+ "systems.";

	/** Longitud del fragmento en {@code char}. */
	public static final int FRAGMENT_LENGTH = 279;

	/** Patrón del ejemplo. */
	public static final String PATTERN = "systems";

	/** Posiciones de {@link #PATTERN} en {@link #FRAGMENT}. */
	public static final List<Integer> POSITIONS = List.of(214, 271);

	/** Número de ventanas de Rabin-Karp: n − m + 1. */
	public static final int WINDOW_COUNT = 273;

	/** Candidatos k probados al calcular π[i], para i = 1 … m − 1. */
	public static final List<List<Integer>> PI_CANDIDATES =
			List.of(List.of(0), List.of(0), List.of(1, 0), List.of(0), List.of(0), List.of(0));

	/** Tabla del carácter malo, en orden de primera aparición. */
	public static final Map<Character, Integer> LAST_OCCURRENCE = lastOccurrence();

	/** borde(P): longitud del borde propio más largo de P. */
	public static final int PATTERN_BORDER = 1;

	/** Salto de Boyer-Moore tras una ocurrencia completa: m − borde(P). */
	public static final int MATCH_SHIFT = 6;

	/** Módulo q de Rabin-Karp del ejemplo (el valor por defecto). */
	public static final long Q = 101L;

	/** d mod q. */
	public static final long D_MOD_Q = 88L;

	/** h = d^(m−1) mod q. */
	public static final long H = 19L;

	/** hash(P) con q = 101. */
	public static final long PATTERN_HASH = 4L;

	private GuideFragment() {
	}

	/**
	 * Función de prefijo π del patrón.
	 *
	 * <p>Complejidad: O(m) en tiempo y espacio.
	 *
	 * @return [0, 0, 1, 0, 0, 0, 1]
	 */
	public static int[] pi() {
		return new int[] {0, 0, 1, 0, 0, 0, 1};
	}

	/**
	 * Arreglo f del preprocesamiento de bordes de Boyer-Moore.
	 *
	 * <p>Complejidad: O(m) en tiempo y espacio.
	 *
	 * @return [6, 7, 6, 7, 7, 7, 7, 8]
	 */
	public static int[] borderPositions() {
		return new int[] {6, 7, 6, 7, 7, 7, 7, 8};
	}

	/**
	 * Tabla del sufijo bueno fuerte, indexada por la posición j del desajuste.
	 *
	 * <p>Complejidad: O(m) en tiempo y espacio.
	 *
	 * @return [6, 6, 6, 6, 6, 4, 1]
	 */
	public static int[] goodSuffixShift() {
		return new int[] {6, 6, 6, 6, 6, 4, 1};
	}

	/** Mapa inmodificable {s: 6, y: 1, t: 3, e: 4, m: 5} que conserva el orden de primera aparición. */
	private static Map<Character, Integer> lastOccurrence() {
		Map<Character, Integer> last = new LinkedHashMap<>();
		last.put('s', 6);
		last.put('y', 1);
		last.put('t', 3);
		last.put('e', 4);
		last.put('m', 5);
		return Collections.unmodifiableMap(last);
	}

}
