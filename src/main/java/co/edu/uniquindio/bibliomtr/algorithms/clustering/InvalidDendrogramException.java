package co.edu.uniquindio.bibliomtr.algorithms.clustering;

/**
 * Un algoritmo implementado devolvió un dendrograma que no cumple la convención de {@link Merge} (REQ-7, AC-10).
 *
 * <p><b>Por qué.</b> Distingue un error del algoritmo del estudiante de un stub sin implementar
 * ({@code UnsupportedOperationException}): la API lo responde con HTTP 500 y el mensaje en español, que dice
 * qué fusión está mal.
 */
public class InvalidDendrogramException extends IllegalStateException {

	private static final long serialVersionUID = 1L;

	/**
	 * Crea la excepción con el mensaje en español.
	 *
	 * <p>Complejidad: O(1) en tiempo y espacio.
	 *
	 * @param message p. ej. "El algoritmo single-linkage devolvió un dendrograma inválido: …"
	 */
	public InvalidDendrogramException(String message) {
		super(message);
	}

}
