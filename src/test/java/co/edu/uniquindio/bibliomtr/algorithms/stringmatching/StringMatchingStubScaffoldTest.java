package co.edu.uniquindio.bibliomtr.algorithms.stringmatching;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Comprueba que los 3 stubs de búsqueda de patrones siguen sin implementar (spec 004, REQ-1, REQ-7 y AC-1, AC-2):
 * {@code preprocess} y {@code search} de cada uno lanzan {@code UnsupportedOperationException} con un mensaje que
 * empieza por {@code "TODO estudiante:"}, el cuerpo de cada método no tiene más sentencia que ese {@code throw} y
 * el Javadoc de la clase deja pendientes las dos complejidades.
 *
 * <p><b>Por qué la etiqueta {@code scaffold}.</b> Describe el andamiaje recién entregado y deja de pasar en cuanto
 * un estudiante implementa un algoritmo; por eso no corre en {@code ./gradlew test} y se ejecuta con
 * {@code ./gradlew scaffoldTest}.
 *
 * <p>Complejidad: O(L) por stub en tiempo y espacio, con L = longitud del archivo.
 */
@Tag("scaffold")
class StringMatchingStubScaffoldTest {

	private static final String TODO_PREFIX = "TODO estudiante:";
	private static final String PREPROCESS_SIGNATURE = "preprocess(String pattern, MatchOptions options) {";
	private static final String SEARCH_DECLARATION = "public MatchResult<";

	@ParameterizedTest(name = "{0}")
	@ValueSource(strings = {"KnuthMorrisPratt", "BoyerMoore", "RabinKarp"})
	@DisplayName("AC-1: preprocess de cada stub lanza UnsupportedOperationException con \"TODO estudiante:\"")
	void preprocessIsPending(String className) {
		StringMatcher<?, ?> matcher = matcher(className);

		assertThatThrownBy(() -> matcher.preprocess("abab", MatchOptions.defaults()))
				.isInstanceOf(UnsupportedOperationException.class)
				.hasMessageStartingWith(TODO_PREFIX);
	}

	@ParameterizedTest(name = "{0}")
	@ValueSource(strings = {"KnuthMorrisPratt", "BoyerMoore", "RabinKarp"})
	@DisplayName("AC-1: search de cada stub lanza UnsupportedOperationException con \"TODO estudiante:\"")
	void searchIsPending(String className) {
		StringMatcher<?, ?> matcher = matcher(className);

		assertThatThrownBy(() -> matcher.search("abababca", null, MatchOptions.defaults()))
				.isInstanceOf(UnsupportedOperationException.class)
				.hasMessageStartingWith(TODO_PREFIX);
	}

	@ParameterizedTest(name = "{0}")
	@ValueSource(strings = {"KnuthMorrisPratt", "BoyerMoore", "RabinKarp"})
	@DisplayName("AC-2: el cuerpo de preprocess y el de search solo lanzan la excepción, sin lógica del algoritmo")
	void stubBodiesOnlyThrow(String className) throws IOException {
		String source = StringMatchingStubDocumentationTest.source(className);

		assertSingleThrow(className, "preprocess", source, source.indexOf(PREPROCESS_SIGNATURE));
		assertSingleThrow(className, "search", source, source.indexOf(SEARCH_DECLARATION));
	}

	@ParameterizedTest(name = "{0}")
	@ValueSource(strings = {"KnuthMorrisPratt", "BoyerMoore", "RabinKarp"})
	@DisplayName("AC-1: el Javadoc de cada stub deja pendientes las complejidades del preprocesamiento y la búsqueda")
	void stubJavadocLeavesComplexityPending(String className) throws IOException {
		assertThat(StringMatchingStubDocumentationTest.classJavadoc(className))
				.contains("Complejidad del preprocesamiento: TODO estudiante")
				.contains("Complejidad de la búsqueda: TODO estudiante");
	}

	/**
	 * El cuerpo del método que empieza en {@code declaration} (desde su primera llave hasta la llave de cierre con
	 * un tabulador) es un único {@code throw new UnsupportedOperationException(…);}.
	 *
	 * <p>Complejidad: O(L) en tiempo y espacio.
	 */
	private static void assertSingleThrow(String className, String method, String source, int declaration) {
		assertThat(declaration).as("declaración de %s.%s", className, method).isNotNegative();
		int open = source.indexOf('{', declaration);
		int close = source.indexOf("\n\t}", open);
		String body = source.substring(open + 1, close).strip();

		assertThat(body).as("cuerpo de %s.%s", className, method).startsWith("throw new UnsupportedOperationException(");
		assertThat(body.chars().filter(c -> c == ';').count()).as("sentencias de %s.%s", className, method)
				.isEqualTo(1);
	}

	/** Instancia del stub por nombre de clase. */
	private static StringMatcher<?, ?> matcher(String className) {
		return switch (className) {
			case "KnuthMorrisPratt" -> new KnuthMorrisPratt();
			case "BoyerMoore" -> new BoyerMoore();
			default -> new RabinKarp();
		};
	}

}
