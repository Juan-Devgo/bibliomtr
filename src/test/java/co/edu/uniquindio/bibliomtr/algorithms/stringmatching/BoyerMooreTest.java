package co.edu.uniquindio.bibliomtr.algorithms.stringmatching;

import static co.edu.uniquindio.bibliomtr.algorithms.stringmatching.StringMatchingCases.HAND_PATTERN;
import static co.edu.uniquindio.bibliomtr.algorithms.stringmatching.StringMatchingCases.HAND_POSITIONS;
import static co.edu.uniquindio.bibliomtr.algorithms.stringmatching.StringMatchingCases.HAND_TEXT;
import static co.edu.uniquindio.bibliomtr.algorithms.stringmatching.StringMatchingCases.run;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import co.edu.uniquindio.bibliomtr.trace.stringmatching.BoyerMoorePreprocessing;
import co.edu.uniquindio.bibliomtr.trace.stringmatching.BoyerMooreTrace;
import co.edu.uniquindio.bibliomtr.trace.stringmatching.BoyerMooreTrace.Alignment;
import co.edu.uniquindio.bibliomtr.trace.stringmatching.BoyerMooreTrace.CharComparison;

/**
 * Tests de estudiante de {@link BoyerMoore} (spec 004, REQ-5, REQ-9, REQ-24, REQ-32; AC-5 a AC-8, AC-10, AC-24,
 * AC-25; constitución P10).
 *
 * <p>Los valores esperados están calculados a mano con la regla del carácter malo (última aparición, −1 si el
 * carácter no está), la regla del sufijo bueno fuerte indexada por la posición j del desajuste y el salto
 * max(carácter malo, sufijo bueno, 1); coinciden con {@code docs/guia-implementacion.md}, § 11. Fallan hasta que
 * se implemente el algoritmo: {@code ./gradlew studentTest --tests BoyerMooreTest}.
 */
@Tag("student")
class BoyerMooreTest {

	private final BoyerMoore boyerMoore = new BoyerMoore();

	@Test
	@DisplayName("AC-24: \"abab\" → carácter malo {a: 2, b: 3}, f = [2, 3, 4, 4, 5], sufijo bueno [2, 2, 4, 1], salto tras ocurrencia 2")
	void tablesOfHandPattern() {
		BoyerMoorePreprocessing preprocessing =
				boyerMoore.preprocess(HAND_PATTERN, MatchOptions.tracing(MatchOptions.DEFAULT_Q));

		assertThat(preprocessing.pattern()).isEqualTo(HAND_PATTERN);
		assertThat(preprocessing.lastOccurrence()).containsExactly(entry('a', 2), entry('b', 3));
		// Un carácter ausente vale −1 y no se guarda en el mapa.
		assertThat(preprocessing.lastOccurrence()).doesNotContainKey('c');
		assertThat(preprocessing.borderPositions()).containsExactly(2, 3, 4, 4, 5);
		assertThat(preprocessing.goodSuffixShift()).containsExactly(2, 2, 4, 1);
		assertThat(preprocessing.patternBorder()).isEqualTo(2);
		assertThat(preprocessing.matchShift()).isEqualTo(2);
	}

	@Test
	@DisplayName("AC-5: \"abababca\"/\"abab\" prueba las alineaciones s = 0, 2 (ocurrencias, salto 2) y 4 (desajuste en j = 3, salto 1)")
	void handCaseAlignments() {
		MatchResult<BoyerMooreTrace> result =
				run(boyerMoore, HAND_TEXT, HAND_PATTERN, MatchOptions.tracing(MatchOptions.DEFAULT_Q));

		assertThat(result.positions()).containsExactlyElementsOf(HAND_POSITIONS);
		assertThat(result.collisions()).isEmpty();
		BoyerMooreTrace trace = result.trace();
		assertThat(trace.occurrences()).containsExactlyElementsOf(HAND_POSITIONS);
		assertThat(trace.alignments()).containsExactly(
				new Alignment(0, List.of(
						new CharComparison(3, 3, 'b', 'b', true),
						new CharComparison(2, 2, 'a', 'a', true),
						new CharComparison(1, 1, 'b', 'b', true),
						new CharComparison(0, 0, 'a', 'a', true)),
						null, true, null, 2, 2),
				new Alignment(2, List.of(
						new CharComparison(3, 5, 'b', 'b', true),
						new CharComparison(2, 4, 'a', 'a', true),
						new CharComparison(1, 3, 'b', 'b', true),
						new CharComparison(0, 2, 'a', 'a', true)),
						null, true, null, 2, 2),
				// Desajuste en j = 3 con T[7] = 'a': carácter malo 3 − last('a') = 3 − 2 = 1, sufijo bueno shift[3] = 1.
				new Alignment(4, List.of(new CharComparison(3, 7, 'a', 'b', false)), 3, false, 1, 1, 1));
	}

