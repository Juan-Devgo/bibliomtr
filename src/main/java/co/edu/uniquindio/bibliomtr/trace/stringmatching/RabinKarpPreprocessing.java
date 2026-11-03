package co.edu.uniquindio.bibliomtr.trace.stringmatching;

/**
 * Preprocesamiento de Rabin-Karp: parámetros del hash, factor h y hash del patrón (REQ-6, REQ-25).
 *
 * <p><b>Fórmulas.</b> hash(S) = (Σ S[i]·d^(m−1−i)) mod q y h = d^(m−1) mod q.
 *
 * <p><b>Ejemplo.</b> P = "abab", d = 65 536, q = 101: d mod q = 88, {@code h = 25} y {@code patternHash = 48}.
 *
 * <p>Complejidad: construcción O(1) en tiempo y espacio.
 *
 * @param pattern     patrón preprocesado, tal como llegó
 * @param d           base del hash, 65 536
 * @param q           módulo primo
 * @param h           d^(m−1) mod q
 * @param patternHash hash(P)
 */
public record RabinKarpPreprocessing(String pattern, long d, long q, long h, long patternHash) {
}
