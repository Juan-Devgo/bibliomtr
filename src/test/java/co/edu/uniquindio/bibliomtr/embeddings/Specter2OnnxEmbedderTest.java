package co.edu.uniquindio.bibliomtr.embeddings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

/**
 * Tests de {@link Specter2OnnxEmbedder} con el modelo real (spec 001, REQ-9a y AC-8a).
 *
 * <p>El modelo no se versiona ({@code scripts/download-model.sh}); si no está en el classpath, los tests se
 * omiten con Assumptions en lugar de fallar.
 */
class Specter2OnnxEmbedderTest {

	private static final Resource MODEL = new ClassPathResource("models/specter2-base/model.onnx");
	private static final Resource TOKENIZER = new ClassPathResource("models/specter2-base/tokenizer.json");

	/** Id del token [SEP] en el vocabulario de SPECTER2 ({@code tokenizer_config.json}). */
	private static final long SEP_ID = 103L;
	/** Id del token [CLS] en el vocabulario de SPECTER2. */
	private static final long CLS_ID = 102L;

	private static final String TITLE = "Bibliometrics of generative AI in education";
	/** Unos 1100 tokens: supera con holgura los 512 de SPECTER2. */
	private static final String LONG_ABSTRACT =
			"We analyze the scientific production on machine learning and generative models in higher education. "
					.repeat(70);

	private static Specter2OnnxEmbedder embedder;

	@BeforeAll
	static void loadModel() {
		assumeTrue(MODEL.exists() && TOKENIZER.exists(),
				"Modelo SPECTER2 ausente; ejecute scripts/download-model.sh");
		embedder = new Specter2OnnxEmbedder(MODEL, TOKENIZER);
	}

	@AfterAll
	static void closeModel() {
		if (embedder != null) {
			embedder.close();
		}
	}

	@Test
	@DisplayName("AC-8a: un texto de más de 512 tokens se codifica en 512 ids que empiezan en [CLS] y terminan en [SEP]")
	void encodeTruncatesTo512Tokens() {
		Specter2OnnxEmbedder.Specter2Encoding encoding = embedder.encode(Specter2Input.of(TITLE, LONG_ABSTRACT));

		assertThat(encoding.truncated()).isTrue();
		assertThat(encoding.tokenCount()).isGreaterThan(Specter2OnnxEmbedder.MAX_TOKENS);
		assertThat(encoding.inputIds()).hasSize(Specter2OnnxEmbedder.MAX_TOKENS);
		assertThat(encoding.inputIds()[0]).isEqualTo(CLS_ID);
		assertThat(encoding.inputIds()[Specter2OnnxEmbedder.MAX_TOKENS - 1]).isEqualTo(SEP_ID);
		assertThat(encoding.attentionMask()).hasSize(Specter2OnnxEmbedder.MAX_TOKENS).containsOnly(1L);
		assertThat(encoding.tokenTypeIds()).hasSize(Specter2OnnxEmbedder.MAX_TOKENS).containsOnly(0L);
	}

	@Test
	@DisplayName("Un texto corto no se trunca y el [SEP] del medio se reconoce como token especial")
	void shortTextIsNotTruncated() {
		Specter2OnnxEmbedder.Specter2Encoding encoding = embedder.encode(Specter2Input.of("A title", "An abstract."));

		assertThat(encoding.truncated()).isFalse();
		assertThat(encoding.inputIds()).hasSize(encoding.tokenCount());
		assertThat(encoding.inputIds()[0]).isEqualTo(CLS_ID);
		assertThat(encoding.inputIds()[encoding.tokenCount() - 1]).isEqualTo(SEP_ID);
		// [CLS] a title [SEP] an abstract . [SEP]: exactamente dos [SEP].
		long seps = 0;
		for (long id : encoding.inputIds()) {
			if (id == SEP_ID) {
				seps++;
			}
		}
		assertThat(seps).isEqualTo(2);
	}

	@Test
	@DisplayName("AC-8a: el embedding de un texto truncado queda marcado, con 768 dimensiones y norma L2 = 1 ± 1e-5")
	void embedTruncatedTextHasUnitNorm() {
		Specter2Embedding embedding = embedder.embed(TITLE, LONG_ABSTRACT);

		assertThat(embedding.truncated()).isTrue();
		assertThat(embedding.tokenCount()).isGreaterThan(Specter2OnnxEmbedder.MAX_TOKENS);
		assertThat(embedding.vector()).hasSize(EmbeddingModel.SPECTER2.dimension());
		assertThat(norm(embedding.vector())).isCloseTo(1.0, within(1e-5));
	}

	@Test
	@DisplayName("El embedding de un texto corto no se marca como truncado y tiene norma 1")
	void embedShortText() {
		Specter2Embedding embedding = embedder.embed(TITLE, "We study the use of language models in classrooms.");

		assertThat(embedding.truncated()).isFalse();
		assertThat(embedding.tokenCount()).isLessThanOrEqualTo(Specter2OnnxEmbedder.MAX_TOKENS);
		assertThat(embedding.vector()).hasSize(EmbeddingModel.SPECTER2.dimension());
		assertThat(norm(embedding.vector())).isCloseTo(1.0, within(1e-5));
	}

	private static double norm(float[] v) {
		double sum = 0;
		for (float x : v) {
			sum += (double) x * x;
		}
		return Math.sqrt(sum);
	}
}
