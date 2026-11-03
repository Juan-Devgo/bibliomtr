package co.edu.uniquindio.bibliomtr.algorithms.stringmatching;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests de {@link MatchPositionsValidator} (spec 004, REQ-8, AC-3): acepta posiciones ascendentes dentro de
 * [0, n − m] y rechaza con mensaje en español las desordenadas, repetidas, fuera de rango o nulas, sin leer el texto
 * ni el patrón.
 *
 * <p>Complejidad: O(k) por caso, con k = número de posiciones.
 */
class MatchPositionsValidatorTest {

	private static final String PREFIX = "El algoritmo kmp devolvió posiciones inválidas en el artículo 7: ";

	/** n = 8 ("abababca") y m = 4 ("abab"): posiciones válidas en [0, 4]. */
	private static final int N = 8;
	private static final int M = 4;

	@Test
	@DisplayName("AC-3: [0, 2] con n = 8 y m = 4 es válida")
	void validPositionsPass() {
		assertThatCode(() -> MatchPositionsValidator.validate("kmp", 7, N, M, List.of(0, 2))).doesNotThrowAnyException();
	}

	@Test
	@DisplayName("AC-3: una lista vacía es válida, también con n < m")
	void emptyPositionsPass() {
		assertThatCode(() -> MatchPositionsValidator.validate("kmp", 7, N, M, List.of())).doesNotThrowAnyException();
		assertThatCode(() -> MatchPositionsValidator.validate("kmp", 7, 2, 3, List.of())).doesNotThrowAnyException();
	}

	@Test
	@DisplayName("AC-3: los extremos 0 y n − m son válidos")
	void boundaryPositionsPass() {
		assertThatCode(() -> MatchPositionsValidator.validate("kmp", 7, N, M, List.of(0, N - M)))
				.doesNotThrowAnyException();
	}

	@Test
	@DisplayName("AC-3: [1, 0] (desordenada) se rechaza")
	void unorderedPositionsAreRejected() {
		assertThatThrownBy(() -> MatchPositionsValidator.validate("kmp", 7, N, M, List.of(1, 0)))
				.isInstanceOf(InvalidMatchPositionsException.class)
				.hasMessage(PREFIX + "la posición 0 no es mayor que la anterior 1");
	}

	@Test
	@DisplayName("AC-3: [0, 0] (repetida) se rechaza")
	void repeatedPositionsAreRejected() {
		assertThatThrownBy(() -> MatchPositionsValidator.validate("kmp", 7, N, M, List.of(0, 0)))
				.isInstanceOf(InvalidMatchPositionsException.class)
				.hasMessage(PREFIX + "la posición 0 no es mayor que la anterior 0");
	}

	@Test
	@DisplayName("AC-3: [−1] y [n − m + 1] (fuera de rango) se rechazan")
	void outOfRangePositionsAreRejected() {
		assertThatThrownBy(() -> MatchPositionsValidator.validate("kmp", 7, N, M, List.of(-1)))
				.isInstanceOf(InvalidMatchPositionsException.class)
				.hasMessage(PREFIX + "la posición -1 está fuera de [0, 4]");
		assertThatThrownBy(() -> MatchPositionsValidator.validate("kmp", 7, N, M, List.of(N - M + 1)))
				.isInstanceOf(InvalidMatchPositionsException.class)
				.hasMessage(PREFIX + "la posición 5 está fuera de [0, 4]");
	}

	@Test
	@DisplayName("AC-3: con n < m toda posición es inválida")
	void anyPositionWithShortTextIsRejected() {
		assertThatThrownBy(() -> MatchPositionsValidator.validate("kmp", 7, 2, 3, List.of(0)))
				.isInstanceOf(InvalidMatchPositionsException.class)
				.hasMessage(PREFIX + "la posición 0 está fuera de [0, -1]");
	}

	@Test
	@DisplayName("AC-3: una lista nula se rechaza")
	void nullListIsRejected() {
		assertThatThrownBy(() -> MatchPositionsValidator.validate("kmp", 7, N, M, null))
				.isInstanceOf(InvalidMatchPositionsException.class)
				.hasMessage(PREFIX + "la lista de posiciones es nula");
	}

	@Test
	@DisplayName("AC-3: [null] se rechaza con el índice de la posición nula")
	void nullPositionIsRejected() {
		assertThatThrownBy(() -> MatchPositionsValidator.validate("kmp", 7, N, M, Arrays.asList(0, null)))
				.isInstanceOf(InvalidMatchPositionsException.class)
				.hasMessage(PREFIX + "la posición 1 es nula");
		List<Integer> single = new ArrayList<>();
		single.add(null);
		assertThatThrownBy(() -> MatchPositionsValidator.validate("kmp", 7, N, M, single))
				.isInstanceOf(InvalidMatchPositionsException.class)
				.hasMessage(PREFIX + "la posición 0 es nula");
	}

	@Test
	@DisplayName("AC-3: un resultado nulo tiene su propio mensaje para el servicio")
	void nullResultHasItsMessage() {
		assertThat(MatchPositionsValidator.nullResult("kmp", 7))
				.isInstanceOf(InvalidMatchPositionsException.class)
				.hasMessage(PREFIX + "el resultado es nulo");
	}

	@Test
	@DisplayName("MatchResult conserva una lista nula y elementos nulos para que el validador los rechace")
	void matchResultKeepsNulls() {
		assertThat(new MatchResult<Void>(null, null, null).positions()).isNull();
		assertThat(new MatchResult<Void>(Arrays.asList(0, null), null, null).positions()).containsExactly(0, null);
		assertThat(new MatchResult<Void>(List.of(0), null, null).collisions()).isEmpty();
	}

}
