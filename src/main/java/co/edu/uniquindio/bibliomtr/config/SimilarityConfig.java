package co.edu.uniquindio.bibliomtr.config;

import java.net.http.HttpClient;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import co.edu.uniquindio.bibliomtr.algorithms.similarity.CosineSimilarity;
import co.edu.uniquindio.bibliomtr.algorithms.similarity.EuclideanDistance;
import co.edu.uniquindio.bibliomtr.algorithms.similarity.Jaccard;
import co.edu.uniquindio.bibliomtr.algorithms.similarity.Levenshtein;
import co.edu.uniquindio.bibliomtr.algorithms.similarity.NeedlemanWunsch;
import co.edu.uniquindio.bibliomtr.algorithms.similarity.NeedlemanWunschScoring;
import co.edu.uniquindio.bibliomtr.algorithms.similarity.TfIdfCosineSimilarity;
import co.edu.uniquindio.bibliomtr.api.similarity.SimilarityCatalog;
import co.edu.uniquindio.bibliomtr.embeddings.NomicEmbedder;
import co.edu.uniquindio.bibliomtr.embeddings.NomicOllamaEmbedder;
import co.edu.uniquindio.bibliomtr.embeddings.Specter2Embedder;
import co.edu.uniquindio.bibliomtr.embeddings.Specter2OnnxEmbedder;

/**
 * Infraestructura del Requerimiento 1, similitud textual (spec 001): cliente de Ollama, generadores de
 * embeddings y catálogo de los 6 algoritmos de similitud.
 *
 * <p><b>Por qué aquí.</b> Los módulos {@code embeddings} y {@code algorithms} no conocen la configuración de
 * Spring; esta clase les pasa las propiedades {@code bibliomtr.*}. Los tests reemplazan los embedders y el
 * catálogo por fakes {@code @Primary} sin modelo, red ni algoritmos implementados.
 */
@Configuration(proxyBeanMethods = false)
public class SimilarityConfig {

	/**
	 * Cliente HTTP de Ollama para {@code POST /api/embed}, con la URL base {@code bibliomtr.ollama.base-url} y el
	 * límite {@code bibliomtr.ollama.timeout} para conectar y para esperar la respuesta (REQ-9).
	 *
	 * <p>Complejidad: O(1) en tiempo y espacio.
	 *
	 * @param properties propiedades {@code bibliomtr.*}
	 * @return el cliente de Ollama
	 */
	@Bean
	public RestClient ollamaRestClient(BibliomtrProperties properties) {
		BibliomtrProperties.Ollama ollama = properties.ollama();
		return ollamaClient(ollama, ollama.timeout());
	}

	/**
	 * Cliente HTTP de Ollama usado solo para {@code POST /api/pull}: misma URL base y límite de conexión
	 * {@code bibliomtr.ollama.timeout}, pero límite de lectura {@code bibliomtr.ollama.pull-timeout} (REQ-9).
	 *
	 * <p><b>Por qué.</b> Ollama responde al pull cuando termina de descargar nomic-embed-text (~274 MB), lo que
	 * supera los 60 s de {@code bibliomtr.ollama.timeout}. Subir ese límite para todo retrasaría la detección de un
	 * {@code /api/embed} colgado, así que el pull tiene su propio cliente.
	 *
	 * <p>Complejidad: O(1) en tiempo y espacio.
	 *
	 * @param properties propiedades {@code bibliomtr.*}
	 * @return el cliente de pull de Ollama
	 */
	@Bean
	public RestClient ollamaPullRestClient(BibliomtrProperties properties) {
		BibliomtrProperties.Ollama ollama = properties.ollama();
		return ollamaClient(ollama, ollama.pullTimeout());
	}

	/**
	 * Cliente con la URL base de Ollama, conexión limitada por {@code bibliomtr.ollama.timeout} y lectura
	 * limitada por {@code readTimeout}.
	 *
	 * <p>Complejidad: O(1) en tiempo y espacio.
	 */
	private static RestClient ollamaClient(BibliomtrProperties.Ollama ollama, Duration readTimeout) {
		HttpClient httpClient = HttpClient.newBuilder().connectTimeout(ollama.timeout()).build();
		JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
		requestFactory.setReadTimeout(readTimeout);
		return RestClient.builder()
				.baseUrl(ollama.baseUrl().toString())
				.requestFactory(requestFactory)
				.build();
	}

	/**
	 * nomic-embed-text vía Ollama con el modelo {@code bibliomtr.ollama.model} (REQ-9).
	 *
	 * <p>Complejidad: O(1) en tiempo y espacio.
	 *
	 * @param ollamaRestClient     cliente de {@link #ollamaRestClient}, para {@code /api/embed}
	 * @param ollamaPullRestClient cliente de {@link #ollamaPullRestClient}, solo para {@code /api/pull}
	 * @param properties           propiedades {@code bibliomtr.*}
	 * @return el embedder de nomic-embed-text
	 */
	@Bean
	public NomicEmbedder nomicEmbedder(@Qualifier("ollamaRestClient") RestClient ollamaRestClient,
			@Qualifier("ollamaPullRestClient") RestClient ollamaPullRestClient, BibliomtrProperties properties) {
		return new NomicOllamaEmbedder(ollamaRestClient, ollamaPullRestClient, properties.ollama().model());
	}

	/**
	 * SPECTER2 con el modelo y el tokenizer de {@code bibliomtr.embeddings.*} (REQ-9, REQ-9a). La carga es
	 * perezosa, así que crear el bean no lee el modelo; Spring lo cierra con el contexto.
	 *
	 * <p>Complejidad: O(1) en tiempo y espacio.
	 *
	 * @param properties propiedades {@code bibliomtr.*}
	 * @return el embedder de SPECTER2
	 */
	@Bean
	public Specter2Embedder specter2Embedder(BibliomtrProperties properties) {
		return new Specter2OnnxEmbedder(properties.embeddings().modelPath(), properties.embeddings().tokenizerPath());
	}

	/**
	 * Catálogo con los 6 stubs de {@code algorithms/similarity} que escriben los estudiantes (REQ-14).
	 * Needleman-Wunsch usa el esquema fijo {@link NeedlemanWunschScoring#DEFAULT} = (1, −1, −1), porque los
	 * valores esperados de sus tests dependen de él.
	 *
	 * <p>Complejidad: O(1) en tiempo y espacio.
	 *
	 * @return el catálogo de producción
	 */
	@Bean
	public SimilarityCatalog similarityCatalog() {
		return new SimilarityCatalog(new Levenshtein(), new NeedlemanWunsch(NeedlemanWunschScoring.DEFAULT),
				new TfIdfCosineSimilarity(), new Jaccard(), new CosineSimilarity(), new EuclideanDistance());
	}
}
