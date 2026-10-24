package co.edu.uniquindio.bibliomtr.api.similarity;

import java.util.List;

import co.edu.uniquindio.bibliomtr.api.corpus.ArticleSummary;

/**
 * Cuerpos JSON de {@code POST /api/similarity/comparisons} y {@code GET /api/similarity/trace} (spec 001,
 * REQ-14, REQ-16, REQ-20, REQ-22).
 *
 * <p><b>Por qué separadas.</b> La comparación devuelve solo las matrices N×N y los tiempos; la traza, que puede
 * ser grande (p. ej. la matriz de Levenshtein), se pide aparte para un par y un algoritmo.
 */
public final class ComparisonDtos {

	private ComparisonDtos() {
	}

	/**
	 * Solicitud de comparación (REQ-13).
	 *
	 * <p>Complejidad: construcción O(1) en tiempo y espacio.
	 *
	 * @param articleIds de 2 a 20 ids distintos de artículos seleccionables; las matrices siguen este orden
	 */
	public record ComparisonRequest(List<Long> articleIds) {
	}

	/**
	 * Resultado de una comparación (REQ-14, REQ-22).
	 *
	 * <p>Complejidad: construcción O(N + R) en tiempo y espacio por las copias de las listas, con N = artículos
	 * y R = 8 resultados.
	 *
	 * @param articles        artículos comparados, en el orden de la solicitud (filas y columnas de las matrices)
	 * @param editGranularity granularidad de Levenshtein y Needleman-Wunsch ({@code WORDS} o {@code CHARACTERS})
	 * @param preprocessingMs tiempo en ms de cargar los abstracts seleccionables, tokenizarlos y armar las
	 *                        secuencias de edición
	 * @param results         los 8 resultados en el orden de {@link ComparisonAlgorithm}
	 */
	public record ComparisonResponse(List<ArticleSummary> articles, String editGranularity, double preprocessingMs,
			List<AlgorithmMatrix> results) {

		/**
		 * Copia defensiva de las listas.
		 *
		 * <p>Complejidad: O(N + R) en tiempo y espacio.
		 */
		public ComparisonResponse {
			articles = List.copyOf(articles);
			results = List.copyOf(results);
		}
	}

	/**
	 * Matriz de similitud de un resultado (REQ-14, REQ-20, REQ-22).
	 *
	 * <p>Complejidad: construcción O(1) en tiempo y espacio (la matriz no se copia).
	 *
	 * @param algorithm       slug del resultado, p. ej. {@code specter2-cosine}
	 * @param label           nombre legible
	 * @param status          {@code CALCULADO} o {@code PENDIENTE}
	 * @param matrix          similitudes N×N en [0,1], simétrica; {@code null} si está PENDIENTE
	 * @param elapsedMs       tiempo en ms de recorrer todos los pares; {@code null} si está PENDIENTE
	 * @param pendingLocation {@code <archivo>#<método>} del stub si está PENDIENTE; {@code null} si no
	 */
	public record AlgorithmMatrix(String algorithm, String label, String status, double[][] matrix,
			Double elapsedMs, String pendingLocation) {
	}

	/**
	 * Resultado y traza de un algoritmo para un par de artículos (REQ-16, REQ-20).
	 *
	 * <p>Complejidad: construcción O(1) en tiempo y espacio.
	 *
	 * @param algorithm       slug del resultado
	 * @param label           nombre legible
	 * @param articleA        primer artículo
	 * @param articleB        segundo artículo (puede ser el mismo que {@code articleA})
	 * @param editGranularity granularidad de Levenshtein y Needleman-Wunsch
	 * @param status          {@code CALCULADO} o {@code PENDIENTE}
	 * @param value           valor crudo del algoritmo (distancia, puntaje, coseno…); {@code null} si PENDIENTE
	 * @param similarity      similitud en [0,1]; {@code null} si PENDIENTE
	 * @param trace           record de traza de {@code trace/similarity}; {@code null} si PENDIENTE
	 * @param elapsedMs       tiempo en ms del cálculo del par; {@code null} si PENDIENTE
	 * @param pendingLocation {@code <archivo>#<método>} del stub si está PENDIENTE; {@code null} si no
	 */
	public record PairTraceResponse(String algorithm, String label, ArticleSummary articleA, ArticleSummary articleB,
			String editGranularity, String status, Double value, Double similarity, Object trace, Double elapsedMs,
			String pendingLocation) {
	}

}
