package co.edu.uniquindio.bibliomtr.api.similarity;

import java.util.StringJoiner;

/**
 * Los 8 resultados de una comparación del Requerimiento 1 (spec 001, REQ-14, REQ-20): los 4 algoritmos clásicos
 * y las 2 métricas sobre los embeddings de cada uno de los 2 modelos.
 *
 * <p><b>Por qué.</b> Centraliza el slug de la API, la etiqueta que muestra la interfaz y el stub que implementa
 * cada resultado, para que la respuesta, el aviso "Pendiente de implementar: &lt;archivo&gt;#&lt;método&gt;"
 * (REQ-21) y la guía no diverjan. El orden de declaración es el orden de la respuesta.
 *
 * <p>Las dos métricas sobre embeddings comparten stub: {@code specter2-cosine} y {@code nomic-cosine} usan
 * {@code CosineSimilarity}, y {@code specter2-euclidean} y {@code nomic-euclidean} usan
 * {@code EuclideanDistance}; solo cambia el vector de entrada.
 */
public enum ComparisonAlgorithm {

	/** Distancia de Levenshtein sobre las secuencias de edición. */
	LEVENSHTEIN("levenshtein", "Levenshtein", "Levenshtein"),

	/** Alineamiento global de Needleman-Wunsch sobre las secuencias de edición. */
	NEEDLEMAN_WUNSCH("needleman-wunsch", "Needleman-Wunsch", "NeedlemanWunsch"),

	/** Coseno entre los vectores TF-IDF de los tokens, con IDF sobre todos los seleccionables. */
	TFIDF_COSINE("tfidf-cosine", "Coseno TF-IDF", "TfIdfCosineSimilarity"),

	/** Índice de Jaccard entre los conjuntos de tokens. */
	JACCARD("jaccard", "Jaccard", "Jaccard"),

	/** Similitud coseno entre los embeddings de SPECTER2. */
	SPECTER2_COSINE("specter2-cosine", "SPECTER2 (coseno)", "CosineSimilarity"),

	/** Distancia euclidiana entre los embeddings de SPECTER2. */
	SPECTER2_EUCLIDEAN("specter2-euclidean", "SPECTER2 (euclidiana)", "EuclideanDistance"),

	/** Similitud coseno entre los embeddings de nomic-embed-text. */
	NOMIC_COSINE("nomic-cosine", "nomic-embed-text (coseno)", "CosineSimilarity"),

	/** Distancia euclidiana entre los embeddings de nomic-embed-text. */
	NOMIC_EUCLIDEAN("nomic-euclidean", "nomic-embed-text (euclidiana)", "EuclideanDistance");

	/** Directorio de los stubs, relativo a la raíz del repositorio. */
	static final String STUB_DIRECTORY = "src/main/java/co/edu/uniquindio/bibliomtr/algorithms/similarity/";

	/** Método que implementa el estudiante en cada stub. */
	static final String STUB_METHOD = "compare";

	private final String slug;
	private final String label;
	private final String stubClass;

	ComparisonAlgorithm(String slug, String label, String stubClass) {
		this.slug = slug;
		this.label = label;
		this.stubClass = stubClass;
	}

	/**
	 * Nombre del resultado en la API ({@code algorithm} de la respuesta y parámetro de
	 * {@code GET /api/similarity/trace}).
	 *
	 * <p>Complejidad: O(1) en tiempo y espacio.
	 *
	 * @return slug en minúsculas, p. ej. {@code needleman-wunsch}
	 */
	public String slug() {
		return slug;
	}

	/**
	 * Nombre legible que muestra la interfaz.
	 *
	 * <p>Complejidad: O(1) en tiempo y espacio.
	 *
	 * @return etiqueta, p. ej. {@code SPECTER2 (coseno)}
	 */
	public String label() {
		return label;
	}

	/**
	 * Archivo del stub que implementa este resultado, relativo a la raíz del repositorio.
	 *
	 * <p>Complejidad: O(1) en tiempo y espacio.
	 *
	 * @return p. ej. {@code src/main/java/co/edu/uniquindio/bibliomtr/algorithms/similarity/Levenshtein.java}
	 */
	public String stubFile() {
		return STUB_DIRECTORY + stubClass + ".java";
	}

	/**
	 * Método del stub que escribe el estudiante.
	 *
	 * <p>Complejidad: O(1) en tiempo y espacio.
	 *
	 * @return {@code compare}
	 */
	public String stubMethod() {
		return STUB_METHOD;
	}

	/**
	 * Ubicación que se informa cuando el resultado está PENDIENTE (REQ-20, REQ-21):
	 * {@code <archivo>#<método>}.
	 *
	 * <p>Complejidad: O(1) en tiempo y espacio.
	 *
	 * @return p. ej. {@code src/main/java/co/edu/uniquindio/bibliomtr/algorithms/similarity/Jaccard.java#compare}
	 */
	public String pendingLocation() {
		return stubFile() + "#" + stubMethod();
	}

	/**
	 * Busca el resultado por su slug.
	 *
	 * <p>Complejidad: O(1) en tiempo y espacio (8 valores).
	 *
	 * @param slug p. ej. {@code levenshtein} o {@code nomic-euclidean}
	 * @return el resultado correspondiente
	 * @throws IllegalArgumentException si el slug es desconocido, con el mensaje en español de la API (HTTP 400)
	 */
	public static ComparisonAlgorithm fromSlug(String slug) {
		StringJoiner valid = new StringJoiner(", ");
		for (ComparisonAlgorithm algorithm : values()) {
			if (algorithm.slug.equals(slug)) {
				return algorithm;
			}
			valid.add(algorithm.slug);
		}
		throw new IllegalArgumentException("Algoritmo desconocido: " + slug + ". Valores válidos: " + valid);
	}

}
