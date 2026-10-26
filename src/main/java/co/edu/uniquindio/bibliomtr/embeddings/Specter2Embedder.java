package co.edu.uniquindio.bibliomtr.embeddings;

/**
 * Genera el embedding de SPECTER2 de un artículo (spec 001, REQ-9 y REQ-9a).
 *
 * <p><b>Por qué es una interfaz.</b> La implementación real ({@link Specter2OnnxEmbedder}) carga un modelo de
 * unos 440 MB; los tests de ingesta la reemplazan por un fake sin modelo.
 */
public interface Specter2Embedder {

	/**
	 * Calcula el vector de {@code título [SEP] abstract} ({@link Specter2Input#of}): tokeniza, trunca a 512
	 * tokens si hace falta, toma el vector del token [CLS] y lo normaliza con L2.
	 *
	 * <p>Complejidad: la determina el modelo (O(L²·h) por capa de atención, con L ≤ 512 tokens y h = 768).
	 *
	 * @param title        título del artículo; no nulo
	 * @param abstractText abstract del artículo; no nulo
	 * @return el vector, si se truncó y el número de tokens antes de truncar
	 */
	Specter2Embedding embed(String title, String abstractText);
}
