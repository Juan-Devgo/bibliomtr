package co.edu.uniquindio.bibliomtr.algorithms.similarity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.util.List;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Comprueba que los 6 stubs del andamiaje siguen sin implementar (spec 001, REQ-1, REQ-4 y AC-1): cada uno
 * lanza {@code UnsupportedOperationException} con un mensaje que empieza por {@code "TODO estudiante:"} y su
 * Javadoc deja la complejidad pendiente con {@code "Complejidad: TODO estudiante"}.
 *
 * <p><b>Por qué la etiqueta {@code scaffold}.</b> Este test describe el estado del andamiaje recién entregado:
 * deja de pasar en cuanto un estudiante implementa un algoritmo. Por eso no corre en {@code ./gradlew test} y
 * se ejecuta aparte con {@code ./gradlew scaffoldTest}. Por la misma razón, la verificación de
 * "Complejidad: TODO estudiante" vive aquí y no en {@link StubDocumentationTest}, que está en la suite estándar
 * y solo exige la sección "Complejidad:".
 *
 * <p>Complejidad: O(1) por stub en tiempo y espacio.
 */
@Tag("scaffold")
class StubScaffoldTest {

	private static final String TODO_PREFIX = "TODO estudiante:";
	private static final String PENDING_COMPLEXITY = "Complejidad: TODO estudiante";

	private static void assertPending(ThrowingCallable call) {
		assertThatThrownBy(call)
				.isInstanceOf(UnsupportedOperationException.class)
				.hasMessageStartingWith(TODO_PREFIX);
	}

	@Test
	@DisplayName("AC-1: Levenshtein lanza UnsupportedOperationException con \"TODO estudiante:\"")
	void levenshteinIsPending() {
		assertPending(() -> new Levenshtein().compare(List.of("g", "a", "t", "o"), List.of("p", "a", "t", "o")));
	}

	@Test
	@DisplayName("AC-1: Needleman-Wunsch lanza UnsupportedOperationException con \"TODO estudiante:\"")
	void needlemanWunschIsPending() {
		assertPending(() -> new NeedlemanWunsch(NeedlemanWunschScoring.DEFAULT)
				.compare(List.of("g", "a", "t", "o"), List.of("p", "a", "t", "o")));
	}

	@Test
	@DisplayName("AC-1: Jaccard lanza UnsupportedOperationException con \"TODO estudiante:\"")
	void jaccardIsPending() {
		assertPending(() -> new Jaccard().compare(List.of("a", "b", "c"), List.of("b", "c", "d")));
	}

	@Test
	@DisplayName("AC-1: coseno TF-IDF lanza UnsupportedOperationException con \"TODO estudiante:\"")
	void tfIdfCosineIsPending() {
		List<List<String>> corpus = List.of(List.of("a", "b"), List.of("b", "c"), List.of("c", "d"));
		assertPending(() -> new TfIdfCosineSimilarity().compare(corpus.get(0), corpus.get(1), corpus));
	}

	@Test
	@DisplayName("AC-1: similitud coseno lanza UnsupportedOperationException con \"TODO estudiante:\"")
	void cosineIsPending() {
		assertPending(() -> new CosineSimilarity().compare(new float[] {1, 0}, new float[] {1, 1}));
	}

	@Test
	@DisplayName("AC-1: distancia euclidiana lanza UnsupportedOperationException con \"TODO estudiante:\"")
	void euclideanIsPending() {
		assertPending(() -> new EuclideanDistance().compare(new float[] {0, 0}, new float[] {3, 4}));
	}

	@ParameterizedTest(name = "{0}")
	@ValueSource(strings = {"Levenshtein", "NeedlemanWunsch", "Jaccard", "TfIdfCosineSimilarity",
			"CosineSimilarity", "EuclideanDistance"})
	@DisplayName("AC-1: el Javadoc de cada stub deja la complejidad como \"Complejidad: TODO estudiante\"")
	void stubJavadocLeavesComplexityPending(String className) throws IOException {
		assertThat(StubDocumentationTest.classJavadoc(className)).contains(PENDING_COMPLEXITY);
	}

}
