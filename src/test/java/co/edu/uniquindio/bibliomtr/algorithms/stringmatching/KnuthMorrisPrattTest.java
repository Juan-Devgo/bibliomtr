package co.edu.uniquindio.bibliomtr.algorithms.stringmatching;

import static co.edu.uniquindio.bibliomtr.algorithms.stringmatching.StringMatchingCases.HAND_PATTERN;
import static co.edu.uniquindio.bibliomtr.algorithms.stringmatching.StringMatchingCases.HAND_POSITIONS;
import static co.edu.uniquindio.bibliomtr.algorithms.stringmatching.StringMatchingCases.HAND_TEXT;
import static co.edu.uniquindio.bibliomtr.algorithms.stringmatching.StringMatchingCases.run;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import co.edu.uniquindio.bibliomtr.trace.stringmatching.KmpPreprocessing;
import co.edu.uniquindio.bibliomtr.trace.stringmatching.KmpPreprocessing.PrefixStep;
import co.edu.uniquindio.bibliomtr.trace.stringmatching.KmpTrace;
import co.edu.uniquindio.bibliomtr.trace.stringmatching.KmpTrace.KmpComparison;

/**
 * Tests de estudiante de {@link KnuthMorrisPratt} (spec 004, REQ-4, REQ-9, REQ-11, REQ-23, REQ-32; AC-5 a AC-8,
 * AC-10, AC-25; constitución P10).
 *
 * <p>Los valores esperados están calculados a mano con π[i] = prefijo propio más largo de P[0..i] que también es
 * sufijo y coinciden con {@code docs/guia-implementacion.md}, § 11. Fallan hasta que se implemente el algoritmo:
 * {@code ./gradlew studentTest --tests KnuthMorrisPrattTest}.
 */
@Tag("student")
class KnuthMorrisPrattTest {

	private static final long SEED = 42L;
	private static final int RANDOM_TEXTS = 50;

	private final KnuthMorrisPratt kmp = new KnuthMorrisPratt();

	@Test
	@DisplayName("AC-5: π(\"abab\") = [0, 0, 1, 2] con los candidatos [0], [0], [1] de cada paso")
	void prefixFunctionOfHandPattern() {
		KmpPreprocessing preprocessing = kmp.preprocess(HAND_PATTERN, MatchOptions.tracing(MatchOptions.DEFAULT_Q));

		assertThat(preprocessing.pattern()).isEqualTo(HAND_PATTERN);
		assertThat(preprocessing.pi()).containsExactly(0, 0, 1, 2);
		assertThat(preprocessing.steps()).containsExactly(
				new PrefixStep(1, 'b', List.of(0), 0),
				new PrefixStep(2, 'a', List.of(0), 1),
				new PrefixStep(3, 'b', List.of(1), 2));
	}

	@Test
	@DisplayName("AC-5: sin traza, preprocess calcula π igual pero no guarda los pasos")
	void prefixFunctionWithoutTraceHasNoSteps() {
		KmpPreprocessing preprocessing = kmp.preprocess(HAND_PATTERN, MatchOptions.defaults());

		assertThat(preprocessing.pi()).containsExactly(0, 0, 1, 2);
		assertThat(preprocessing.steps()).isEmpty();
	}

	@Test
	@DisplayName("AC-5: \"abababca\"/\"abab\" hace 9 comparaciones (i, j, s) con sus retrocesos y ocurrencias en 0 y 2")
	void handCaseComparisons() {
		MatchResult<KmpTrace> result = run(kmp, HAND_TEXT, HAND_PATTERN, MatchOptions.tracing(MatchOptions.DEFAULT_Q));

		assertThat(result.positions()).containsExactlyElementsOf(HAND_POSITIONS);
		assertThat(result.collisions()).isEmpty();
		KmpTrace trace = result.trace();
		assertThat(trace.occurrences()).containsExactlyElementsOf(HAND_POSITIONS);
		assertThat(trace.comparisons()).containsExactly(
				new KmpComparison(0, 0, 0, 'a', 'a', true, null, null),
				new KmpComparison(1, 1, 0, 'b', 'b', true, null, null),
				new KmpComparison(2, 2, 0, 'a', 'a', true, null, null),
				new KmpComparison(3, 3, 0, 'b', 'b', true, 2, 0),
				new KmpComparison(4, 2, 2, 'a', 'a', true, null, null),
				new KmpComparison(5, 3, 2, 'b', 'b', true, 2, 2),
				new KmpComparison(6, 2, 4, 'c', 'a', false, 0, null),
				new KmpComparison(6, 0, 6, 'c', 'a', false, null, null),
				new KmpComparison(7, 0, 7, 'a', 'a', true, null, null));
	}

