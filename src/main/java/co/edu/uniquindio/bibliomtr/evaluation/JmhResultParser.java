package co.edu.uniquindio.bibliomtr.evaluation;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import co.edu.uniquindio.bibliomtr.algorithms.ranking.SortAlgorithm;

/**
 * Lee el archivo de resultados JSON de JMH ({@code -rf json}) y extrae score y scoreError de cada algoritmo
 * (REQ-22; constitución P2: Jackson solo lee el JSON).
 *
 * <p><b>Formato.</b> JMH escribe un arreglo con un objeto por benchmark. De cada uno se usan:
 * <ul>
 *   <li>{@code benchmark}: nombre completo, p. ej.
 *       {@code co.edu.uniquindio.bibliomtr.evaluation.SortingBenchmark.countingSort}; el método
 *       ({@code countingSort}, {@code radixSort}, {@code bucketSort}) identifica el algoritmo.</li>
 *   <li>{@code primaryMetric.score} y {@code primaryMetric.scoreError}: tiempo promedio y semiamplitud del
 *       intervalo de confianza al 99,9 %. JMH escribe {@code "NaN"} como cadena cuando no puede calcular el
 *       error; se lee como {@link Double#NaN}.</li>
 *   <li>{@code primaryMetric.scoreUnit}: debe ser {@code ms/op} (REQ-18).</li>
 * </ul>
 *
 * <p><b>Por qué exige los tres.</b> Una medición COMPLETADA muestra los tres algoritmos (REQ-22); si falta
 * alguno, la corrida de JMH no terminó bien y la medición debe quedar FALLIDA.
 */
public final class JmhResultParser {

	private static final String EXPECTED_UNIT = "ms/op";

	private final ObjectMapper mapper = JsonMapper.builder().build();

	/**
	 * Lee los resultados de los tres algoritmos.
	 *
	 * <p>Complejidad: O(B) en tiempo y espacio, con B = tamaño del archivo JSON.
	 *
	 * @param resultJson archivo escrito por JMH con {@code -rff}
	 * @return un {@link JmhScore} por algoritmo, en el orden Counting, Radix, Bucket
	 * @throws IllegalStateException si el archivo no se puede leer, no tiene el formato de JMH, la unidad no es
	 *                               {@code ms/op}, aparece un benchmark desconocido o falta algún algoritmo
	 */
	public List<JmhScore> parse(Path resultJson) {
		Objects.requireNonNull(resultJson, "El archivo de resultados no puede ser nulo");
		JsonNode root;
		try {
			root = mapper.readTree(resultJson);
		} catch (JacksonException exception) {
			throw new IllegalStateException("No se pudo leer el resultado de JMH: " + exception.getOriginalMessage(),
					exception);
		}
		if (root == null || !root.isArray()) {
			throw new IllegalStateException("El resultado de JMH no es un arreglo de benchmarks");
		}

		Map<SortAlgorithm, JmhScore> scores = new EnumMap<>(SortAlgorithm.class);
		for (JsonNode benchmark : root) {
			SortAlgorithm algorithm = algorithmOf(benchmark.path("benchmark").asString(""));
			JsonNode metric = benchmark.path("primaryMetric");
			String unit = metric.path("scoreUnit").asString("");
			if (!EXPECTED_UNIT.equals(unit)) {
				throw new IllegalStateException(
						"Unidad inesperada en el resultado de JMH: %s (se esperaba %s)".formatted(unit, EXPECTED_UNIT));
			}
			scores.put(algorithm, new JmhScore(algorithm, number(metric.path("score"), "score"),
					number(metric.path("scoreError"), "scoreError")));
		}

		// Se recorre el enum (orden de declaración) en lugar de ordenar: el resultado queda Counting, Radix, Bucket.
		List<JmhScore> ordered = new ArrayList<>(SortAlgorithm.values().length);
		for (SortAlgorithm algorithm : SortAlgorithm.values()) {
			JmhScore score = scores.get(algorithm);
			if (score == null) {
				throw new IllegalStateException("El resultado de JMH no tiene la medición de " + algorithm.slug());
			}
			ordered.add(score);
		}
		return List.copyOf(ordered);
	}

	/**
	 * Algoritmo de un benchmark según el nombre de su método: {@code <slug>Sort}.
	 *
	 * <p>Complejidad: O(l) en tiempo y espacio, con l = longitud del nombre.
	 */
	private static SortAlgorithm algorithmOf(String benchmarkName) {
		String method = benchmarkName.substring(benchmarkName.lastIndexOf('.') + 1);
		for (SortAlgorithm algorithm : SortAlgorithm.values()) {
			if (method.equals(algorithm.slug() + "Sort")) {
				return algorithm;
			}
		}
		throw new IllegalStateException("Benchmark desconocido en el resultado de JMH: " + benchmarkName);
	}

	/**
	 * Valor numérico de un campo; acepta la cadena {@code "NaN"} que JMH escribe cuando no hay error calculable.
	 *
	 * <p>Complejidad: O(1) en tiempo y espacio.
	 */
	private static double number(JsonNode node, String field) {
		if (node.isNumber()) {
			return node.doubleValue();
		}
		if (node.isString() && "NaN".equals(node.stringValue())) {
			return Double.NaN;
		}
		throw new IllegalStateException("El resultado de JMH no tiene un valor numérico en " + field);
	}

}
