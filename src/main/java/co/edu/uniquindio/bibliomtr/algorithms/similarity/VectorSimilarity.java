package co.edu.uniquindio.bibliomtr.algorithms.similarity;

/**
 * Estrategia de similitud entre dos vectores de embeddings (coseno y distancia euclidiana).
 *
 * <p><b>Qué recibe.</b> Dos vectores de la misma dimensión producidos por el mismo modelo preentrenado
 * (SPECTER2 o nomic-embed-text, 768 dimensiones). La métrica se calcula a mano (constitución P3).
 *
 * <p><b>Por qué.</b> Una sola firma (patrón Strategy) permite aplicar cada métrica a los dos modelos, lo que
 * da los 4 resultados de IA por par.
 *
 * <p>Complejidad: la declara cada implementación.
 *
 * @param <T> tipo de la traza que devuelve la implementación
 */
public interface VectorSimilarity<T> {

	/**
	 * Compara dos vectores.
	 *
	 * <p>Complejidad: la declara cada implementación.
	 *
	 * @param a primer vector (no nulo)
	 * @param b segundo vector (no nulo)
	 * @return valor crudo, similitud en [0,1] y traza
	 * @throws IllegalArgumentException si las dimensiones difieren o son 0, con el mensaje
	 *                                  {@code "Los vectores deben tener la misma dimensión mayor que 0"}
	 */
	SimilarityResult<T> compare(float[] a, float[] b);

}
