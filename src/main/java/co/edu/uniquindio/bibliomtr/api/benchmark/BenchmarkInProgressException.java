package co.edu.uniquindio.bibliomtr.api.benchmark;

/**
 * Se pidió una medición JMH mientras otra está EN_CURSO (REQ-19, AC-14). {@code ApiExceptionHandler} la
 * traduce a HTTP 409 con el mensaje "Ya hay una medición en curso".
 */
public class BenchmarkInProgressException extends RuntimeException {

	/** Mensaje de la spec para el 409. */
	public static final String MESSAGE = "Ya hay una medición en curso";

	/**
	 * Crea la excepción con el mensaje de la spec.
	 *
	 * <p>Complejidad: O(1) en tiempo y espacio.
	 */
	public BenchmarkInProgressException() {
		super(MESSAGE);
	}

}
