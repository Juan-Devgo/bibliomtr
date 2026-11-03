package co.edu.uniquindio.bibliomtr.algorithms.stringmatching;

/**
 * Un algoritmo implementado devolvió posiciones que no cumplen el contrato de {@link StringMatcher}: nulas,
 * desordenadas, repetidas o fuera de [0, n − m] (REQ-8, AC-3).
 *
 * <p><b>Por qué.</b> Distingue un error del algoritmo del estudiante de un stub sin implementar
 * ({@code UnsupportedOperationException}): la API lo responde con HTTP 500 y el mensaje en español, que dice qué
 * posición está mal y en qué artículo.
 */
public class InvalidMatchPositionsException extends IllegalStateException {

	private static final long serialVersionUID = 1L;

	/**
	 * Crea la excepción con el mensaje en español.
	 *
	 * <p>Complejidad: O(1) en tiempo y espacio.
	 *
	 * @param message p. ej. "El algoritmo kmp devolvió posiciones inválidas en el artículo 3: …"
	 */
	public InvalidMatchPositionsException(String message) {
		super(message);
	}

}
