package co.edu.uniquindio.bibliomtr.embeddings;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

/**
 * nomic-embed-text servido por Ollama local, consumido por HTTP (spec 001, REQ-9 y AC-8).
 *
 * <p><b>Qué hace.</b>
 * <ul>
 *   <li>{@link #ensureModel()}: {@code POST /api/pull} con {@code {"model": "<model>", "stream": false}} por el
 *       cliente de pull; Ollama responde cuando el modelo ya está descargado.</li>
 *   <li>{@link #embed(String)}: {@code POST /api/embed}, por el cliente de embed, con
 *       {@code {"model": "<model>", "input": "clustering: <abstract>"}} y lee {@code embeddings[0]} de la
 *       respuesta.</li>
 * </ul>
 *
 * <p><b>Por qué.</b> Ollama solo devuelve el vector; la métrica de similitud se implementa a mano (constitución
 * P3). El prefijo {@code clustering: } es la tarea que nomic-embed-text define para agrupar textos por tema
 * (Clarifications 2026-10-23). El modelo lleva versión fijada en {@code bibliomtr.ollama.model} (P12) y la
 * dimensión se valida para no guardar vectores de otro modelo en la columna {@code vector(768)}.
 *
 * <p><b>Por qué dos clientes.</b> Bajar nomic-embed-text (~274 MB) con {@code /api/pull} tarda más que el límite
 * de lectura de {@code bibliomtr.ollama.timeout} (60 s), así que el pull va por un cliente propio con
 * {@code bibliomtr.ollama.pull-timeout} (15 min). {@code /api/embed} conserva el límite corto, para detectar
 * pronto un Ollama colgado.
 *
 * <p>Las instancias son seguras para uso concurrente ({@link RestClient} lo es).
 */
public final class NomicOllamaEmbedder implements NomicEmbedder {

	/** Prefijo de tarea de nomic-embed-text antepuesto al abstract. */
	public static final String CLUSTERING_PREFIX = "clustering: ";

	private final RestClient restClient;
	private final RestClient pullRestClient;
	private final String model;

	/**
	 * Crea el cliente.
	 *
	 * <p>Complejidad: O(1) en tiempo y espacio.
	 *
	 * @param restClient     cliente HTTP de {@code /api/embed}, con la URL base de Ollama y
	 *                       {@code bibliomtr.ollama.timeout}; no nulo
	 * @param pullRestClient cliente HTTP usado solo para {@code /api/pull}, con la URL base de Ollama y
	 *                       {@code bibliomtr.ollama.pull-timeout} como límite de lectura; no nulo
	 * @param model          modelo con versión fijada, p. ej. {@code nomic-embed-text:v1.5}; no nulo
	 */
	public NomicOllamaEmbedder(RestClient restClient, RestClient pullRestClient, String model) {
		this.restClient = Objects.requireNonNull(restClient, "restClient");
		this.pullRestClient = Objects.requireNonNull(pullRestClient, "pullRestClient");
		this.model = Objects.requireNonNull(model, "model");
	}

	/**
	 * {@inheritDoc}
	 *
	 * @throws org.springframework.web.client.RestClientException si Ollama no responde o responde con error
	 */
	@Override
	public void ensureModel() {
		// Cliente de pull: la descarga del modelo puede tardar minutos (bibliomtr.ollama.pull-timeout).
		pullRestClient.post()
				.uri("/api/pull")
				.contentType(MediaType.APPLICATION_JSON)
				.body(new PullRequest(model, false))
				.retrieve()
				.toBodilessEntity();
	}

	/**
	 * {@inheritDoc}
	 *
	 * @throws org.springframework.web.client.RestClientException si Ollama no responde o responde con error
	 * @throws IllegalStateException si la respuesta no trae un vector de {@link EmbeddingModel#NOMIC}
	 *                               dimensiones
	 */
	@Override
	public float[] embed(String abstractText) {
		Objects.requireNonNull(abstractText, "abstractText");
		EmbedResponse response = restClient.post()
				.uri("/api/embed")
				.contentType(MediaType.APPLICATION_JSON)
				.body(new EmbedRequest(model, CLUSTERING_PREFIX + abstractText))
				.retrieve()
				.body(EmbedResponse.class);
		if (response == null || response.embeddings() == null || response.embeddings().length == 0
				|| response.embeddings()[0] == null) {
			throw new IllegalStateException("Ollama no devolvió ningún embedding para " + model);
		}
		// Una sola entrada en "input" → un solo vector en embeddings[0].
		float[] vector = response.embeddings()[0];
		int expected = EmbeddingModel.NOMIC.dimension();
		if (vector.length != expected) {
			throw new IllegalStateException(
					model + " devolvió " + vector.length + " dimensiones; se esperaban " + expected);
		}
		return vector;
	}

	/** Cuerpo de {@code POST /api/pull}. */
	record PullRequest(String model, boolean stream) {
	}

	/** Cuerpo de {@code POST /api/embed} con una sola entrada. */
	record EmbedRequest(String model, String input) {
	}

	/**
	 * Respuesta de {@code POST /api/embed}; solo interesa {@code embeddings}. Se ignoran los demás campos
	 * ({@code total_duration}, {@code load_duration}, {@code prompt_eval_count}).
	 */
	@JsonIgnoreProperties(ignoreUnknown = true)
	record EmbedResponse(String model, float[][] embeddings) {
	}
}
