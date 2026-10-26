package co.edu.uniquindio.bibliomtr.persistence.benchmark;

import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import co.edu.uniquindio.bibliomtr.algorithms.ranking.AlgorithmParameters;
import co.edu.uniquindio.bibliomtr.algorithms.ranking.SortAlgorithm;

/**
 * Resultado de un algoritmo en una medición COMPLETADA, guardado en {@code benchmark_algorithm_result}
 * (REQ-22, REQ-27).
 *
 * <p><b>Qué guarda.</b> El tiempo promedio y el scoreError de JMH en ms, los parámetros propios del algoritmo
 * (Counting: {@code max_frequency} = k; Radix: {@code max_digits} = d y {@code radix_base} = b; Bucket:
 * {@code bucket_count} = m; los demás en {@code NULL}) y su complejidad teórica, para mostrarlos juntos.
 */
@Entity
@Table(name = "benchmark_algorithm_result")
public class BenchmarkAlgorithmResult {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "run_id", nullable = false)
	private BenchmarkRun run;

	@Enumerated(EnumType.STRING)
	@Column(name = "algorithm", nullable = false, length = 8)
	private SortAlgorithm algorithm;

	@Column(name = "score_ms", nullable = false)
	private double scoreMs;

	@Column(name = "score_error_ms", nullable = false)
	private double scoreErrorMs;

	@Column(name = "max_frequency")
	private Integer maxFrequency;

	@Column(name = "max_digits")
	private Integer maxDigits;

	@Column(name = "radix_base")
	private Integer radixBase;

	@Column(name = "bucket_count")
	private Integer bucketCount;

	@Column(name = "complexity", nullable = false, length = 64)
	private String complexity;

	/** Constructor que exige JPA. */
	protected BenchmarkAlgorithmResult() {
	}

	/**
	 * Crea el resultado de un algoritmo; se asocia a su medición con {@link BenchmarkRun#complete}.
	 *
	 * <p>Complejidad: O(1) en tiempo y espacio.
	 *
	 * @param algorithm    algoritmo medido; de él se toma la complejidad teórica
	 * @param scoreMs      tiempo promedio por operación en ms
	 * @param scoreErrorMs scoreError de JMH en ms
	 * @param parameters   parámetros propios del algoritmo (k; d y b; m)
	 */
	public BenchmarkAlgorithmResult(SortAlgorithm algorithm, double scoreMs, double scoreErrorMs,
			AlgorithmParameters parameters) {
		this.algorithm = Objects.requireNonNull(algorithm, "El algoritmo no puede ser nulo");
		Objects.requireNonNull(parameters, "Los parámetros del algoritmo no pueden ser nulos");
		this.scoreMs = scoreMs;
		this.scoreErrorMs = scoreErrorMs;
		this.maxFrequency = parameters.k();
		this.maxDigits = parameters.d();
		this.radixBase = parameters.b();
		this.bucketCount = parameters.m();
		this.complexity = algorithm.complexity();
	}

	/**
	 * Asocia el resultado a su medición (lado dueño de la relación).
	 *
	 * <p>Complejidad: O(1) en tiempo y espacio.
	 */
	void attachTo(BenchmarkRun owner) {
		this.run = owner;
	}

	/**
	 * Algoritmo medido.
	 *
	 * <p>Complejidad: O(1) en tiempo y espacio.
	 *
	 * @return el algoritmo
	 */
	public SortAlgorithm getAlgorithm() {
		return algorithm;
	}

	/**
	 * Tiempo promedio por operación.
	 *
	 * <p>Complejidad: O(1) en tiempo y espacio.
	 *
	 * @return score de JMH en ms
	 */
	public double getScoreMs() {
		return scoreMs;
	}

	/**
	 * Semiamplitud del intervalo de confianza al 99,9 %.
	 *
	 * <p>Complejidad: O(1) en tiempo y espacio.
	 *
	 * @return scoreError de JMH en ms
	 */
	public double getScoreErrorMs() {
		return scoreErrorMs;
	}

	/**
	 * Parámetros propios del algoritmo, reconstruidos de sus columnas.
	 *
	 * <p>Complejidad: O(1) en tiempo y espacio.
	 *
	 * @return k; d y b; m, con {@code null} en los que no aplican
	 */
	public AlgorithmParameters getParameters() {
		return new AlgorithmParameters(maxFrequency, maxDigits, radixBase, bucketCount);
	}

	/**
	 * Complejidad teórica guardada con la medición.
	 *
	 * <p>Complejidad: O(1) en tiempo y espacio.
	 *
	 * @return p. ej. {@code O(n+k)}
	 */
	public String getComplexity() {
		return complexity;
	}

}
