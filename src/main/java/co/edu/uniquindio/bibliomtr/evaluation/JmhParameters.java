package co.edu.uniquindio.bibliomtr.evaluation;

import co.edu.uniquindio.bibliomtr.algorithms.ranking.SortOptions;

/**
 * Parámetros de una medición JMH del Requerimiento 2 (REQ-18; constitución P12).
 *
 * <p><b>Qué es.</b> {@link #FIXED} es la única configuración que se usa: 1 fork, 5 iteraciones de
 * calentamiento de 1 s, 10 iteraciones de medición de 1 s, modo tiempo promedio ({@code avgt}), unidad
 * milisegundos, {@code bucketSize} por defecto (5) y la grabación de traza desactivada. Cada medición guarda
 * estos valores junto a sus resultados (REQ-27).
 *
 * <p><b>Por qué fijos.</b> Los tiempos de distintas corridas solo son comparables si se miden igual. Los mismos
 * valores aparecen en las anotaciones de {@code SortingBenchmark}, en los argumentos de
 * {@link JmhProcessLauncher} y en el bloque {@code jmh} de {@code build.gradle.kts}.
 *
 * <p>Complejidad: construcción O(1) en tiempo y espacio.
 *
 * @param forks                 número de JVM hijas que lanza JMH
 * @param warmupIterations      iteraciones de calentamiento, descartadas
 * @param warmupSeconds         duración en segundos de cada iteración de calentamiento
 * @param measurementIterations iteraciones de medición
 * @param measurementSeconds    duración en segundos de cada iteración de medición
 * @param mode                  modo de JMH ({@code avgt}: tiempo promedio por operación)
 * @param timeUnit              unidad de los resultados ({@code ms})
 * @param bucketSize            ancho de cubeta de Bucket Sort
 * @param traceEnabled          si se graba la traza durante la medición (siempre {@code false})
 */
public record JmhParameters(int forks, int warmupIterations, int warmupSeconds, int measurementIterations,
		int measurementSeconds, String mode, String timeUnit, int bucketSize, boolean traceEnabled) {

	/** Parámetros fijos y documentados de REQ-18. */
	public static final JmhParameters FIXED =
			new JmhParameters(1, 5, 1, 10, 1, "avgt", "ms", SortOptions.DEFAULT_BUCKET_SIZE, false);

}
