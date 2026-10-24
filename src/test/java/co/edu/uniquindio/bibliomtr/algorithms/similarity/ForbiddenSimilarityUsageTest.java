package co.edu.uniquindio.bibliomtr.algorithms.similarity;

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
 * Análisis estático que verifica que el código de producción no calcula similitud con librerías ni con la base
 * de datos (spec 001, REQ-4 y AC-2; constitución P1 y P3; reglas de PostgreSQL prohibido de AGENTS.md).
 *
 * <p><b>Alcance.</b>
 * <ul>
 *   <li>Los {@code *.java} de {@code src/main/java}, sin comentarios ({@code //…} y {@code /* … *}{@code /}),
 *       para que el Javadoc pueda nombrar lo prohibido.</li>
 *   <li>Los archivos de texto de {@code src/main/resources} que pueden llevar SQL o configuración
 *       ({@code *.sql}, {@code *.properties}, {@code *.yml}, {@code *.yaml}, {@code *.xml}), sin comentarios
 *       ({@code --} y {@code /* … *}{@code /} en SQL; líneas {@code #} o {@code !} en el resto). Se excluye
 *       {@code models/}, que solo contiene artefactos del modelo (pesos ONNX y vocabulario del tokenizer).</li>
 *   <li>{@code build.gradle.kts}, sin comentarios.</li>
 * </ul>
 * {@code src/test/**}{@code /oracle/} queda fuera: es el único lugar donde estas funciones están permitidas.
 *
 * <p><b>Criterio.</b> Falla ante cualquiera de estos patrones:
 * <ul>
 *   <li>Java (código sin cadenas): imports o nombres calificados de {@code org.apache.commons.text},
 *       {@code org.apache.lucene}, {@code smile.}, {@code org.apache.commons.math3} e {@code info.debatty}.</li>
 *   <li>Cadenas Java (incluidos los text blocks, que es donde viven {@code @Query} y el SQL nativo) y SQL de
 *       los recursos, sin distinguir mayúsculas: {@code levenshtein(}, {@code similarity(}, {@code strpos(},
 *       {@code position(}, {@code like}, {@code ilike}, {@code tsvector}, {@code tsquery}, {@code pg_trgm},
 *       {@code fuzzystrmatch}, {@code <=>}, {@code <->} y {@code <#>}. {@code like} se busca como palabra
 *       completa, para no marcar identificadores que la contengan.</li>
 *   <li>Java (código sin cadenas): métodos derivados de Spring Data ({@code find…By…}, {@code count…By…},
 *       etc.) que terminen en {@code Like} o {@code Containing}. También se marcan {@code StartingWith} y
 *       {@code EndingWith}, con o sin {@code IgnoreCase}, porque Spring Data los traduce igualmente a
 *       {@code LIKE}.</li>
 *   <li>{@code build.gradle.kts}: las coordenadas {@code commons-text}, {@code lucene}, {@code smile} y
 *       {@code commons-math}.</li>
 * </ul>
 *
 * <p>Complejidad: O(L) en tiempo, con L = número total de caracteres recorridos, y O(l) en espacio, con
 * l = tamaño del archivo más grande.
 */
class ForbiddenSimilarityUsageTest {

	private static final Path JAVA_SOURCES = Path.of("src", "main", "java");
	private static final Path RESOURCES = Path.of("src", "main", "resources");
	private static final Path MODELS = RESOURCES.resolve("models");
	private static final Path BUILD_FILE = Path.of("build.gradle.kts");

	private static final List<String> RESOURCE_EXTENSIONS = List.of(".sql", ".properties", ".yml", ".yaml", ".xml");

	/** Librerías de similitud, clustering o estadística prohibidas en el código Java (AGENTS.md, Stack). */
	private static final List<Pattern> FORBIDDEN_LIBRARIES = List.of(
			Pattern.compile("\\borg\\.apache\\.commons\\.text\\."),
			Pattern.compile("\\borg\\.apache\\.lucene\\."),
			Pattern.compile("\\bimport\\s+(?:static\\s+)?smile\\."),
			Pattern.compile("\\borg\\.apache\\.commons\\.math3\\."),
			Pattern.compile("\\binfo\\.debatty\\."));

	/** Funciones, operadores y extensiones de PostgreSQL prohibidos en SQL y en cadenas (AGENTS.md). */
	private static final List<Pattern> FORBIDDEN_SQL = List.of(
			Pattern.compile("(?i)levenshtein\\w*\\s*\\("),
			Pattern.compile("(?i)similarity\\s*\\("),
			Pattern.compile("(?i)\\bstrpos\\s*\\("),
			Pattern.compile("(?i)\\bposition\\s*\\("),
			Pattern.compile("(?i)\\blike\\b"),
			Pattern.compile("(?i)\\bilike\\b"),
			Pattern.compile("(?i)tsvector"),
			Pattern.compile("(?i)tsquery"),
			Pattern.compile("(?i)pg_trgm"),
			Pattern.compile("(?i)fuzzystrmatch"),
			Pattern.compile("<=>"),
			Pattern.compile("<->"),
			Pattern.compile("<#>"));

	/** Métodos derivados de Spring Data que generan {@code LIKE}. */
	private static final Pattern FORBIDDEN_DERIVED_METHOD = Pattern.compile(
			"\\b(?:find|read|get|query|search|stream|count|exists|delete|remove)\\w*By\\w*?"
					+ "(?:Like|Containing|StartingWith|EndingWith)(?:IgnoreCase)?\\s*\\(");

	/** Coordenadas de dependencias prohibidas en el build. */
	private static final List<Pattern> FORBIDDEN_DEPENDENCIES = List.of(
			Pattern.compile("commons-text"),
			Pattern.compile("lucene"),
			Pattern.compile("smile"),
			Pattern.compile("commons-math"));

	@Test
	@DisplayName("AC-2: el código Java de producción no usa librerías, SQL ni métodos derivados prohibidos")
	void javaProductionCodeHasNoForbiddenSimilarity() {
		assertThat(JAVA_SOURCES).as("directorio de fuentes Java").isDirectory();
		List<String> violations = new ArrayList<>();
		for (Path file : files(JAVA_SOURCES, List.of(".java"))) {
			violations.addAll(javaViolations(file.toString(), read(file)));
		}
		assertThat(violations).isEmpty();
	}

	@Test
	@DisplayName("AC-2: los recursos de producción (SQL y configuración) no usan funciones ni operadores prohibidos")
	void resourcesHaveNoForbiddenSql() {
		assertThat(RESOURCES).as("directorio de recursos").isDirectory();
		List<String> violations = new ArrayList<>();
		for (Path file : files(RESOURCES, RESOURCE_EXTENSIONS)) {
			if (file.startsWith(MODELS)) {
				continue;
			}
			String content = file.toString().endsWith(".sql")
					? stripSqlComments(read(file))
					: stripHashComments(read(file));
			violations.addAll(matches(file.toString(), content, FORBIDDEN_SQL));
		}
		assertThat(violations).isEmpty();
	}

	@Test
	@DisplayName("AC-2: build.gradle.kts no declara commons-text, lucene, smile ni commons-math")
	void buildHasNoForbiddenDependencies() {
		String build = lexJava(read(BUILD_FILE)).withoutComments();
		assertThat(matches(BUILD_FILE.toString(), build, FORBIDDEN_DEPENDENCIES)).isEmpty();
	}

	@Test
	@DisplayName("El detector marca los patrones prohibidos y no marca comentarios ni accesores legítimos")
	void detectorRecognizesPatterns() {
		// Librerías.
		assertThat(javaViolations("A", "import org.apache.commons.text.similarity.LevenshteinDistance;")).isNotEmpty();
		assertThat(javaViolations("A", "import org.apache.lucene.search.Query;")).hasSize(1);
		assertThat(javaViolations("A", "import smile.clustering.HierarchicalClustering;")).hasSize(1);
		assertThat(javaViolations("A", "var x = new org.apache.commons.math3.ml.distance.EuclideanDistance();"))
				.hasSize(1);
		assertThat(javaViolations("A", "import info.debatty.java.stringsimilarity.Jaccard;")).hasSize(1);
		// SQL en cadenas y text blocks.
		assertThat(javaViolations("A", "@Query(\"select levenshtein(a, b) from t\")")).hasSize(1);
		assertThat(javaViolations("A", "String q = \"\"\"\n  SELECT * FROM t WHERE title LIKE ?\n  \"\"\";")).hasSize(1);
		assertThat(javaViolations("A", "String q = \"select strpos(a, 'x'), POSITION('x' in a)\";")).hasSize(2);
		assertThat(javaViolations("A", "String q = \"where a ilike b\";")).hasSize(1);
		assertThat(javaViolations("A", "String q = \"to_tsvector(a) @@ to_tsquery(b)\";")).hasSize(2);
		assertThat(javaViolations("A", "String q = \"create extension pg_trgm; select similarity(a, b)\";")).hasSize(2);
		assertThat(javaViolations("A", "String q = \"create extension fuzzystrmatch\";")).hasSize(1);
		assertThat(javaViolations("A", "String q = \"a <=> b, a <-> b, a <#> b\";")).hasSize(3);
		// Métodos derivados.
		assertThat(javaViolations("A", "List<Article> findByTitleContaining(String t);")).hasSize(1);
		assertThat(javaViolations("A", "List<Article> findAllByTitleLike(String t);")).hasSize(1);
		assertThat(javaViolations("A", "long countByTitleStartingWithIgnoreCase(String t);")).hasSize(1);
		// Lo permitido.
		assertThat(javaViolations("A", "/** levenshtein( LIKE <=> */ int x; // a <-> b")).isEmpty();
		assertThat(javaViolations("A", "double s = result.similarity(); int p = buffer.position();")).isEmpty();
		assertThat(javaViolations("A", "List<Article> findAllByAbstractDetectedTrueOrderByFileNameAsc();")).isEmpty();
		assertThat(javaViolations("A", "String m = \"TODO estudiante: implementar Levenshtein.compare (ver …)\";"))
				.isEmpty();
		assertThat(javaViolations("A", "char c = '\"'; String s = \"likelihood\";")).isEmpty();
		assertThat(matches("B", stripSqlComments("-- where a like b\n/* levenshtein( */ select 1"), FORBIDDEN_SQL))
				.isEmpty();
		assertThat(matches("B", stripSqlComments("select * from t where a LIKE b"), FORBIDDEN_SQL)).hasSize(1);
		assertThat(matches("C", stripHashComments("# like\nspring.datasource.url=x"), FORBIDDEN_SQL)).isEmpty();
		assertThat(matches("D", lexJava("// lucene, smile\nimplementation(\"org.apache.pdfbox:pdfbox:3\")")
				.withoutComments(), FORBIDDEN_DEPENDENCIES)).isEmpty();
		assertThat(matches("D", lexJava("implementation(\"org.apache.commons:commons-text:1.12.0\")")
				.withoutComments(), FORBIDDEN_DEPENDENCIES)).hasSize(1);
	}

	/** Violaciones de un archivo Java: librerías y métodos en el código, SQL en las cadenas. */
	private static List<String> javaViolations(String file, String source) {
		Lexed lexed = lexJava(source);
		List<String> violations = new ArrayList<>(matches(file, lexed.code(), FORBIDDEN_LIBRARIES));
		violations.addAll(matches(file, lexed.code(), List.of(FORBIDDEN_DERIVED_METHOD)));
		for (String literal : lexed.strings()) {
			violations.addAll(matches(file, literal, FORBIDDEN_SQL));
		}
		return violations;
	}

	/** Una entrada {@code archivo: coincidencia} por cada aparición de cada patrón. */
	private static List<String> matches(String file, String text, List<Pattern> patterns) {
		List<String> found = new ArrayList<>();
		for (Pattern pattern : patterns) {
			Matcher matcher = pattern.matcher(text);
			while (matcher.find()) {
				found.add(file + ": " + matcher.group());
			}
		}
		return found;
	}

	/**
	 * Resultado de separar un archivo Java o Kotlin.
	 *
	 * @param code            código sin comentarios, con cada cadena reemplazada por {@code ""} y cada
	 *                        carácter por {@code ' '}
	 * @param withoutComments código sin comentarios, con las cadenas intactas
	 * @param strings         contenido de cada cadena, incluidos los text blocks
	 */
	private record Lexed(String code, String withoutComments, List<String> strings) {
	}

	/**
	 * Separa código, comentarios y cadenas de un archivo Java o Kotlin: los comentarios se descartan, las
	 * cadenas se devuelven aparte y en el código quedan como {@code ""}.
	 *
	 * <p>Complejidad: O(l) en tiempo y espacio, con l = longitud del código.
	 */
	private static Lexed lexJava(String source) {
		StringBuilder code = new StringBuilder(source.length());
		StringBuilder withoutComments = new StringBuilder(source.length());
		List<String> strings = new ArrayList<>();
		int i = 0;
		while (i < source.length()) {
			int tokenStart = i;
			if (source.startsWith("//", i)) {
				// Comentario de línea: se salta hasta el fin de línea.
				while (i < source.length() && source.charAt(i) != '\n') {
					i++;
				}
				code.append(' ');
				withoutComments.append(' ');
				continue;
			} else if (source.startsWith("/*", i)) {
				// Comentario de bloque (incluye Javadoc): se salta hasta el cierre.
				int end = source.indexOf("*/", i + 2);
				i = end < 0 ? source.length() : end + 2;
				code.append(' ');
				withoutComments.append(' ');
				continue;
			} else if (source.startsWith("\"\"\"", i)) {
				// Text block: hasta el siguiente """ no escapado.
				int start = i + 3;
				int j = start;
				while (j < source.length() && !(source.startsWith("\"\"\"", j) && source.charAt(j - 1) != '\\')) {
					j++;
				}
				strings.add(source.substring(start, Math.min(j, source.length())));
				code.append("\"\"");
				i = Math.min(j + 3, source.length());
			} else if (source.charAt(i) == '"' || source.charAt(i) == '\'') {
				// Cadena o carácter: hasta la comilla de cierre, respetando los escapes.
				char quote = source.charAt(i);
				StringBuilder literal = new StringBuilder();
				i++;
				while (i < source.length() && source.charAt(i) != quote && source.charAt(i) != '\n') {
					if (source.charAt(i) == '\\' && i + 1 < source.length()) {
						literal.append(source.charAt(i));
						i++;
					}
					literal.append(source.charAt(i));
					i++;
				}
				i++;
				if (quote == '"') {
					strings.add(literal.toString());
					code.append("\"\"");
				} else {
					code.append("' '");
				}
			} else {
				code.append(source.charAt(i));
				i++;
			}
			// Todo lo que no es comentario se conserva tal cual.
			withoutComments.append(source, tokenStart, Math.min(i, source.length()));
		}
		return new Lexed(code.toString(), withoutComments.toString(), strings);
	}

	/**
	 * Quita los comentarios SQL de línea ({@code --}) y de bloque, sin tocar las cadenas {@code '…'}.
	 *
	 * <p>Complejidad: O(l) en tiempo y espacio, con l = longitud del script.
	 */
	private static String stripSqlComments(String sql) {
		StringBuilder out = new StringBuilder(sql.length());
		int i = 0;
		while (i < sql.length()) {
			if (sql.startsWith("--", i)) {
				while (i < sql.length() && sql.charAt(i) != '\n') {
					i++;
				}
				out.append(' ');
			} else if (sql.startsWith("/*", i)) {
				int end = sql.indexOf("*/", i + 2);
				i = end < 0 ? sql.length() : end + 2;
				out.append(' ');
			} else if (sql.charAt(i) == '\'') {
				// Cadena SQL: se copia completa; '' es una comilla escapada y no la cierra.
				int end = i + 1;
				while (end < sql.length() && !(sql.charAt(end) == '\'' && !sql.startsWith("''", end))) {
					end += sql.startsWith("''", end) ? 2 : 1;
				}
				end = Math.min(end + 1, sql.length());
				out.append(sql, i, end);
				i = end;
			} else {
				out.append(sql.charAt(i));
				i++;
			}
		}
		return out.toString();
	}

	/**
	 * Quita las líneas de comentario de {@code .properties} y YAML ({@code #} o {@code !} al inicio, tras
	 * espacios).
	 *
	 * <p>Complejidad: O(l) en tiempo y espacio, con l = longitud del archivo.
	 */
	private static String stripHashComments(String content) {
		StringBuilder out = new StringBuilder(content.length());
		for (String line : content.split("\n", -1)) {
			String trimmed = line.strip();
			if (!trimmed.startsWith("#") && !trimmed.startsWith("!")) {
				out.append(line);
			}
			out.append('\n');
		}
		return out.toString();
	}

	/** Archivos regulares bajo {@code root} con alguna de las extensiones. */
	private static List<Path> files(Path root, List<String> extensions) {
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
