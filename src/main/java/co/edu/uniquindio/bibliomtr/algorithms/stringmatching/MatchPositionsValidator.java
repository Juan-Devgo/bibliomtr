package co.edu.uniquindio.bibliomtr.algorithms.stringmatching;

import java.util.List;

/**
 * Revisa que las posiciones que devuelve un algoritmo implementado cumplan el contrato de {@link StringMatcher}
 * (REQ-8, AC-3).
 *
 * <p><b>Qué revisa</b>, en orden y posición por posición; la primera regla que falla lanza
 * {@link InvalidMatchPositionsException} con el mensaje "El algoritmo &lt;slug&gt; devolvió posiciones inválidas en
 * el artículo &lt;id&gt;: &lt;razón&gt;":
 * <ol>
 *   <li>La lista no es nula ("la lista de posiciones es nula").</li>
 *   <li>La posición k no es nula ("la posición &lt;k&gt; es nula").</li>
 *   <li>Cada posición p está en [0, n − m] ("la posición &lt;p&gt; está fuera de [0, &lt;n − m&gt;]"); con n &lt; m
 *       toda posición es inválida.</li>
 *   <li>Cada posición es mayor que la anterior: ascendentes y sin repetidos ("la posición &lt;p&gt; no es mayor
 *       que la anterior &lt;q&gt;").</li>
 * </ol>
 * Un resultado nulo se informa con {@link #nullResult} ("el resultado es nulo").
 *
 * <p><b>Por qué.</b> Es la frontera entre el código del estudiante y la interfaz: unas posiciones mal formadas no
 * se pueden resaltar, y es mejor un 500 que diga qué posición está mal. Solo compara enteros: no lee el texto ni
 * el patrón, ni comprueba que en cada posición haya una ocurrencia; eso lo verifican los tests {@code student}, y
 * comprobarlo aquí metería la búsqueda en el andamiaje (constitución P1, REQ-7).
 *
 * <p>Complejidad: O(k) en tiempo y O(1) en espacio, con k = número de posiciones.
 */
public final class MatchPositionsValidator {

	private MatchPositionsValidator() {
	}

	/**
	 * Valida las posiciones de un artículo.
	 *
	 * <p>Complejidad: O(k) en tiempo y O(1) en espacio.
	 *
	 * @param algorithm     slug del algoritmo, para el mensaje (p. ej. {@code kmp})
	 * @param articleId     id del artículo, para el mensaje
	 * @param textLength    n, longitud del abstract en {@code char}
	 * @param patternLength m, longitud del patrón en {@code char}
	 * @param positions     posiciones que devolvió el algoritmo; puede ser nula
	 * @throws InvalidMatchPositionsException si alguna regla no se cumple
	 */
	public static void validate(String algorithm, long articleId, int textLength, int patternLength,
			List<Integer> positions) {
		if (positions == null) {
			throw invalid(algorithm, articleId, "la lista de posiciones es nula");
		}
		// Última posición válida: n − m (negativa si n < m, y entonces ninguna posición cabe).
		int maxPosition = textLength - patternLength;
		Integer previous = null;
		for (int k = 0; k < positions.size(); k++) {
			Integer position = positions.get(k);
			if (position == null) {
				throw invalid(algorithm, articleId, "la posición %d es nula".formatted(k));
			}
			if (position < 0 || position > maxPosition) {
				throw invalid(algorithm, articleId,
						"la posición %d está fuera de [0, %d]".formatted(position, maxPosition));
			}
			if (previous != null && position <= previous) {
				throw invalid(algorithm, articleId,
						"la posición %d no es mayor que la anterior %d".formatted(position, previous));
			}
			previous = position;
		}
	}

	/**
	 * Excepción para un algoritmo que devolvió un {@code MatchResult} nulo; la lanza el servicio.
	 *
	 * <p>Complejidad: O(1) en tiempo y espacio.
	 *
	 * @param algorithm slug del algoritmo
	 * @param articleId id del artículo
	 * @return la excepción con la razón "el resultado es nulo"
	 */
	public static InvalidMatchPositionsException nullResult(String algorithm, long articleId) {
		return invalid(algorithm, articleId, "el resultado es nulo");
	}

	/**
	 * Excepción con el prefijo común del mensaje.
	 *
	 * <p>Complejidad: O(1) en tiempo y espacio.
	 */
	private static InvalidMatchPositionsException invalid(String algorithm, long articleId, String reason) {
		return new InvalidMatchPositionsException("El algoritmo %s devolvió posiciones inválidas en el artículo %d: %s"
				.formatted(algorithm, articleId, reason));
	}

}
