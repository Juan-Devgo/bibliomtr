package co.edu.uniquindio.bibliomtr.algorithms.similarity;

import java.util.List;

/**
 * Estrategia de similitud entre dos secuencias de símbolos (Levenshtein, Needleman-Wunsch y Jaccard).
 *
 * <p><b>Qué recibe.</b> Dos secuencias genéricas de símbolos. Según la granularidad configurada en
 * {@code bibliomtr.similarity.edit-granularity} (REQ-3), cada símbolo es una palabra
 * ({@code TextPreprocessor.tokenize}) o un carácter ({@code TextPreprocessor.characters}); el algoritmo no
 * necesita saber cuál.
 *
 * <p><b>Por qué.</b> Una sola firma por tipo de entrada (patrón Strategy) permite que el servicio de
 * comparación, el benchmark y los tests llamen a cualquier implementación, y que los tests de la API la
 * reemplacen por un algoritmo falso.
 *
 * <p>Complejidad: la declara cada implementación.
 *
 * @param <T> tipo de la traza que devuelve la implementación
 */
public interface SequenceSimilarity<T> {

	/**
	 * Compara dos secuencias de símbolos.
	 *
	 * <p>Complejidad: la declara cada implementación.
	 *
	 * @param a primera secuencia (no nula; puede ser vacía)
	 * @param b segunda secuencia (no nula; puede ser vacía)
	 * @return valor crudo, similitud en [0,1] y traza
	 */
	SimilarityResult<T> compare(List<String> a, List<String> b);

}
