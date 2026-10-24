package co.edu.uniquindio.bibliomtr.embeddings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests de {@link Specter2Input} (spec 001, REQ-9 y AC-8): SPECTER2 recibe {@code título [SEP] abstract}.
 */
class Specter2InputTest {

	@Test
	@DisplayName("AC-8: el texto de SPECTER2 es el título, \" [SEP] \" y el abstract")
	void joinsTitleAndAbstractWithSep() {
		assertThat(Specter2Input.of("Deep Learning in Education", "We study models."))
				.isEqualTo("Deep Learning in Education [SEP] We study models.");
	}

	@Test
	@DisplayName("Un título o abstract nulo lanza NullPointerException")
	void rejectsNulls() {
		assertThatThrownBy(() -> Specter2Input.of(null, "a")).isInstanceOf(NullPointerException.class);
		assertThatThrownBy(() -> Specter2Input.of("t", null)).isInstanceOf(NullPointerException.class);
	}
}
