package co.edu.uniquindio.bibliomtr.algorithms.similarity;

/**
 * Resultado de un algoritmo de similitud del Requerimiento 1: valor crudo, similitud normalizada y traza.
 *
 * <p><b>Qué guarda.</b>
 * <ul>
 *   <li>{@code value}: el valor propio del algoritmo, sin normalizar (p. ej. la distancia de Levenshtein, el
 *       puntaje de Needleman-Wunsch, el coseno en [−1,1] o la distancia euclidiana).</li>
 *   <li>{@code similarity}: el mismo resultado llevado a [0,1], donde 1 significa idénticos. La normalización
 *       la escribe el estudiante dentro de cada stub (REQ-4).</li>
 *   <li>{@code trace}: los pasos intermedios del cálculo (constitución P7), un record de
 *       {@code co.edu.uniquindio.bibliomtr.trace.similarity}.</li>
 * </ul>
 *
 * <p><b>Por qué.</b> La matriz de comparación usa {@code similarity} para que los 8 resultados sean
 * comparables, y el visor de traza muestra {@code value}, {@code similarity} y {@code trace} juntos, igual que
 * el documento técnico.
 *
 * <p>Complejidad: construcción O(1) en tiempo y espacio.
 *
 * @param value      valor crudo del algoritmo
 * @param similarity similitud normalizada en [0,1]
 * @param trace      traza de pasos intermedios
 * @param <T>        tipo de la traza del algoritmo
 */
public record SimilarityResult<T>(double value, double similarity, T trace) {
}
