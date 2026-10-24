package co.edu.uniquindio.bibliomtr.api.benchmark;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import co.edu.uniquindio.bibliomtr.algorithms.ranking.SortAlgorithm;
import co.edu.uniquindio.bibliomtr.algorithms.ranking.TermFrequency;
import co.edu.uniquindio.bibliomtr.evaluation.BenchmarkLauncher;
import co.edu.uniquindio.bibliomtr.evaluation.BenchmarkTimeoutException;
import co.edu.uniquindio.bibliomtr.evaluation.JmhScore;

/**
 * {@link BenchmarkLauncher} falso para los tests (REQ-17, REQ-21): responde al instante, sin correr JMH, según
 * su modo.
 * <ul>
 *   <li>{@link Mode#COMPLETE}: devuelve {@link #SCORES}.</li>
 *   <li>{@link Mode#FAIL}: lanza {@link IllegalStateException} con {@link #FAILURE_CAUSE}.</li>
 *   <li>{@link Mode#TIMEOUT}: lanza {@link BenchmarkTimeoutException} con el límite recibido.</li>
 *   <li>{@link Mode#BLOCK}: espera hasta {@link #release()} (un {@link CountDownLatch}) y luego devuelve
 *       {@link #SCORES}; deja la medición EN_CURSO mientras tanto.</li>
 * </ul>
 * Registra cada invocación (entrada y límite) para comprobar qué recibió.
 *
 * <p>Se registra con {@code @Import(FakeBenchmarkLauncher.Config.class)} como bean {@code @Primary}, de modo
 * que reemplaza al {@code JmhProcessLauncher} de producción.
 */
public class FakeBenchmarkLauncher implements BenchmarkLauncher {

	/** Comportamiento del launcher falso. */
	public enum Mode {
		/** Termina con resultados. */
		COMPLETE,
		/** Falla con un error. */
		FAIL,
		/** Supera el límite de tiempo. */
		TIMEOUT,
		/** Espera a {@link #release()} y termina con resultados. */
		BLOCK
	}

	/** Causa del error en modo {@link Mode#FAIL}. */
	public static final String FAILURE_CAUSE = "JMH terminó con código 1";

	/** Resultados en modo {@link Mode#COMPLETE} y {@link Mode#BLOCK}, en el orden Counting, Radix, Bucket. */
	public static final List<JmhScore> SCORES = List.of(
			new JmhScore(SortAlgorithm.COUNTING, 0.0271, 0.0012),
			new JmhScore(SortAlgorithm.RADIX, 0.1004, 0.0047),
			new JmhScore(SortAlgorithm.BUCKET, 0.0752, 0.0031));

	/** Espera máxima en modo {@link Mode#BLOCK}, para que un test roto no cuelgue el build. */
	private static final long MAX_BLOCK_SECONDS = 30;

	/**
	 * Una llamada a {@link #run}.
	 *
	 * @param input   lista recibida
	 * @param timeout límite recibido
	 */
	public record Invocation(List<TermFrequency> input, Duration timeout) {
	}

	private volatile Mode mode = Mode.COMPLETE;
	private volatile CountDownLatch blocker = new CountDownLatch(0);
	private final List<Invocation> invocations = new CopyOnWriteArrayList<>();

	/**
	 * {@inheritDoc}
	 *
	 * <p>Complejidad: O(n) en tiempo y espacio por la copia de la entrada.
	 */
	@Override
	public List<JmhScore> run(List<TermFrequency> input, Duration timeout) throws BenchmarkTimeoutException {
		invocations.add(new Invocation(List.copyOf(input), timeout));
		switch (mode) {
			case FAIL -> throw new IllegalStateException(FAILURE_CAUSE);
			case TIMEOUT -> throw new BenchmarkTimeoutException(timeout);
			case BLOCK -> awaitRelease();
			case COMPLETE -> {
			}
		}
		return SCORES;
	}

	/**
	 * Fija el modo de las próximas mediciones.
	 *
	 * @param newMode modo
	 */
	public void setMode(Mode newMode) {
		if (newMode == Mode.BLOCK) {
			blocker = new CountDownLatch(1);
		}
		this.mode = newMode;
	}

	/** Libera la medición bloqueada en modo {@link Mode#BLOCK}. */
	public void release() {
		blocker.countDown();
	}

	/** Vuelve al modo {@link Mode#COMPLETE}, libera cualquier bloqueo y olvida las invocaciones. */
	public void reset() {
		release();
		mode = Mode.COMPLETE;
		invocations.clear();
	}

	/**
	 * Invocaciones recibidas, en orden.
	 *
	 * @return copia de las invocaciones
	 */
	public List<Invocation> invocations() {
		return List.copyOf(invocations);
	}

	/** Espera a {@link #release()}; una interrupción (cierre de la aplicación) se trata como falla. */
	private void awaitRelease() {
		try {
			if (!blocker.await(MAX_BLOCK_SECONDS, TimeUnit.SECONDS)) {
				throw new IllegalStateException("el test no liberó la medición bloqueada");
			}
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("la medición fue interrumpida", exception);
		}
	}

	/**
	 * Configuración de test que registra el fake como {@link BenchmarkLauncher} principal.
	 */
	@TestConfiguration(proxyBeanMethods = false)
	public static class Config {

		/**
		 * Fake en modo {@link Mode#COMPLETE}; cada test ajusta su modo.
		 *
		 * @return el fake compartido del contexto
		 */
		@Bean
		@Primary
		FakeBenchmarkLauncher fakeBenchmarkLauncher() {
			return new FakeBenchmarkLauncher();
		}

	}

}
