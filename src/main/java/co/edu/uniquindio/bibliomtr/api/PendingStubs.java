package co.edu.uniquindio.bibliomtr.api;

/**
 * Reconoce la excepción de un stub sin implementar: {@link UnsupportedOperationException} cuyo mensaje empieza por
 * {@value #PREFIX} (spec 004, REQ-28).
 *
 * <p><b>Por qué aquí.</b> {@code ForbiddenStringMatchingUsageTest} prohíbe {@code .startsWith(} en
 * {@code api/stringmatching} (REQ-14), así que la comparación del prefijo vive fuera de ese paquete. No compara
 * texto ni patrón de una búsqueda: solo el mensaje de la excepción. Las specs 001 y 003 conservan su propia
 * comprobación, sin cambios.
 *
 * <p>Complejidad: O(p) en tiempo, con p = longitud del prefijo, y O(1) en espacio.
 */
public final class PendingStubs {

	/** Prefijo del mensaje de cada stub sin implementar. */
	public static final String PREFIX = "TODO estudiante:";

	private PendingStubs() {
	}

	/**
	 * Indica si la excepción la lanzó un stub sin implementar.
	 *
	 * <p>Complejidad: O(p) en tiempo y O(1) en espacio.
	 *
	 * @param e excepción lanzada por un algoritmo; puede ser nula
	 * @return {@code true} si su mensaje empieza por {@value #PREFIX}
	 */
	public static boolean isPending(UnsupportedOperationException e) {
		if (e == null) {
			return false;
		}
		String message = e.getMessage();
		return message != null && message.startsWith(PREFIX);
	}

}
