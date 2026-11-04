package co.edu.uniquindio.bibliomtr.algorithms.stringmatching;

import java.util.List;
import java.util.ArrayList;

import co.edu.uniquindio.bibliomtr.trace.stringmatching.RabinKarpPreprocessing;
import co.edu.uniquindio.bibliomtr.trace.stringmatching.RabinKarpTrace;
import co.edu.uniquindio.bibliomtr.trace.stringmatching.RabinKarpTrace.Window;

/**
 * Rabin-Karp (Requerimiento 4). STUB: los cuerpos de {@link #preprocess} y {@link #search} los escribe el
 * estudiante (constitución P1; ver {@code docs/guia-implementacion.md}, § 11).
 *
 * <p><b>Qué calcula.</b> Las posiciones iniciales de todas las ocurrencias exactas de un patrón P (|P| = m) en un
 * texto T (|T| = n), incluidas las solapadas. Compara el hash de cada ventana T[s..s + m − 1] con el de P y solo
 * verifica carácter a carácter las ventanas con el mismo hash; las que tienen hash igual y texto distinto son
 * colisiones.
 *
 * <p><b>Definición formal.</b> Con d = 65 536 (tamaño del alfabeto de {@code char}, fijo,
 * {@link MatchOptions#D}) y q primo (por defecto q = 101, {@link MatchOptions#DEFAULT_Q}):
 * <pre>
 *   h(S)   = (Σ_{i=0}^{m−1} S[i]·d^(m−1−i)) mod q        (hash polinomial modular)
 *   h      = d^(m−1) mod q
 *   t(0)   = h(T[0..m−1])
 *   t(s+1) = (d·(t(s) − T[s]·h) + T[s+m]) mod q          (rolling hash), llevado a [0, q)
 * </pre>
 * Si t(s) = h(P) se verifica P = T[s..s + m − 1] carácter a carácter: si coinciden es una ocurrencia; si no, una
 * colisión. Use {@code long} y {@code Math.floorMod} para que el resultado quede en [0, q); con q ≤ 1 000 000 007 y
 * d = 65 536 los productos caben en {@code long}.
 *
 * <p><b>Preprocesamiento.</b> {@code preprocess(P, opciones)} lanza {@code IllegalArgumentException} si P es nulo o
 * vacío y calcula h = d^(m−1) mod q y h(P) con la regla de Horner, en O(m). Devuelve
 * {@link RabinKarpPreprocessing} con d, q, h y h(P).
 *
 * <p><b>Búsqueda.</b> {@code search(T, preprocesado)} calcula t(0) y desliza la ventana con el rolling hash para
 * s = 0 … n − m, verificando solo las coincidencias de hash. Devuelve las posiciones ascendentes y en
 * {@code collisions} los s de las colisiones, ascendentes.
 *
 * <p><b>Traza.</b> {@link RabinKarpTrace}: d, q, h, h(P), el hash inicial t(0) y un {@code Window} por s con su
 * hash, si coincide con h(P), el resultado de la verificación ({@code null} si no se verificó) y si es colisión;
 * más las colisiones y las ocurrencias. Con {@code recordTrace = false} la traza es {@code null}.
 *
 * <p><b>Caso a mano.</b> T = "abababca", P = "abab", q = 101: d mod q = 88, h = 25, h(P) = 48, hashes de las
 * ventanas s = 0 … 4: [48, 92, 48, 94, 21]; coincidencias de hash en s = 0 y 2, ambas verificadas; posiciones
 * [0, 2], sin colisiones. Colisión: T = "abba", P = "ab", q = 3: h = 1, h(P) = 0, ventanas [0, 1, 0]; s = 2 es
 * colisión; posiciones [0], colisiones [2].
 *
 * <p><b>Casos de borde.</b> Patrón vacío → {@code IllegalArgumentException}. Texto vacío o n &lt; m → [] (sin
 * ventanas, t(0) es {@code null} en la traza). "abc"/"abc" → [0]; "aaaa"/"aa" → [0, 1, 2] (solapadas);
 * "Algoritmo algoritmo"/"algoritmo" → [10] (distingue mayúsculas); "canción cancion"/"cancion" → [8] (distingue
 * tildes); "a b c"/" " → [1, 3]. Dos abstracts idénticos con el mismo preprocesado dan las mismas posiciones.
 *
 * <p><b>Complejidad del preprocesamiento: TODO estudiante</b> (tiempo y espacio en función de m).
 *
 * <p><b>Complejidad de la búsqueda: TODO estudiante</b> (caso esperado y peor caso en función de n, m y el número
 * de coincidencias de hash; explique el efecto de q en las colisiones).
 */
