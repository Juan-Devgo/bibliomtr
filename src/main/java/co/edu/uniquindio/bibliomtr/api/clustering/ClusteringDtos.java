package co.edu.uniquindio.bibliomtr.api.clustering;

import java.util.List;

import co.edu.uniquindio.bibliomtr.algorithms.clustering.Merge;
import co.edu.uniquindio.bibliomtr.api.corpus.ArticleSummary;

/**
 * Cuerpos JSON de {@code POST /api/clustering} y {@code GET /api/clustering/trace} (spec 003, REQ-11, REQ-16,
 * REQ-18, REQ-19, REQ-22).
 *
 * <p><b>Por qué separadas.</b> El clustering devuelve solo los dendrogramas y los tiempos; la traza, que guarda
 * una matriz por paso, se pide aparte para un algoritmo.
 */
public final class ClusteringDtos {

	private ClusteringDtos() {
	}

	/**
	 * Solicitud de clustering (REQ-11).
	 *
	 * <p>Complejidad: construcción O(1) en tiempo y espacio.
	 *
	 * @param articleIds de 2 a 20 ids distintos de artículos seleccionables; las hojas siguen este orden
	 * @param source     slug de la fuente de similitud del Requerimiento 1, p. ej. {@code specter2-cosine}
	 */
	public record ClusteringRequest(List<Long> articleIds, String source) {
	}

	/**
	 * Fuente de similitud usada y su estado (REQ-19).
	 *
	 * <p>Complejidad: construcción O(1) en tiempo y espacio.
	 *
	 * @param source          slug de la fuente
	 * @param label           nombre legible
	 * @param status          {@code CALCULADO} o {@code PENDIENTE}
	 * @param pendingLocation {@code <archivo>#<método>} del stub del Requerimiento 1 si está PENDIENTE; {@code null}
	 *                        si no
	 */
	public record SourceSummary(String source, String label, String status, String pendingLocation) {
	}

	/**
	 * Resultado de un clustering (REQ-11, REQ-19, REQ-22).
	 *
	 * <p>Complejidad: construcción O(N + R) en tiempo y espacio por las copias de las listas, con N = artículos y
	 * R = 4 algoritmos (la matriz no se copia).
	 *
	 * @param articles      artículos agrupados, en el orden de la solicitud (hoja i = artículo i)
	 * @param source        fuente de similitud y su estado
	 * @param preparationMs tiempo en ms del preprocesamiento y de la matriz de similitud; {@code null} si la fuente
	 *                      está PENDIENTE
	 * @param distances     distancias N×N d = 1 − s con diagonal 0; {@code null} si la fuente está PENDIENTE
	 * @param results       los 4 resultados en el orden de {@link ClusteringAlgorithm}; vacía si la fuente está
	 *                      PENDIENTE
	 */
	public record ClusteringResponse(List<ArticleSummary> articles, SourceSummary source, Double preparationMs,
			double[][] distances, List<DendrogramResult> results) {

		/**
		 * Copia defensiva de las listas.
		 *
		 * <p>Complejidad: O(N + R) en tiempo y espacio.
		 */
		public ClusteringResponse {
			articles = List.copyOf(articles);
			results = List.copyOf(results);
		}
	}

	/**
	 * Dendrograma de un algoritmo (REQ-11, REQ-18, REQ-22).
	 *
	 * <p>Complejidad: construcción O(N) en tiempo y espacio por la copia de las fusiones.
	 *
	 * @param algorithm       slug del algoritmo, p. ej. {@code single-linkage}
	 * @param label           nombre legible
	 * @param status          {@code CALCULADO} o {@code PENDIENTE}
	 * @param merges          N − 1 fusiones validadas; {@code null} si está PENDIENTE
	 * @param elapsedMs       tiempo en ms del algoritmo; {@code null} si está PENDIENTE
	 * @param pendingLocation {@code <archivo>#<método>} del stub si está PENDIENTE; {@code null} si no
	 */
	public record DendrogramResult(String algorithm, String label, String status, List<Merge> merges,
			Double elapsedMs, String pendingLocation) {

		/**
		 * Copia defensiva de las fusiones, si las hay.
		 *
		 * <p>Complejidad: O(N) en tiempo y espacio.
		 */
		public DendrogramResult {
			merges = merges == null ? null : List.copyOf(merges);
		}
	}

	/**
	 * Dendrograma y traza de un algoritmo (REQ-16, REQ-18, REQ-19, REQ-22).
	 *
	 * <p>Complejidad: construcción O(N + M) en tiempo y espacio por las copias de las listas, con M = fusiones.
	 *
	 * @param algorithm       slug del algoritmo
	 * @param label           nombre legible
	 * @param articles        artículos agrupados, en el orden de la solicitud
	 * @param source          fuente de similitud y su estado
	 * @param preparationMs   tiempo en ms del preprocesamiento y la matriz; {@code null} si la fuente está PENDIENTE
	 * @param distances       distancias N×N; {@code null} si la fuente está PENDIENTE
	 * @param status          {@code CALCULADO} o {@code PENDIENTE}; {@code null} si la fuente está PENDIENTE
	 * @param merges          fusiones validadas; {@code null} si el algoritmo o la fuente están PENDIENTE
	 * @param trace           {@code LinkageTrace} o {@code DianaTrace}; {@code null} si el algoritmo o la fuente
	 *                        están PENDIENTE
	 * @param elapsedMs       tiempo en ms del algoritmo; {@code null} si el algoritmo o la fuente están PENDIENTE
	 * @param pendingLocation {@code <archivo>#<método>} del stub de clustering si está PENDIENTE; {@code null} si no
	 */
	public record ClusteringTraceResponse(String algorithm, String label, List<ArticleSummary> articles,
			SourceSummary source, Double preparationMs, double[][] distances, String status, List<Merge> merges,
			Object trace, Double elapsedMs, String pendingLocation) {

		/**
		 * Copia defensiva de las listas.
		 *
		 * <p>Complejidad: O(N + M) en tiempo y espacio.
		 */
		public ClusteringTraceResponse {
			articles = List.copyOf(articles);
			merges = merges == null ? null : List.copyOf(merges);
		}
	}

}
