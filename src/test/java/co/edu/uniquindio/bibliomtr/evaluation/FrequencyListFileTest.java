package co.edu.uniquindio.bibliomtr.evaluation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import co.edu.uniquindio.bibliomtr.algorithms.ranking.TermFrequency;

/**
 * Tests del archivo TSV que recibe el benchmark JMH (REQ-17).
 */
class FrequencyListFileTest {

	@TempDir
	Path directory;

	@Test
	@DisplayName("Ida y vuelta: read devuelve los mismos pares en el mismo orden de entrada")
	void roundTripKeepsInputOrder() throws IOException {
		// Orden alfabético descendente con un empate (graph, data): el orden debe conservarse tal cual.
		List<TermFrequency> input = List.of(
				new TermFrequency("model", 42),
				new TermFrequency("graph", 30),
				new TermFrequency("data", 30),
				new TermFrequency("árbol", 0));
		Path file = directory.resolve("input.tsv");

		FrequencyListFile.write(file, input);

		assertThat(Files.readString(file, StandardCharsets.UTF_8))
				.isEqualTo("model\t42" + System.lineSeparator() + "graph\t30" + System.lineSeparator()
						+ "data\t30" + System.lineSeparator() + "árbol\t0" + System.lineSeparator());
		assertThat(FrequencyListFile.read(file)).containsExactlyElementsOf(input);
	}

	@Test
	@DisplayName("Lista vacía: el archivo queda vacío y read devuelve una lista vacía")
	void emptyList() {
		Path file = directory.resolve("empty.tsv");

		FrequencyListFile.write(file, List.of());

		assertThat(FrequencyListFile.read(file)).isEmpty();
	}

	@Test
	@DisplayName("Un término con tabulador se rechaza porque rompería el formato")
	void rejectsTermWithTab() {
		Path file = directory.resolve("bad.tsv");

		assertThatThrownBy(() -> FrequencyListFile.write(file, List.of(new TermFrequency("a\tb", 1))))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("tabuladores");
	}

	@Test
	@DisplayName("Una línea sin frecuencia entera se rechaza con un mensaje en español")
	void rejectsMalformedLine() throws IOException {
		Path file = directory.resolve("malformed.tsv");
		Files.writeString(file, "model\tabc\n");

		assertThatThrownBy(() -> FrequencyListFile.read(file))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Línea 1");
	}

}
