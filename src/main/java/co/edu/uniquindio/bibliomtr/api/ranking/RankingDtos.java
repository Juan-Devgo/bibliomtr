package co.edu.uniquindio.bibliomtr.api.ranking;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import co.edu.uniquindio.bibliomtr.algorithms.ranking.TermFrequency;

/**
 * Respuestas JSON de {@code GET /api/ranking} y {@code GET /api/ranking/trace/{algorithm}} (REQ-3, REQ-11,
 * REQ-16).
 *
 * <p><b>Por qué separadas.</b> El ranking devuelve solo el top N de cada algoritmo y sus tiempos, medidos sin
 * traza; la traza completa de un algoritmo se pide aparte, porque sobre el corpus completo es grande.
 */
public final class RankingDtos {

	private RankingDtos() {
	}

	/**
	 * Ranking de términos calculado por los tres algoritmos sobre la misma lista (REQ-3, REQ-11).
	 *
	 * <p>Complejidad: construcción O(1) en tiempo y espacio.
	 *
	 * @param termCount       número de términos distintos n de la lista completa que ordenaron los algoritmos
	 * @param n               número de términos pedidos (el top devuelve {@code min(n, termCount)})
	 * @param bucketSize      ancho de cubeta usado por Bucket Sort
	 * @param preprocessingMs tiempo en ms de obtener los tokens y contar las frecuencias
	 * @param allMatch        {@code true} si los rankings completos de los tres algoritmos son idénticos
	 * @param algorithms      top N y tiempo de cada algoritmo, en el orden Counting, Radix, Bucket
	 */
	public record RankingResponse(int termCount, int n, int bucketSize, double preprocessingMs, boolean allMatch,
			List<AlgorithmRanking> algorithms) {

		/**
		 * Copia defensiva de la lista de algoritmos.
		 *
		 * <p>Complejidad: O(a) en tiempo y espacio, con a = número de algoritmos.
		 */
		public RankingResponse {
			algorithms = List.copyOf(algorithms);
		}
	}

	/**
	 * Resultado de un algoritmo dentro del ranking, sin traza (AC-7).
	 *
	 * <p>Complejidad: construcción O(N) en tiempo y espacio por la copia del top.
	 *
	 * @param algorithm slug del algoritmo ({@code counting}, {@code radix} o {@code bucket})
	 * @param top       los N primeros términos de su ranking
	 * @param elapsedMs tiempo en ms del ordenamiento con la grabación de traza desactivada
	 */
	public record AlgorithmRanking(String algorithm, List<TermFrequency> top, double elapsedMs) {

		/**
		 * Copia defensiva del top.
		 *
		 * <p>Complejidad: O(N) en tiempo y espacio.
		 */
		public AlgorithmRanking {
			top = List.copyOf(top);
		}
	}

	/**
	 * Traza completa de un algoritmo sobre la lista de frecuencias del corpus completo (REQ-16, AC-23).
	 *
	 * <p>Complejidad: construcción O(n) en tiempo y espacio por las copias de las listas.
	 *
	 * @param algorithm  slug del algoritmo
	 * @param bucketSize ancho de cubeta recibido; solo afecta la traza de Bucket Sort
	 * @param input      entrada en orden alfabético descendente
	 * @param output     salida ascendente y estable por frecuencia; en el JSON conserva el nombre
	 *                   {@code sorted}
	 * @param ranking    salida leída de derecha a izquierda
	 * @param trace      {@code CountingSortTrace}, {@code RadixSortTrace} o {@code BucketSortTrace}
	 */
	public record SortTraceResponse(String algorithm, int bucketSize, List<TermFrequency> input,
			@JsonProperty("sorted") List<TermFrequency> output, List<TermFrequency> ranking, Object trace) {

		/**
		 * Copia defensiva de las listas.
		 *
		 * <p>Complejidad: O(n) en tiempo y espacio.
		 */
		public SortTraceResponse {
			input = List.copyOf(input);
			output = List.copyOf(output);
			ranking = List.copyOf(ranking);
		}
	}

}
