package co.edu.uniquindio.bibliomtr.config;

import co.edu.uniquindio.bibliomtr.preprocessing.EditGranularity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;
import org.springframework.validation.annotation.Validated;

import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;

/**
 * Propiedades {@code bibliomtr.*} de la aplicación, validadas al arrancar.
 *
 * @param corpus     ubicación de los 20 PDF del corpus (constitución P5)
 * @param embeddings ubicación del modelo ONNX de SPECTER2 y su tokenizer (spec 001, REQ-9)
 * @param benchmark  jar de JMH y límite de tiempo de cada medición (spec 002, REQ-17, REQ-21)
 * @param ingestion  activación de la ingesta del corpus al arrancar (spec 001, REQ-8)
 * @param ollama     servidor de Ollama para nomic-embed-text (spec 001, REQ-9)
 * @param similarity granularidad de las distancias de edición (spec 001, REQ-3)
 */
@Validated
@ConfigurationProperties(prefix = "bibliomtr")
public record BibliomtrProperties(@NotNull Corpus corpus, @NotNull Embeddings embeddings,
		@NotNull Benchmark benchmark, @NotNull Ingestion ingestion, @NotNull Ollama ollama,
		@NotNull Similarity similarity) {

	/**
	 * @param dir directorio con los PDF del corpus
	 */
	public record Corpus(@NotNull Path dir) {
	}

	/**
	 * @param modelPath     archivo {@code model.onnx} de SPECTER2 base ({@code scripts/download-model.sh})
	 * @param tokenizerPath archivo {@code tokenizer.json} de HuggingFace de SPECTER2
	 */
	public record Embeddings(@NotNull Resource modelPath, @NotNull Resource tokenizerPath) {
	}

	/**
	 * Medición JMH del Requerimiento 2, que corre como proceso hijo {@code java -jar <jar>}.
	 *
	 * @param jar     {@code benchmarks.jar} generado por {@code ./gradlew jmhJar}
	 * @param timeout límite de una medición; al superarlo queda FALLIDA (REQ-21: 10 minutos)
	 */
	public record Benchmark(@NotNull Path jar, @NotNull Duration timeout) {
	}

	/**
	 * Ingesta del corpus al arrancar (spec 001, REQ-8). Los tests la desactivan para no leer
	 * {@code corpus/} ni cargar modelos.
	 *
	 * @param enabled {@code true} para ingerir los PDF de {@code bibliomtr.corpus.dir} en
	 *                {@code ApplicationReadyEvent}
	 */
	public record Ingestion(boolean enabled) {
	}

	/**
	 * Servidor local de Ollama que genera el embedding de nomic-embed-text (spec 001, REQ-9).
	 * Solo devuelve el vector; la métrica de similitud se implementa a mano (constitución P3).
	 *
	 * @param baseUrl     URL base de Ollama, p. ej. {@code http://localhost:11434}
	 * @param model       modelo con versión fijada (constitución P12), p. ej. {@code nomic-embed-text:v1.5}
	 * @param timeout     límite de conexión de las peticiones a Ollama y de lectura de {@code POST /api/embed}
	 * @param pullTimeout límite de lectura de {@code POST /api/pull}, que puede tardar minutos mientras Ollama
	 *                    descarga el modelo (~274 MB); p. ej. {@code 15m}
	 */
	public record Ollama(@NotNull URI baseUrl, @NotBlank String model, @NotNull Duration timeout,
			@NotNull Duration pullTimeout) {
	}

	/**
	 * Parámetros de la comparación de similitud textual (spec 001).
	 *
	 * @param editGranularity símbolos de Levenshtein y Needleman-Wunsch: palabras o caracteres
	 *                        (REQ-3, decisión de los estudiantes)
	 */
	public record Similarity(@NotNull EditGranularity editGranularity) {
	}
}
