package co.edu.uniquindio.bibliomtr.api.corpus;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import co.edu.uniquindio.bibliomtr.embeddings.EmbeddingModel;
import co.edu.uniquindio.bibliomtr.embeddings.NomicEmbedder;
import co.edu.uniquindio.bibliomtr.embeddings.Specter2Embedder;
import co.edu.uniquindio.bibliomtr.embeddings.Specter2Embedding;
import co.edu.uniquindio.bibliomtr.embeddings.Specter2OnnxEmbedder;
import co.edu.uniquindio.bibliomtr.pdf.ExtractedArticle;
import co.edu.uniquindio.bibliomtr.pdf.PdfArticleExtractor;
import co.edu.uniquindio.bibliomtr.persistence.article.Article;
import co.edu.uniquindio.bibliomtr.persistence.article.ArticleRepository;

/**
 * Ingesta los PDF del corpus en la tabla {@code article} (spec 001, REQ-8, REQ-9, REQ-9a, REQ-10; AC-8, AC-8a,
 * AC-9, AC-10, AC-11).
 *
 * <p><b>Qué hace con cada {@code *.pdf} del directorio.</b>
 * <ol>
 *   <li>Calcula el SHA-256 del contenido; si ya está en la base, lo salta (AC-11).</li>
 *   <li>Extrae título, autores y abstract con {@link PdfArticleExtractor}. Sin abstract, guarda el artículo como
 *       no seleccionable y deja un WARN (AC-9).</li>
 *   <li>Antes del primer embedding de la corrida llama a {@link NomicEmbedder#ensureModel()}. El pull queda
 *       marcado como hecho solo si termina sin excepción; si falla, ese PDF cuenta como fallido y el siguiente
 *       PDF con abstract de la misma corrida lo reintenta.</li>
 *   <li>Calcula los dos embeddings: SPECTER2 sobre {@code título [SEP] abstract} y nomic-embed-text sobre
 *       {@code clustering: <abstract>}. Si SPECTER2 truncó el texto, deja un WARN (REQ-9a).</li>
 *   <li>Si algo falla, no guarda el artículo y deja un ERROR con el archivo y la causa. Como su SHA-256 no quedó
 *       en la base, se reintenta en el siguiente arranque.</li>
 * </ol>
 * Al final deja un INFO con el {@link IngestionReport}.
 *
 * <p><b>Por qué cada artículo se guarda por separado.</b> Un PDF que falla no debe impedir que se guarden los
 * demás, así que no hay una transacción para toda la corrida: cada {@code save} es su propia transacción.
 *
 * <p>Complejidad: O(P · (B + c + E)) en tiempo, con P = número de PDF, B = bytes de cada PDF (SHA-256 y
 * lectura), c = caracteres de sus dos primeras páginas y E = costo de los dos embeddings; O(B + c + d) en
 * espacio por PDF, con d = 768.
 */
@Service
public class CorpusIngestionService {

	private static final Logger LOG = LoggerFactory.getLogger(CorpusIngestionService.class);

	private static final String PDF_EXTENSION = ".pdf";

	private final ArticleRepository repository;
	private final Specter2Embedder specter2Embedder;
	private final NomicEmbedder nomicEmbedder;
	private final PdfArticleExtractor extractor = new PdfArticleExtractor();

	/**
	 * Servicio de ingesta sobre el repositorio y los dos generadores de embeddings.
	 *
	 * <p>Complejidad: O(1) en tiempo y espacio.
	 *
	 * @param repository       artículos guardados
	 * @param specter2Embedder generador de SPECTER2
	 * @param nomicEmbedder    generador de nomic-embed-text
	 */
	public CorpusIngestionService(ArticleRepository repository, Specter2Embedder specter2Embedder,
			NomicEmbedder nomicEmbedder) {
		this.repository = repository;
		this.specter2Embedder = specter2Embedder;
		this.nomicEmbedder = nomicEmbedder;
	}

	/**
	 * Ingesta los PDF de {@code corpusDir} que aún no estén en la base.
	 *
	 * <p>Complejidad: la de la clase, O(P · (B + c + E)) en tiempo.
	 *
	 * @param corpusDir directorio del corpus ({@code bibliomtr.corpus.dir}); puede no existir
	 * @return cuántos PDF se guardaron, se saltaron, quedaron sin abstract o fallaron
	 */
	public IngestionReport ingest(Path corpusDir) {
		Objects.requireNonNull(corpusDir, "corpusDir");
		List<Path> pdfs = listPdfs(corpusDir);
		if (pdfs.isEmpty()) {
			LOG.info("Corpus vacío o inexistente: {}", corpusDir.toAbsolutePath());
			return IngestionReport.EMPTY;
		}

		Run run = new Run();
		for (Path pdf : pdfs) {
			ingestOne(pdf, run);
		}
		IngestionReport report = new IngestionReport(run.ingested, run.alreadyPresent, run.withoutAbstract,
				run.failed);
		LOG.info("Ingesta del corpus terminada: {}", report);
		return report;
	}

