package co.edu.uniquindio.bibliomtr.algorithms.ranking;

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
 * Análisis estático que verifica que el código de producción no usa funciones ni librerías de ordenamiento
 * (REQ-12, AC-8; constitución P1).
 *
 * <p><b>Alcance.</b> Recorre los {@code *.java} de {@code src/main/java} y los {@code *.ts}/{@code *.tsx} de
 * {@code web/src} que no sean tests (se excluyen {@code *.test.*}, {@code *.spec.*} y {@code web/src/test/}).
 * {@code docs/referencias/} queda fuera del recorrido. Antes de buscar se quitan los comentarios
 * ({@code //…} y {@code /* … *}{@code /}), respetando las cadenas, para que la documentación pueda nombrar las
 * funciones prohibidas.
 *
 * <p><b>Criterio.</b> Falla ante cualquiera de estos patrones:
 * <ul>
 *   <li>Java: {@code Collections\.sort\s*\(}, {@code Arrays\.sort\s*\(}, {@code Arrays\.parallelSort\s*\(}
 *       y {@code \.sorted\s*\(} ({@code Stream.sorted}).</li>
 *   <li>Java: {@code \.sort\s*\(} cuyo primer argumento es un comparador: {@code null},
 *       {@code Comparator.}, una lambda {@code ->} o una referencia {@code ::}. Así se detectan
 *       {@code List.sort} y las llamadas {@code .sort(} sobre {@code List}/{@code Comparator}, sin marcar
 *       {@code TermSorter.sort(input, options)}.</li>
 *   <li>Java: imports de librerías de ordenamiento, {@code com.google.common.collect.Ordering} y
 *       {@code org.apache.commons.collections4.ComparatorUtils}.</li>
 *   <li>Java: {@code TreeMap}, {@code TreeSet} o {@code PriorityQueue} fuera de
 *       {@code algorithms/ranking/TermFrequencyCounter.java}, único lugar donde {@code TreeMap} está
 *       permitido para armar la entrada (REQ-2).</li>
 *   <li>TypeScript: {@code \.sort\s*\(} y {@code \.toSorted\s*\(} ({@code Array.prototype}).</li>
 * </ul>
 *
 * <p>Complejidad: O(L) en tiempo, con L = número total de caracteres recorridos, y O(l) en espacio, con
 * l = tamaño del archivo más grande.
 */
class ForbiddenSortUsageTest {

	private static final Path JAVA_SOURCES = Path.of("src", "main", "java");
	private static final Path WEB_SOURCES = Path.of("web", "src");
	private static final Path WEB_TESTS = WEB_SOURCES.resolve("test");
	private static final String TREE_MAP_ALLOWED_FILE = "TermFrequencyCounter.java";

	private static final List<Pattern> JAVA_PATTERNS = List.of(
			Pattern.compile("Collections\\.sort\\s*\\("),
			Pattern.compile("Arrays\\.sort\\s*\\("),
			Pattern.compile("Arrays\\.parallelSort\\s*\\("),
			Pattern.compile("\\.sorted\\s*\\("),
			// .sort( cuyo primer argumento es null, Comparator., una lambda o una referencia a método.
			Pattern.compile("\\.sort\\s*\\(\\s*(?:null\\b|Comparator\\s*\\.|\\([^()]*\\)\\s*->|[A-Za-z_$][\\w$]*\\s*->"
					+ "|[\\w$.]+\\s*::)"),
			Pattern.compile("import\\s+(?:static\\s+)?com\\.google\\.common\\.collect\\.Ordering\\b"),
			Pattern.compile("import\\s+(?:static\\s+)?org\\.apache\\.commons\\.collections4\\.ComparatorUtils\\b"));

	private static final Pattern SORTED_STRUCTURES = Pattern.compile("\\b(?:TreeMap|TreeSet|PriorityQueue)\\b");

	private static final List<Pattern> TS_PATTERNS = List.of(
			Pattern.compile("\\.sort\\s*\\("),
			Pattern.compile("\\.toSorted\\s*\\("));

	@Test
	@DisplayName("AC-8: el código de producción Java no usa ordenamientos de la librería")
	void javaProductionCodeHasNoForbiddenSort() {
		assertThat(JAVA_SOURCES).as("directorio de fuentes Java").isDirectory();
		List<String> violations = new ArrayList<>();
		for (Path file : sourceFiles(JAVA_SOURCES, ".java")) {
			boolean treeMapAllowed = file.endsWith(Path.of("algorithms", "ranking", TREE_MAP_ALLOWED_FILE));
			violations.addAll(javaViolations(file.toString(), read(file), treeMapAllowed));
		}
		assertThat(violations).isEmpty();
	}

	@Test
	@DisplayName("AC-8: el código de producción TypeScript no usa Array.sort ni toSorted")
	void typescriptProductionCodeHasNoForbiddenSort() {
		if (!Files.isDirectory(WEB_SOURCES)) {
			return;
		}
		List<String> violations = new ArrayList<>();
		for (Path file : sourceFiles(WEB_SOURCES, ".ts", ".tsx")) {
			String name = file.getFileName().toString();
			if (file.startsWith(WEB_TESTS) || name.contains(".test.") || name.contains(".spec.")) {
				continue;
			}
			violations.addAll(matches(file.toString(), stripComments(read(file), true), TS_PATTERNS));
		}
		assertThat(violations).isEmpty();
	}

	@Test
	@DisplayName("El detector marca los patrones prohibidos y no marca TermSorter.sort ni los comentarios")
	void detectorRecognizesPatterns() {
		assertThat(javaViolations("A", "Collections.sort(list);", false)).hasSize(1);
		assertThat(javaViolations("A", "Arrays.sort (array);", false)).hasSize(1);
		assertThat(javaViolations("A", "Arrays.parallelSort(array);", false)).hasSize(1);
		assertThat(javaViolations("A", "list.stream().sorted().toList();", false)).hasSize(1);
		assertThat(javaViolations("A", "list.sort(null);", false)).hasSize(1);
		assertThat(javaViolations("A", "list.sort(Comparator.comparingInt(x -> x));", false)).isNotEmpty();
		assertThat(javaViolations("A", "list.sort((a, b) -> a - b);", false)).hasSize(1);
		assertThat(javaViolations("A", "list.sort(a -> 0);", false)).hasSize(1);
		assertThat(javaViolations("A", "list.sort(Integer::compare);", false)).hasSize(1);
		assertThat(javaViolations("A", "import com.google.common.collect.Ordering;", false)).hasSize(1);
		assertThat(javaViolations("A", "import org.apache.commons.collections4.ComparatorUtils;", false)).hasSize(1);
		assertThat(javaViolations("A", "new TreeMap<String, Integer>();", false)).hasSize(1);
		assertThat(javaViolations("A", "new TreeSet<>(); new PriorityQueue<>();", false)).hasSize(2);
		assertThat(javaViolations("A", "new TreeMap<String, Integer>();", true)).isEmpty();
		assertThat(javaViolations("A", "sorter.sort(input, options);", false)).isEmpty();
		assertThat(javaViolations("A", "sorter.sort(input, new SortOptions(5, false));", false)).isEmpty();
		assertThat(javaViolations("A", "/** {@code Collections.sort} */ int x; // Arrays.sort(a)", false)).isEmpty();
		assertThat(javaViolations("A", "String s = \"http://x\"; Arrays.sort(a);", false)).hasSize(1);
		assertThat(matches("B", stripComments("items.sort((a, b) => a - b)", true), TS_PATTERNS)).hasSize(1);
		assertThat(matches("B", stripComments("const s = items.toSorted()", true), TS_PATTERNS)).hasSize(1);
		assertThat(matches("B", stripComments("// items.sort()\nconst u = `a//b`", true), TS_PATTERNS)).isEmpty();
	}

	/** Violaciones Java de un archivo, con los comentarios ya quitados. */
	private static List<String> javaViolations(String file, String source, boolean treeMapAllowed) {
		String code = stripComments(source, false);
		List<String> violations = new ArrayList<>(matches(file, code, JAVA_PATTERNS));
		if (!treeMapAllowed) {
			violations.addAll(matches(file, code, List.of(SORTED_STRUCTURES)));
		}
		return violations;
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
	 * Quita comentarios de línea y de bloque sin tocar el contenido de las cadenas ({@code "…"},
	 * {@code '…'} y, en TypeScript, {@code `…`}). Cada comentario se reemplaza por un espacio.
	 *
	 * <p>Complejidad: O(l) en tiempo y espacio, con l = longitud del código.
	 */
	private static String stripComments(String source, boolean typescript) {
		StringBuilder code = new StringBuilder(source.length());
		int i = 0;
		while (i < source.length()) {
			char c = source.charAt(i);
			char next = i + 1 < source.length() ? source.charAt(i + 1) : '\0';
			if (c == '/' && next == '/') {
				// Comentario de línea: se salta hasta el fin de línea.
				while (i < source.length() && source.charAt(i) != '\n') {
					i++;
				}
				code.append(' ');
			} else if (c == '/' && next == '*') {
				// Comentario de bloque (incluye Javadoc): se salta hasta el cierre.
				int end = source.indexOf("*/", i + 2);
				i = end < 0 ? source.length() : end + 2;
				code.append(' ');
			} else if (c == '"' || c == '\'' || (typescript && c == '`')) {
				// Cadena o carácter: se copia completa, respetando los escapes.
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
