package co.edu.uniquindio.bibliomtr.algorithms.stringmatching;

/**
 * Opciones de una búsqueda de patrones: el módulo q de Rabin-Karp y si se graba la traza (REQ-6, REQ-29).
 *
 * <p><b>Parámetros de Rabin-Karp.</b> La base d = {@value #D} es fija: es el tamaño del alfabeto de {@code char}
 * (2¹⁶), así cada carácter es un dígito del número en base d. El módulo q es un primo configurable, por defecto
 * {@value #DEFAULT_Q}, pequeño para que las colisiones de hash sean observables en el corpus; la API lo acepta hasta
 * {@value #MAX_Q}. Con q ≤ {@value #MAX_Q} y d = {@value #D} los productos del rolling hash caben en {@code long}
 * (constitución P12). KMP y Boyer-Moore ignoran q.
 *
 * <p><b>Traza.</b> Con {@code recordTrace = false} (búsqueda y mediciones de tiempo) la búsqueda devuelve la traza
 * en {@code null} y el preprocesamiento no guarda los pasos de construcción, para no medir el costo de grabarlos
 * (REQ-29). Con {@code true} (endpoint de traza) se graban todos los pasos.
 *
 * <p>Complejidad: construcción O(1) en tiempo y espacio.
 *
 * @param q           módulo de Rabin-Karp, q ≥ 2 (la API exige además que sea primo y ≤ {@value #MAX_Q})
 * @param recordTrace si se graban los pasos intermedios
 */
public record MatchOptions(long q, boolean recordTrace) {

	/** Base d del hash polinomial de Rabin-Karp: tamaño del alfabeto de {@code char} (2¹⁶). */
	public static final long D = 65_536L;

	/** Módulo q por defecto de Rabin-Karp. */
	public static final long DEFAULT_Q = 101L;

	/** Módulo q máximo que acepta la API (primo 10⁹ + 7). */
	public static final long MAX_Q = 1_000_000_007L;

	/**
	 * Rechaza un q menor que 2: con q = 1 todo hash vale 0 y el módulo no tiene sentido.
	 *
	 * <p>Complejidad: O(1) en tiempo y espacio.
	 *
	 * @throws IllegalArgumentException si q &lt; 2
	 */
	public MatchOptions {
		if (q < 2) {
			throw new IllegalArgumentException("El módulo q debe ser al menos 2; se recibió: " + q);
		}
	}

	/**
	 * Opciones por defecto: q = {@value #DEFAULT_Q} y sin traza.
	 *
	 * <p>Complejidad: O(1) en tiempo y espacio.
	 *
	 * @return {@code MatchOptions(101, false)}
	 */
	public static MatchOptions defaults() {
		return new MatchOptions(DEFAULT_Q, false);
	}

	/**
	 * Opciones con traza para el módulo dado.
	 *
	 * <p>Complejidad: O(1) en tiempo y espacio.
	 *
	 * @param q módulo de Rabin-Karp, q ≥ 2
	 * @return {@code MatchOptions(q, true)}
	 */
	public static MatchOptions tracing(long q) {
		return new MatchOptions(q, true);
	}

}
