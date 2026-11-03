package co.edu.uniquindio.bibliomtr.algorithms.stringmatching;

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
 * Análisis estático que verifica que el código de producción del Requerimiento 4 no delega la búsqueda de patrones
 * a funciones nativas de cadenas ni a expresiones regulares (spec 004, REQ-7, REQ-14 y AC-2; constitución P1).
 *
 * <p><b>Alcance en Java.</b> Los {@code *.java} de {@code algorithms/stringmatching}, {@code trace/stringmatching}
 * y {@code api/stringmatching} (este último se escanea en cuanto existe). En cada archivo:
 * <ul>
 *   <li>Sobre el código sin comentarios ni contenido de cadenas o caracteres: ninguna llamada
 *       {@code .<nombre>(} con {@code indexOf}, {@code lastIndexOf}, {@code contains}, {@code matches},
 *       {@code split}, {@code replace}, {@code replaceAll}, {@code replaceFirst}, {@code startsWith},
 *       {@code endsWith} o {@code regionMatches}. Así {@code List.contains(} y {@code String.matches(} fallan, y
 *       {@code Map.containsKey(} y {@code getOrDefault(} no.</li>
 *   <li>Sobre el mismo código: las palabras {@code Pattern} y {@code Matcher} como identificadores completos
 *       ({@code StringMatcher} no cuenta).</li>
 *   <li>Sobre el archivo crudo (comentarios incluidos): la cadena {@code java.util.regex}.</li>
 * </ul>
 *
 * <p><b>Alcance en TypeScript.</b> El detector {@link #typescriptViolations} busca, en el código sin comentarios
 * ni contenido de cadenas, los nombres {@code RegExp}, {@code indexOf}, {@code lastIndexOf}, {@code includes},
 * {@code search}, {@code match}, {@code matchAll}, {@code split}, {@code replace}, {@code replaceAll},
 * {@code startsWith} y {@code endsWith} como identificadores completos, aunque se usen sobre arreglos
 * ({@code searchMs} no cuenta). La interpolación {@code ${…}} de las plantillas se conserva como código. La
 * verificación de los archivos de la pestaña "Búsqueda Patrones" se agrega con la interfaz (fase 4 de la spec 004);
 * aquí solo se prueba el detector.
 *
 * <p>Complejidad: O(L) en tiempo, con L = número total de caracteres recorridos, y O(l) en espacio, con
 * l = tamaño del archivo más grande.
 */
class ForbiddenStringMatchingUsageTest {

	private static final Path MAIN = Path.of("src", "main", "java", "co", "edu", "uniquindio", "bibliomtr");
	private static final Path ALGORITHMS = MAIN.resolve(Path.of("algorithms", "stringmatching"));
	private static final Path TRACES = MAIN.resolve(Path.of("trace", "stringmatching"));
	private static final Path API = MAIN.resolve(Path.of("api", "stringmatching"));

	private static final String REGEX_PACKAGE = "java.util.regex";

	private static final Pattern JAVA_FORBIDDEN_CALLS = Pattern.compile("\\.\\s*(?:indexOf|lastIndexOf|contains"
			+ "|matches|split|replace|replaceAll|replaceFirst|startsWith|endsWith|regionMatches)\\s*\\(");

	private static final Pattern JAVA_REGEX_IDENTIFIERS = Pattern.compile("\\b(?:Pattern|Matcher)\\b");

	private static final Pattern TS_FORBIDDEN_NAMES = Pattern.compile("(?<![\\w$])(?:RegExp|indexOf|lastIndexOf"
			+ "|includes|search|match|matchAll|split|replace|replaceAll|startsWith|endsWith)(?![\\w$])");

	@Test
	@DisplayName("AC-2: algorithms, trace y api de búsqueda de patrones no usan búsquedas nativas ni regex")
	void javaHasNoForbiddenStringMatching() {
		assertThat(ALGORITHMS).as("paquete de algoritmos").isDirectory();
		assertThat(TRACES).as("paquete de trazas").isDirectory();
		List<Path> roots = new ArrayList<>(List.of(ALGORITHMS, TRACES));
		if (Files.isDirectory(API)) {
			roots.add(API);
		}
		List<String> violations = new ArrayList<>();
		for (Path root : roots) {
			for (Path file : sourceFiles(root, ".java")) {
				violations.addAll(javaViolations(file.toString(), read(file)));
			}
		}
		assertThat(violations).isEmpty();
	}

	@Test
	@DisplayName("AC-2: el detector marca las llamadas prohibidas y regex, y no Map.containsKey ni comentarios")
	void detectorRecognizesPatterns() {
		// Java: deben fallar.
		assertThat(javaViolations("A", "boolean b = list.contains(x);")).hasSize(1);
		assertThat(javaViolations("A", "boolean b = s.matches(\"a\");")).hasSize(1);
		assertThat(javaViolations("A", "import java.util.regex.Pattern;")).isNotEmpty();
		assertThat(javaViolations("A", "var p = Pattern.compile(\"ab\");")).hasSize(1);
		assertThat(javaViolations("A", "Matcher m = null;")).hasSize(1);
		assertThat(javaViolations("A", "int k = t . indexOf ('a');")).hasSize(1);
		assertThat(javaViolations("A", "s.replace(a, b); s.replaceAll(a, b); s.replaceFirst(a, b);")).hasSize(3);
		assertThat(javaViolations("A", "/** usa {@code java.util.regex} */ int x;")).hasSize(1);
		// Java: deben pasar.
		assertThat(javaViolations("A", "boolean b = map.containsKey(c);")).isEmpty();
		assertThat(javaViolations("A", "int v = map.getOrDefault(c, -1);")).isEmpty();
		assertThat(javaViolations("A", "StringMatcher<P, T> matcher = null;")).isEmpty();
		assertThat(javaViolations("A", "String s = \"a.split(b)\";")).isEmpty();
		assertThat(javaViolations("A", "String s = \"\\\"x.indexOf(y)\";")).isEmpty();
		assertThat(javaViolations("A", "String s = \"\"\"\n  t.contains(x)\n  \"\"\";")).isEmpty();
		assertThat(javaViolations("A", "char c = '.'; int k = 0; // t.indexOf(x)")).isEmpty();
		assertThat(javaViolations("A", "/* Pattern.compile(x) y s.matches(y) */ int k = 0;")).isEmpty();

		// TypeScript: deben fallar.
		assertThat(typescriptViolations("B", "const ok = xs.includes(1)")).hasSize(1);
		assertThat(typescriptViolations("B", "const r = new RegExp('a')")).hasSize(1);
		assertThat(typescriptViolations("B", "const parts = text.split(' ')")).hasSize(1);
		assertThat(typescriptViolations("B", "const m = text.match(p); const n = t.matchAll(p)")).hasSize(2);
		assertThat(typescriptViolations("B", "const s = `${text.indexOf('a')}`")).hasSize(1);
		// TypeScript: deben pasar.
		assertThat(typescriptViolations("B", "const t = response.searchMs")).isEmpty();
		assertThat(typescriptViolations("B", "const allMatch = r.allMatch; const i = f.mismatchIndex")).isEmpty();
		assertThat(typescriptViolations("B", "const s = \"a.split(b)\"")).isEmpty();
		assertThat(typescriptViolations("B", "const label = 'search'; const u = `includes`")).isEmpty();
		assertThat(typescriptViolations("B", "// t.indexOf(x)\nconst k = 0")).isEmpty();
		assertThat(typescriptViolations("B", "/* xs.includes(1) */ const k = 0")).isEmpty();
	}

	/**
	 * Violaciones Java de un archivo: llamadas prohibidas e identificadores de regex sobre el código sin comentarios
	 * ni cadenas, y {@code java.util.regex} sobre el archivo crudo.
	 *
	 * <p>Complejidad: O(l) en tiempo y espacio.
	 */
	private static List<String> javaViolations(String file, String source) {
		String code = stripCommentsAndStrings(source, false);
		List<String> violations = new ArrayList<>(matches(file, code, JAVA_FORBIDDEN_CALLS));
		violations.addAll(matches(file, code, JAVA_REGEX_IDENTIFIERS));
		if (source.contains(REGEX_PACKAGE)) {
			violations.add(file + ": " + REGEX_PACKAGE);
		}
		return violations;
	}

	/**
	 * Violaciones TypeScript de un archivo: nombres prohibidos como identificadores completos sobre el código sin
	 * comentarios ni cadenas. Lo usa la verificación de la pestaña "Búsqueda Patrones".
	 *
	 * <p>Complejidad: O(l) en tiempo y espacio.
	 */
	static List<String> typescriptViolations(String file, String source) {
		return matches(file, stripCommentsAndStrings(source, true), TS_FORBIDDEN_NAMES);
	}

	/** Una entrada {@code archivo: coincidencia} por cada aparición del patrón. */
	private static List<String> matches(String file, String code, Pattern pattern) {
		List<String> found = new ArrayList<>();
		Matcher matcher = pattern.matcher(code);
		while (matcher.find()) {
			found.add(file + ": " + matcher.group());
		}
		return found;
	}

	/**
	 * Quita los comentarios ({@code //…} y {@code /* … *}{@code /}) y el contenido de las cadenas y caracteres, que
	 * quedan como delimitadores vacíos ({@code ""}, {@code ''}). En Java también vacía los bloques de texto
	 * ({@code """…"""}); en TypeScript vacía las plantillas ({@code `…`}) pero conserva como código sus
	 * interpolaciones {@code ${…}}.
	 *
	 * <p>Complejidad: O(l) en tiempo y espacio, con l = longitud del código.
	 */
	private static String stripCommentsAndStrings(String source, boolean typescript) {
		StringBuilder code = new StringBuilder(source.length());
		int i = 0;
		int n = source.length();
		while (i < n) {
			char c = source.charAt(i);
			char next = i + 1 < n ? source.charAt(i + 1) : '\0';
			if (c == '/' && next == '/') {
				// Comentario de línea: se salta hasta el fin de línea.
				while (i < n && source.charAt(i) != '\n') {
					i++;
				}
				code.append(' ');
			} else if (c == '/' && next == '*') {
				// Comentario de bloque (incluye Javadoc): se salta hasta el cierre.
				int end = source.indexOf("*/", i + 2);
				i = end < 0 ? n : end + 2;
				code.append(' ');
			} else if (!typescript && source.startsWith("\"\"\"", i)) {
				// Bloque de texto de Java: se salta hasta el cierre.
				int end = source.indexOf("\"\"\"", i + 3);
				i = end < 0 ? n : end + 3;
				code.append("\"\"");
			} else if (typescript && c == '`') {
				i = skipTemplate(source, i + 1, code);
			} else if (c == '"' || c == '\'') {
				// Cadena o carácter: se salta el contenido, respetando los escapes.
				i++;
				while (i < n && source.charAt(i) != c && source.charAt(i) != '\n') {
					i += source.charAt(i) == '\\' ? 2 : 1;
				}
				i = Math.min(n, i + 1);
				code.append(c).append(c);
			} else {
				code.append(c);
				i++;
			}
		}
		return code.toString();
	}

	/**
	 * Salta una plantilla de TypeScript que empieza en {@code start} (después de la comilla invertida): agrega
	 * {@code ``} y copia como código cada interpolación {@code ${…}}, contando llaves para hallar su cierre.
	 *
	 * <p>Complejidad: O(l) en tiempo y espacio.
	 *
	 * @return índice siguiente a la comilla invertida de cierre
	 */
	private static int skipTemplate(String source, int start, StringBuilder code) {
		int n = source.length();
		int i = start;
		code.append("``");
		while (i < n && source.charAt(i) != '`') {
			char c = source.charAt(i);
			if (c == '\\') {
				i += 2;
			} else if (c == '$' && i + 1 < n && source.charAt(i + 1) == '{') {
				int depth = 1;
				i += 2;
				code.append(' ');
				while (i < n && depth > 0) {
					char inner = source.charAt(i);
					if (inner == '{') {
						depth++;
					} else if (inner == '}') {
						depth--;
					}
					if (depth > 0) {
						code.append(inner);
					}
					i++;
				}
				code.append(' ');
			} else {
				i++;
			}
		}
		return Math.min(n, i + 1);
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
