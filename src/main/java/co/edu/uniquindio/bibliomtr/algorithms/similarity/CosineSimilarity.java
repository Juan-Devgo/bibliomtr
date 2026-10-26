package co.edu.uniquindio.bibliomtr.algorithms.similarity;

import co.edu.uniquindio.bibliomtr.trace.similarity.CosineTrace;

/**
 * Similitud coseno entre dos vectores de embeddings (Requerimiento 1, métrica sobre IA). STUB: el cuerpo de
 * {@link #compare} lo escribe el estudiante (constitución P1 y P3; ver {@code docs/guia-implementacion.md}).
 *
 * <p><b>Qué calcula.</b> El coseno del ángulo entre los vectores {@code a} y {@code b} de dimensión d,
 * producidos por el mismo modelo (SPECTER2 o nomic-embed-text).
 *
 * <p><b>Fórmula.</b>
 * <pre>
 *   cos(a, b) = Σᵢ aᵢ bᵢ / (‖a‖₂ · ‖b‖₂),   con ‖v‖₂ = √(Σᵢ vᵢ²)
 * </pre>
 * El valor crudo es {@code cos}.
 *
 * <p><b>Normalización sugerida.</b> {@code similarity = (1 + cos) / 2}.
 *
 * <p><b>Traza.</b> {@link CosineTrace}: d, los productos aᵢ bᵢ, el producto punto y las dos normas.
 *
 * <p><b>Rango.</b> {@code value} ∈ [−1, 1]; {@code similarity} ∈ [0, 1].
 *
 * <p><b>Casos de borde.</b> Idénticos: cos 1 y similitud 1. Opuestos ((1, 0) y (−1, 0)): cos −1 y similitud 0.
 * Ortogonales ((1, 0) y (0, 1)): cos 0 y similitud 0.5. Dimensiones distintas o 0:
 * {@code IllegalArgumentException("Los vectores deben tener la misma dimensión mayor que 0")}.
 *
 * <p><b>Complejidad: TODO estudiante</b> (tiempo y espacio en función de d).
 */
public final class CosineSimilarity implements VectorSimilarity<CosineTrace> {

	/**
	 * Calcula el coseno entre {@code a} y {@code b}, su similitud normalizada y la traza.
	 *
	 * <p>Complejidad: TODO estudiante.
	 *
	 * @param a primer vector (no nulo)
	 * @param b segundo vector (no nulo)
	 * @return {@code value} = cos, {@code similarity} en [0,1] y la traza {@link CosineTrace}
	 * @throws IllegalArgumentException si las dimensiones difieren o son 0
	 */
	@Override
	public SimilarityResult<CosineTrace> compare(float[] a, float[] b) {
		throw new UnsupportedOperationException(
				"TODO estudiante: implementar CosineSimilarity.compare (ver docs/guia-implementacion.md)");
	}

}
