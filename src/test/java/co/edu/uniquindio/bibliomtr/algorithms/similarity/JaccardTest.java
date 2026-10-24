package co.edu.uniquindio.bibliomtr.algorithms.similarity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import co.edu.uniquindio.bibliomtr.trace.similarity.JaccardTrace;

/**
 * Tests de estudiante de {@link Jaccard} (spec 001, REQ-5, AC-6 y AC-7; constitución P10).
 *
 * <p>Los valores esperados están calculados a mano con {@code J = |A ∩ B| / |A ∪ B|} sobre los conjuntos de
 * símbolos y coinciden con la tabla de {@code docs/guia-implementacion.md}. Las listas de la traza van en orden
 * de primera aparición. Fallan hasta que se implemente el algoritmo:
 * {@code ./gradlew studentTest --tests JaccardTest}.
 */
@Tag("student")
class JaccardTest {

	private static final double EPSILON = 1e-9;

	private final Jaccard jaccard = new Jaccard();

	@Test
	@DisplayName("AC-7: {a,b,c} vs {b,c,d} = 2/4 = 0.5, con intersección [b,c] y unión [a,b,c,d]")
	void handCase() {
		SimilarityResult<JaccardTrace> result = jaccard.compare(List.of("a", "b", "c"), List.of("b", "c", "d"));

		assertThat(result.value()).isCloseTo(0.5, within(EPSILON));
		assertThat(result.similarity()).isCloseTo(0.5, within(EPSILON));
		JaccardTrace trace = result.trace();
		assertThat(trace.setA()).containsExactly("a", "b", "c");
		assertThat(trace.setB()).containsExactly("b", "c", "d");
		assertThat(trace.intersection()).containsExactly("b", "c");
		assertThat(trace.union()).containsExactly("a", "b", "c", "d");
	}

	@Test
	@DisplayName("Los duplicados no cuentan: [a,a,b] vs [a,b,b] = 1")
	void duplicatesAreIgnored() {
		SimilarityResult<JaccardTrace> result = jaccard.compare(List.of("a", "a", "b"), List.of("a", "b", "b"));

		assertThat(result.similarity()).isEqualTo(1.0);
		assertThat(result.trace().setA()).containsExactly("a", "b");
		assertThat(result.trace().setB()).containsExactly("a", "b");
	}

	@Test
	@DisplayName("Borde: ambas vacías dan 1")
	void bothEmpty() {
		SimilarityResult<JaccardTrace> result = jaccard.compare(List.of(), List.of());

		assertThat(result.value()).isEqualTo(1.0);
		assertThat(result.similarity()).isEqualTo(1.0);
	}

	@Test
	@DisplayName("Borde: una vacía da 0")
	void oneEmpty() {
		assertThat(jaccard.compare(List.of(), List.of("a", "b")).similarity()).isEqualTo(0.0);
	}

	@Test
	@DisplayName("Borde: secuencias idénticas dan 1")
	void identical() {
		List<String> tokens = List.of("graph", "neural", "network");
		assertThat(jaccard.compare(tokens, tokens).similarity()).isEqualTo(1.0);
	}

	@Test
	@DisplayName("Borde: disjuntas dan 0 con intersección vacía")
	void disjoint() {
		SimilarityResult<JaccardTrace> result = jaccard.compare(List.of("a", "b", "c"), List.of("x", "y", "z"));

		assertThat(result.similarity()).isEqualTo(0.0);
		assertThat(result.trace().intersection()).isEmpty();
		assertThat(result.trace().union()).containsExactly("a", "b", "c", "x", "y", "z");
	}

	@Test
	@DisplayName("Borde: longitudes muy distintas [a] vs [a..j] = 1/10 = 0.1")
	void veryDifferentLengths() {
		List<String> b = List.of("a", "b", "c", "d", "e", "f", "g", "h", "i", "j");
		assertThat(jaccard.compare(List.of("a"), b).similarity()).isCloseTo(0.1, within(EPSILON));
	}

}
