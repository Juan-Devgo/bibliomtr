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
  	if (a.length != b.length || a.length == 0) {
  		throw new IllegalArgumentException("Los vectores deben tener la misma dimensión mayor que 0");
  	}
  	int dimension = a.length;

  	// Un solo recorrido: productos aᵢ·bᵢ (su suma es el producto punto) y sumas de cuadrados para las normas.
  	double[] products = new double[dimension];
  	double dotProduct = 0.0;
  	double sumSquaresA = 0.0;
  	double sumSquaresB = 0.0;
  	for (int i = 0; i < dimension; i++) {
  		double x = a[i];
  		double y = b[i];
  		products[i] = x * y;
  		dotProduct += products[i];
  		sumSquaresA += x * x;
  		sumSquaresB += y * y;
  	}
  	double normA = Math.sqrt(sumSquaresA);
  	double normB = Math.sqrt(sumSquaresB);

  	// cos = a·b / (‖a‖ ‖b‖). Con un vector nulo el ángulo no está definido: se toma 0.
  	double cosine = 0.0;
  	if (normA != 0.0 && normB != 0.0) {
  		// El redondeo puede dar 1.0000000000000002: se lleva a [−1, 1].
  		cosine = Math.max(-1.0, Math.min(1.0, dotProduct / (normA * normB)));
  	}
  	double similarity = (1.0 + cosine) / 2.0; // [−1, 1] → [0, 1]
  	return new SimilarityResult<>(cosine, similarity, new CosineTrace(dimension, products, dotProduct, normA, normB));
	}

}
