package co.edu.uniquindio.bibliomtr.evaluation;

import java.time.Duration;
import java.util.List;

import co.edu.uniquindio.bibliomtr.algorithms.ranking.TermFrequency;

/**
 * Ejecuta una medición JMH de los tres algoritmos sobre una lista de frecuencias (REQ-17, REQ-18, REQ-21).
 *
 * <p><b>Por qué una interfaz.</b> Una corrida real con los parámetros fijos de REQ-18 dura cerca de un minuto.
 * La implementación de producción es {@link JmhProcessLauncher}; los tests usan un launcher falso para probar
 * el 409, la medición FALLIDA y el límite de tiempo sin correr JMH.
 */
public interface BenchmarkLauncher {

	/**
	 * Mide los tres algoritmos sobre {@code input} con {@link JmhParameters#FIXED} y espera a que termine,
	 * como máximo {@code timeout}.
	 *
	 * <p>Complejidad: la de JMH; con los parámetros fijos, unos 3 · (5 + 10) s de pared más el arranque de la
	 * JVM, independiente de n mientras cada operación dure menos que una iteración.
	 *
	 * @param input   lista de frecuencias en el orden de entrada de los algoritmos
	 * @param timeout límite de la medición (REQ-21: 10 minutos)
	 * @return un resultado por algoritmo, en el orden Counting, Radix, Bucket
	 * @throws BenchmarkTimeoutException si la medición supera {@code timeout}
	 * @throws IllegalStateException     si JMH falla o su resultado no se puede leer
	 */
	List<JmhScore> run(List<TermFrequency> input, Duration timeout) throws BenchmarkTimeoutException;

}
