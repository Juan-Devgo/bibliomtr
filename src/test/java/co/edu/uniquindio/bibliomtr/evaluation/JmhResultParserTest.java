package co.edu.uniquindio.bibliomtr.evaluation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import co.edu.uniquindio.bibliomtr.algorithms.ranking.SortAlgorithm;

/**
 * Tests de la lectura del JSON de JMH (REQ-22, AC-13). La muestra {@code jmh/sample-result.json} tiene el
 * formato real de JMH 1.37 con 10 iteraciones de medición, con los benchmarks en el orden alfabético en que
 * JMH los escribe (bucket, counting, radix).
 */
class JmhResultParserTest {

	private final JmhResultParser parser = new JmhResultParser();

	@TempDir
	Path directory;

	@Test
	@DisplayName("AC-13: lee score y scoreError en ms de los tres algoritmos, en el orden Counting, Radix, Bucket")
	void readsScoreAndScoreError() throws URISyntaxException {
		Path sample = Path.of(getClass().getResource("/jmh/sample-result.json").toURI());

		List<JmhScore> scores = parser.parse(sample);

		assertThat(scores).containsExactly(
				new JmhScore(SortAlgorithm.COUNTING, 0.0271, 0.0012),
				new JmhScore(SortAlgorithm.RADIX, 0.1004, 0.0047),
				new JmhScore(SortAlgorithm.BUCKET, 0.0752, 0.0031));
	}

	@Test
	@DisplayName("scoreError \"NaN\" (cadena que escribe JMH) se lee como Double.NaN")
	void readsNanScoreError() throws IOException {
		Path file = write(benchmark("countingSort", "0.5", "\"NaN\"", "ms/op") + ","
				+ benchmark("radixSort", "0.6", "0.01", "ms/op") + ","
				+ benchmark("bucketSort", "0.7", "0.02", "ms/op"));

		List<JmhScore> scores = parser.parse(file);

		assertThat(scores.getFirst().scoreMs()).isEqualTo(0.5);
		assertThat(scores.getFirst().scoreErrorMs()).isNaN();
	}

	@Test
	@DisplayName("Si falta un algoritmo, falla con un mensaje en español")
	void missingAlgorithmFails() throws IOException {
		Path file = write(benchmark("countingSort", "0.5", "0.01", "ms/op") + ","
				+ benchmark("radixSort", "0.6", "0.01", "ms/op"));

		assertThatThrownBy(() -> parser.parse(file))
				.isInstanceOf(IllegalStateException.class)
				.hasMessage("El resultado de JMH no tiene la medición de bucket");
	}

	@Test
	@DisplayName("Una unidad distinta de ms/op falla")
	void wrongUnitFails() throws IOException {
		Path file = write(benchmark("countingSort", "500", "1", "us/op"));

		assertThatThrownBy(() -> parser.parse(file))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("Unidad inesperada");
	}

	@Test
	@DisplayName("Un archivo que no es JSON falla con un mensaje en español")
	void invalidJsonFails() throws IOException {
		Path file = directory.resolve("result.json");
		Files.writeString(file, "no es json");

		assertThatThrownBy(() -> parser.parse(file))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageStartingWith("No se pudo leer el resultado de JMH");
	}

	/** Escribe un arreglo JSON de JMH con los benchmarks dados. */
	private Path write(String benchmarks) throws IOException {
		Path file = directory.resolve("result.json");
		Files.writeString(file, "[" + benchmarks + "]");
		return file;
	}

	/** Un objeto de benchmark de JMH reducido a los campos que lee el parser. */
	private static String benchmark(String method, String score, String scoreError, String unit) {
		return """
				{"benchmark": "co.edu.uniquindio.bibliomtr.evaluation.SortingBenchmark.%s",
				 "mode": "avgt",
				 "primaryMetric": {"score": %s, "scoreError": %s, "scoreUnit": "%s"}}
				""".formatted(method, score, scoreError, unit);
	}

}
