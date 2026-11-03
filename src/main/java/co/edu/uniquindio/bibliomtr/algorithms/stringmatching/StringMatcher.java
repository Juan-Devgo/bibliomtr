package co.edu.uniquindio.bibliomtr.algorithms.stringmatching;

/**
 * Estrategia de coincidencia exacta de cadenas en dos fases: preprocesamiento del patrón y búsqueda en un texto
 * (KMP, Boyer-Moore y Rabin-Karp; REQ-1).
 *
 * <p><b>Por qué dos fases.</b> En una búsqueda sobre el corpus el patrón se preprocesa una sola vez y las mismas
 * tablas se reutilizan en cada abstract (REQ-1); además, el tiempo de cada fase se mide por separado
 * (constitución P11, REQ-29 y REQ-30). Una sola firma (patrón Strategy) permite que el servicio y los tests llamen
 * a cualquiera de los 3 algoritmos, y que los tests de la API los reemplacen por algoritmos falsos.
 *
 * <p><b>Contrato.</b> {@link #preprocess} lanza {@code IllegalArgumentException} si el patrón es nulo o vacío.
 * {@link #search} devuelve las posiciones iniciales de todas las ocurrencias exactas, incluidas las solapadas, como
 * índices base 0 de {@code char} en orden ascendente; con texto vacío o más corto que el patrón devuelve una lista
 * vacía. La comparación es {@code char} a {@code char}, sin normalizar (REQ-2, REQ-3).
 *
 * <p>Complejidad: la declara cada implementación.
 *
 * @param <P> tipo del resultado del preprocesamiento (las tablas del algoritmo)
 * @param <T> tipo de la traza de la búsqueda
 */
public interface StringMatcher<P, T> {

	/**
	 * Preprocesa el patrón y devuelve las tablas que usa la búsqueda; también son la traza del preprocesamiento
	 * (constitución P7).
	 *
	 * <p>Complejidad: la declara cada implementación.
	 *
	 * @param pattern patrón no vacío, usado tal como llega (sin recortar)
	 * @param options q de Rabin-Karp y grabación de traza
	 * @return las tablas del algoritmo
	 * @throws IllegalArgumentException si el patrón es nulo o vacío
	 */
	P preprocess(String pattern, MatchOptions options);

	/**
	 * Busca en {@code text} todas las ocurrencias del patrón preprocesado.
	 *
	 * <p>Complejidad: la declara cada implementación.
	 *
	 * @param text         texto donde se busca (el abstract tal como quedó almacenado)
	 * @param preprocessed resultado de {@link #preprocess} para el patrón
	 * @param options      q de Rabin-Karp y grabación de traza; con {@code recordTrace = false} la traza es
	 *                     {@code null}
	 * @return posiciones, colisiones de hash (solo Rabin-Karp) y traza
	 */
	MatchResult<T> search(String text, P preprocessed, MatchOptions options);

}
