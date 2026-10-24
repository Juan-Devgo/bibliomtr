package co.edu.uniquindio.bibliomtr.api.benchmark;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import co.edu.uniquindio.bibliomtr.algorithms.ranking.AlgorithmParameters;
import co.edu.uniquindio.bibliomtr.algorithms.ranking.SortAlgorithm;
import co.edu.uniquindio.bibliomtr.algorithms.ranking.TermFrequency;
import co.edu.uniquindio.bibliomtr.algorithms.ranking.TermFrequencyCounter;
import co.edu.uniquindio.bibliomtr.api.benchmark.BenchmarkDtos.BenchmarkRunDto;
import co.edu.uniquindio.bibliomtr.api.ranking.CorpusTokenSource;
import co.edu.uniquindio.bibliomtr.config.BibliomtrProperties;
import co.edu.uniquindio.bibliomtr.evaluation.BenchmarkLauncher;
import co.edu.uniquindio.bibliomtr.evaluation.BenchmarkTimeoutException;
import co.edu.uniquindio.bibliomtr.evaluation.JmhParameters;
import co.edu.uniquindio.bibliomtr.evaluation.JmhScore;
import co.edu.uniquindio.bibliomtr.persistence.benchmark.BenchmarkAlgorithmResult;
import co.edu.uniquindio.bibliomtr.persistence.benchmark.BenchmarkRun;
import co.edu.uniquindio.bibliomtr.persistence.benchmark.BenchmarkRunRepository;
import co.edu.uniquindio.bibliomtr.persistence.benchmark.BenchmarkStatus;

/**
 * Lanza mediciones JMH de los tres algoritmos sobre la lista de frecuencias del corpus completo y guarda su
 * historial (REQ-17, REQ-19, REQ-20, REQ-21, REQ-22, REQ-27, REQ-28).
 *
 * <p><b>Flujo de {@link #start()}.</b>
 * <ol>
 *   <li>Rechaza con {@link BenchmarkInProgressException} (HTTP 409) si ya hay una medición EN_CURSO.</li>
 *   <li>Arma la lista de frecuencias igual que el ranking (tokens del corpus y conteo en orden alfabético
 *       descendente) y calcula n y los parámetros de cada algoritmo (k; d y b; m) con {@code bucketSize} = 5.</li>
 *   <li>Guarda la medición EN_CURSO y responde de inmediato (HTTP 202).</li>
 *   <li>En el executor de un hilo, {@link BenchmarkLauncher} corre JMH con límite de
 *       {@code bibliomtr.benchmark.timeout} (10 minutos). La medición queda COMPLETADA con un resultado por
 *       algoritmo, o FALLIDA con "La medición superó el límite de 10 minutos" o "La medición falló:
 *       &lt;causa&gt;". Una FALLIDA no toca las COMPLETADA anteriores, que siguen en el historial.</li>
 * </ol>
 *
 * <p><b>Por qué {@code synchronized} y sin transacción en {@code start}.</b> La comprobación del 409 y el
 * guardado EN_CURSO deben ser atómicos frente a otra petición. El guardado hace commit dentro del bloque
 * sincronizado; con una transacción que envolviera el método, el commit llegaría después de liberar el
 * bloqueo y dos peticiones simultáneas podrían pasar la comprobación.
 */
@Service
public class BenchmarkService {

	/** Prefijo del mensaje de una medición que falló (REQ-21). */
	static final String FAILURE_PREFIX = "La medición falló: ";

	private static final Logger LOG = LoggerFactory.getLogger(BenchmarkService.class);

	private final BenchmarkRunRepository repository;
	private final CorpusTokenSource tokenSource;
	private final BenchmarkLauncher launcher;
	private final ExecutorService executor;
	private final Duration timeout;
	private final TransactionTemplate transactions;
	private final TermFrequencyCounter counter = new TermFrequencyCounter();

	/**
	 * Servicio de mediciones.
	 *
	 * <p>Complejidad: O(1) en tiempo y espacio.
	 *
	 * @param repository         historial de mediciones
	 * @param tokenSource        tokens preprocesados del corpus
	 * @param launcher           ejecutor de JMH
	 * @param executor           executor de un hilo {@code benchmarkExecutor}
	 * @param properties         propiedades {@code bibliomtr.*}, de donde sale el límite de tiempo
	 * @param transactionManager gestor de transacciones para actualizar la medición desde el executor
	 */
	public BenchmarkService(BenchmarkRunRepository repository, CorpusTokenSource tokenSource,
			BenchmarkLauncher launcher, @Qualifier("benchmarkExecutor") ExecutorService executor,
			BibliomtrProperties properties, PlatformTransactionManager transactionManager) {
		this.repository = Objects.requireNonNull(repository);
		this.tokenSource = Objects.requireNonNull(tokenSource);
		this.launcher = Objects.requireNonNull(launcher);
		this.executor = Objects.requireNonNull(executor);
		this.timeout = properties.benchmark().timeout();
		this.transactions = new TransactionTemplate(transactionManager);
	}

