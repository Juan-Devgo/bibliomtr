package co.edu.uniquindio.bibliomtr.evaluation;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import co.edu.uniquindio.bibliomtr.algorithms.ranking.TermFrequency;

/**
 * Archivo TSV con la lista de frecuencias que mide JMH (REQ-17).
 *
 * <p><b>Qué hace.</b> Escribe y lee una línea {@code term\tfrequency} por par, en UTF-8 y en el mismo orden
 * de la lista (el de entrada de los algoritmos: alfabético descendente, REQ-2). No hay encabezado.
 *
 * <p><b>Por qué.</b> La API calcula la lista una vez y el benchmark, que corre en otro proceso
 * ({@code java -jar benchmarks.jar}), recibe la ruta del archivo como parámetro {@code inputFile}. Así JMH mide
 * exactamente la misma lista que la API, sin repetir la ingesta de la spec 001. Conservar el orden importa:
 * los tres algoritmos son estables, y el desempate alfabético del ranking depende del orden de entrada.
 */
public final class FrequencyListFile {

	private static final char SEPARATOR = '\t';

	private FrequencyListFile() {
	}

	/**
	 * Escribe la lista en {@code file}, reemplazando su contenido.
	 *
	 * <p>Complejidad: O(L) en tiempo, con L = número total de caracteres escritos, y O(1) en espacio adicional
	 * (escritura con búfer).
	 *
	 * @param file  archivo destino
	 * @param input pares en el orden de entrada de los algoritmos
	 * @throws IllegalArgumentException si un término contiene un tabulador o un salto de línea, que romperían
	 *                                  el formato
	 * @throws UncheckedIOException     si no se puede escribir el archivo
	 */
	public static void write(Path file, List<TermFrequency> input) {
		Objects.requireNonNull(file, "El archivo no puede ser nulo");
		Objects.requireNonNull(input, "La lista de frecuencias no puede ser nula");
		try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
			for (TermFrequency element : input) {
				String term = element.term();
				if (term.indexOf(SEPARATOR) >= 0 || term.indexOf('\n') >= 0 || term.indexOf('\r') >= 0) {
					throw new IllegalArgumentException(
							"El término no puede contener tabuladores ni saltos de línea: " + term);
				}
				writer.write(term);
				writer.write(SEPARATOR);
				writer.write(Integer.toString(element.frequency()));
				writer.newLine();
			}
		} catch (IOException exception) {
			throw new UncheckedIOException("No se pudo escribir la lista de frecuencias en " + file, exception);
		}
	}

	/**
	 * Lee la lista de {@code file} en el orden de sus líneas; las líneas vacías se ignoran.
	 *
	 * <p>Complejidad: O(L) en tiempo y O(n) en espacio, con L = número de caracteres del archivo y n = número
	 * de pares.
	 *
	 * @param file archivo escrito por {@link #write(Path, List)}
	 * @return pares en el orden del archivo (lista inmutable)
	 * @throws IllegalArgumentException si una línea no tiene la forma {@code term\tfrequency}
	 * @throws UncheckedIOException     si no se puede leer el archivo
	 */
	public static List<TermFrequency> read(Path file) {
		Objects.requireNonNull(file, "El archivo no puede ser nulo");
		List<TermFrequency> input = new ArrayList<>();
		try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
			int lineNumber = 0;
			for (String line = reader.readLine(); line != null; line = reader.readLine()) {
				lineNumber++;
				if (line.isEmpty()) {
					continue;
				}
				// El término no contiene tabuladores (ver write), así que el separador es el último tabulador.
				int separator = line.lastIndexOf(SEPARATOR);
				if (separator < 0) {
					throw new IllegalArgumentException(
							"Línea %d de %s sin separador de tabulador".formatted(lineNumber, file));
				}
				input.add(new TermFrequency(line.substring(0, separator), parseFrequency(line, separator,
						lineNumber, file)));
			}
		} catch (IOException exception) {
			throw new UncheckedIOException("No se pudo leer la lista de frecuencias de " + file, exception);
		}
		return List.copyOf(input);
	}

	/**
	 * Frecuencia de la línea, a la derecha del separador.
	 *
	 * <p>Complejidad: O(l) en tiempo y O(1) en espacio, con l = longitud de la línea.
	 */
	private static int parseFrequency(String line, int separator, int lineNumber, Path file) {
		try {
			return Integer.parseInt(line.substring(separator + 1));
		} catch (NumberFormatException exception) {
			throw new IllegalArgumentException(
					"Línea %d de %s con una frecuencia que no es entera".formatted(lineNumber, file), exception);
		}
	}

}
