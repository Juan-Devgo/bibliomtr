package co.edu.uniquindio.bibliomtr.algorithms.stringmatching;

import static co.edu.uniquindio.bibliomtr.algorithms.stringmatching.StringMatchingCases.COLLISION_PATTERN;
import static co.edu.uniquindio.bibliomtr.algorithms.stringmatching.StringMatchingCases.COLLISION_Q;
import static co.edu.uniquindio.bibliomtr.algorithms.stringmatching.StringMatchingCases.COLLISION_TEXT;
import static co.edu.uniquindio.bibliomtr.algorithms.stringmatching.StringMatchingCases.HAND_PATTERN;
import static co.edu.uniquindio.bibliomtr.algorithms.stringmatching.StringMatchingCases.HAND_POSITIONS;
import static co.edu.uniquindio.bibliomtr.algorithms.stringmatching.StringMatchingCases.HAND_TEXT;
import static co.edu.uniquindio.bibliomtr.algorithms.stringmatching.StringMatchingCases.run;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import co.edu.uniquindio.bibliomtr.trace.stringmatching.RabinKarpPreprocessing;
import co.edu.uniquindio.bibliomtr.trace.stringmatching.RabinKarpTrace;
import co.edu.uniquindio.bibliomtr.trace.stringmatching.RabinKarpTrace.Window;

/**
 * Tests de estudiante de {@link RabinKarp} (spec 004, REQ-6, REQ-9, REQ-10, REQ-25, REQ-32; AC-5 a AC-10, AC-25;
 * constitución P10).
 *
 * <p>Los valores esperados están calculados a mano con d = 65 536, h = d^(m−1) mod q, hash(S) =
 * (Σ S[i]·d^(m−1−i)) mod q y el rolling hash t(s+1) = (d·(t(s) − T[s]·h) + T[s+m]) mod q; coinciden con
 * {@code docs/guia-implementacion.md}, § 11. Fallan hasta que se implemente el algoritmo:
 * {@code ./gradlew studentTest --tests RabinKarpTest}.
 */
@Tag("student")
class RabinKarpTest {

	private final RabinKarp rabinKarp = new RabinKarp();

	@Test
	@DisplayName("AC-9: \"abab\" con q = 101 → d = 65 536, h = 25 y hash(P) = 48")
	void preprocessingOfHandPattern() {
		RabinKarpPreprocessing preprocessing =
				rabinKarp.preprocess(HAND_PATTERN, MatchOptions.tracing(MatchOptions.DEFAULT_Q));

		assertThat(preprocessing).isEqualTo(
				new RabinKarpPreprocessing(HAND_PATTERN, MatchOptions.D, MatchOptions.DEFAULT_Q, 25, 48));
	}

	@Test
	@DisplayName("AC-5, AC-9: \"abababca\"/\"abab\" con q = 101 → ventanas [48, 92, 48, 94, 21], verificadas en 0 y 2, sin colisiones")
	void handCaseWindows() {
		MatchResult<RabinKarpTrace> result =
				run(rabinKarp, HAND_TEXT, HAND_PATTERN, MatchOptions.tracing(MatchOptions.DEFAULT_Q));

		assertThat(result.positions()).containsExactlyElementsOf(HAND_POSITIONS);
		assertThat(result.collisions()).isEmpty();
		RabinKarpTrace trace = result.trace();
		assertThat(trace.d()).isEqualTo(MatchOptions.D);
		assertThat(trace.q()).isEqualTo(MatchOptions.DEFAULT_Q);
		assertThat(trace.h()).isEqualTo(25);
		assertThat(trace.patternHash()).isEqualTo(48);
		assertThat(trace.initialWindowHash()).isEqualTo(48L);
		assertThat(trace.windows()).containsExactly(
				new Window(0, 48, true, true, false),
				new Window(1, 92, false, null, false),
				new Window(2, 48, true, true, false),
				new Window(3, 94, false, null, false),
				new Window(4, 21, false, null, false));
		assertThat(trace.collisions()).isEmpty();
		assertThat(trace.occurrences()).containsExactlyElementsOf(HAND_POSITIONS);
	}

