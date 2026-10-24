package co.edu.uniquindio.bibliomtr.config;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import co.edu.uniquindio.bibliomtr.evaluation.BenchmarkLauncher;
import co.edu.uniquindio.bibliomtr.evaluation.JmhProcessLauncher;

/**
 * Infraestructura de las mediciones JMH del Requerimiento 2 (REQ-17, REQ-19).
 *
 * <p><b>Por qué un solo hilo.</b> Solo puede haber una medición en curso (REQ-19) y dos JMH en paralelo se
 * robarían CPU, falseando los tiempos. Con un hilo, las mediciones nunca se solapan aunque fallara la
 * comprobación del 409.
 */
@Configuration(proxyBeanMethods = false)
public class BenchmarkConfig {

	/**
	 * Executor de un hilo donde corren las mediciones. Al cerrar la aplicación se interrumpe
	 * ({@code shutdownNow}), lo que detiene el proceso de JMH en curso.
	 *
	 * <p>Complejidad: O(1) en tiempo y espacio.
	 *
	 * @return executor de un hilo llamado {@code benchmark-1}
	 */
	@Bean(name = "benchmarkExecutor", destroyMethod = "shutdownNow")
	public ExecutorService benchmarkExecutor() {
		return Executors.newSingleThreadExecutor(Thread.ofPlatform().name("benchmark-", 1).factory());
	}

	/**
	 * Launcher de producción: JMH como proceso hijo sobre {@code bibliomtr.benchmark.jar}.
	 *
	 * <p>Complejidad: O(1) en tiempo y espacio.
	 *
	 * @param properties propiedades {@code bibliomtr.*}
	 * @return el launcher
	 */
	@Bean
	public BenchmarkLauncher benchmarkLauncher(BibliomtrProperties properties) {
		return new JmhProcessLauncher(properties.benchmark().jar());
	}

}
