package co.edu.uniquindio.bibliomtr.algorithms.ranking;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests de {@link TermFrequencyCounter}: suma de ocurrencias en todos los abstracts y entrada en orden
 * alfabético descendente (REQ-1, REQ-2, AC-1).
 */
class TermFrequencyCounterTest {

	private final TermFrequencyCounter counter = new TermFrequencyCounter();

	@Test
	@DisplayName("AC-1: model×42, data×30, graph×30 dan la entrada [model 42, graph 30, data 30]")
	void buildsInputInDescendingAlphabeticalOrder() {
		// Las ocurrencias se reparten en dos abstracts para comprobar que se suman en todo el corpus.
		List<String> first = new ArrayList<>();
		first.addAll(Collections.nCopies(20, "model"));
		first.addAll(Collections.nCopies(30, "data"));
		first.addAll(Collections.nCopies(10, "graph"));
		List<String> second = new ArrayList<>();
		second.addAll(Collections.nCopies(20, "graph"));
		second.addAll(Collections.nCopies(22, "model"));

		List<TermFrequency> input = counter.count(List.of(first, second));

		assertThat(input).containsExactly(
				new TermFrequency("model", 42),
				new TermFrequency("graph", 30),
				new TermFrequency("data", 30));
	}

	@Test
	@DisplayName("Sin abstracts o con abstracts vacíos la entrada es vacía")
	void returnsEmptyInputWithoutTokens() {
		assertThat(counter.count(List.of())).isEmpty();
		assertThat(counter.count(List.of(List.of(), List.of()))).isEmpty();
	}

	@Test
	@DisplayName("El orden alfabético es el natural de String (compareTo) sobre los tokens")
	void usesNaturalStringOrder() {
		List<TermFrequency> input = counter.count(List.of(List.of("b", "a10", "a2", "a", "b")));

		// compareTo: "a" < "a10" < "a2" < "b"; en orden descendente queda al revés.
		assertThat(input).containsExactly(
				new TermFrequency("b", 2),
				new TermFrequency("a2", 1),
				new TermFrequency("a10", 1),
				new TermFrequency("a", 1));
	}

}
