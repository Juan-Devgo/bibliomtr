package co.edu.uniquindio.bibliomtr.api.benchmark;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import co.edu.uniquindio.bibliomtr.algorithms.ranking.AlgorithmParameters;
import co.edu.uniquindio.bibliomtr.evaluation.JmhParameters;
import co.edu.uniquindio.bibliomtr.persistence.benchmark.BenchmarkAlgorithmResult;
import co.edu.uniquindio.bibliomtr.persistence.benchmark.BenchmarkRun;

/**
 * Respuestas JSON de {@code POST /api/benchmarks} y {@code GET /api/benchmarks} (REQ-19, REQ-22, REQ-27,
 * REQ-28).
 *
 * <p><b>Por qué DTOs.</b> La API no expone las entidades JPA: así la relación perezosa con los resultados y
 * los detalles de las columnas no llegan al JSON.
 */
public final class BenchmarkDtos {

	private BenchmarkDtos() {
	}

	/**
	 * Una medición JMH con su estado, sus parámetros y sus resultados.
	 *
	 * <p>Complejidad: construcción O(a) en tiempo y espacio por la copia de los resultados.
	 *
	 * @param id         identificador de la medición
	 * @param startedAt  instante de inicio
	 * @param finishedAt instante de fin, {@code null} si está EN_CURSO
	 * @param status     {@code EN_CURSO}, {@code COMPLETADA} o {@code FALLIDA}
	 * @param message    mensaje en español de una medición FALLIDA, {@code null} en otro caso
	 * @param parameters parámetros JMH con que se midió (REQ-18)
	 * @param termCount  número de términos n de la lista medida
	 * @param results    un resultado por algoritmo (Counting, Radix, Bucket) si está COMPLETADA; vacío si no
	 */
	public record BenchmarkRunDto(long id, Instant startedAt, Instant finishedAt, String status, String message,
			JmhParameters parameters, Integer termCount, List<BenchmarkResultDto> results) {

		/**
		 * Copia defensiva de los resultados.
		 *
		 * <p>Complejidad: O(a) en tiempo y espacio, con a = número de resultados.
		 */
		public BenchmarkRunDto {
			results = List.copyOf(results);
		}

		/**
		 * Convierte una medición guardada. Debe llamarse dentro de una transacción, porque lee sus resultados.
		 *
		 * <p>Complejidad: O(a) en tiempo y espacio, con a = número de resultados.
		 *
		 * @param run medición guardada (con id)
		 * @return el DTO
		 */
		public static BenchmarkRunDto from(BenchmarkRun run) {
			List<BenchmarkResultDto> results = new ArrayList<>(run.getResults().size());
			for (BenchmarkAlgorithmResult result : run.getResults()) {
				results.add(BenchmarkResultDto.from(result));
			}
			return new BenchmarkRunDto(run.getId(), run.getStartedAt(), run.getFinishedAt(), run.getStatus().name(),
					run.getMessage(), run.getParameters(), run.getTermCount(), results);
		}
	}

	/**
	 * Resultado de un algoritmo: ms ± scoreError, sus parámetros propios y su complejidad teórica (REQ-22).
	 * El n de la fórmula es {@link BenchmarkRunDto#termCount()}.
	 *
	 * <p>Complejidad: construcción O(1) en tiempo y espacio.
	 *
	 * @param algorithm    slug del algoritmo ({@code counting}, {@code radix} o {@code bucket})
	 * @param scoreMs      tiempo promedio por operación en ms
	 * @param scoreErrorMs semiamplitud del intervalo de confianza al 99,9 % en ms
	 * @param parameters   k (Counting); d y b (Radix); m (Bucket); los que no aplican en {@code null}
	 * @param complexity   complejidad teórica, p. ej. {@code O(n+k)}
	 */
	public record BenchmarkResultDto(String algorithm, double scoreMs, double scoreErrorMs,
			AlgorithmParameters parameters, String complexity) {

		/**
		 * Convierte un resultado guardado.
		 *
		 * <p>Complejidad: O(1) en tiempo y espacio.
		 *
		 * @param result resultado guardado
		 * @return el DTO
		 */
		public static BenchmarkResultDto from(BenchmarkAlgorithmResult result) {
			return new BenchmarkResultDto(result.getAlgorithm().slug(), result.getScoreMs(), result.getScoreErrorMs(),
					result.getParameters(), result.getComplexity());
		}
	}

}
