package co.edu.uniquindio.bibliomtr.api.benchmark;

import java.time.Instant;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import co.edu.uniquindio.bibliomtr.persistence.benchmark.BenchmarkRun;
import co.edu.uniquindio.bibliomtr.persistence.benchmark.BenchmarkRunRepository;
import co.edu.uniquindio.bibliomtr.persistence.benchmark.BenchmarkStatus;

/**
 * Al arrancar la aplicación, pasa a FALLIDA toda medición que quedó EN_CURSO (REQ-21, AC-24).
 *
 * <p><b>Por qué.</b> El proceso de JMH muere con la aplicación, de modo que una medición EN_CURSO de un
 * arranque anterior nunca terminará. Si quedara así, cualquier medición nueva daría HTTP 409 para siempre.
 */
@Component
public class BenchmarkStartupRecovery {

	/** Mensaje de la spec para una medición cortada por un reinicio. */
	public static final String INTERRUPTED_MESSAGE = "Medición interrumpida por reinicio de la aplicación";

	private static final Logger LOG = LoggerFactory.getLogger(BenchmarkStartupRecovery.class);

	private final BenchmarkRunRepository repository;

	/**
	 * Recuperación sobre el historial de mediciones.
	 *
	 * <p>Complejidad: O(1) en tiempo y espacio.
	 *
	 * @param repository historial de mediciones
	 */
	public BenchmarkStartupRecovery(BenchmarkRunRepository repository) {
		this.repository = repository;
	}

	/**
	 * Cierra como FALLIDA cada medición EN_CURSO, con {@code finishedAt} = ahora.
	 *
	 * <p>Complejidad: O(r) en tiempo y espacio, con r = mediciones EN_CURSO (a lo sumo una en uso normal).
	 */
	@EventListener(ApplicationReadyEvent.class)
	@Transactional
	public void failInterruptedRuns() {
		List<BenchmarkRun> interrupted = repository.findAllByStatus(BenchmarkStatus.EN_CURSO);
		Instant now = Instant.now();
		for (BenchmarkRun run : interrupted) {
			run.fail(now, INTERRUPTED_MESSAGE);
		}
		repository.saveAll(interrupted);
		if (!interrupted.isEmpty()) {
			LOG.info("{} medición(es) JMH EN_CURSO pasaron a FALLIDA por reinicio", interrupted.size());
		}
	}

}