	@Test
	@DisplayName("REQ-11: en el caso a mano el índice del texto i nunca disminuye y s = i − j en cada paso")
	void textIndexNeverDecreasesInHandCase() {
		KmpTrace trace = run(kmp, HAND_TEXT, HAND_PATTERN, MatchOptions.tracing(MatchOptions.DEFAULT_Q)).trace();

		assertTextIndexNeverDecreases(trace, HAND_TEXT);
	}

	@Test
	@DisplayName("REQ-11: en 50 textos aleatorios (semilla 42) el índice del texto i nunca disminuye")
	void textIndexNeverDecreasesInRandomTexts() {
		Random random = new Random(SEED);
		for (int k = 0; k < RANDOM_TEXTS; k++) {
			String text = randomString(random, random.nextInt(61), "ab");
			String pattern = randomString(random, 1 + random.nextInt(4), "ab");

			KmpTrace trace = run(kmp, text, pattern, MatchOptions.tracing(MatchOptions.DEFAULT_Q)).trace();

			assertTextIndexNeverDecreases(trace, text);
		}
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("co.edu.uniquindio.bibliomtr.algorithms.stringmatching.StringMatchingCases#edgeCases")
	@DisplayName("AC-5 a AC-8, AC-25: casos de borde")
	void edgeCases(String description, String text, String pattern, List<Integer> expected) {
		StringMatchingCases.assertPositions(kmp, text, pattern, expected);
	}

	@Test
	@DisplayName("AC-7: un patrón vacío lanza IllegalArgumentException")
	void emptyPatternIsRejected() {
		StringMatchingCases.assertEmptyPatternRejected(kmp);
	}

	@Test
	@DisplayName("AC-10: dos abstracts idénticos dan las mismas posiciones")
	void identicalAbstracts() {
		StringMatchingCases.assertIdenticalAbstracts(kmp);
	}

	@Test
	@DisplayName("REQ-32: fragmento de la guía, π(\"systems\") = [0, 0, 1, 0, 0, 0, 1] y posiciones [214, 271]")
	void guideFragment() {
		MatchOptions options = MatchOptions.tracing(GuideFragment.Q);
		KmpPreprocessing preprocessing = kmp.preprocess(GuideFragment.PATTERN, options);

		assertThat(preprocessing.pi()).containsExactly(GuideFragment.pi());
		assertThat(preprocessing.steps()).extracting(PrefixStep::candidates)
				.containsExactlyElementsOf(GuideFragment.PI_CANDIDATES);

		MatchResult<KmpTrace> result = kmp.search(GuideFragment.FRAGMENT, preprocessing, options);

		assertThat(result.positions()).containsExactlyElementsOf(GuideFragment.POSITIONS);
		assertThat(result.trace().occurrences()).containsExactlyElementsOf(GuideFragment.POSITIONS);
		assertTextIndexNeverDecreases(result.trace(), GuideFragment.FRAGMENT);
	}

	/**
	 * En la traza el índice i del texto no disminuye, s = i − j, los caracteres son T[i] y P[j], y cada
	 * {@code occurrence} está en la lista de ocurrencias.
	 *
	 * <p>Complejidad: O(c) en tiempo y O(1) en espacio, con c = comparaciones.
	 */
	private static void assertTextIndexNeverDecreases(KmpTrace trace, String text) {
		int previous = -1;
		for (KmpComparison comparison : trace.comparisons()) {
			assertThat(comparison.i()).as("i en %s sobre \"%s\"", comparison, text).isGreaterThanOrEqualTo(previous);
			assertThat(comparison.shift()).as("s = i − j en %s", comparison).isEqualTo(comparison.i() - comparison.j());
			assertThat(comparison.textChar()).as("T[i] en %s", comparison).isEqualTo(text.charAt(comparison.i()));
			if (comparison.occurrence() != null) {
				assertThat(trace.occurrences()).contains(comparison.occurrence());
			}
			previous = comparison.i();
		}
	}

	/** Cadena de la longitud dada sobre el alfabeto. */
	private static String randomString(Random random, int length, String alphabet) {
		StringBuilder text = new StringBuilder(length);
		for (int k = 0; k < length; k++) {
			text.append(alphabet.charAt(random.nextInt(alphabet.length())));
		}
		return text.toString();
	}

}