public final class RabinKarp implements StringMatcher<RabinKarpPreprocessing, RabinKarpTrace> {

	/**
	 * Calcula h = d^(m−1) mod q y el hash del patrón.
	 *
	 * <p>Complejidad: TODO estudiante.
	 *
	 * @param pattern patrón no vacío
	 * @param options módulo q y grabación de traza
	 * @return d, q, h y el hash del patrón
	 * @throws IllegalArgumentException si el patrón es nulo o vacío
	 */
	@Override
	public RabinKarpPreprocessing preprocess(String pattern, MatchOptions options) {

  	if (pattern == null || pattern.isEmpty()) {
  		throw new IllegalArgumentException("El patrón no puede ser nulo ni vacío");
  	}
  	long d = MatchOptions.D;
  	long q = options.q();
  	int m = pattern.length();

  	// h = d^(m−1) mod q, multiplicando de a uno y reduciendo en cada paso para no desbordar.
  	long h = 1;
  	for (int i = 0; i < m - 1; i++) {
  		h = (h * d) % q;
  	}

  	// hash(P) con la regla de Horner: t ← (d·t + P[i]) mod q.
  	long patternHash = 0;
  	for (int i = 0; i < m; i++) {
  		patternHash = (d * patternHash + pattern.charAt(i)) % q;
  	}
  	return new RabinKarpPreprocessing(pattern, d, q, h, patternHash);
	}

	/**
	 * Busca el patrón preprocesado con el rolling hash y verifica cada coincidencia de hash.
	 *
	 * <p>Complejidad: TODO estudiante.
	 *
	 * @param text         texto donde se busca
	 * @param preprocessed resultado de {@link #preprocess}
	 * @param options      módulo q (el mismo del preprocesamiento) y grabación de traza
	 * @return posiciones ascendentes, colisiones ascendentes y la traza {@link RabinKarpTrace} (o {@code null})
	 */
	@Override
	public MatchResult<RabinKarpTrace> search(String text, RabinKarpPreprocessing preprocessed, MatchOptions options) {

  	String pattern = preprocessed.pattern();
  	long d = preprocessed.d();
  	long q = preprocessed.q();
  	long h = preprocessed.h();
  	long patternHash = preprocessed.patternHash();
  	int n = text.length();
  	int m = pattern.length();
  	List<Integer> positions = new ArrayList<>();
  	List<Integer> collisions = new ArrayList<>();
  	List<Window> windows = new ArrayList<>();
  	Long initialWindowHash = null;

  	if (n >= m) {
  		// t(0): hash de la primera ventana T[0..m−1] con Horner.
  		long windowHash = 0;
  		for (int i = 0; i < m; i++) {
  			windowHash = (d * windowHash + text.charAt(i)) % q;
  		}
  		initialWindowHash = windowHash;

  		for (int s = 0; s <= n - m; s++) {
  			boolean hashMatches = windowHash == patternHash;
  			Boolean verified = null;
  			boolean collision = false;
  			if (hashMatches) {
  				// Hash igual no implica texto igual: se verifica carácter a carácter.
  				verified = true;
  				for (int k = 0; k < m; k++) {
  					if (text.charAt(s + k) != pattern.charAt(k)) {
  						verified = false;
  						break;
  					}
  				}
  				if (verified) {
  					positions.add(s);
  				} else {
  					collision = true;
  					collisions.add(s);
  				}
  			}
  			if (options.recordTrace()) {
  				windows.add(new Window(s, windowHash, hashMatches, verified, collision));
  			}
  			// Rolling hash: quita T[s], desplaza una posición y agrega T[s + m]; floorMod lo deja en [0, q).
  			if (s < n - m) {
  				windowHash = Math.floorMod(d * (windowHash - text.charAt(s) * h) + text.charAt(s + m), q);
  			}
  		}
  	}
  	RabinKarpTrace trace = options.recordTrace()
  			? new RabinKarpTrace(preprocessed.d(), q, h, patternHash, initialWindowHash, windows, collisions, positions)
  			: null;
  	return new MatchResult<>(positions, collisions, trace);
	}

}
