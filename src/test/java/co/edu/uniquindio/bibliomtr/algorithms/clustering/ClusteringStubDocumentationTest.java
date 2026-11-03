package co.edu.uniquindio.bibliomtr.algorithms.clustering;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Verifica que el Javadoc de los 4 stubs de clustering trae las secciones que necesita el estudiante (spec 003,
 * REQ-4, REQ-5 y AC-18; constitución P6 y P8).
 *
 * <p><b>Cómo.</b> Lee el código fuente de cada stub en {@code src/main/java} (el directorio de trabajo de los
 * tests es la raíz del proyecto) y toma el Javadoc de la clase: el último {@code /** … *}{@code /} antes de
 * {@code public final class}.
 *
 * <p><b>Criterio.</b> Los 4 deben tener "Qué calcula", "Fórmula", "Desempate", "Traza", "Rango", "Caso a mano",
 * "Casos de borde" y "Complejidad:". No exige "TODO estudiante", para seguir en verde cuando el estudiante
 * complete la complejidad; eso lo verifica {@link ClusteringStubScaffoldTest} (etiqueta {@code scaffold}).
 *
 * <p>Complejidad: O(L) en tiempo y espacio por archivo, con L = longitud del archivo.
 */
class ClusteringStubDocumentationTest {

	private static final Path SOURCES = Path.of("src", "main", "java", "co", "edu", "uniquindio", "bibliomtr",
			"algorithms", "clustering");

	private static final List<String> REQUIRED_SECTIONS = List.of(
			"Qué calcula", "Fórmula", "Desempate", "Traza", "Rango", "Caso a mano", "Casos de borde", "Complejidad:");

	@ParameterizedTest(name = "{0}")
	@ValueSource(strings = {"SingleLinkage", "CompleteLinkage", "AverageLinkage", "Diana"})
	@DisplayName("REQ-4, REQ-5: el Javadoc de cada stub tiene fórmula, desempate, traza, rango, bordes y complejidad")
	void stubJavadocHasRequiredSections(String className) throws IOException {
		assertThat(classJavadoc(className)).contains(REQUIRED_SECTIONS);
	}

	@ParameterizedTest(name = "{0}")
	@ValueSource(strings = {"SingleLinkage", "CompleteLinkage", "AverageLinkage"})
	@DisplayName("REQ-4: los aglomerativos documentan su recálculo de Lance-Williams")
	void linkageJavadocHasLanceWilliams(String className) throws IOException {
		assertThat(classJavadoc(className)).contains("Lance-Williams", "d(k, h)");
	}

	@Test
	@DisplayName("REQ-4: DIANA documenta el diámetro, la diferencia D(i) y el orden inverso de las fusiones")
	void dianaJavadocHasDivisionRule() throws IOException {
		assertThat(classJavadoc("Diana")).contains("diam(C)", "D(i)", "orden inverso");
	}

	/**
	 * Javadoc de la clase: el último bloque {@code /**} antes de la declaración {@code public final class}. Lo
	 * reutiliza {@link ClusteringStubScaffoldTest}.
	 *
	 * <p>Complejidad: O(L) en tiempo y espacio, con L = longitud del archivo.
	 */
	static String classJavadoc(String className) throws IOException {
		String source = source(className);
		int declaration = source.indexOf("public final class " + className);
		assertThat(declaration).as("declaración de %s", className).isNotNegative();
		int start = source.lastIndexOf("/**", declaration);
		assertThat(start).as("Javadoc de %s", className).isNotNegative();
		int end = source.indexOf("*/", start);
		assertThat(end).as("cierre del Javadoc de %s", className).isBetween(start, declaration);
		return source.substring(start, end);
	}

	/**
	 * Código fuente del stub.
	 *
	 * <p>Complejidad: O(L) en tiempo y espacio.
	 */
	static String source(String className) throws IOException {
		return Files.readString(SOURCES.resolve(className + ".java"));
	}

}
