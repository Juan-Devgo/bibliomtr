package co.edu.uniquindio.bibliomtr.algorithms.stringmatching;

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
 * Verifica que el Javadoc de los 3 stubs de búsqueda de patrones trae las secciones y la definición formal que
 * necesita el estudiante (spec 004, REQ-4, REQ-5, REQ-6 y AC-23; constitución P6 y P8).
 *
 * <p><b>Cómo.</b> Lee el código fuente de cada stub en {@code src/main/java} (el directorio de trabajo de los
 * tests es la raíz del proyecto) y toma el Javadoc de la clase: el último {@code /** … *}{@code /} antes de
 * {@code public final class}.
 *
 * <p><b>Criterio.</b> Los 3 deben tener "Qué calcula", "Definición formal", "Preprocesamiento", "Búsqueda", "Traza",
 * "Caso a mano", "Casos de borde", "Complejidad del preprocesamiento:" y "Complejidad de la búsqueda:", más las
 * fórmulas de su algoritmo. No exige "TODO estudiante", para seguir en verde cuando el estudiante complete la
 * complejidad; eso lo verifica {@link StringMatchingStubScaffoldTest} (etiqueta {@code scaffold}).
 *
 * <p>Complejidad: O(L) en tiempo y espacio por archivo, con L = longitud del archivo.
 */
class StringMatchingStubDocumentationTest {

	private static final Path SOURCES = Path.of("src", "main", "java", "co", "edu", "uniquindio", "bibliomtr",
			"algorithms", "stringmatching");

	private static final List<String> REQUIRED_SECTIONS = List.of(
			"Qué calcula", "Definición formal", "Preprocesamiento", "Búsqueda", "Traza", "Caso a mano",
			"Casos de borde", "Complejidad del preprocesamiento:", "Complejidad de la búsqueda:");

	@ParameterizedTest(name = "{0}")
	@ValueSource(strings = {"KnuthMorrisPratt", "BoyerMoore", "RabinKarp"})
	@DisplayName("AC-23: el Javadoc de cada stub tiene definición, fases, traza, caso a mano, bordes y complejidades")
	void stubJavadocHasRequiredSections(String className) throws IOException {
		assertThat(classJavadoc(className)).contains(REQUIRED_SECTIONS);
	}

	@ParameterizedTest(name = "{0}")
	@ValueSource(strings = {"KnuthMorrisPratt", "BoyerMoore", "RabinKarp"})
	@DisplayName("REQ-2, REQ-3: cada stub documenta el patrón vacío y las solapadas, mayúsculas y tildes")
	void stubJavadocHasEdgeCases(String className) throws IOException {
		assertThat(classJavadoc(className)).contains("IllegalArgumentException", "\"aaaa\"/\"aa\" → [0, 1, 2]",
				"\"Algoritmo algoritmo\"/\"algoritmo\" → [10]", "\"canción cancion\"/\"cancion\" → [8]",
				"\"a b c\"/\" \" → [1, 3]", "abstracts", "idénticos");
	}

	@Test
	@DisplayName("REQ-4: KMP documenta π, el prefijo propio más largo y que i nunca retrocede")
	void kmpJavadocHasPrefixFunction() throws IOException {
		assertThat(classJavadoc("KnuthMorrisPratt")).contains("π[i]", "prefijo propio más largo",
				"j ← π[j − 1]", "nunca retrocede", "s = i − j", "π = [0, 0, 1, 2]");
	}

	@Test
	@DisplayName("REQ-5: Boyer-Moore documenta carácter malo, sufijo bueno fuerte, derecha a izquierda y máximo")
	void boyerMooreJavadocHasBothRules() throws IOException {
		assertThat(classJavadoc("BoyerMoore")).contains("carácter malo", "sufijo bueno", "fuerte",
				"derecha a izquierda", "máx(salto_cm(j), salto_sb(j), 1)", "j − last(T[s + j])", "−1",
				"m − borde(P)", "last = {a: 2, b: 3}", "[2, 2, 4, 1]", "4 − 2 = 2");
	}

	@Test
	@DisplayName("REQ-6: Rabin-Karp documenta el hash polinomial, h, el rolling hash, d = 65 536 y q = 101")
	void rabinKarpJavadocHasRollingHash() throws IOException {
		assertThat(classJavadoc("RabinKarp")).contains("d = 65 536", "q = 101",
				"S[i]·d^(m−1−i)) mod q", "= d^(m−1) mod q","t(s+1) = (d·(t(s) − T[s]·h) + T[s+m]) mod q",
				"[0, q)", "carácter a carácter", "colisión", "h = 25", "h(P) = 48", "[48, 92, 48, 94, 21]");
	}

	/**
	 * Javadoc de la clase: el último bloque {@code /**} antes de la declaración {@code public final class}. Lo
	 * reutiliza {@link StringMatchingStubScaffoldTest}.
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
