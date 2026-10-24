package co.edu.uniquindio.bibliomtr.algorithms.similarity;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Verifica que el Javadoc de los 6 stubs trae las secciones que necesita el estudiante (spec 001, REQ-2,
 * REQ-3 y AC-3; constitución P6 y P8).
 *
 * <p><b>Cómo.</b> Lee el código fuente de cada stub en {@code src/main/java} (el directorio de trabajo de los
 * tests es la raíz del proyecto) y toma el Javadoc de la clase: el último {@code /** … *}{@code /} antes de
 * {@code public final class}.
 *
 * <p><b>Criterio.</b> Los 6 deben tener "Qué calcula", "Fórmula", "Normalización sugerida", "Traza",
 * "Rango", "Casos de borde" y "Complejidad:". Levenshtein y Needleman-Wunsch, además, "Granularidad" y la
 * propiedad {@code bibliomtr.similarity.edit-granularity}, que dice dónde se configura.
 *
 * <p><b>Por qué no exige "TODO estudiante".</b> Este test está en la suite estándar y debe seguir en verde
 * cuando el estudiante complete la complejidad. Que la complejidad siga pendiente en el andamiaje recién
 * entregado lo verifica {@link StubScaffoldTest} (etiqueta {@code scaffold}).
 *
 * <p>Complejidad: O(L) en tiempo y espacio por archivo, con L = longitud del archivo.
 */
class StubDocumentationTest {

	private static final Path SOURCES = Path.of("src", "main", "java", "co", "edu", "uniquindio", "bibliomtr",
			"algorithms", "similarity");

	private static final List<String> REQUIRED_SECTIONS = List.of(
			"Qué calcula", "Fórmula", "Normalización sugerida", "Traza", "Rango", "Casos de borde",
			"Complejidad:");

	private static final List<String> GRANULARITY_SECTIONS = List.of(
			"Granularidad", "bibliomtr.similarity.edit-granularity");

	@ParameterizedTest(name = "{0}")
	@ValueSource(strings = {"Levenshtein", "NeedlemanWunsch", "Jaccard", "TfIdfCosineSimilarity",
			"CosineSimilarity", "EuclideanDistance"})
	@DisplayName("REQ-2: el Javadoc de cada stub tiene fórmula, normalización, traza, rango, bordes y complejidad")
	void stubJavadocHasRequiredSections(String className) throws IOException {
		assertThat(classJavadoc(className)).contains(REQUIRED_SECTIONS);
	}

	@ParameterizedTest(name = "{0}")
	@ValueSource(strings = {"Levenshtein", "NeedlemanWunsch"})
	@DisplayName("AC-3: el Javadoc de las distancias de edición explica la granularidad y dónde se configura")
	void editDistanceJavadocExplainsGranularity(String className) throws IOException {
		assertThat(classJavadoc(className)).contains(GRANULARITY_SECTIONS);
	}

	/**
	 * Javadoc de la clase: el último bloque {@code /**} antes de la declaración {@code public final class}. Lo
	 * reutiliza {@link StubScaffoldTest}.
	 *
	 * <p>Complejidad: O(L) en tiempo y espacio, con L = longitud del archivo.
	 */
	static String classJavadoc(String className) throws IOException {
		String source = Files.readString(SOURCES.resolve(className + ".java"));
		int declaration = source.indexOf("public final class " + className);
		assertThat(declaration).as("declaración de %s", className).isNotNegative();
		int start = source.lastIndexOf("/**", declaration);
		assertThat(start).as("Javadoc de %s", className).isNotNegative();
		int end = source.indexOf("*/", start);
		assertThat(end).as("cierre del Javadoc de %s", className).isBetween(start, declaration);
		return source.substring(start, end);
	}

}
