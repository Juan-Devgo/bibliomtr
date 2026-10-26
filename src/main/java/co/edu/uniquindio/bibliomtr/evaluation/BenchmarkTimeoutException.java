package co.edu.uniquindio.bibliomtr.evaluation;

import java.time.Duration;
import java.util.Objects;

/**
 * La medición JMH superó su límite de tiempo y se detuvo (REQ-21, AC-15).
 *
 * <p><b>Qué hace.</b> Lleva el mensaje en español con el que la medición queda FALLIDA, p. ej. "La medición
 * superó el límite de 10 minutos".
 *
 * <p><b>Por qué verificada.</b> Superar el límite es un resultado esperado de {@link BenchmarkLauncher#run},
 * no un error de programación; el compilador obliga a quien lanza la medición a tratarlo.
 */
public class BenchmarkTimeoutException extends Exception {

	private static final long SECONDS_PER_MINUTE = 60;

	private final transient Duration timeout;

	/**
	 * Crea la excepción con el límite que se superó.
	 *
	 * <p>Complejidad: O(1) en tiempo y espacio.
	 *
	 * @param timeout límite de la medición
	 */
	public BenchmarkTimeoutException(Duration timeout) {
		super("La medición superó el límite de " + describe(Objects.requireNonNull(timeout)));
		this.timeout = timeout;
	}

	/**
	 * Límite de tiempo que se superó.
	 *
	 * <p>Complejidad: O(1) en tiempo y espacio.
	 *
	 * @return el límite
	 */
	public Duration timeout() {
		return timeout;
	}

	/**
	 * Límite en palabras: en minutos si es un número entero de minutos ("10 minutos"), si no en segundos.
	 *
	 * <p>Complejidad: O(1) en tiempo y espacio.
	 */
	private static String describe(Duration timeout) {
		long seconds = timeout.toSeconds();
		if (seconds >= SECONDS_PER_MINUTE && seconds % SECONDS_PER_MINUTE == 0) {
			long minutes = seconds / SECONDS_PER_MINUTE;
			return minutes + (minutes == 1 ? " minuto" : " minutos");
		}
		return seconds + (seconds == 1 ? " segundo" : " segundos");
	}

}
