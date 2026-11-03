package co.edu.uniquindio.bibliomtr.docs;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import co.edu.uniquindio.bibliomtr.algorithms.stringmatching.GuideFragment;

/**
 * Verifica la guía de implementación y la trazabilidad del Requerimiento 4 (spec 004, REQ-31, REQ-32, REQ-33,
 * AC-22, AC-23; constitución P4 y P9).
 *
 * <p><b>Cómo.</b> Lee {@code docs/guia-implementacion.md} y {@code docs/traceability.md} desde la raíz del
 * proyecto. La sección de cada algoritmo es la que empieza con un encabezado {@code ### } que contiene el nombre
 * de la clase entre comillas invertidas y termina en el siguiente encabezado {@code ##} o {@code ###}; la del
 * ejemplo es la que contiene "Ejemplo reproducible"; la sección 11 completa termina en el siguiente encabezado
 * {@code ## }.
 *
 * <p><b>Criterio.</b>
 * <ul>
 *   <li>AC-23: la sección de cada stub trae su ruta, {@code preprocess(}, {@code search(}, "Definición formal",
 *       "Traza", "Caso a mano", el comando {@code ./gradlew studentTest --tests <Clase>Test} y los valores de su
 *       caso a mano (los mismos de los tests {@code student}).</li>
 *   <li>REQ-32: el ejemplo trae el PDF, el fragmento literal, el patrón, las posiciones y las tablas de los 3
 *       algoritmos, tomados de {@link GuideFragment} para que la guía y los tests no se separen.</li>
 *   <li>REQ-31, AC-22: la sección 11 existe, documenta el comando JMH y el archivo de resultados, y declara el uso
 *       de IA generativa en el andamiaje del Requerimiento 4.</li>
 *   <li>REQ-33: {@code docs/traceability.md} tiene una fila del Requerimiento 4 por módulo y por la vista
 *       {@code pattern-search}, con la sección del documento técnico {@code (pendiente)}.</li>
 * </ul>
 *
 * <p><b>Nota.</b> Gradle no registra estos archivos como entradas de {@code test}: tras editar solo la guía o la
 * trazabilidad, ejecutar {@code ./gradlew test --rerun}.
 *
 * <p>Complejidad: O(L) en tiempo y espacio por caso, con L = longitud de los dos documentos.
 */
class PatternSearchGuideTest {

	private static final Path GUIDE = Path.of("docs", "guia-implementacion.md");
	private static final Path TRACEABILITY = Path.of("docs", "traceability.md");
	private static final String SOURCES = "src/main/java/co/edu/uniquindio/bibliomtr/algorithms/stringmatching/";
	private static final String SECTION_HEADING = "## 11. Requerimiento 4: Coincidencia exacta de cadenas";
	private static final String REQUIREMENT = "Requerimiento 4";
	private static final String JMH_COMMAND = "./gradlew jmhJar && mkdir -p build/jmh-results && java -jar "
			+ "build/libs/benchmarks.jar '(StringMatchingBenchmark|RabinKarpBenchmark)' -rf json "
			+ "-rff build/jmh-results/string-matching.json";

	/**
	 * Por cada algoritmo, la clase del stub y los valores de su caso a mano que deben aparecer en su sección,
	 * tomados de los tests {@code student}.
	 */
	static List<Arguments> handCases() {
		return List.of(
				Arguments.of("KnuthMorrisPratt", List.of("π = [0, 0, 1, 2]",
						"(0,0,0) (1,1,0) (2,2,0) (3,3,0) (4,2,2) (5,3,2) (6,2,4) (6,0,6) (7,0,7)",
						"[T, T, T, T, T, T, F, F, T]", "Posiciones [0, 2]")),
				Arguments.of("BoyerMoore", List.of("{a: 2, b: 3}", "f = [2, 3, 4, 4, 5]", "[2, 2, 4, 1]",
						"4 − 2 = 2", "3 − 2 = 1", "Posiciones [0, 2]")),
				Arguments.of("RabinKarp", List.of("d mod q = 88", "h = 25", "hash(P) = 48", "[48, 92, 48, 94, 21]",
						"\"abba\"", "h = 1, hash(P) = 0, ventanas [0, 1, 0]", "Posiciones [0], colisiones [2]")));
	}

