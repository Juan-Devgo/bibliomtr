package co.edu.uniquindio.bibliomtr.evaluation;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Cronómetro de una sola ejecución con {@link System#nanoTime()}, con el resultado en milisegundos (REQ-11;
 * constitución P11).
 *
 * <p><b>Qué hace.</b> Ejecuta un trabajo y devuelve su valor junto con el tiempo transcurrido
 * {@code Δt = (t_fin − t_inicio) / 10⁶} ms, con {@code t} en nanosegundos.
 *
 * <p><b>Por qué.</b> La petición de ranking reporta por separado el tiempo de preprocesamiento y el de cada
 * algoritmo. {@code nanoTime} es monótono (no le afectan los cambios del reloj del sistema), a diferencia de
 * {@code currentTimeMillis}. Es una medición de una sola corrida, sin calentamiento; la medición con
 * calentamiento la hace JMH (REQ-18).
 */
public final class Stopwatch {

	private static final double NANOS_PER_MILLI = 1_000_000.0;

	private Stopwatch() {
	}

	/**
	 * Ejecuta {@code work} y mide su duración.
	 *
	 * <p>Complejidad: O(1) en tiempo y espacio, más el costo de {@code work}.
	 *
	 * @param work trabajo a cronometrar
	 * @param <T>  tipo del resultado
	 * @return el resultado y el tiempo transcurrido en milisegundos
	 */
	public static <T> Timed<T> time(Supplier<T> work) {
		Objects.requireNonNull(work, "El trabajo a cronometrar no puede ser nulo");
		long start = System.nanoTime();
		T value = work.get();
		long elapsedNanos = System.nanoTime() - start;
		return new Timed<>(value, elapsedNanos / NANOS_PER_MILLI);
	}

	/**
	 * Resultado de un trabajo cronometrado.
	 *
	 * <p>Complejidad: construcción O(1) en tiempo y espacio.
	 *
	 * @param value  valor que devolvió el trabajo
	 * @param millis tiempo transcurrido en milisegundos, mayor o igual a 0
	 * @param <T>    tipo del resultado
	 */
	public record Timed<T>(T value, double millis) {
	}

}
