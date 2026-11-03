package co.edu.uniquindio.bibliomtr.api.stringmatching;

import java.util.Objects;

import co.edu.uniquindio.bibliomtr.algorithms.stringmatching.StringMatcher;

/**
 * Registro de las 3 estrategias de coincidencia exacta de cadenas (spec 004, REQ-15, REQ-22, REQ-28; patrón
 * Strategy del plan).
 *
 * <p><b>Por qué un registro.</b> {@code PatternSearchService} recibe un solo bean con los 3 algoritmos; en
 * producción son los stubs de {@code algorithms/stringmatching} ({@code StringMatchingConfig}), y los tests de API
 * lo reemplazan por un catálogo falso {@code @Primary} para no depender de que los estudiantes hayan implementado
 * algo. El preprocesamiento y la traza son de tipo comodín porque la API los expone como JSON sin interpretarlos.
 *
 * <p>Complejidad: construcción O(1) en tiempo y espacio.
 *
 * @param kmp        Knuth-Morris-Pratt
 * @param boyerMoore Boyer-Moore
 * @param rabinKarp  Rabin-Karp
 */
public record StringMatchingCatalog(StringMatcher<?, ?> kmp, StringMatcher<?, ?> boyerMoore,
		StringMatcher<?, ?> rabinKarp) {

	/**
	 * Exige las 3 estrategias.
	 *
	 * <p>Complejidad: O(1) en tiempo y espacio.
	 */
	public StringMatchingCatalog {
		Objects.requireNonNull(kmp, "kmp");
		Objects.requireNonNull(boyerMoore, "boyerMoore");
		Objects.requireNonNull(rabinKarp, "rabinKarp");
	}

	/**
	 * Estrategia de un algoritmo.
	 *
	 * <p>Complejidad: O(1) en tiempo y espacio.
	 *
	 * @param algorithm algoritmo pedido
	 * @return su estrategia
	 */
	public StringMatcher<?, ?> strategy(StringMatchingAlgorithm algorithm) {
		return switch (algorithm) {
			case KMP -> kmp;
			case BOYER_MOORE -> boyerMoore;
			case RABIN_KARP -> rabinKarp;
		};
	}

}