	/**
	 * Inicia una medición JMH asíncrona y la devuelve EN_CURSO (REQ-17, REQ-19).
	 *
	 * <p>Complejidad: O(T · log V) en tiempo para armar la lista y O(V) en espacio, con T = total de tokens y
	 * V = número de términos; la medición corre aparte.
	 *
	 * @return la medición recién creada, EN_CURSO
	 * @throws BenchmarkInProgressException si ya hay una medición EN_CURSO
	 */
	public synchronized BenchmarkRunDto start() {
		if (repository.existsByStatus(BenchmarkStatus.EN_CURSO)) {
			throw new BenchmarkInProgressException();
		}
		JmhParameters parameters = JmhParameters.FIXED;
		List<TermFrequency> input = counter.count(tokenSource.selectableAbstractTokens());
		Map<SortAlgorithm, AlgorithmParameters> algorithmParameters = new EnumMap<>(SortAlgorithm.class);
		for (SortAlgorithm algorithm : SortAlgorithm.values()) {
			algorithmParameters.put(algorithm, algorithm.parameters(input, parameters.bucketSize()));
		}

		BenchmarkRun run = repository.save(BenchmarkRun.start(Instant.now(), parameters, input.size()));
		long id = run.getId();
		try {
			executor.submit(() -> execute(id, input, algorithmParameters));
		} catch (RejectedExecutionException exception) {
			// Solo ocurre si la aplicación se está cerrando.
			finishAsFailed(id, FAILURE_PREFIX + "la aplicación se está cerrando");
			return BenchmarkRunDto.from(repository.findById(id).orElseThrow());
		}
		return BenchmarkRunDto.from(run);
	}

	/**
	 * Historial completo, de la más reciente a la más antigua, sin paginar (REQ-28).
	 *
	 * <p>Complejidad: O(R · a) en tiempo y espacio, con R = mediciones y a = resultados por medición.
	 *
	 * @return todas las mediciones con sus resultados
	 */
	@Transactional(readOnly = true)
	public List<BenchmarkRunDto> history() {
		List<BenchmarkRun> runs = repository.findAllByOrderByStartedAtDesc();
		List<BenchmarkRunDto> history = new ArrayList<>(runs.size());
		for (BenchmarkRun run : runs) {
			history.add(BenchmarkRunDto.from(run));
		}
		return history;
	}

	/**
	 * Cuerpo de la tarea del executor: corre JMH y cierra la medición como COMPLETADA o FALLIDA.
	 *
	 * <p>Complejidad: la de JMH más O(a) en tiempo y espacio para guardar los resultados.
	 */
	private void execute(long id, List<TermFrequency> input, Map<SortAlgorithm, AlgorithmParameters> parameters) {
		try {
			List<JmhScore> scores = launcher.run(input, timeout);
			transactions.executeWithoutResult(status -> {
				BenchmarkRun run = repository.findById(id).orElseThrow();
				run.complete(Instant.now(), results(scores, parameters));
			});
		} catch (BenchmarkTimeoutException exception) {
			LOG.warn("Medición JMH {} detenida por límite de tiempo", id);
			finishAsFailed(id, exception.getMessage());
		} catch (RuntimeException exception) {
			LOG.warn("Medición JMH {} fallida", id, exception);
			finishAsFailed(id, FAILURE_PREFIX + causeOf(exception));
		}
	}

	/**
	 * Un resultado por algoritmo medido, con sus parámetros propios.
	 *
	 * <p>Complejidad: O(a) en tiempo y espacio, con a = número de algoritmos.
	 */
	private static List<BenchmarkAlgorithmResult> results(List<JmhScore> scores,
			Map<SortAlgorithm, AlgorithmParameters> parameters) {
		List<BenchmarkAlgorithmResult> results = new ArrayList<>(scores.size());
		for (JmhScore score : scores) {
			results.add(new BenchmarkAlgorithmResult(score.algorithm(), score.scoreMs(), score.scoreErrorMs(),
					parameters.get(score.algorithm())));
		}
		return results;
	}

	/**
	 * Pasa la medición a FALLIDA si sigue EN_CURSO; un error al guardar solo se registra, porque al siguiente
	 * arranque {@code BenchmarkStartupRecovery} la cerrará.
	 *
	 * <p>Complejidad: O(1) en tiempo y espacio, más la consulta.
	 */
	private void finishAsFailed(long id, String message) {
		try {
			transactions.executeWithoutResult(status -> repository.findById(id)
					.filter(run -> run.getStatus() == BenchmarkStatus.EN_CURSO)
					.ifPresent(run -> run.fail(Instant.now(), message)));
		} catch (RuntimeException exception) {
			LOG.error("No se pudo marcar como FALLIDA la medición JMH {}", id, exception);
		}
	}

	/**
	 * Causa legible de un error: su mensaje o, si no tiene, el nombre de su tipo.
	 *
	 * <p>Complejidad: O(1) en tiempo y espacio.
	 */
	private static String causeOf(RuntimeException exception) {
		String message = exception.getMessage();
		return message == null || message.isBlank() ? exception.getClass().getSimpleName() : message;
	}

}
