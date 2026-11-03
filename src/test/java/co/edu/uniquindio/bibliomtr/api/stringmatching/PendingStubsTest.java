package co.edu.uniquindio.bibliomtr.api.stringmatching;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Detección del prefijo "TODO estudiante:" sin {@code startsWith} (spec 004, REQ-14, REQ-28).
 */
class PendingStubsTest {

	@Test
	@DisplayName("REQ-28: solo un mensaje que empieza por \"TODO estudiante:\" marca el algoritmo como pendiente")
	void recognizesOnlyThePrefix() {
		assertThat(PendingStubs.isPending(new UnsupportedOperationException("TODO estudiante: implementar X")))
				.isTrue();
		assertThat(PendingStubs.isPending(new UnsupportedOperationException("TODO estudiante:"))).isTrue();

		assertThat(PendingStubs.isPending(new UnsupportedOperationException("TODO estudiante"))).isFalse();
		assertThat(PendingStubs.isPending(new UnsupportedOperationException("todo estudiante: x"))).isFalse();
		assertThat(PendingStubs.isPending(new UnsupportedOperationException("Falta: TODO estudiante: x"))).isFalse();
		assertThat(PendingStubs.isPending(new UnsupportedOperationException(""))).isFalse();
		assertThat(PendingStubs.isPending(new UnsupportedOperationException((String) null))).isFalse();
		assertThat(PendingStubs.isPending(null)).isFalse();
	}

}
