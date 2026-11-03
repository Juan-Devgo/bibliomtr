package co.edu.uniquindio.bibliomtr.api.stringmatching;

import co.edu.uniquindio.bibliomtr.algorithms.stringmatching.MatchOptions;

/**
 * Reglas de los parámetros {@code pattern} y {@code q} de la búsqueda de patrones y de su traza (spec 004, REQ-15,
 * REQ-17, REQ-22; AC-13, AC-19, AC-25).
 *
 * <p><b>Reglas.</b>
 * <ul>
 *   <li>{@code pattern}: no nulo y de 1 a {@value #MAX_PATTERN_LENGTH} {@code char}, usado exactamente como llega,
 *       sin recortar espacios (" " es un patrón válido de 1 carácter).</li>
 *   <li>{@code q}: opcional; ausente vale {@value MatchOptions#DEFAULT_Q}. Si viene, debe ser un entero decimal
 *       primo en [2, {@value MatchOptions#MAX_Q}].</li>
 * </ul>
 * Un valor inválido lanza {@link IllegalArgumentException} con el mensaje en español, que
 * {@code ApiExceptionHandler} convierte en HTTP 400.
 *
 * <p><b>Por qué q llega como texto.</b> Así {@code q=abc} recibe el mismo mensaje que {@code q=100}, en lugar del
 * mensaje genérico de los parámetros enteros.
 */
public final class PatternSearchValidator {

	/** Longitud máxima del patrón, en {@code char}. */
	public static final int MAX_PATTERN_LENGTH = 200;

	private PatternSearchValidator() {
	}

	/**
	 * Valida el patrón y lo devuelve sin cambios.
	 *
	 * <p>Complejidad: O(1) en tiempo y espacio.
	 *
	 * @param raw patrón recibido; puede ser nulo
	 * @return el mismo patrón, sin recortar
	 * @throws IllegalArgumentException si es nulo, vacío o tiene más de {@value #MAX_PATTERN_LENGTH} {@code char}
	 */
	public static String pattern(String raw) {
		if (raw == null || raw.isEmpty()) {
			throw new IllegalArgumentException("El patrón no puede estar vacío");
		}
		if (raw.length() > MAX_PATTERN_LENGTH) {
			throw new IllegalArgumentException("El patrón tiene %d caracteres; el máximo es %d"
					.formatted(raw.length(), MAX_PATTERN_LENGTH));
		}
		return raw;
	}

	/**
	 * Convierte y valida el módulo q de Rabin-Karp.
	 *
	 * <p>Complejidad: O(√q) en tiempo por la prueba de primalidad (a lo sumo ≈ 16 000 divisiones con
	 * q ≤ {@value MatchOptions#MAX_Q}) y O(1) en espacio.
	 *
	 * @param raw q recibido; nulo si no vino
	 * @return {@value MatchOptions#DEFAULT_Q} si es nulo; si no, q
	 * @throws IllegalArgumentException si no es un entero decimal, está fuera de [2, {@value MatchOptions#MAX_Q}] o
	 *                                  es compuesto
	 */
	public static long q(String raw) {
		if (raw == null) {
			return MatchOptions.DEFAULT_Q;
		}
		long q;
		try {
			q = Long.parseLong(raw);
		} catch (NumberFormatException e) {
			throw invalidQ(raw);
		}
		if (q < 2 || q > MatchOptions.MAX_Q || !isPrime(q)) {
			throw invalidQ(raw);
		}
		return q;
	}

	/**
	 * Primalidad por división de prueba: q ≥ 2 es primo si ni 2 ni ningún impar d ≤ √q lo divide.
	 *
	 * <p>Complejidad: O(√q) en tiempo y O(1) en espacio.
	 */
	private static boolean isPrime(long q) {
		if (q % 2 == 0) {
			return q == 2;
		}
		// d ≤ q / d equivale a d² ≤ q sin desbordar.
		for (long d = 3; d <= q / d; d += 2) {
			if (q % d == 0) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Error de q con el valor recibido.
	 *
	 * <p>Complejidad: O(1) en tiempo y espacio.
	 */
	private static IllegalArgumentException invalidQ(String raw) {
		return new IllegalArgumentException("El parámetro q debe ser un número primo entre 2 y %d; se recibió: %s"
				.formatted(MatchOptions.MAX_Q, raw));
	}

}
