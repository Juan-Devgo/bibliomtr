package co.edu.uniquindio.bibliomtr.trace.stringmatching;

import java.util.List;

/**
 * Traza de la búsqueda de Rabin-Karp: parámetros, hash inicial y una entrada por ventana T[s..s + m − 1]
 * (REQ-25).
 *
 * <p><b>Rolling hash.</b> t(s+1) = (d·(t(s) − T[s]·h) + T[s+m]) mod q, llevado a [0, q).
 *
 * <p><b>Ejemplos.</b> T = "abababca", P = "abab", q = 101: h = 25, hash(P) = 48, ventanas s = 0 … 4 con hashes
 * [48, 92, 48, 94, 21], coincidencias de hash en s = 0 y 2, ambas verificadas, sin colisiones. T = "abba",
 * P = "ab", q = 3: h = 1, hash(P) = 0, ventanas [0, 1, 0]; s = 2 es colisión, {@code collisions = [2]} y
 * {@code occurrences = [0]}.
 *
 * <p>Complejidad: construcción O(w + c + k) en tiempo y espacio por las copias, con w = ventanas, c = colisiones y
 * k = ocurrencias.
 *
 * @param d                 base del hash
 * @param q                 módulo primo
 * @param h                 d^(m−1) mod q
 * @param patternHash       hash(P)
 * @param initialWindowHash hash de la primera ventana T[0..m − 1], o {@code null} si no hay ventanas (n &lt; m)
 * @param windows           una entrada por s = 0 … n − m
 * @param collisions        desplazamientos con hash igual y texto distinto, ascendentes
 * @param occurrences       posiciones de las ocurrencias, ascendentes
 */
public record RabinKarpTrace(long d, long q, long h, long patternHash, Long initialWindowHash, List<Window> windows,
		List<Integer> collisions, List<Integer> occurrences) {

	/**
	 * Copia defensiva de las listas.
	 *
	 * <p>Complejidad: O(w + c + k) en tiempo y espacio.
	 */
	public RabinKarpTrace {
		windows = List.copyOf(windows);
		collisions = List.copyOf(collisions);
		occurrences = List.copyOf(occurrences);
	}

	/**
	 * Una ventana del texto en el desplazamiento s.
	 *
	 * <p>Complejidad: construcción O(1) en tiempo y espacio.
	 *
	 * @param shift       desplazamiento s
	 * @param hash        t(s), en [0, q)
	 * @param hashMatches si t(s) = hash(P)
	 * @param verified    resultado de la verificación carácter a carácter; {@code null} si {@code hashMatches} es
	 *                    {@code false} (no se verifica)
	 * @param collision   {@code hashMatches && !verified}: hash igual con texto distinto
	 */
	public record Window(int shift, long hash, boolean hashMatches, Boolean verified, boolean collision) {
	}

}
