package co.edu.uniquindio.bibliomtr.api.similarity;

import java.util.Objects;

import co.edu.uniquindio.bibliomtr.algorithms.similarity.CorpusSimilarity;
import co.edu.uniquindio.bibliomtr.algorithms.similarity.SequenceSimilarity;
import co.edu.uniquindio.bibliomtr.algorithms.similarity.VectorSimilarity;

/**
 * Registro de las 6 estrategias de similitud que usa la comparación (spec 001, REQ-14; patrón Strategy del plan).
 *
 * <p><b>Por qué un registro.</b> {@code ComparisonService} recibe un solo bean con los 6 algoritmos; en
 * producción son los stubs de {@code algorithms/similarity} ({@code SimilarityConfig}), y los tests de API lo
 * reemplazan por un catálogo falso {@code @Primary} para no depender de que los estudiantes hayan implementado
 * algo. Las trazas son de tipo comodín porque la API las expone como JSON sin interpretarlas.
 *
 * <p>Complejidad: construcción O(1) en tiempo y espacio.
 *
 * @param levenshtein     distancia de Levenshtein sobre secuencias de símbolos
 * @param needlemanWunsch alineamiento global de Needleman-Wunsch sobre secuencias de símbolos
 * @param tfIdfCosine     coseno entre vectores TF-IDF, con el corpus para el IDF
 * @param jaccard         índice de Jaccard sobre los conjuntos de tokens
 * @param cosine          similitud coseno entre embeddings (ambos modelos)
 * @param euclidean       distancia euclidiana entre embeddings (ambos modelos)
 */
public record SimilarityCatalog(SequenceSimilarity<?> levenshtein, SequenceSimilarity<?> needlemanWunsch,
		CorpusSimilarity<?> tfIdfCosine, SequenceSimilarity<?> jaccard, VectorSimilarity<?> cosine,
		VectorSimilarity<?> euclidean) {

	/**
	 * Exige las 6 estrategias.
	 *
	 * <p>Complejidad: O(1) en tiempo y espacio.
	 */
	public SimilarityCatalog {
		Objects.requireNonNull(levenshtein, "levenshtein");
		Objects.requireNonNull(needlemanWunsch, "needlemanWunsch");
		Objects.requireNonNull(tfIdfCosine, "tfIdfCosine");
		Objects.requireNonNull(jaccard, "jaccard");
		Objects.requireNonNull(cosine, "cosine");
		Objects.requireNonNull(euclidean, "euclidean");
	}

}
