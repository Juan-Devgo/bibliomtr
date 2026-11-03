package co.edu.uniquindio.bibliomtr.algorithms.stringmatching;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.params.provider.Arguments;

/**
 * Casos y aserciones compartidos por los tests de estudiante de búsqueda de patrones (spec 004, REQ-2, REQ-3,
 * REQ-9; constitución P10).
 *
 * <p><b>Casos.</b>
 * <ul>
 *   <li>Caso a mano: T = {@value #HAND_TEXT}, P = {@value #HAND_PATTERN} → [0, 2] (AC-5).</li>
 *   <li>Bordes ({@link #edgeCases()}): texto vacío, patrón más largo que el texto, patrón igual al texto, sin
 *       ocurrencias, ocurrencias al inicio y al final, solapadas, mayúsculas y tildes distintas y un patrón de un
 *       espacio (AC-6, AC-7, AC-8, AC-25). Las posiciones se contaron a mano, base 0 en {@code char}.</li>
 *   <li>Patrón vacío: {@code preprocess("")} lanza {@code IllegalArgumentException} (AC-7).</li>
 *   <li>Abstracts idénticos: dos cadenas distintas con el mismo contenido {@value #IDENTICAL_ABSTRACT} y el mismo
 *       preprocesado de {@value #IDENTICAL_PATTERN} dan [0, 20] en ambas (AC-10).</li>
 * </ul>
 * Las guardas (patrón vacío, texto vacío o más corto) las escribe el estudiante, así que con los stubs sin
 * implementar todos estos casos fallan (AC-4).
 *
 * <p><b>{@link #run}.</b> Hace lo mismo que el servicio con cada abstract: un {@code preprocess} y un
 * {@code search} con el mismo preprocesado y las mismas opciones (REQ-1). Es público porque también lo usa el
 * oráculo de {@code co.edu.uniquindio.bibliomtr.oracle}.
 *
 * <p>Complejidad: la de cada algoritmo; los casos son constantes.
 */
public final class StringMatchingCases {

	/** Texto del caso a mano. */
	static final String HAND_TEXT = "abababca";

	/** Patrón del caso a mano. */
	static final String HAND_PATTERN = "abab";

	/** Posiciones del caso a mano. */
	static final List<Integer> HAND_POSITIONS = List.of(0, 2);

	/** Texto del caso de colisión de Rabin-Karp. */
	static final String COLLISION_TEXT = "abba";

	/** Patrón del caso de colisión de Rabin-Karp. */
	static final String COLLISION_PATTERN = "ab";

	/** Módulo pequeño del caso de colisión. */
	static final long COLLISION_Q = 3L;

	/** Contenido de los dos abstracts idénticos. */
	static final String IDENTICAL_ABSTRACT = "AI in education and AI in assessment";

	/** Patrón buscado en los abstracts idénticos. */
	static final String IDENTICAL_PATTERN = "AI in";

	/** Posiciones de {@link #IDENTICAL_PATTERN} en {@link #IDENTICAL_ABSTRACT}. */
	static final List<Integer> IDENTICAL_POSITIONS = List.of(0, 20);

	private StringMatchingCases() {
	}

	/**
	 * Preprocesa el patrón y busca en el texto con las mismas opciones.
	 *
	 * <p>Complejidad: la del preprocesamiento más la de la búsqueda del algoritmo.
	 *
	 * @param matcher algoritmo
	 * @param text    texto donde se busca
	 * @param pattern patrón
	 * @param options opciones de ambas fases
	 * @param <P>     tipo del preprocesado
	 * @param <T>     tipo de la traza
	 * @return el resultado de la búsqueda
	 */
	public static <P, T> MatchResult<T> run(StringMatcher<P, T> matcher, String text, String pattern,
			MatchOptions options) {
		P preprocessed = matcher.preprocess(pattern, options);
		return matcher.search(text, preprocessed, options);
	}

	/**
	 * Casos de borde: descripción, texto, patrón y posiciones esperadas. Fuente de
	 * {@code @MethodSource("co.edu.uniquindio.bibliomtr.algorithms.stringmatching.StringMatchingCases#edgeCases")}.
	 *
	 * <p>Complejidad: O(1).
	 *
	 * @return los casos
	 */
	static Stream<Arguments> edgeCases() {
		return Stream.of(
				Arguments.of("caso a mano: \"abababca\"/\"abab\"", HAND_TEXT, HAND_PATTERN, HAND_POSITIONS),
				Arguments.of("texto vacío", "", "ab", List.of()),
				Arguments.of("patrón más largo que el texto", "ab", "abc", List.of()),
				Arguments.of("patrón igual al texto", "abc", "abc", List.of(0)),
				Arguments.of("sin ocurrencias", "abc", "xy", List.of()),
				Arguments.of("ocurrencias al inicio y al final", "abcab", "ab", List.of(0, 3)),
				Arguments.of("ocurrencias solapadas", "aaaa", "aa", List.of(0, 1, 2)),
				Arguments.of("distingue mayúsculas", "Algoritmo algoritmo", "algoritmo", List.of(10)),
				Arguments.of("distingue tildes", "canción cancion", "cancion", List.of(8)),
				Arguments.of("patrón de un espacio, sin recortar", "a b c", " ", List.of(1, 3)));
	}

	/**
	 * El patrón vacío se rechaza en el preprocesamiento con {@code IllegalArgumentException} (AC-7).
	 *
	 * <p>Complejidad: O(1).
	 */
	static void assertEmptyPatternRejected(StringMatcher<?, ?> matcher) {
		assertThatThrownBy(() -> matcher.preprocess("", MatchOptions.defaults()))
				.as("preprocess(\"\")")
				.isInstanceOf(IllegalArgumentException.class);
	}

	/**
	 * Dos abstracts idénticos, buscados con el mismo preprocesado, dan las mismas posiciones (AC-10).
	 *
	 * <p>Complejidad: la de un preprocesamiento y dos búsquedas.
	 */
	static <P, T> void assertIdenticalAbstracts(StringMatcher<P, T> matcher) {
		MatchOptions options = MatchOptions.defaults();
		String first = IDENTICAL_ABSTRACT;
		// Una instancia distinta con el mismo contenido: el algoritmo no puede depender de la identidad.
		String second = new String(IDENTICAL_ABSTRACT.toCharArray());
		P preprocessed = matcher.preprocess(IDENTICAL_PATTERN, options);

		List<Integer> firstPositions = matcher.search(first, preprocessed, options).positions();
		List<Integer> secondPositions = matcher.search(second, preprocessed, options).positions();

		assertThat(firstPositions).as("primer abstract").containsExactlyElementsOf(IDENTICAL_POSITIONS);
		assertThat(secondPositions).as("segundo abstract").containsExactlyElementsOf(firstPositions);
	}

	/**
	 * Las posiciones son las esperadas, en orden, y con {@code recordTrace = false} la traza es {@code null}.
	 *
	 * <p>Complejidad: la del algoritmo.
	 */
	static void assertPositions(StringMatcher<?, ?> matcher, String text, String pattern, List<Integer> expected) {
		MatchResult<?> result = run(matcher, text, pattern, MatchOptions.defaults());

		assertThat(result.positions()).as("posiciones de \"%s\" en \"%s\"", pattern, text)
				.containsExactlyElementsOf(expected);
		assertThat(result.trace()).as("traza con recordTrace = false").isNull();
	}

}
