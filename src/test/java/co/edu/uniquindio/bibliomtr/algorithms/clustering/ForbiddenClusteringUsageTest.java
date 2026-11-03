package co.edu.uniquindio.bibliomtr.algorithms.clustering;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Análisis estático que verifica que ni el backend ni la interfaz calculan clustering con librerías (spec 003,
 * REQ-6, REQ-15 y AC-2; constitución P1 y P2).
 *
 * <p><b>Alcance y criterio.</b>
 * <ul>
 *   <li>Los {@code *.java} de {@code src/main/java}, sin comentarios: imports o nombres calificados de
 *       {@code smile.}, {@code weka.}, {@code org.apache.commons.math3.ml.clustering}, {@code com.apporiented},
 *       {@code org.apache.spark.ml.clustering}, {@code org.tribuo.clustering} y {@code jsat.clustering}.</li>
 *   <li>{@code build.gradle.kts}: las coordenadas {@code smile-}, {@code weka}, {@code hierarchical-clustering},
 *       {@code spark-mllib}, {@code tribuo} y {@code jsat}.</li>
 *   <li>{@code web/package.json}: las dependencias {@code ml-hclust}, {@code hclust}, {@code clusterfck},
 *       {@code density-clustering}, {@code ml-kmeans} y {@code hierarchical-clustering}.</li>
 *   <li>Los {@code *.ts}/{@code *.tsx} de {@code web/src}: imports de esos mismos paquetes.</li>
 * </ul>
 * D3 sí está permitido: solo dibuja las fusiones que devuelve la API (AGENTS.md, Stack).
 *
 * <p>Complejidad: O(L) en tiempo, con L = número total de caracteres recorridos, y O(l) en espacio, con
 * l = tamaño del archivo más grande.
 */
class ForbiddenClusteringUsageTest {

	private static final Path JAVA_SOURCES = Path.of("src", "main", "java");
	private static final Path BUILD_FILE = Path.of("build.gradle.kts");
	private static final Path WEB_PACKAGE = Path.of("web", "package.json");
	private static final Path WEB_SOURCES = Path.of("web", "src");

	private static final List<Pattern> JAVA_LIBRARIES = List.of(
			Pattern.compile("\\bsmile\\.\\w"),
			Pattern.compile("\\bweka\\.\\w"),
			Pattern.compile("\\borg\\.apache\\.commons\\.math3\\.ml\\.clustering\\b"),
			Pattern.compile("\\bcom\\.apporiented\\b"),
			Pattern.compile("\\borg\\.apache\\.spark\\.ml\\.clustering\\b"),
			Pattern.compile("\\borg\\.tribuo\\.clustering\\b"),
			Pattern.compile("\\bjsat\\.clustering\\b"));

	private static final List<Pattern> GRADLE_COORDINATES = List.of(
			Pattern.compile("[\"':]smile-"),
			Pattern.compile("\\bweka\\b"),
			Pattern.compile("hierarchical-clustering"),
			Pattern.compile("spark-mllib"),
			Pattern.compile("\\btribuo\\b"),
			Pattern.compile("\\bjsat\\b"));

	private static final String NPM_PACKAGES = "(?:ml-hclust|hclust|clusterfck|density-clustering|ml-kmeans"
			+ "|hierarchical-clustering)";

	private static final Pattern NPM_DEPENDENCY = Pattern.compile("\"" + NPM_PACKAGES + "\"\\s*:");

	private static final Pattern NPM_IMPORT = Pattern.compile("(?:from\\s+|import\\s*\\(\\s*|require\\s*\\(\\s*)['\"]"
			+ NPM_PACKAGES + "['\"]");

	@Test
	@DisplayName("AC-2: el código Java de producción no usa librerías de clustering")
	void javaHasNoClusteringLibraries() {
		List<String> violations = new ArrayList<>();
		for (Path file : sourceFiles(JAVA_SOURCES, ".java")) {
			violations.addAll(matches(file.toString(), stripComments(read(file)), JAVA_LIBRARIES));
		}
		assertThat(violations).isEmpty();
	}

