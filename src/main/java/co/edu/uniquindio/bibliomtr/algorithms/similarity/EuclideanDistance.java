package co.edu.uniquindio.bibliomtr.algorithms.similarity;

import co.edu.uniquindio.bibliomtr.trace.similarity.EuclideanTrace;

/**
 * Distancia euclidiana entre dos vectores de embeddings (Requerimiento 1, métrica sobre IA). STUB: el cuerpo
 * de {@link #compare} lo escribe el estudiante (constitución P1 y P3; ver {@code docs/guia-implementacion.md}).
 *
 * <p><b>Qué calcula.</b> La longitud del segmento entre los vectores {@code a} y {@code b} de dimensión d,
 * producidos por el mismo modelo (SPECTER2 o nomic-embed-text).
 *
 * <p><b>Fórmula.</b>
 * <pre>
 *   d(a, b) = √( Σᵢ (aᵢ − bᵢ)² )
 * </pre>
 * El valor crudo es {@code d}.
 *
 * <p><b>Normalización sugerida.</b> {@code similarity = 1 / (1 + d)}.
 *
 * <p><b>Traza.</b> {@link EuclideanTrace}: d (dimensión), las diferencias al cuadrado (aᵢ − bᵢ)² y su suma.
 *
 * <p><b>Rango.</b> {@code value} ∈ [0, ∞); {@code similarity} ∈ (0, 1].
 *
 * <p><b>Casos de borde.</b> Idénticos: distancia 0 y similitud 1. (0, 0) y (3, 4): distancia 5 y similitud
 * 1/6. Dimensiones distintas o 0:
 * {@code IllegalArgumentException("Los vectores deben tener la misma dimensión mayor que 0")}.
 *
 * <p><b>Complejidad: TODO estudiante</b> (tiempo y espacio en función de d).
 */
public final class EuclideanDistance implements VectorSimilarity<EuclideanTrace> {

	/**
	 * Calcula la distancia euclidiana entre {@code a} y {@code b}, su similitud normalizada y la traza.
	 *
	 * <p>Complejidad: TODO estudiante.
	 *
	 * @param a primer vector (no nulo)
	 * @param b segundo vector (no nulo)
	 * @return {@code value} = d(a, b), {@code similarity} en (0,1] y la traza {@link EuclideanTrace}
	 * @throws IllegalArgumentException si las dimensiones difieren o son 0
	 */
	@Override
	public SimilarityResult<EuclideanTrace> compare(float[] a, float[] b) {
  	if (a.length != b.length || a.length == 0) {
  		throw new IllegalArgumentException("Los vectores deben tener la misma dimensión mayor que 0");
  	}
  	int dimension = a.length;

  	// (aᵢ − bᵢ)² por coordenada y su suma; la diferencia se hace en double para no perder precisión.
  	double[] squaredDifferences = new double[dimension];
  	double sumOfSquares = 0.0;
  	for (int i = 0; i < dimension; i++) {
  		double difference = (double) a[i] - (double) b[i];
  		squaredDifferences[i] = difference * difference;
  		sumOfSquares += squaredDifferences[i];
  	}

  	double distance = Math.sqrt(sumOfSquares); // d(a, b) = √Σ(aᵢ − bᵢ)²
  	double similarity = 1.0 / (1.0 + distance); // [0, ∞) → (0, 1]; d = 0 da 1
  	return new SimilarityResult<>(distance, similarity, new EuclideanTrace(dimension, squaredDifferences, sumOfSquares));
	}

}
