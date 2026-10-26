package co.edu.uniquindio.bibliomtr.algorithms.ranking;

import java.util.List;

/**
 * Resultado de un {@link TermSorter}: la lista ordenada ascendente y estable por frecuencia y, si se pidió,
 * la traza de pasos intermedios (constitución P7).
 *
 * <p><b>Por qué.</b> Cada algoritmo devuelve resultado y traza juntos para que la API, la interfaz y el
 * documento técnico muestren exactamente los mismos pasos.
 *
 * <p>Complejidad: construcción O(1) en tiempo y espacio.
 *
 * @param output lista ordenada ascendente por frecuencia (inmutable)
 * @param trace  traza del algoritmo, o {@code null} si {@link SortOptions#recordTrace()} es {@code false}
 * @param <T>    tipo de la traza del algoritmo
 */
public record SortResult<T>(List<TermFrequency> output, T trace) {
}
