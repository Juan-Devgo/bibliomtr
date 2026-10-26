package co.edu.uniquindio.bibliomtr.algorithms.similarity;

import java.util.List;

/**
 * Estrategia de similitud entre dos documentos que depende de un corpus (coseno con TF-IDF).
 *
 * <p><b>Qué recibe.</b> Los tokens de los dos documentos y los tokens de cada documento del corpus, que en la
 * aplicación son todos los abstracts seleccionables, para que el IDF de un par no dependa de la selección.
 *
 * <p><b>Por qué.</b> Firma propia (patrón Strategy) porque, a diferencia de {@link SequenceSimilarity}, el
 * resultado depende de estadísticas de todo el corpus.
 *
 * <p>Complejidad: la declara cada implementación.
 *
 * @param <T> tipo de la traza que devuelve la implementación
 */
public interface CorpusSimilarity<T> {

	/**
	 * Compara dos documentos con estadísticas del corpus.
	 *
	 * <p>Complejidad: la declara cada implementación.
	 *
	 * @param a      tokens del primer documento (no nulo; puede ser vacío)
	 * @param b      tokens del segundo documento (no nulo; puede ser vacío)
	 * @param corpus tokens de cada documento del corpus (no nulo)
	 * @return valor crudo, similitud en [0,1] y traza
	 */
	SimilarityResult<T> compare(List<String> a, List<String> b, List<List<String>> corpus);

}