	@Test
	@DisplayName("AC-2: build.gradle.kts no declara librerías de clustering")
	void gradleHasNoClusteringLibraries() {
		assertThat(matches(BUILD_FILE.toString(), stripComments(read(BUILD_FILE)), GRADLE_COORDINATES)).isEmpty();
	}

	@Test
	@DisplayName("AC-2: la interfaz no depende ni importa librerías de clustering")
	void webHasNoClusteringLibraries() {
		if (!Files.isDirectory(WEB_SOURCES)) {
			return;
		}
		List<String> violations = new ArrayList<>(matches(WEB_PACKAGE.toString(), read(WEB_PACKAGE),
				List.of(NPM_DEPENDENCY)));
		for (Path file : sourceFiles(WEB_SOURCES, ".ts", ".tsx")) {
			violations.addAll(matches(file.toString(), read(file), List.of(NPM_IMPORT)));
		}
		assertThat(violations).isEmpty();
	}

	@Test
	@DisplayName("El detector marca los patrones prohibidos y no marca D3 ni los comentarios")
	void detectorRecognizesPatterns() {
		assertThat(matches("A", stripComments("import smile.clustering.HierarchicalClustering;"), JAVA_LIBRARIES))
				.hasSize(1);
		assertThat(matches("A", stripComments("import weka.clusterers.HierarchicalClusterer;"), JAVA_LIBRARIES))
				.hasSize(1);
		assertThat(matches("A", stripComments("// smile.clustering\nint x;"), JAVA_LIBRARIES)).isEmpty();
		assertThat(matches("B", "implementation(\"com.github.haifengl:smile-core:3.0\")", GRADLE_COORDINATES))
				.hasSize(1);
		assertThat(matches("C", "\"ml-hclust\": \"^3.1.0\"", List.of(NPM_DEPENDENCY))).hasSize(1);
		assertThat(matches("D", "import { agnes } from 'ml-hclust'", List.of(NPM_IMPORT))).hasSize(1);
		assertThat(matches("D", "import * as d3 from 'd3'", List.of(NPM_IMPORT))).isEmpty();
	}

	/** Una entrada {@code archivo: coincidencia} por cada aparición de cada patrón. */
	private static List<String> matches(String file, String code, List<Pattern> patterns) {
		List<String> found = new ArrayList<>();
		for (Pattern pattern : patterns) {
			Matcher matcher = pattern.matcher(code);
			while (matcher.find()) {
				found.add(file + ": " + matcher.group());
			}
		}
		return found;
	}

	/**
	 * Quita comentarios {@code //…} y {@code /* … *}{@code /} sin tocar las cadenas.
	 *
	 * <p>Complejidad: O(l) en tiempo y espacio, con l = longitud del código.
	 */
	private static String stripComments(String source) {
		StringBuilder code = new StringBuilder(source.length());
		int i = 0;
		while (i < source.length()) {
			char c = source.charAt(i);
			char next = i + 1 < source.length() ? source.charAt(i + 1) : '\0';
			if (c == '/' && next == '/') {
				while (i < source.length() && source.charAt(i) != '\n') {
					i++;
				}
				code.append(' ');
			} else if (c == '/' && next == '*') {
				int end = source.indexOf("*/", i + 2);
				i = end < 0 ? source.length() : end + 2;
				code.append(' ');
			} else if (c == '"') {
				// Cadena: se copia completa, respetando los escapes.
				code.append(c);
				i++;
				while (i < source.length() && source.charAt(i) != c) {
					if (source.charAt(i) == '\\' && i + 1 < source.length()) {
						code.append(source.charAt(i));
						i++;
					}
					code.append(source.charAt(i));
					i++;
				}
				if (i < source.length()) {
					code.append(c);
					i++;
				}
			} else {
				code.append(c);
				i++;
			}
		}
		return code.toString();
	}

	/** Archivos regulares bajo {@code root} con alguna de las extensiones. */
	private static List<Path> sourceFiles(Path root, String... extensions) {
		try (Stream<Path> paths = Files.walk(root)) {
			return paths.filter(Files::isRegularFile)
					.filter(path -> {
						String name = path.getFileName().toString();
						for (String extension : extensions) {
							if (name.endsWith(extension)) {
								return true;
							}
						}
						return false;
					})
					.toList();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private static String read(Path file) {
		try {
			return Files.readString(file);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

}
