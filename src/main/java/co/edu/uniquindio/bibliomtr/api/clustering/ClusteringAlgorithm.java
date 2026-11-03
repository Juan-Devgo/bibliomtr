package co.edu.uniquindio.bibliomtr.api.clustering;

import java.util.StringJoiner;

/**
 * Los 4 algoritmos de clustering jerárquico del Requerimiento 3 (spec 003, REQ-11, REQ-18): 3 aglomerativos y
 * el divisivo DIANA.
 *
 * <p><b>Por qué.</b> Centraliza el slug de la API, la etiqueta que muestra la interfaz y el stub que implementa
 * cada algoritmo, para que la respuesta, el aviso "Pendiente de implementar: &lt;archivo&gt;#&lt;método&gt;" y la
 * guía no diverjan. El orden de declaración es el orden de la respuesta.
 */
public enum ClusteringAlgorithm {

	/** Aglomerativo con la distancia mínima entre grupos. */
	SINGLE_LINKAGE("single-linkage", "Single Linkage", "SingleLinkage"),

	/** Aglomerativo con la distancia máxima entre grupos. */
	COMPLETE_LINKAGE("complete-linkage", "Complete Linkage", "CompleteLinkage"),

	/** Aglomerativo con la distancia promedio entre grupos (UPGMA). */
	AVERAGE_LINKAGE("average-linkage", "Average Linkage (UPGMA)", "AverageLinkage"),

	/** Divisivo (top-down) que parte el grupo de mayor diámetro. */
	DIANA("diana", "DIANA (divisivo)", "Diana");

	/** Directorio de los stubs, relativo a la raíz del repositorio. */
	static final String STUB_DIRECTORY = "src/main/java/co/edu/uniquindio/bibliomtr/algorithms/clustering/";

	/** Método que implementa el estudiante en cada stub. */
	static final String STUB_METHOD = "cluster";

	private final String slug;
	private final String label;
	private final String stubClass;

	ClusteringAlgorithm(String slug, String label, String stubClass) {
		this.slug = slug;
		this.label = label;
		this.stubClass = stubClass;
	}

	/**
	 * Nombre del algoritmo en la API ({@code algorithm} de la respuesta y parámetro de
	 * {@code GET /api/clustering/trace}).
	 *
	 * <p>Complejidad: O(1) en tiempo y espacio.
	 *
	 * @return slug en minúsculas, p. ej. {@code average-linkage}
	 */
	public String slug() {
		return slug;
	}

	/**
	 * Nombre legible que muestra la interfaz.
	 *
	 * <p>Complejidad: O(1) en tiempo y espacio.
	 *
	 * @return etiqueta, p. ej. {@code Average Linkage (UPGMA)}
	 */
	public String label() {
		return label;
	}

	/**
	 * Archivo del stub que implementa este algoritmo, relativo a la raíz del repositorio.
	 *
	 * <p>Complejidad: O(1) en tiempo y espacio.
	 *
	 * @return p. ej. {@code src/main/java/co/edu/uniquindio/bibliomtr/algorithms/clustering/Diana.java}
	 */
	public String stubFile() {
		return STUB_DIRECTORY + stubClass + ".java";
	}

	/**
	 * Método del stub que escribe el estudiante.
	 *
	 * <p>Complejidad: O(1) en tiempo y espacio.
	 *
	 * @return {@code cluster}
	 */
	public String stubMethod() {
		return STUB_METHOD;
	}

	/**
	 * Ubicación que se informa cuando el algoritmo está PENDIENTE (REQ-18): {@code <archivo>#<método>}.
	 *
	 * <p>Complejidad: O(1) en tiempo y espacio.
	 *
	 * @return p. ej. {@code src/main/java/co/edu/uniquindio/bibliomtr/algorithms/clustering/SingleLinkage.java#cluster}
	 */
	public String pendingLocation() {
		return stubFile() + "#" + stubMethod();
	}

	/**
	 * Busca el algoritmo por su slug.
	 *
	 * <p>Complejidad: O(1) en tiempo y espacio (4 valores).
	 *
	 * @param slug p. ej. {@code single-linkage} o {@code diana}
	 * @return el algoritmo correspondiente
	 * @throws IllegalArgumentException si el slug es desconocido, con el mensaje en español de la API (HTTP 400)
	 */
	public static ClusteringAlgorithm fromSlug(String slug) {
		StringJoiner valid = new StringJoiner(", ");
		for (ClusteringAlgorithm algorithm : values()) {
			if (algorithm.slug.equals(slug)) {
				return algorithm;
			}
			valid.add(algorithm.slug);
		}
		throw new IllegalArgumentException(
				"Algoritmo de clustering desconocido: " + slug + ". Valores válidos: " + valid);
	}

}