	@ParameterizedTest(name = "{0}")
	@ValueSource(strings = {"KnuthMorrisPratt", "BoyerMoore", "RabinKarp"})
	@DisplayName("AC-23: cada stub tiene archivo, preprocess, search, Definición formal, Traza, Caso a mano y comando")
	void algorithmSectionHasRequiredContent(String className) throws IOException {
		assertThat(algorithmSection(className)).contains(
				SOURCES + className + ".java",
				"preprocess(",
				"search(",
				"Definición formal",
				"Traza",
				"Caso a mano",
				"./gradlew studentTest --tests " + className + "Test");
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("handCases")
	@DisplayName("AC-23: la sección de cada stub documenta los valores de su caso a mano")
	void algorithmSectionDocumentsHandCase(String className, List<String> expectedValues) throws IOException {
		assertThat(algorithmSection(className)).contains(expectedValues);
	}

	@Test
	@DisplayName("REQ-32: el ejemplo trae el PDF, el fragmento, el patrón, las posiciones y las tablas de GuideFragment")
	void exampleMatchesGuideFragment() throws IOException {
		String example = section(Files.readAllLines(GUIDE), "### ", "Ejemplo reproducible", false);
		int m = GuideFragment.PATTERN.length();

		assertThat(example).as("sección del ejemplo en %s", GUIDE).isNotEmpty();
		assertThat(example).contains(
				GuideFragment.PDF_FILE,
				GuideFragment.FRAGMENT,
				GuideFragment.FRAGMENT_LENGTH + " `char`",
				"P = \"" + GuideFragment.PATTERN + "\" (m = " + m + ",",
				"Posiciones " + GuideFragment.POSITIONS,
				GuideFragment.WINDOW_COUNT + " ventanas",
				"π = " + Arrays.toString(GuideFragment.pi()),
				formatMap(GuideFragment.LAST_OCCURRENCE),
				"f = " + Arrays.toString(GuideFragment.borderPositions()),
				Arrays.toString(GuideFragment.goodSuffixShift()),
				"borde(P) = " + GuideFragment.PATTERN_BORDER,
				"m − borde(P) = " + m + " − " + GuideFragment.PATTERN_BORDER + " = " + GuideFragment.MATCH_SHIFT,
				"q = " + GuideFragment.Q,
				"d mod q = 65 536 − 648·101 = " + GuideFragment.D_MOD_Q,
				"→ h = " + GuideFragment.H,
				"→ hash(P) = " + GuideFragment.PATTERN_HASH);
		for (List<Integer> candidates : GuideFragment.PI_CANDIDATES) {
			assertThat(example).contains("| " + candidates + " |");
		}
	}

	@Test
	@DisplayName("REQ-31, AC-22: la sección 11 existe, documenta JMH y declara el uso de IA generativa")
	void guideHasSectionJmhAndAiDeclaration() throws IOException {
		List<String> lines = Files.readAllLines(GUIDE);
		assertThat(lines).contains(SECTION_HEADING);

		String requirement = section(lines, "## ", SECTION_HEADING, true);
		assertThat(requirement).contains(
				JMH_COMMAND,
				"build/jmh-results/string-matching.json",
				"./gradlew scaffoldTest --tests StringMatchingStubScaffoldTest",
				"./gradlew studentTest --tests KnuthMorrisPrattTest --tests BoyerMooreTest --tests RabinKarpTest "
						+ "--tests StringMatchingOracleTest",
				"Complejidad del preprocesamiento: TODO estudiante",
				"Complejidad de la búsqueda: TODO estudiante");

		String ai = section(lines, "### ", "Uso de IA generativa", false);
		assertThat(ai).as("declaración de IA de la sección 11").contains(
				"IA generativa", REQUIREMENT, "docs/specs/004-busqueda-patrones/");
	}

	@ParameterizedTest(name = "{1}")
	@MethodSource("traceabilityModules")
	@DisplayName("REQ-33: docs/traceability.md tiene una fila del Requerimiento 4 por módulo, con la sección pendiente")
	void traceabilityHasRowPerModule(int column, String module) throws IOException {
		List<String[]> rows = new ArrayList<>();
		for (String line : Files.readAllLines(TRACEABILITY)) {
			String[] cells = line.split("\\|");
			// Celdas de una fila de la tabla: [vacía, requerimiento, spec, módulo, vista, sección, tests].
			if (line.startsWith("|") && cells.length >= 7 && cells[1].contains(REQUIREMENT)) {
				rows.add(cells);
			}
		}
		assertThat(rows).as("filas del Requerimiento 4 en %s", TRACEABILITY)
				.anyMatch(cells -> cells[column].contains(module) && cells[5].contains("(pendiente)"));
	}

	/** Columna de la tabla de trazabilidad (3 = módulo, 4 = vista) y el módulo que debe tener una fila. */
	static List<Arguments> traceabilityModules() {
		return List.of(
				Arguments.of(3, "`algorithms.stringmatching`"),
				Arguments.of(3, "`trace.stringmatching`"),
				Arguments.of(3, "`api.stringmatching`"),
				Arguments.of(3, "`evaluation`"),
				Arguments.of(4, "`pattern-search`"));
	}

	/**
	 * Sección de la guía del algoritmo {@code className}: desde su encabezado {@code ###} hasta el siguiente
	 * encabezado de nivel 2 o 3.
	 *
	 * <p>Complejidad: O(L) en tiempo y espacio, con L = longitud de la guía.
	 */
	private static String algorithmSection(String className) throws IOException {
		String section = section(Files.readAllLines(GUIDE), "### ", "`" + className + "`", false);
		assertThat(section).as("sección de %s en %s", className, GUIDE).isNotEmpty();
		return section;
	}

	/**
	 * Texto desde el primer encabezado con el prefijo {@code level} que contiene {@code marker} hasta el siguiente
	 * encabezado de nivel 2 o, si {@code onlyLevel2} es falso, también de nivel 3.
	 *
	 * <p>Complejidad: O(L) en tiempo y espacio.
	 */
	private static String section(List<String> lines, String level, String marker, boolean onlyLevel2) {
		StringBuilder section = new StringBuilder();
		boolean inside = false;
		for (String line : lines) {
			boolean heading = line.startsWith("## ") || (!onlyLevel2 && line.startsWith("### "));
			if (heading && inside) {
				break;
			}
			if (line.startsWith(level) && line.contains(marker)) {
				inside = true;
			}
			if (inside) {
				section.append(line).append('\n');
			}
		}
		return section.toString();
	}

	/**
	 * Formatea la tabla del carácter malo como en la guía: {@code {s: 6, y: 1}}.
	 *
	 * <p>Complejidad: O(k) en tiempo y espacio, con k = número de entradas.
	 */
	private static String formatMap(Map<Character, Integer> map) {
		StringJoiner joiner = new StringJoiner(", ", "{", "}");
		for (Map.Entry<Character, Integer> entry : map.entrySet()) {
			joiner.add(entry.getKey() + ": " + entry.getValue());
		}
		return joiner.toString();
	}

}
