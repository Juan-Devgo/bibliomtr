package co.edu.uniquindio.bibliomtr.api.similarity;

/**
 * Estado de un resultado de comparación o de traza (spec 001, REQ-20).
 *
 * <p><b>Por qué.</b> Mientras los estudiantes no implementen un stub, la comparación no falla: ese resultado queda
 * PENDIENTE, sin valores, y los demás se devuelven con normalidad (US-7).
 */
public enum AlgorithmStatus {

	/** El algoritmo se ejecutó y devolvió similitudes en [0,1]. */
	CALCULADO,

	/** El stub lanzó {@code UnsupportedOperationException} con prefijo "TODO estudiante:". */
	PENDIENTE

}
