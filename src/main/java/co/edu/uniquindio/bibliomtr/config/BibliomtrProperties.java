package co.edu.uniquindio.bibliomtr.config;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;
import org.springframework.validation.annotation.Validated;

import java.nio.file.Path;

/**
 * Propiedades {@code bibliomtr.*} de la aplicación, validadas al arrancar.
 *
 * @param corpus     ubicación de los 20 PDF del corpus (constitución P5)
 * @param embeddings ubicación del modelo ONNX y su tokenizer
 */
@Validated
@ConfigurationProperties(prefix = "bibliomtr")
public record BibliomtrProperties(@NotNull Corpus corpus, @NotNull Embeddings embeddings) {

	/**
	 * @param dir directorio con los PDF del corpus
	 */
	public record Corpus(@NotNull Path dir) {
	}

	/**
	 * @param modelPath     archivo {@code model.onnx} del modelo sentence-transformers
	 * @param tokenizerPath archivo {@code tokenizer.json} de HuggingFace
	 */
	public record Embeddings(@NotNull Resource modelPath, @NotNull Resource tokenizerPath) {
	}
}
