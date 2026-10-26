package co.edu.uniquindio.bibliomtr.embeddings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.StringJoiner;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/**
 * Tests de {@link NomicOllamaEmbedder} contra un Ollama simulado con {@link MockRestServiceServer}
 * (spec 001, REQ-9 y AC-8): cuerpos de {@code /api/pull} y {@code /api/embed}, lectura de {@code embeddings[0]}
 * y validación de la dimensión.
 *
 * <p>Hay un servidor simulado por cliente: {@link #embedServer} para el cliente de {@code /api/embed} y
 * {@link #pullServer} para el de {@code /api/pull}. Un servidor sin expectativas rechaza cualquier petición, así
 * que cada test comprueba que la llamada llega solo al cliente que le corresponde.
 */
class NomicOllamaEmbedderTest {

	private static final String BASE_URL = "http://ollama.test";
	private static final String MODEL = "nomic-embed-text:v1.5";

	private MockRestServiceServer embedServer;
	private MockRestServiceServer pullServer;
	private NomicOllamaEmbedder embedder;

	@BeforeEach
	void setUp() {
		RestClient.Builder embedBuilder = RestClient.builder().baseUrl(BASE_URL);
		embedServer = MockRestServiceServer.bindTo(embedBuilder).build();
		RestClient.Builder pullBuilder = RestClient.builder().baseUrl(BASE_URL);
		pullServer = MockRestServiceServer.bindTo(pullBuilder).build();
		embedder = new NomicOllamaEmbedder(embedBuilder.build(), pullBuilder.build(), MODEL);
	}

	@Test
	@DisplayName("ensureModel envía POST /api/pull con el modelo fijado y stream false solo por el cliente de pull")
	void ensureModelPullsWithoutStreaming() {
		pullServer.expect(requestTo(BASE_URL + "/api/pull"))
				.andExpect(method(HttpMethod.POST))
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(content().json("{\"model\": \"" + MODEL + "\", \"stream\": false}", JsonCompareMode.STRICT))
				.andRespond(withSuccess("{\"status\": \"success\"}", MediaType.APPLICATION_JSON));

		embedder.ensureModel();

		pullServer.verify();
		// Sin expectativas: falla si /api/pull hubiera llegado al cliente de embed.
		embedServer.verify();
	}

	@Test
	@DisplayName("AC-8: embed envía el modelo y \"clustering: <abstract>\" a /api/embed solo por el cliente de embed"
			+ " y devuelve embeddings[0]")
	void embedSendsClusteringPrefixAndReadsFirstEmbedding() {
		embedServer.expect(requestTo(BASE_URL + "/api/embed"))
				.andExpect(method(HttpMethod.POST))
				.andExpect(content().json(
						"{\"model\": \"" + MODEL + "\", \"input\": \"clustering: We study AI in education.\"}",
						JsonCompareMode.STRICT))
				.andRespond(withSuccess(embedResponse(768), MediaType.APPLICATION_JSON));

		float[] vector = embedder.embed("We study AI in education.");

		embedServer.verify();
		// Sin expectativas: falla si /api/embed hubiera llegado al cliente de pull.
		pullServer.verify();
		assertThat(vector).hasSize(768);
		assertThat(vector[0]).isEqualTo(0f);
		assertThat(vector[1]).isEqualTo(0.001f);
		assertThat(vector[767]).isEqualTo(0.767f);
	}

	@Test
	@DisplayName("Un vector con una dimensión distinta de 768 lanza IllegalStateException")
	void wrongDimensionIsRejected() {
		embedServer.expect(requestTo(BASE_URL + "/api/embed"))
				.andRespond(withSuccess(embedResponse(384), MediaType.APPLICATION_JSON));

		assertThatThrownBy(() -> embedder.embed("abstract"))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("384")
				.hasMessageContaining("768");
	}

	@Test
	@DisplayName("Una respuesta sin embeddings lanza IllegalStateException")
	void missingEmbeddingsAreRejected() {
		embedServer.expect(requestTo(BASE_URL + "/api/embed"))
				.andRespond(withSuccess("{\"model\": \"" + MODEL + "\", \"embeddings\": []}",
						MediaType.APPLICATION_JSON));

		assertThatThrownBy(() -> embedder.embed("abstract")).isInstanceOf(IllegalStateException.class);
	}

	/** Respuesta de Ollama con un solo vector cuyos componentes son i/1000. */
	private static String embedResponse(int dimension) {
		StringJoiner values = new StringJoiner(", ", "[", "]");
		for (int i = 0; i < dimension; i++) {
			values.add(Float.toString(i / 1000f));
		}
		return "{\"model\": \"" + MODEL + "\", \"embeddings\": [" + values + "], \"total_duration\": 1,"
				+ " \"load_duration\": 1, \"prompt_eval_count\": 8}";
	}
}
