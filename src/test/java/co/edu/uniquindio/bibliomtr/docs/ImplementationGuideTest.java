package co.edu.uniquindio.bibliomtr.docs;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Verifica la guía de implementación y la trazabilidad del Requerimiento 1 (spec 001, REQ-18, REQ-19, AC-7 y
 * AC-16; constitución P4 y P9).
 *
 * <p><b>Cómo.</b> Lee {@code docs/guia-implementacion.md} y {@code docs/traceability.md} desde la raíz del
 * proyecto (el directorio de trabajo de los tests). La sección de cada algoritmo en la guía es la que empieza
 * con un encabezado {@code ### } que contiene el nombre de la clase entre comillas invertidas y termina en el
 * siguiente encabezado {@code ##} o {@code ###}.
 *
 * <p><b>Criterio.</b>
 * <ul>
 *   <li>AC-16: la sección de cada uno de los 6 algoritmos trae la ruta del archivo, el método {@code compare},
 *       la "Fórmula", la "Traza" y el comando {@code ./gradlew studentTest --tests <Clase>Test}; la guía tiene la
 *       sección "Uso de IA generativa" (P4).</li>
 *   <li>AC-7: la sección de cada algoritmo documenta los valores de sus casos a mano, los mismos que comprueban
 *       sus tests {@code student}.</li>
 *   <li>REQ-19: {@code docs/traceability.md} tiene una fila del Requerimiento 1 por cada módulo.</li>
 * </ul>
 *
 * <p><b>Nota.</b> Gradle no registra estos archivos como entradas de {@code test}: tras editar solo la guía o la
 * trazabilidad, ejecutar {@code ./gradlew test --rerun}.
 *
 * <p>Complejidad: O(L) en tiempo y espacio por caso, con L = longitud de los dos documentos.
 */
class ImplementationGuideTest {

	private static final Path GUIDE = Path.of("docs", "guia-implementacion.md");
	private static final Path TRACEABILITY = Path.of("docs", "traceability.md");
	private static final String SOURCES = "src/main/java/co/edu/uniquindio/bibliomtr/algorithms/similarity/";
	private static final String REQUIREMENT = "Requerimiento 1";

	/**
	 * Por cada algoritmo, la clase del stub y valores de sus casos a mano que deben aparecer en su sección,
	 * tomados de los tests {@code student} (con el signo menos tipográfico {@code −} de la guía).
	 */
	static List<Arguments> handCases() {
		return List.of(
				Arguments.of("Levenshtein", List.of("g,a,t,o vs p,a,t,o", "0.75", "0.02", "| 49 |")),
				Arguments.of("NeedlemanWunsch",
						List.of("g,a,t,o vs p,a,t,o", "minScore = −max(m, n)", "| 2 | 4 | −4 | 0.75 |",
								"| −8 | −6 | −10 | 0.5 |")),
				Arguments.of("Jaccard", List.of("{a,b,c} vs {b,c,d}", "0.5", "0.1")),
				Arguments.of("TfIdfCosineSimilarity",
						List.of("0.244830", "0.549306", "0.202733", "0.041100", "0.585523", "0.286707")),
				Arguments.of("CosineSimilarity",
						List.of("0.707107", "0.853553", "Los vectores deben tener la misma dimensión mayor que 0")),
				Arguments.of("EuclideanDistance", List.of("(0,0) vs (3,4)", "| 5 |", "1/6", "[9, 16]", "25")));
	}

	@ParameterizedTest(name = "{0}")
	@ValueSource(strings = {"Levenshtein", "NeedlemanWunsch", "Jaccard", "TfIdfCosineSimilarity",
			"CosineSimilarity", "EuclideanDistance"})
	@DisplayName("AC-16: cada algoritmo tiene archivo, compare, Fórmula, Traza y comando studentTest")
	void algorithmSectionHasRequiredContent(String className) throws IOException {
		assertThat(algorithmSection(className)).contains(
				SOURCES + className + ".java",
				"compare(",
				"Fórmula",
				"Traza",
				"./gradlew studentTest --tests " + className + "Test");
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("handCases")
	@DisplayName("AC-7: la sección de cada algoritmo documenta los valores de sus casos a mano")
	void algorithmSectionDocumentsHandCases(String className, List<String> expectedValues) throws IOException {
		assertThat(algorithmSection(className)).contains(expectedValues);
	}

	@Test
	@DisplayName("REQ-18: la guía tiene la sección \"Uso de IA generativa\" y las decisiones pendientes")
	void guideDeclaresGenerativeAiAndPendingDecisions() throws IOException {
		List<String> headings = new ArrayList<>();
		for (String line : Files.readAllLines(GUIDE)) {
			if (line.startsWith("#")) {
				headings.add(line);
			}
		}
		assertThat(headings).anyMatch(heading -> heading.startsWith("## ") && heading.contains("Uso de IA generativa"));
		assertThat(headings).anyMatch(heading -> heading.startsWith("## ") && heading.contains("Decisiones pendientes"));
		assertThat(Files.readString(GUIDE)).contains(
				"bibliomtr.similarity.edit-granularity",
				"Complejidad: TODO estudiante",
				"./gradlew scaffoldTest",
				"java -jar build/libs/benchmarks.jar SimilarityBenchmark");
	}

	@ParameterizedTest(name = "{0}")
	@ValueSource(strings = {"`pdf`", "`preprocessing`", "`embeddings`", "`algorithms.similarity`",
			"`trace.similarity`", "`persistence.article`", "`api.corpus`", "`api.similarity`", "`evaluation`",
			"vistas web"})
	@DisplayName("REQ-19: docs/traceability.md tiene una fila del Requerimiento 1 por módulo")
	void traceabilityHasRowPerModule(String module) throws IOException {
		List<String> rows = new ArrayList<>();
		for (String line : Files.readAllLines(TRACEABILITY)) {
			String[] cells = line.split("\\|");
			// Celdas de una fila de la tabla: [vacía, requerimiento, spec, módulo, vista, sección, tests].
			if (line.startsWith("|") && cells.length >= 7 && cells[1].contains(REQUIREMENT)) {
				rows.add(cells[3].trim());
			}
		}
		assertThat(rows).as("módulos del Requerimiento 1 en %s", TRACEABILITY).anyMatch(cell -> cell.contains(module));
	}

	/**
	 * Sección de la guía del algoritmo {@code className}: desde su encabezado {@code ###} hasta el siguiente
	 * encabezado de nivel 2 o 3.
	 *
	 * <p>Complejidad: O(L) en tiempo y espacio, con L = longitud de la guía.
	 */
	private static String algorithmSection(String className) throws IOException {
		List<String> lines = Files.readAllLines(GUIDE);
		StringBuilder section = new StringBuilder();
		boolean inside = false;
		for (String line : lines) {
			boolean heading = line.startsWith("## ") || line.startsWith("### ");
			if (heading && inside) {
				break;
			}
			if (line.startsWith("### ") && line.contains("`" + className + "`")) {
				inside = true;
			}
			if (inside) {
				section.append(line).append('\n');
			}
		}
		assertThat(section).as("sección de %s en %s", className, GUIDE).isNotEmpty();
		return section.toString();
	}

}
