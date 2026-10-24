package co.edu.uniquindio.bibliomtr.embeddings;

/**
 * Embedding de SPECTER2 de un artículo y datos de su truncado (spec 001, REQ-9 y REQ-9a).
 *
 * <p><b>Por qué incluye el truncado.</b> SPECTER2 acepta como máximo 512 tokens. Si
 * {@code título [SEP] abstract} los supera, el vector solo representa los primeros 512, y la ingesta debe
 * marcar el artículo como truncado en la base y en el log (REQ-9a).
 *
 * <p>Complejidad: O(1) en tiempo y espacio para los accesores (el arreglo no se copia).
 *
 * @param vector     vector del token [CLS] con norma L2 igual a 1, de {@link EmbeddingModel#SPECTER2}
 *                   dimensiones
 * @param truncated  {@code true} si el texto tenía más de 512 tokens y se truncó
 * @param tokenCount número de tokens del texto antes de truncar, contando [CLS] y [SEP]
 */
public record Specter2Embedding(float[] vector, boolean truncated, int tokenCount) {
}
