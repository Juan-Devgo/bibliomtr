package co.edu.uniquindio.bibliomtr.persistence.benchmark;

/**
 * Estado de una medición JMH (REQ-20, REQ-21).
 *
 * <p>Los valores están en español porque la spec los fija como contrato de la API y de la interfaz; se guardan
 * con este mismo nombre en {@code benchmark_run.status}.
 * <ul>
 *   <li>{@link #EN_CURSO}: JMH está corriendo; solo puede haber una (REQ-19).</li>
 *   <li>{@link #COMPLETADA}: terminó y tiene los resultados de los tres algoritmos.</li>
 *   <li>{@link #FALLIDA}: falló, superó el límite de 10 minutos o se interrumpió por un reinicio; tiene un
 *       mensaje en español.</li>
 * </ul>
 */
public enum BenchmarkStatus {

	/** Medición en ejecución. */
	EN_CURSO,

	/** Medición terminada con resultados. */
	COMPLETADA,

	/** Medición terminada sin resultados, con mensaje de error. */
	FALLIDA

}
