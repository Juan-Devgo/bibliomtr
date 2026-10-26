package co.edu.uniquindio.bibliomtr.algorithms.ranking;

import java.util.List;

/**
 * Contrato común (patrón Strategy) de los tres algoritmos de ordenamiento del Requerimiento 2 (REQ-10).
 *
 * <p><b>Qué hace.</b> Ordena pares (término, frecuencia) de forma ascendente y estable usando la frecuencia
 * como clave: si dos pares tienen la misma frecuencia, conservan su orden relativo de la entrada.
 *
 * <p><b>Por qué.</b> El servicio de ranking, el servicio de mediciones y el benchmark JMH recorren los tres
 * algoritmos con la misma firma; así los tres reciben la misma lista y producen el mismo ranking.
 *
 * <p>La complejidad depende de cada implementación: Counting O(n + k), Radix O(d · (n + b)) y Bucket
 * O(n + m) promedio y O(n²) en el peor caso.
 *
 * @param <T> tipo de la traza del algoritmo
 */
public interface TermSorter<T> {

	/**
	 * Identifica el algoritmo (slug, complejidad teórica y parámetros).
	 *
	 * <p>Complejidad: O(1) en tiempo y espacio.
	 *
	 * @return el algoritmo que implementa esta estrategia
	 */
	SortAlgorithm algorithm();

	/**
	 * Ordena ascendente y de forma estable por {@code frequency}; no modifica {@code input}.
	 *
	 * @param input   pares a ordenar (puede ser vacía)
	 * @param options ancho de cubeta y si se registra la traza
	 * @return lista ordenada y traza ({@code null} si no se pidió)
	 */
	SortResult<T> sort(List<TermFrequency> input, SortOptions options);

}
