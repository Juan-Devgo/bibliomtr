package co.edu.uniquindio.bibliomtr.api.stringmatching;

import java.util.StringJoiner;

/**
 * Los 3 algoritmos de coincidencia exacta de cadenas del Requerimiento 4 (spec 004, REQ-15, REQ-22, REQ-28).
 *
 * <p><b>Por qué.</b> Centraliza el slug de la API, la etiqueta que muestra la interfaz y el stub que implementa
 * cada algoritmo, para que la respuesta, el aviso "Pendiente de implementar: &lt;archivo&gt;#&lt;método&gt;" y la
 * guía no diverjan. El orden de declaración (KMP, Boyer-Moore, Rabin-Karp) es el orden de la respuesta y el que usa
 * la interfaz para elegir el algoritmo por defecto.
 */
public enum StringMatchingAlgorithm {

	/** Knuth-Morris-Pratt: función de prefijo π y búsqueda sin retroceder en el texto. */
	KMP("kmp", "Knuth-Morris-Pratt (KMP)", "KnuthMorrisPratt"),

	/** Boyer-Moore: comparación de derecha a izquierda con las reglas del carácter malo y del sufijo bueno. */
	BOYER_MOORE("boyer-moore", "Boyer-Moore", "BoyerMoore"),

	/** Rabin-Karp: hash polinomial modular con rolling hash y verificación de cada coincidencia de hash. */
	RABIN_KARP("rabin-karp", "Rabin-Karp", "RabinKarp");

	/** Directorio de los stubs, relativo a la raíz del repositorio. */
	static final String STUB_DIRECTORY = "src/main/java/co/edu/uniquindio/bibliomtr/algorithms/stringmatching/";

	/** Método de preprocesamiento del patrón que implementa el estudiante. */
	public static final String PREPROCESS_METHOD = "preprocess";

	/** Método de búsqueda en un texto que implementa el estudiante. */
	public static final String SEARCH_METHOD = "search";

	private final String slug;
	private final String label;
	private final String stubClass;

	StringMatchingAlgorithm(String slug, String label, String stubClass) {
		this.slug = slug;
		this.label = label;
		this.stubClass = stubClass;
	}

	/**
	 * Nombre del algoritmo en la API ({@code algorithm} de la respuesta y parámetro de
	 * {@code GET /api/pattern-search/trace}).
	 *
	 * <p>Complejidad: O(1) en tiempo y espacio.
	 *
	 * @return slug en minúsculas, p. ej. {@code boyer-moore}
	 */
	public String slug() {
		return slug;
	}

	/**
	 * Nombre legible que muestra la interfaz.
	 *
	 * <p>Complejidad: O(1) en tiempo y espacio.
	 *
	 * @return etiqueta, p. ej. {@code Knuth-Morris-Pratt (KMP)}
	 */
	public String label() {
		return label;
	}

	/**
	 * Archivo del stub que implementa este algoritmo, relativo a la raíz del repositorio.
	 *
	 * <p>Complejidad: O(1) en tiempo y espacio.
	 *
	 * @return p. ej. {@code src/main/java/co/edu/uniquindio/bibliomtr/algorithms/stringmatching/RabinKarp.java}
	 */
	public String stubFile() {
		return STUB_DIRECTORY + stubClass + ".java";
	}

	/**
	 * Ubicación que se informa cuando el algoritmo está PENDIENTE (REQ-28): {@code <archivo>#<método>}, con el
	 * método que lanzó "TODO estudiante:".
	 *
	 * <p>Complejidad: O(1) en tiempo y espacio.
	 *
	 * @param method {@value #PREPROCESS_METHOD} o {@value #SEARCH_METHOD}
	 * @return p. ej. {@code src/main/java/co/edu/uniquindio/bibliomtr/algorithms/stringmatching/BoyerMoore.java#preprocess}
	 */
	public String pendingLocation(String method) {
		return stubFile() + "#" + method;
	}

	/**
	 * Busca el algoritmo por su slug.
	 *
	 * <p>Complejidad: O(1) en tiempo y espacio (3 valores).
	 *
	 * @param slug p. ej. {@code kmp} o {@code rabin-karp}
	 * @return el algoritmo correspondiente
	 * @throws IllegalArgumentException si el slug es desconocido, con el mensaje en español de la API (HTTP 400)
	 */
	public static StringMatchingAlgorithm fromSlug(String slug) {
		StringJoiner valid = new StringJoiner(", ");
		for (StringMatchingAlgorithm algorithm : values()) {
			if (algorithm.slug.equals(slug)) {
				return algorithm;
			}
			valid.add(algorithm.slug);
		}
		throw new IllegalArgumentException(
				"Algoritmo de búsqueda de patrones desconocido: " + slug + ". Valores válidos: " + valid);
	}

}
