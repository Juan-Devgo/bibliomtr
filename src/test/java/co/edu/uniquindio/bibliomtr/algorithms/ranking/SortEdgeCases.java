package co.edu.uniquindio.bibliomtr.algorithms.ranking;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.params.provider.Arguments;

/**
 * Casos de borde compartidos por los tests de los tres algoritmos (REQ-24, AC-17), para
 * {@code @MethodSource("co.edu.uniquindio.bibliomtr.algorithms.ranking.SortEdgeCases#cases")}.
 *
 * <p>Cada caso trae su nombre, la entrada y la salida esperada, calculada a mano: orden ascendente por
 * frecuencia y, en los empates, el mismo orden relativo de la entrada (estabilidad). La lista vacía (AC-18)
 * se prueba aparte porque además verifica la forma de la traza.
 */
final class SortEdgeCases {

	private SortEdgeCases() {
	}

	/** Atajo para construir un par. */
	static TermFrequency tf(String term, int frequency) {
		return new TermFrequency(term, frequency);
	}

	/**
	 * Casos: un término, todas las frecuencias iguales, ya ordenada, orden inverso, empates mezclados y
	 * frecuencia máxima mucho mayor que el número de términos (k ≫ n).
	 *
	 * @return argumentos (nombre, entrada, salida esperada)
	 */
	static Stream<Arguments> cases() {
		return Stream.of(
				Arguments.of("un término",
						List.of(tf("model", 7)),
						List.of(tf("model", 7))),
				Arguments.of("todas las frecuencias iguales (estable)",
						List.of(tf("d", 3), tf("c", 3), tf("b", 3), tf("a", 3)),
						List.of(tf("d", 3), tf("c", 3), tf("b", 3), tf("a", 3))),
				Arguments.of("ya ordenada",
						List.of(tf("a", 1), tf("b", 2), tf("c", 3), tf("d", 4)),
						List.of(tf("a", 1), tf("b", 2), tf("c", 3), tf("d", 4))),
				Arguments.of("orden inverso",
						List.of(tf("a", 4), tf("b", 3), tf("c", 2), tf("d", 1)),
						List.of(tf("d", 1), tf("c", 2), tf("b", 3), tf("a", 4))),
				Arguments.of("empates mezclados (estable)",
						List.of(tf("d", 2), tf("c", 1), tf("b", 2), tf("a", 1)),
						List.of(tf("c", 1), tf("a", 1), tf("d", 2), tf("b", 2))),
				Arguments.of("k ≫ n: [1, 1000]",
						List.of(tf("a", 1), tf("b", 1000)),
						List.of(tf("a", 1), tf("b", 1000))),
				Arguments.of("k ≫ n con empates: [1000, 1, 1000]",
						List.of(tf("z", 1000), tf("y", 1), tf("x", 1000)),
						List.of(tf("y", 1), tf("z", 1000), tf("x", 1000))));
	}

}