	@Test
	@DisplayName("REQ-24: cada alineación compara de derecha a izquierda y el salto aplicado es max(reglas, 1)")
	void alignmentsCompareRightToLeft() {
		BoyerMooreTrace trace = run(boyerMoore, GuideFragment.FRAGMENT, GuideFragment.PATTERN,
				MatchOptions.tracing(GuideFragment.Q)).trace();

		assertThat(trace.alignments()).isNotEmpty();
		int expectedShift = 0;
		for (Alignment alignment : trace.alignments()) {
			assertThat(alignment.shift()).as("s de %s", alignment).isEqualTo(expectedShift);
			int expectedJ = GuideFragment.PATTERN.length() - 1;
			for (CharComparison comparison : alignment.comparisons()) {
				assertThat(comparison.j()).as("j en %s", alignment).isEqualTo(expectedJ);
				assertThat(comparison.textIndex()).as("s + j en %s", alignment).isEqualTo(alignment.shift() + expectedJ);
				expectedJ--;
			}
			if (alignment.occurrence()) {
				assertThat(alignment.appliedShift()).isEqualTo(GuideFragment.MATCH_SHIFT);
			} else {
				int rules = Math.max(alignment.badCharacterShift(), alignment.goodSuffixShift());
				assertThat(alignment.appliedShift()).as("salto de %s", alignment).isEqualTo(Math.max(rules, 1));
			}
			expectedShift += alignment.appliedShift();
		}
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("co.edu.uniquindio.bibliomtr.algorithms.stringmatching.StringMatchingCases#edgeCases")
	@DisplayName("AC-5 a AC-8, AC-25: casos de borde")
	void edgeCases(String description, String text, String pattern, List<Integer> expected) {
		StringMatchingCases.assertPositions(boyerMoore, text, pattern, expected);
	}

	@Test
	@DisplayName("AC-7: un patrón vacío lanza IllegalArgumentException")
	void emptyPatternIsRejected() {
		StringMatchingCases.assertEmptyPatternRejected(boyerMoore);
	}

	@Test
	@DisplayName("AC-10: dos abstracts idénticos dan las mismas posiciones")
	void identicalAbstracts() {
		StringMatchingCases.assertIdenticalAbstracts(boyerMoore);
	}

	@Test
	@DisplayName("REQ-32: fragmento de la guía, tablas de \"systems\" y posiciones [214, 271]")
	void guideFragment() {
		MatchOptions options = MatchOptions.tracing(GuideFragment.Q);
		BoyerMoorePreprocessing preprocessing = boyerMoore.preprocess(GuideFragment.PATTERN, options);

		assertThat(preprocessing.lastOccurrence()).containsExactlyEntriesOf(GuideFragment.LAST_OCCURRENCE);
		assertThat(preprocessing.borderPositions()).containsExactly(GuideFragment.borderPositions());
		assertThat(preprocessing.goodSuffixShift()).containsExactly(GuideFragment.goodSuffixShift());
		assertThat(preprocessing.patternBorder()).isEqualTo(GuideFragment.PATTERN_BORDER);
		assertThat(preprocessing.matchShift()).isEqualTo(GuideFragment.MATCH_SHIFT);

		MatchResult<BoyerMooreTrace> result = boyerMoore.search(GuideFragment.FRAGMENT, preprocessing, options);

		assertThat(result.positions()).containsExactlyElementsOf(GuideFragment.POSITIONS);
		assertThat(result.trace().occurrences()).containsExactlyElementsOf(GuideFragment.POSITIONS);
	}

}
