package co.edu.uniquindio.bibliomtr.oracle;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import co.edu.uniquindio.bibliomtr.algorithms.stringmatching.BoyerMoore;
import co.edu.uniquindio.bibliomtr.algorithms.stringmatching.GuideFragment;
import co.edu.uniquindio.bibliomtr.algorithms.stringmatching.KnuthMorrisPratt;
import co.edu.uniquindio.bibliomtr.algorithms.stringmatching.MatchOptions;
import co.edu.uniquindio.bibliomtr.algorithms.stringmatching.RabinKarp;
import co.edu.uniquindio.bibliomtr.algorithms.stringmatching.StringMatcher;
import co.edu.uniquindio.bibliomtr.algorithms.stringmatching.StringMatchingCases;

/**
 * Oráculo de KMP, Boyer-Moore y Rabin-Karp contra {@link String#indexOf(String, int)} (spec 004, REQ-12, REQ-32 y
 * AC-11).
 *
 * <p><b>Cómo.</b> Con {@code java.util.Random(42)} (constitución P12) se generan {@value #TEXTS} textos de longitud 0
 * a {@value #MAX_TEXT_LENGTH}, la mitad sobre el alfabeto "ab" (muchas ocurrencias solapadas) y la otra mitad
 * sobre a–z y espacio; cada patrón tiene de 1 a {@value #MAX_PATTERN_LENGTH} caracteres y se toma del propio texto
 * o al azar. Las posiciones de cada algoritmo deben ser las de {@code indexOf} avanzando de 1 en 1 desde cada
 * ocurrencia (así se cuentan las solapadas). Rabin-Karp se prueba con q ∈ {3, 101, 1 000 000 007}: con q = 3 hay
 * muchas colisiones y ninguna debe aparecer como ocurrencia. También se comprueba el fragmento de la guía.
 *
 * <p><b>Por qué aquí.</b> {@code indexOf} está prohibido en el código de producción del requerimiento (REQ-14);
 * este paquete es el único lugar donde se permite, como referencia. Falla hasta que se implementen los
 * algoritmos: {@code ./gradlew studentTest --tests StringMatchingOracleTest}.
 *
 * <p>Complejidad: O(N · n · m) en tiempo y O(n) en espacio, con N = textos, n ≤ 200 y m ≤ 6.
 */
@Tag("student")
class StringMatchingOracleTest {

	private static final long SEED = 42L;
	private static final int TEXTS = 200;
	private static final int MAX_TEXT_LENGTH = 200;
	private static final int MAX_PATTERN_LENGTH = 6;
	private static final String BINARY_ALPHABET = "ab";
	private static final String LETTERS_AND_SPACE = "abcdefghijklmnopqrstuvwxyz ";

	@Test
	@DisplayName("AC-11: KMP da las posiciones de indexOf en 200 textos aleatorios (semilla 42)")
	void kmpMatchesIndexOf() {
		assertMatchesIndexOf(new KnuthMorrisPratt(), MatchOptions.defaults());
	}

	@Test
	@DisplayName("AC-11: Boyer-Moore da las posiciones de indexOf en 200 textos aleatorios (semilla 42)")
	void boyerMooreMatchesIndexOf() {
		assertMatchesIndexOf(new BoyerMoore(), MatchOptions.defaults());
	}

	@ParameterizedTest(name = "q = {0}")
	@ValueSource(longs = {3L, 101L, 1_000_000_007L})
	@DisplayName("AC-11: Rabin-Karp da las posiciones de indexOf en 200 textos aleatorios (semilla 42)")
	void rabinKarpMatchesIndexOf(long q) {
		assertMatchesIndexOf(new RabinKarp(), new MatchOptions(q, false));
	}

	@Test
	@DisplayName("AC-11, REQ-32: en el fragmento de la guía los 3 algoritmos dan las posiciones de indexOf")
	void guideFragmentMatchesIndexOf() {
		List<Integer> expected = indexOfPositions(GuideFragment.FRAGMENT, GuideFragment.PATTERN);
		MatchOptions options = new MatchOptions(GuideFragment.Q, false);

		assertThat(expected).containsExactlyElementsOf(GuideFragment.POSITIONS);
		List<StringMatcher<?, ?>> matchers = List.of(new KnuthMorrisPratt(), new BoyerMoore(), new RabinKarp());
		for (StringMatcher<?, ?> matcher : matchers) {
			assertThat(StringMatchingCases.run(matcher, GuideFragment.FRAGMENT, GuideFragment.PATTERN, options)
					.positions())
					.as(matcher.getClass().getSimpleName())
					.containsExactlyElementsOf(expected);
		}
	}

	/**
	 * Compara el algoritmo con {@code indexOf} en los textos de semilla fija; la misma semilla da los mismos textos
	 * para los 3 algoritmos.
	 *
	 * <p>Complejidad: O(N · n · m) en tiempo y O(n) en espacio.
	 */
	private static void assertMatchesIndexOf(StringMatcher<?, ?> matcher, MatchOptions options) {
		Random random = new Random(SEED);
		for (int k = 0; k < TEXTS; k++) {
			String alphabet = k % 2 == 0 ? BINARY_ALPHABET : LETTERS_AND_SPACE;
			String text = randomString(random, random.nextInt(MAX_TEXT_LENGTH + 1), alphabet);
			String pattern = randomPattern(random, text, alphabet);

			List<Integer> actual = StringMatchingCases.run(matcher, text, pattern, options).positions();

			assertThat(actual).as("%s: \"%s\" en \"%s\" (q = %d)", matcher.getClass().getSimpleName(), pattern, text,
					options.q()).containsExactlyElementsOf(indexOfPositions(text, pattern));
		}
	}

	/** Todas las posiciones de {@code pattern} en {@code text}, incluidas las solapadas, con {@code indexOf}. */
	private static List<Integer> indexOfPositions(String text, String pattern) {
		List<Integer> positions = new ArrayList<>();
		int position = text.indexOf(pattern);
		while (position >= 0) {
			positions.add(position);
			position = text.indexOf(pattern, position + 1);
		}
		return positions;
	}

	/**
	 * Patrón de 1 a 6 caracteres: con probabilidad 1/2, y si el texto es suficientemente largo, una subcadena del
	 * texto (al menos una ocurrencia); si no, uno al azar sobre el mismo alfabeto.
	 */
	private static String randomPattern(Random random, String text, String alphabet) {
		int length = 1 + random.nextInt(MAX_PATTERN_LENGTH);
		if (random.nextBoolean() && text.length() >= length) {
			int start = random.nextInt(text.length() - length + 1);
			return text.substring(start, start + length);
		}
		return randomString(random, length, alphabet);
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
