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
 * Verifica la guía de implementación y la trazabilidad del Requerimiento 3 (spec 003, REQ-20, REQ-21, AC-18;
 * constitución P4 y P9).
 *
 * <p><b>Cómo.</b> Lee {@code docs/guia-implementacion.md} y {@code docs/traceability.md} desde la raíz del
 * proyecto. La sección de cada algoritmo es la que empieza con un encabezado {@code ### } que contiene el nombre
 * de la clase entre comillas invertidas y termina en el siguiente encabezado {@code ##} o {@code ###}.
 *
 * <p><b>Criterio.</b>
 * <ul>
 *   <li>AC-18: la sección de cada uno de los 4 algoritmos trae la ruta del archivo, el método {@code cluster},
 *       la "Fórmula", la "Traza", el "Desempate", el "Caso a mano" con las fusiones de D₄ y el comando
 *       {@code ./gradlew studentTest --tests <Clase>Test}.</li>
 *   <li>REQ-20: la guía tiene la sección "## 10. Requerimiento 3: Clustering jerárquico" y su declaración de
 *       uso de IA generativa nombra el Requerimiento 3.</li>
 *   <li>REQ-21: {@code docs/traceability.md} tiene una fila del Requerimiento 3 por módulo, con la sección del
 *       documento técnico {@code (pendiente)}.</li>
 * </ul>
 *
 * <p><b>Nota.</b> Gradle no registra estos archivos como entradas de {@code test}: tras editar solo la guía o la
 * trazabilidad, ejecutar {@code ./gradlew test --rerun}.
 *
 * <p>Complejidad: O(L) en tiempo y espacio por caso, con L = longitud de los dos documentos.
 */
class ClusteringGuideTest {

	private static final Path GUIDE = Path.of("docs", "guia-implementacion.md");
	private static final Path TRACEABILITY = Path.of("docs", "traceability.md");
	private static final String SOURCES = "src/main/java/co/edu/uniquindio/bibliomtr/algorithms/clustering/";
	private static final String REQUIREMENT = "Requerimiento 3";

	/**
	 * Por cada algoritmo, la clase del stub y los valores de su caso a mano que deben aparecer en su sección,
	 * tomados de los tests {@code student}.
	 */
	static List<Arguments> handCases() {
		return List.of(
				Arguments.of("SingleLinkage",
						List.of("(0, 1, 0.2, 2), (2, 4, 0.3, 3), (3, 5, 0.45, 4)", "[0.3, 0.6]", "(0, 1), (2, 3), (4, 5)")),
				Arguments.of("CompleteLinkage",
						List.of("(0, 1, 0.2, 2), (2, 3, 0.45, 2), (4, 5, 0.7, 4)", "[0.5, 0.7]", "(0, 1), (2, 3), (4, 5)")),
				Arguments.of("AverageLinkage",
						List.of("(0, 1, 0.2, 2), (2, 4, 0.4, 3), (3, 5, 0.583333, 4)", "[0.4, 0.65]", "1.75/3")),
				Arguments.of("Diana",
						List.of("(0, 1, 0.2, 2), (2, 4, 0.5, 3), (3, 5, 0.7, 4)", "[−0.35, −0.35, −0.05]", "S = {3}",
								"(2, 3), (1, 4), (0, 5)")));
	}

	@ParameterizedTest(name = "{0}")
	@ValueSource(strings = {"SingleLinkage", "CompleteLinkage", "AverageLinkage", "Diana"})
	@DisplayName("AC-18: cada algoritmo tiene archivo, cluster, Fórmula, Traza, Desempate, Caso a mano y comando")
	void algorithmSectionHasRequiredContent(String className) throws IOException {
		assertThat(algorithmSection(className)).contains(
				SOURCES + className + ".java",
				"cluster(double[][] distances)",
				"Fórmula",
				"Traza",
				"Desempate",
				"Caso a mano",
				"./gradlew studentTest --tests " + className + "Test");
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("handCases")
	@DisplayName("AC-18: la sección de cada algoritmo documenta las fusiones de su caso a mano D₄")
	void algorithmSectionDocumentsHandCase(String className, List<String> expectedValues) throws IOException {
		assertThat(algorithmSection(className)).contains(expectedValues);
	}

	@Test
	@DisplayName("REQ-20: la guía tiene la sección del Requerimiento 3 y declara el uso de IA en su andamiaje")
	void guideHasRequirementSectionAndAiDeclaration() throws IOException {
		List<String> lines = Files.readAllLines(GUIDE);
		assertThat(lines).contains("## 10. Requerimiento 3: Clustering jerárquico");
		String ai = section(lines, "## ", "Uso de IA generativa");
		assertThat(ai).contains(REQUIREMENT, "docs/specs/003-clustering-jerarquico/");
		assertThat(Files.readString(GUIDE)).contains("d(i, j) = 1 − s(i, j)", "./gradlew scaffoldTest --tests ClusteringStubScaffoldTest");
	}

	@ParameterizedTest(name = "{0}")
	@ValueSource(strings = {"`algorithms.clustering`", "`trace.clustering`", "`api.clustering`", "vistas web"})
	@DisplayName("REQ-21: docs/traceability.md tiene una fila del Requerimiento 3 por módulo, con la sección pendiente")
	void traceabilityHasRowPerModule(String module) throws IOException {
		List<String[]> rows = new ArrayList<>();
		for (String line : Files.readAllLines(TRACEABILITY)) {
			String[] cells = line.split("\\|");
			// Celdas de una fila de la tabla: [vacía, requerimiento, spec, módulo, vista, sección, tests].
			if (line.startsWith("|") && cells.length >= 7 && cells[1].contains(REQUIREMENT)) {
				rows.add(cells);
			}
		}
		assertThat(rows).as("filas del Requerimiento 3 en %s", TRACEABILITY)
				.anyMatch(cells -> cells[3].contains(module) && cells[5].contains("(pendiente)"));
	}

	/**
	 * Sección de la guía del algoritmo {@code className}: desde su encabezado {@code ###} hasta el siguiente
	 * encabezado de nivel 2 o 3.
	 *
	 * <p>Complejidad: O(L) en tiempo y espacio, con L = longitud de la guía.
	 */
	private static String algorithmSection(String className) throws IOException {
		String section = section(Files.readAllLines(GUIDE), "### ", "`" + className + "`");
		assertThat(section).as("sección de %s en %s", className, GUIDE).isNotEmpty();
		return section;
	}

	/**
	 * Texto desde el primer encabezado con el prefijo {@code level} que contiene {@code marker} hasta el siguiente
	 * encabezado de nivel 2 o 3.
	 *
	 * <p>Complejidad: O(L) en tiempo y espacio.
	 */
	private static String section(List<String> lines, String level, String marker) {
		StringBuilder section = new StringBuilder();
		boolean inside = false;
		for (String line : lines) {
			boolean heading = line.startsWith("## ") || line.startsWith("### ");
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

}
