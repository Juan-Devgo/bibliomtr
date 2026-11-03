package co.edu.uniquindio.bibliomtr.api.stringmatching;

/**
 * Reconoce la excepción de un stub sin implementar: {@link UnsupportedOperationException} cuyo mensaje empieza por
 * {@value #PREFIX} (spec 004, REQ-28).
 *
 * <p><b>Por qué a mano.</b> Este paquete lo escanea {@code ForbiddenStringMatchingUsageTest} (REQ-14), que prohíbe
 * {@code .startsWith(}; el prefijo se compara carácter a carácter. No compara texto ni patrón de una búsqueda: solo
 * el mensaje de la excepción. Las specs 001 y 003 conservan su propia comprobación, sin cambios.
 *
 * <p>Complejidad: O(p) en tiempo, con p = longitud del prefijo, y O(1) en espacio.
 */
final class PendingStubs {

	/** Prefijo del mensaje de cada stub sin implementar. */
	static final String PREFIX = "TODO estudiante:";

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
	static boolean isPending(UnsupportedOperationException e) {
		if (e == null) {
			return false;
		}
		String message = e.getMessage();
		if (message == null || message.length() < PREFIX.length()) {
			return false;
		}
		for (int i = 0; i < PREFIX.length(); i++) {
			if (message.charAt(i) != PREFIX.charAt(i)) {
				return false;
			}
		}
		return true;
	}

}
