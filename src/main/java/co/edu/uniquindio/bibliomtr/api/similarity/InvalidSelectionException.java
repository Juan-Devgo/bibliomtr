package co.edu.uniquindio.bibliomtr.api.similarity;

/**
 * Selección de artículos inválida para comparar o trazar (spec 001, REQ-15, AC-14): cantidad fuera de 2..20,
 * identificadores repetidos o artículos inexistentes o no seleccionables.
 *
 * <p><b>Por qué extiende {@link IllegalArgumentException}.</b> El {@code ApiExceptionHandler} existente ya
 * traduce {@link IllegalArgumentException} a HTTP 400 con su mensaje, que aquí siempre está en español.
 */
public class InvalidSelectionException extends IllegalArgumentException {

	private static final long serialVersionUID = 1L;

	/**
	 * Crea el rechazo con el mensaje que verá el usuario.
	 *
	 * <p>Complejidad: O(1) en tiempo y espacio.
	 *
	 * @param message mensaje en español, p. ej. "Debe seleccionar entre 2 y 20 artículos; se recibieron 1"
	 */
	public InvalidSelectionException(String message) {
		super(message);
	}

}
