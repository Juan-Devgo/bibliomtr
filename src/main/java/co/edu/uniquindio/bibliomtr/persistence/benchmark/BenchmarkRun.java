package co.edu.uniquindio.bibliomtr.persistence.benchmark;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import co.edu.uniquindio.bibliomtr.evaluation.JmhParameters;

/**
 * Una medición JMH guardada en {@code benchmark_run} (REQ-20, REQ-21, REQ-27).
 *
 * <p><b>Qué guarda.</b> Fecha de inicio y fin, estado, mensaje de error, los parámetros JMH con que se midió
 * ({@link JmhParameters}, en columnas separadas), el número de términos n de la lista y los resultados por
 * algoritmo ({@link BenchmarkAlgorithmResult}).
 *
 * <p><b>Ciclo de vida.</b> Nace EN_CURSO con {@link #start}; pasa una sola vez a COMPLETADA
 * ({@link #complete}) o a FALLIDA ({@link #fail}). Solo almacenamiento: ningún cálculo ocurre en la base.
 */
@Entity
@Table(name = "benchmark_run")
public class BenchmarkRun {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "started_at", nullable = false)
	private Instant startedAt;

	@Column(name = "finished_at")
	private Instant finishedAt;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 12)
	private BenchmarkStatus status;

	@Column(name = "message", columnDefinition = "text")
	private String message;

	@Column(name = "forks", nullable = false)
	private int forks;

	@Column(name = "warmup_iterations", nullable = false)
	private int warmupIterations;

	@Column(name = "warmup_seconds", nullable = false)
	private int warmupSeconds;

	@Column(name = "measurement_iterations", nullable = false)
	private int measurementIterations;

	@Column(name = "measurement_seconds", nullable = false)
	private int measurementSeconds;

	@Column(name = "mode", nullable = false, length = 8)
	private String mode;

	@Column(name = "time_unit", nullable = false, length = 4)
	private String timeUnit;

	@Column(name = "bucket_size", nullable = false)
	private int bucketSize;

	@Column(name = "trace_enabled", nullable = false)
	private boolean traceEnabled;

	@Column(name = "term_count", nullable = false)
	private int termCount;

	// Orden por id, es decir, por orden de inserción: Counting, Radix, Bucket.
	@OneToMany(mappedBy = "run", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("id")
	private List<BenchmarkAlgorithmResult> results = new ArrayList<>();

	/** Constructor que exige JPA. */
	protected BenchmarkRun() {
	}

	/**
	 * Crea una medición EN_CURSO.
	 *
	 * <p>Complejidad: O(1) en tiempo y espacio.
	 *
	 * @param startedAt  instante de inicio
	 * @param parameters parámetros JMH de la medición
	 * @param termCount  número de términos n de la lista medida
	 * @return la medición, sin id hasta que se guarde
	 */
	public static BenchmarkRun start(Instant startedAt, JmhParameters parameters, int termCount) {
		Objects.requireNonNull(startedAt, "El instante de inicio no puede ser nulo");
		Objects.requireNonNull(parameters, "Los parámetros JMH no pueden ser nulos");
		BenchmarkRun run = new BenchmarkRun();
		run.startedAt = startedAt;
		run.status = BenchmarkStatus.EN_CURSO;
		run.forks = parameters.forks();
		run.warmupIterations = parameters.warmupIterations();
		run.warmupSeconds = parameters.warmupSeconds();
		run.measurementIterations = parameters.measurementIterations();
		run.measurementSeconds = parameters.measurementSeconds();
		run.mode = parameters.mode();
		run.timeUnit = parameters.timeUnit();
		run.bucketSize = parameters.bucketSize();
		run.traceEnabled = parameters.traceEnabled();
		run.termCount = termCount;
		return run;
	}

	/**
	 * Pasa la medición a COMPLETADA con los resultados por algoritmo.
	 *
	 * <p>Complejidad: O(a) en tiempo y espacio, con a = número de resultados.
	 *
	 * @param finishedAt instante de fin
	 * @param newResults un resultado por algoritmo, en el orden Counting, Radix, Bucket
	 * @throws IllegalStateException si la medición no está EN_CURSO
	 */
	public void complete(Instant finishedAt, List<BenchmarkAlgorithmResult> newResults) {
		requireInProgress();
		this.finishedAt = Objects.requireNonNull(finishedAt, "El instante de fin no puede ser nulo");
		this.status = BenchmarkStatus.COMPLETADA;
		this.message = null;
		for (BenchmarkAlgorithmResult result : newResults) {
			result.attachTo(this);
			this.results.add(result);
		}
	}

	/**
	 * Pasa la medición a FALLIDA con un mensaje en español; no tiene resultados.
	 *
	 * <p>Complejidad: O(1) en tiempo y espacio.
	 *
	 * @param finishedAt   instante de fin
	 * @param errorMessage motivo, p. ej. "La medición superó el límite de 10 minutos"
	 * @throws IllegalStateException si la medición no está EN_CURSO
	 */
	public void fail(Instant finishedAt, String errorMessage) {
		requireInProgress();
		this.finishedAt = Objects.requireNonNull(finishedAt, "El instante de fin no puede ser nulo");
		this.status = BenchmarkStatus.FALLIDA;
		this.message = Objects.requireNonNull(errorMessage, "El mensaje no puede ser nulo");
	}

	/**
	 * Comprueba que la medición siga EN_CURSO, porque COMPLETADA y FALLIDA son estados finales.
	 *
	 * <p>Complejidad: O(1) en tiempo y espacio.
	 */
	private void requireInProgress() {
		if (status != BenchmarkStatus.EN_CURSO) {
			throw new IllegalStateException("La medición " + id + " ya terminó con estado " + status);
		}
	}

	/**
	 * Identificador asignado por la base.
	 *
	 * <p>Complejidad: O(1) en tiempo y espacio.
	 *
	 * @return el id, o {@code null} si aún no se guardó
	 */
	public Long getId() {
		return id;
	}

	/**
	 * Instante de inicio.
	 *
	 * <p>Complejidad: O(1) en tiempo y espacio.
	 *
	 * @return el instante de inicio
	 */
	public Instant getStartedAt() {
		return startedAt;
	}

	/**
	 * Instante de fin.
	 *
	 * <p>Complejidad: O(1) en tiempo y espacio.
	 *
	 * @return el instante de fin, o {@code null} si está EN_CURSO
	 */
	public Instant getFinishedAt() {
		return finishedAt;
	}

	/**
	 * Estado actual.
	 *
	 * <p>Complejidad: O(1) en tiempo y espacio.
	 *
	 * @return EN_CURSO, COMPLETADA o FALLIDA
	 */
	public BenchmarkStatus getStatus() {
		return status;
	}

	/**
	 * Mensaje en español de una medición FALLIDA.
	 *
	 * <p>Complejidad: O(1) en tiempo y espacio.
	 *
	 * @return el mensaje, o {@code null} si no falló
	 */
	public String getMessage() {
		return message;
	}

	/**
	 * Parámetros JMH con que se midió, reconstruidos de sus columnas.
	 *
	 * <p>Complejidad: O(1) en tiempo y espacio.
	 *
	 * @return los parámetros
	 */
	public JmhParameters getParameters() {
		return new JmhParameters(forks, warmupIterations, warmupSeconds, measurementIterations, measurementSeconds,
				mode, timeUnit, bucketSize, traceEnabled);
	}

	/**
	 * Número de términos n de la lista medida.
	 *
	 * <p>Complejidad: O(1) en tiempo y espacio.
	 *
	 * @return n
	 */
	public int getTermCount() {
		return termCount;
	}

	/**
	 * Resultados por algoritmo, vacíos salvo en una medición COMPLETADA.
	 *
	 * <p>Complejidad: O(1) en tiempo y espacio (vista no modificable).
	 *
	 * @return los resultados en el orden Counting, Radix, Bucket
	 */
	public List<BenchmarkAlgorithmResult> getResults() {
		return Collections.unmodifiableList(results);
	}

}
