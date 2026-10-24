package co.edu.uniquindio.bibliomtr.embeddings;

/**
 * Genera el embedding de nomic-embed-text de un abstract (spec 001, REQ-9).
 *
 * <p><b>Por qué es una interfaz.</b> La implementación real ({@link NomicOllamaEmbedder}) necesita un servidor
 * de Ollama con el modelo descargado; los tests de ingesta la reemplazan por un fake sin red.
 */
public interface NomicEmbedder {

	/**
	 * Garantiza que el modelo esté disponible en el servidor, descargándolo si hace falta. La ingesta lo llama
	 * una vez por corrida, antes del primer embedding.
	 *
	 * <p>Complejidad: O(1) peticiones; la descarga inicial (unos 274 MB) la hace el servidor.
	 */
	void ensureModel();

	/**
	 * Calcula el vector de {@code clustering: <abstract>}. Solo usa el abstract (constitución P5).
	 *
	 * <p>Complejidad: una petición HTTP; O(d) en espacio para el vector, con d = 768.
	 *
	 * @param abstractText abstract del artículo; no nulo
	 * @return vector de {@link EmbeddingModel#NOMIC} dimensiones
	 */
	float[] embed(String abstractText);
}
