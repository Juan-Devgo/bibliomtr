package co.edu.uniquindio.bibliomtr.persistence.article;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.Array;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Un artículo del corpus guardado en {@code article} (spec 001, REQ-8, REQ-9, REQ-9a, REQ-10).
 *
 * <p><b>Qué guarda.</b> Nombre y SHA-256 del PDF, título, autores, abstract (o su ausencia), los dos embeddings
 * de 768 dimensiones (SPECTER2 y nomic-embed-text), si el texto de SPECTER2 se truncó y el instante de la
 * ingesta.
 *
 * <p><b>Por qué dos fábricas.</b> La tabla exige que {@code abstract_detected} coincida con que haya abstract y
 * que todo artículo con abstract tenga los dos vectores. {@link #withAbstract} y {@link #withoutAbstract} solo
 * construyen esos dos estados válidos, así un artículo a medias no llega a la base.
 *
 * <p><b>Solo almacenamiento.</b> Los vectores se mapean a {@code vector(768)} de pgvector, pero nunca se usan
 * sus operadores: la similitud se calcula en Java (AGENTS.md, constitución P3).
 */
@Entity
@Table(name = "article")
public class Article {

	/** Dimensión de las dos columnas de embeddings. */
	public static final int EMBEDDING_DIMENSION = 768;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "file_name", nullable = false, columnDefinition = "text")
	private String fileName;

	@Column(name = "file_sha256", nullable = false, unique = true, length = 64)
	private String fileSha256;

	@Column(name = "title", nullable = false, columnDefinition = "text")
	private String title;

	@Column(name = "authors", nullable = false, columnDefinition = "text[]")
	private String[] authors;

	@Column(name = "abstract_text", columnDefinition = "text")
	private String abstractText;

	@Column(name = "abstract_detected", nullable = false)
	private boolean abstractDetected;

	@JdbcTypeCode(SqlTypes.VECTOR)
	@Array(length = EMBEDDING_DIMENSION)
	@Column(name = "specter2_embedding")
	private float[] specter2Embedding;

	@Column(name = "specter2_truncated", nullable = false)
	private boolean specter2Truncated;

	@Column(name = "specter2_token_count")
	private Integer specter2TokenCount;

	@JdbcTypeCode(SqlTypes.VECTOR)
	@Array(length = EMBEDDING_DIMENSION)
	@Column(name = "nomic_embedding")
	private float[] nomicEmbedding;

	@Column(name = "ingested_at", nullable = false)
	private Instant ingestedAt;

	/** Constructor que exige JPA. */
	protected Article() {
	}

	/**
	 * Crea un artículo seleccionable, con abstract y sus dos embeddings.
	 *
	 * <p>Complejidad: O(a + d) en tiempo y espacio, con a = número de autores y d = 768 (copias defensivas).
	 *
	 * @param fileName           nombre del PDF, sin directorio
	 * @param fileSha256         SHA-256 del contenido del PDF en hexadecimal (64 caracteres)
	 * @param title              título del artículo
	 * @param authors            autores en orden; puede estar vacía
	 * @param abstractText       abstract detectado; no nulo
	 * @param specter2Embedding  vector de SPECTER2 de {@value #EMBEDDING_DIMENSION} dimensiones
	 * @param specter2Truncated  {@code true} si el texto de SPECTER2 superó 512 tokens
	 * @param specter2TokenCount tokens del texto de SPECTER2 antes de truncar
	 * @param nomicEmbedding     vector de nomic-embed-text de {@value #EMBEDDING_DIMENSION} dimensiones
	 * @param ingestedAt         instante de la ingesta
	 * @return el artículo, sin id hasta que se guarde
	 * @throws IllegalArgumentException si un vector no tiene {@value #EMBEDDING_DIMENSION} dimensiones
	 */
	public static Article withAbstract(String fileName, String fileSha256, String title, List<String> authors,
			String abstractText, float[] specter2Embedding, boolean specter2Truncated, int specter2TokenCount,
			float[] nomicEmbedding, Instant ingestedAt) {
		Article article = base(fileName, fileSha256, title, authors, ingestedAt);
		article.abstractText = Objects.requireNonNull(abstractText, "abstractText");
		article.abstractDetected = true;
		article.specter2Embedding = requireDimension(specter2Embedding, "SPECTER2");
		article.specter2Truncated = specter2Truncated;
		article.specter2TokenCount = specter2TokenCount;
		article.nomicEmbedding = requireDimension(nomicEmbedding, "nomic-embed-text");
		return article;
	}

	/**
	 * Crea un artículo sin abstract detectado: queda guardado para no reprocesarlo, pero no es seleccionable
	 * (REQ-10) y no tiene embeddings.
	 *
	 * <p>Complejidad: O(a) en tiempo y espacio, con a = número de autores.
	 *
	 * @param fileName   nombre del PDF, sin directorio
	 * @param fileSha256 SHA-256 del contenido del PDF en hexadecimal (64 caracteres)
	 * @param title      título del artículo
	 * @param authors    autores en orden; puede estar vacía
	 * @param ingestedAt instante de la ingesta
	 * @return el artículo, sin id hasta que se guarde
	 */
	public static Article withoutAbstract(String fileName, String fileSha256, String title, List<String> authors,
			Instant ingestedAt) {
		Article article = base(fileName, fileSha256, title, authors, ingestedAt);
		article.abstractDetected = false;
		article.specter2Truncated = false;
		return article;
	}

	/**
	 * Campos comunes a los dos estados.
	 *
	 * <p>Complejidad: O(a) en tiempo y espacio, con a = número de autores.
	 */
	private static Article base(String fileName, String fileSha256, String title, List<String> authors,
			Instant ingestedAt) {
		Article article = new Article();
		article.fileName = Objects.requireNonNull(fileName, "fileName");
		article.fileSha256 = Objects.requireNonNull(fileSha256, "fileSha256");
		article.title = Objects.requireNonNull(title, "title");
		article.authors = Objects.requireNonNull(authors, "authors").toArray(String[]::new);
		article.ingestedAt = Objects.requireNonNull(ingestedAt, "ingestedAt");
		return article;
	}

	/**
	 * Copia el vector tras comprobar su dimensión, porque la columna {@code vector(768)} rechazaría otra.
	 *
	 * <p>Complejidad: O(d) en tiempo y espacio, con d = 768.
	 */
	private static float[] requireDimension(float[] vector, String model) {
		Objects.requireNonNull(vector, model);
		if (vector.length != EMBEDDING_DIMENSION) {
			throw new IllegalArgumentException("El vector de %s tiene %d dimensiones; se esperaban %d"
					.formatted(model, vector.length, EMBEDDING_DIMENSION));
		}
		return vector.clone();
	}

	/**
	 * Identificador asignado por la base.
	 *
	 * <p>Complejidad: O(1) en tiempo y espacio.
	 *
	 * @return el id, o {@code null} si aún no se guardó
	 */
	public Long getId() {
		return id;
	}

	/**
	 * Nombre del PDF, sin directorio; da el orden de la lista de seleccionables.
	 *
	 * <p>Complejidad: O(1) en tiempo y espacio.
	 *
	 * @return p. ej. {@code "3. Myths.pdf"}
	 */
	public String getFileName() {
		return fileName;
	}

	/**
	 * SHA-256 del contenido del PDF, que evita duplicarlo al reiniciar (AC-11).
	 *
	 * <p>Complejidad: O(1) en tiempo y espacio.
	 *
	 * @return 64 caracteres hexadecimales en minúscula
	 */
	public String getFileSha256() {
		return fileSha256;
	}

	/**
	 * Título del artículo.
	 *
	 * <p>Complejidad: O(1) en tiempo y espacio.
	 *
	 * @return el título
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * Autores en el orden de los metadatos del PDF.
	 *
	 * <p>Complejidad: O(a) en tiempo y espacio, con a = número de autores (copia inmutable).
	 *
	 * @return los autores; vacía si no hay
	 */
	public List<String> getAuthors() {
		return List.copyOf(Arrays.asList(authors));
	}

	/**
	 * Abstract detectado.
	 *
	 * <p>Complejidad: O(1) en tiempo y espacio.
	 *
	 * @return el abstract, o {@code null} si no se detectó
	 */
	public String getAbstractText() {
		return abstractText;
	}

	/**
	 * Indica si se detectó el abstract, es decir, si el artículo es seleccionable (REQ-10).
	 *
	 * <p>Complejidad: O(1) en tiempo y espacio.
	 *
	 * @return {@code true} si hay abstract
	 */
	public boolean isAbstractDetected() {
		return abstractDetected;
	}

	/**
	 * Vector de SPECTER2.
	 *
	 * <p>Complejidad: O(d) en tiempo y espacio, con d = 768 (copia defensiva).
	 *
	 * @return copia del vector, o {@code null} si no hay abstract
	 */
	public float[] getSpecter2Embedding() {
		return specter2Embedding == null ? null : specter2Embedding.clone();
	}

	/**
	 * Indica si el texto de SPECTER2 superó 512 tokens y se truncó (REQ-9a).
	 *
	 * <p>Complejidad: O(1) en tiempo y espacio.
	 *
	 * @return {@code true} si se truncó
	 */
	public boolean isSpecter2Truncated() {
		return specter2Truncated;
	}

	/**
	 * Tokens del texto de SPECTER2 antes de truncar, contando [CLS] y [SEP].
	 *
	 * <p>Complejidad: O(1) en tiempo y espacio.
	 *
	 * @return el número de tokens, o {@code null} si no hay abstract
	 */
	public Integer getSpecter2TokenCount() {
		return specter2TokenCount;
	}

	/**
	 * Vector de nomic-embed-text.
	 *
	 * <p>Complejidad: O(d) en tiempo y espacio, con d = 768 (copia defensiva).
	 *
	 * @return copia del vector, o {@code null} si no hay abstract
	 */
	public float[] getNomicEmbedding() {
		return nomicEmbedding == null ? null : nomicEmbedding.clone();
	}

	/**
	 * Instante de la ingesta.
	 *
	 * <p>Complejidad: O(1) en tiempo y espacio.
	 *
	 * @return el instante
	 */
	public Instant getIngestedAt() {
		return ingestedAt;
	}

}