	/**
	 * Ingesta un PDF y actualiza los contadores de la corrida. Ningún error se propaga: se registra y el PDF
	 * cuenta como fallido.
	 *
	 * <p>Complejidad: O(B + c + E) en tiempo y O(B + c + d) en espacio.
	 */
	private void ingestOne(Path pdf, Run run) {
		String fileName = pdf.getFileName().toString();
		try {
			String sha256 = sha256(Files.readAllBytes(pdf));
			if (repository.existsByFileSha256(sha256)) {
				run.alreadyPresent++;
				return;
			}

			ExtractedArticle extracted = extractor.extract(pdf);
			Instant now = Instant.now();
			if (!extracted.hasAbstract()) {
				repository.save(Article.withoutAbstract(fileName, sha256, extracted.title(), extracted.authors(),
						now));
				LOG.warn("Sin abstract detectado: {}", fileName);
				run.withoutAbstract++;
				return;
			}

			// Descarga o verificación del modelo de Ollama antes del primer embedding de la corrida. Se marca
			// como hecha solo si ensureModel() termina sin excepción: si falla, la excepción deja este PDF como
			// fallido y el siguiente PDF con abstract vuelve a intentarlo.
			if (!run.nomicModelReady) {
				nomicEmbedder.ensureModel();
				run.nomicModelReady = true;
			}
			Specter2Embedding specter2 = specter2Embedder.embed(extracted.title(), extracted.abstractText());
			float[] specter2Vector = requireDimension(specter2.vector(), EmbeddingModel.SPECTER2);
			float[] nomicVector = requireDimension(nomicEmbedder.embed(extracted.abstractText()),
					EmbeddingModel.NOMIC);

			repository.save(Article.withAbstract(fileName, sha256, extracted.title(), extracted.authors(),
					extracted.abstractText(), specter2Vector, specter2.truncated(), specter2.tokenCount(),
					nomicVector, now));
			if (specter2.truncated()) {
				LOG.warn("{}: texto de SPECTER2 truncado a {} tokens ({} tokens)", fileName,
						Specter2OnnxEmbedder.MAX_TOKENS, specter2.tokenCount());
			}
			run.ingested++;
		} catch (IOException | RuntimeException exception) {
			// Sin fila en la base: el SHA-256 no queda registrado y el PDF se reintenta al reiniciar.
			LOG.error("No se pudo ingerir {}: {}", fileName, cause(exception), exception);
			run.failed++;
		}
	}

	/**
	 * Archivos regulares de {@code corpusDir} con extensión {@code .pdf} (sin distinguir mayúsculas), en el orden
	 * en que los devuelve el sistema de archivos. El orden de la lista de seleccionables no depende de este, sino
	 * de {@code ORDER BY file_name}.
	 *
	 * <p>Complejidad: O(F) en tiempo y espacio, con F = número de entradas del directorio.
	 *
	 * @return los PDF; vacía si el directorio no existe, no es un directorio o no se puede leer
	 */
	private static List<Path> listPdfs(Path corpusDir) {
		List<Path> pdfs = new ArrayList<>();
		if (!Files.isDirectory(corpusDir)) {
			return pdfs;
		}
		try (DirectoryStream<Path> entries = Files.newDirectoryStream(corpusDir)) {
			for (Path entry : entries) {
				String name = entry.getFileName().toString().toLowerCase(Locale.ROOT);
				if (name.endsWith(PDF_EXTENSION) && Files.isRegularFile(entry)) {
					pdfs.add(entry);
				}
			}
		} catch (IOException exception) {
			LOG.error("No se pudo leer el directorio del corpus {}: {}", corpusDir.toAbsolutePath(),
					cause(exception), exception);
			return new ArrayList<>();
		}
		return pdfs;
	}

	/**
	 * SHA-256 del contenido en hexadecimal en minúscula (64 caracteres). Identifica el PDF aunque cambie de
	 * nombre.
	 *
	 * <p>Complejidad: O(B) en tiempo y O(1) en espacio adicional, con B = número de bytes.
	 */
	static String sha256(byte[] content) {
		try {
			return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
		} catch (NoSuchAlgorithmException exception) {
			// Toda JVM está obligada a ofrecer SHA-256 (java.security.MessageDigest).
			throw new IllegalStateException("La JVM no ofrece SHA-256", exception);
		}
	}

	/**
	 * Comprueba que el vector tenga la dimensión del modelo antes de guardarlo en {@code vector(768)}.
	 *
	 * <p>Complejidad: O(1) en tiempo y espacio.
	 *
	 * @throws IllegalStateException si el vector es nulo o tiene otra dimensión
	 */
	private static float[] requireDimension(float[] vector, EmbeddingModel model) {
		if (vector == null || vector.length != model.dimension()) {
			throw new IllegalStateException("El embedding de %s tiene %s dimensiones; se esperaban %d".formatted(
					model.modelName(), vector == null ? "0" : String.valueOf(vector.length), model.dimension()));
		}
		return vector;
	}

	/**
	 * Causa legible de un error: su mensaje o, si no tiene, el nombre de su clase.
	 *
	 * <p>Complejidad: O(1) en tiempo y espacio.
	 */
	private static String cause(Exception exception) {
		String message = exception.getMessage();
		return message == null || message.isBlank() ? exception.getClass().getSimpleName() : message;
	}

	/** Contadores de una corrida y si el modelo de Ollama ya quedó disponible. Complejidad: O(1). */
	private static final class Run {
		private int ingested;
		private int alreadyPresent;
		private int withoutAbstract;
		private int failed;
		private boolean nomicModelReady;
	}

}