	@Test
	@DisplayName("AC-9: \"abba\"/\"ab\" con q = 3 → h = 1, hash(P) = 0, ventanas [0, 1, 0]; s = 2 es colisión y no ocurrencia")
	void smallModulusProducesCollision() {
		MatchResult<RabinKarpTrace> result =
				run(rabinKarp, COLLISION_TEXT, COLLISION_PATTERN, MatchOptions.tracing(COLLISION_Q));

		assertThat(result.positions()).containsExactly(0);
		assertThat(result.collisions()).containsExactly(2);
		RabinKarpTrace trace = result.trace();
		assertThat(trace.q()).isEqualTo(COLLISION_Q);
		assertThat(trace.h()).isEqualTo(1);
		assertThat(trace.patternHash()).isEqualTo(0);
		assertThat(trace.initialWindowHash()).isEqualTo(0L);
		assertThat(trace.windows()).containsExactly(
				new Window(0, 0, true, true, false),
				new Window(1, 1, false, null, false),
				new Window(2, 0, true, false, true));
		assertThat(trace.collisions()).containsExactly(2);
		assertThat(trace.occurrences()).containsExactly(0);
	}

	@Test
	@DisplayName("AC-7: con n < m no hay ventanas y el hash inicial de la traza es null")
	void patternLongerThanTextHasNoWindows() {
		MatchResult<RabinKarpTrace> result = run(rabinKarp, "ab", "abc", MatchOptions.tracing(MatchOptions.DEFAULT_Q));

		assertThat(result.positions()).isEmpty();
		assertThat(result.trace().initialWindowHash()).isNull();
		assertThat(result.trace().windows()).isEmpty();
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("co.edu.uniquindio.bibliomtr.algorithms.stringmatching.StringMatchingCases#edgeCases")
	@DisplayName("AC-5 a AC-8, AC-25: casos de borde")
	void edgeCases(String description, String text, String pattern, List<Integer> expected) {
		StringMatchingCases.assertPositions(rabinKarp, text, pattern, expected);
	}

	@Test
	@DisplayName("AC-7: un patrón vacío lanza IllegalArgumentException")
	void emptyPatternIsRejected() {
		StringMatchingCases.assertEmptyPatternRejected(rabinKarp);
	}

	@Test
	@DisplayName("AC-10: dos abstracts idénticos dan las mismas posiciones")
	void identicalAbstracts() {
		StringMatchingCases.assertIdenticalAbstracts(rabinKarp);
	}

	@Test
	@DisplayName("REQ-32: fragmento de la guía con q = 101 → h = 19, hash(\"systems\") = 4 y posiciones [214, 271]")
	void guideFragment() {
		MatchOptions options = MatchOptions.tracing(GuideFragment.Q);
		RabinKarpPreprocessing preprocessing = rabinKarp.preprocess(GuideFragment.PATTERN, options);

		assertThat(preprocessing.h()).isEqualTo(GuideFragment.H);
		assertThat(preprocessing.patternHash()).isEqualTo(GuideFragment.PATTERN_HASH);

		MatchResult<RabinKarpTrace> result = rabinKarp.search(GuideFragment.FRAGMENT, preprocessing, options);

		assertThat(result.positions()).containsExactlyElementsOf(GuideFragment.POSITIONS);
		RabinKarpTrace trace = result.trace();
		assertThat(trace.windows()).hasSize(GuideFragment.WINDOW_COUNT);
		assertThat(trace.collisions()).containsExactlyElementsOf(result.collisions());
		for (Window window : trace.windows()) {
			assertThat(window.hash()).as("hash de la ventana %d en [0, q)", window.shift())
					.isBetween(0L, GuideFragment.Q - 1);
			boolean occurrence = GuideFragment.POSITIONS.contains(window.shift());
			boolean collision = result.collisions().contains(window.shift());
			// Toda ocurrencia y toda colisión tienen el hash del patrón; ninguna colisión es ocurrencia.
			assertThat(window.hashMatches()).as("hashMatches en s = %d", window.shift())
					.isEqualTo(window.hash() == GuideFragment.PATTERN_HASH);
			if (occurrence || collision) {
				assertThat(window.hash()).as("hash en s = %d", window.shift()).isEqualTo(GuideFragment.PATTERN_HASH);
			}
			assertThat(window.collision()).as("colisión en s = %d", window.shift()).isEqualTo(collision);
			assertThat(occurrence && collision).as("s = %d no es ocurrencia y colisión a la vez", window.shift())
					.isFalse();
		}
	}

}
