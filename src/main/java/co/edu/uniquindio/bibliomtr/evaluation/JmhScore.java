package co.edu.uniquindio.bibliomtr.evaluation;

import java.util.Objects;

import co.edu.uniquindio.bibliomtr.algorithms.ranking.SortAlgorithm;

/**
 * Resultado JMH de un algoritmo: tiempo promedio por operación y su error, en milisegundos (REQ-22).
 *
 * <p><b>Qué es.</b> {@code scoreMs} es {@code primaryMetric.score} de JMH en modo {@code avgt}: la media
 * x̄ de los tiempos por operación de las 10 iteraciones de medición. {@code scoreErrorMs} es
 * {@code primaryMetric.scoreError}: la semiamplitud del intervalo de confianza al 99,9 %,
 * {@code t(0,9995; N − 1) · s / √N}, con s = desviación estándar muestral y N = número de iteraciones. La
 * interfaz lo muestra como {@code scoreMs ± scoreErrorMs ms}.
 *
 * <p>Complejidad: construcción O(1) en tiempo y espacio.
 *
 * @param algorithm    algoritmo medido
 * @param scoreMs      tiempo promedio por operación en ms
 * @param scoreErrorMs semiamplitud del intervalo de confianza al 99,9 % en ms ({@code NaN} si JMH no pudo
 *                     calcularla, p. ej. con una sola iteración)
 */
public record JmhScore(SortAlgorithm algorithm, double scoreMs, double scoreErrorMs) {

	/**
	 * Valida que el algoritmo no sea nulo.
	 *
	 * <p>Complejidad: O(1) en tiempo y espacio.
	 */
	public JmhScore {
		Objects.requireNonNull(algorithm, "El algoritmo no puede ser nulo");
	}

}
