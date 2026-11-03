package co.edu.uniquindio.bibliomtr.api.stringmatching;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import co.edu.uniquindio.bibliomtr.trace.stringmatching.BoyerMoorePreprocessing;

/**
 * Cuerpos JSON de {@code GET /api/pattern-search} y {@code GET /api/pattern-search/trace} (spec 004, REQ-15,
 * REQ-16, REQ-22, REQ-28, REQ-29).
 *
 * <p><b>Por qué separadas.</b> La búsqueda devuelve posiciones y tiempos de los 3 algoritmos en todo el corpus, con
 * la traza desactivada para no medir su costo (REQ-29); la traza, que guarda cada comparación, se pide aparte para un
 * algoritmo y un artículo.
 */
public final class PatternSearchDtos {

	private PatternSearchDtos() {
	}

	/**
	 * Resultado de una búsqueda sobre los abstracts seleccionables (REQ-15, REQ-16, REQ-18).
	 *
	 * <p>Complejidad: construcción O(R + A) en tiempo y espacio por las copias de las listas, con R = 3 algoritmos y
	 * A = artículos.
	 *
	 * @param pattern      patrón buscado, tal como llegó
	 * @param q            módulo de Rabin-Karp usado
	 * @param d            base del hash de Rabin-Karp (65 536)
	 * @param articleCount número de abstracts en los que se buscó
	 * @param allMatch     {@code true} si los algoritmos CALCULADO dan las mismas posiciones en cada artículo;
	 *                     {@code null} con menos de 2 algoritmos CALCULADO
	 * @param algorithms   un resultado por algoritmo, en el orden de {@link StringMatchingAlgorithm}
	 * @param articles     los artículos con su abstract, para que la interfaz resalte las posiciones
	 */
	public record PatternSearchResponse(String pattern, long q, long d, int articleCount, Boolean allMatch,
			List<AlgorithmOccurrences> algorithms, List<ArticleText> articles) {

		/**
		 * Copia defensiva de las listas.
		 *
		 * <p>Complejidad: O(R + A) en tiempo y espacio.
		 */
		public PatternSearchResponse {
			algorithms = List.copyOf(algorithms);
			articles = List.copyOf(articles);
		}
	}

	/**
	 * Abstract de un artículo en el que se buscó (REQ-20).
	 *
	 * <p>Complejidad: construcción O(1) en tiempo y espacio.
	 *
	 * @param articleId    id del artículo
	 * @param title        título del artículo
	 * @param abstractText abstract tal como quedó almacenado
	 */
	public record ArticleText(long articleId, String title, String abstractText) {
	}

	/**
	 * Resultado de un algoritmo en todo el corpus (REQ-15, REQ-16, REQ-28, REQ-29).
	 *
	 * <p>Complejidad: construcción O(A) en tiempo y espacio por la copia de los artículos.
	 *
	 * @param algorithm        slug del algoritmo, p. ej. {@code kmp}
	 * @param label            nombre legible
	 * @param status           {@code CALCULADO} o {@code PENDIENTE}
	 * @param pendingLocation  {@code <archivo>#<método>} del stub si está PENDIENTE; {@code null} si no
	 * @param preprocessingMs  tiempo en ms del único preprocesamiento del patrón; {@code null} si está PENDIENTE
	 * @param searchMs         suma de los tiempos en ms de las búsquedas en cada abstract; {@code null} si está
	 *                         PENDIENTE
	 * @param totalOccurrences suma de las ocurrencias de todos los artículos; {@code null} si está PENDIENTE
	 * @param articles         un resultado por artículo, en el orden de {@code articles} de la respuesta; vacía si
	 *                         está PENDIENTE
	 */
	public record AlgorithmOccurrences(String algorithm, String label, String status, String pendingLocation,
			Double preprocessingMs, Double searchMs, Integer totalOccurrences, List<ArticleOccurrences> articles) {

		/**
		 * Copia defensiva de los artículos.
		 *
		 * <p>Complejidad: O(A) en tiempo y espacio.
		 */
		public AlgorithmOccurrences {
			articles = List.copyOf(articles);
		}
	}

	/**
	 * Ocurrencias de un algoritmo en un artículo (REQ-15).
	 *
	 * <p>Complejidad: construcción O(k) en tiempo y espacio por la copia de las posiciones.
	 *
	 * @param articleId  id del artículo
	 * @param title      título del artículo
	 * @param positions  posiciones validadas, base 0 en {@code char}, ascendentes
	 * @param count      número de posiciones
	 * @param collisions número de colisiones de hash en Rabin-Karp; {@code null} en los demás algoritmos
	 */
	public record ArticleOccurrences(long articleId, String title, List<Integer> positions, int count,
			Integer collisions) {

		/**
		 * Copia defensiva de las posiciones.
		 *
		 * <p>Complejidad: O(k) en tiempo y espacio.
		 */
		public ArticleOccurrences {
			positions = List.copyOf(positions);
		}
	}

	/**
	 * Entrada de la tabla del carácter malo de Boyer-Moore en el JSON (REQ-24).
	 *
	 * <p>Complejidad: construcción O(1) en tiempo y espacio.
	 *
	 * @param character carácter del patrón
	 * @param index     última posición de ese carácter en el patrón
	 */
	public record LastOccurrenceEntry(char character, int index) {
	}

	/**
	 * Preprocesamiento de Boyer-Moore tal como lo expone la API (REQ-24).
	 *
	 * <p><b>Por qué no se serializa el record de la traza.</b> {@code BoyerMoorePreprocessing.lastOccurrence} es un
	 * mapa; como objeto JSON, JavaScript enumera primero las claves numéricas ("1", "2", …) y pierde el orden de
	 * primera aparición. Aquí la tabla viaja como lista, en el mismo orden del mapa; el resto de los campos se copia
	 * sin cambios.
	 *
	 * <p>Complejidad: construcción O(m) en tiempo y espacio.
	 *
	 * @param pattern         patrón preprocesado
	 * @param lastOccurrence  tabla del carácter malo, en orden de primera aparición en el patrón
	 * @param borderPositions arreglo f de m + 1 posiciones del preprocesamiento de bordes
	 * @param goodSuffixShift salto por desajuste en la posición j del patrón (regla fuerte)
	 * @param patternBorder   borde(P)
	 * @param matchShift      salto tras una ocurrencia completa, m − borde(P)
	 */
	public record BoyerMoorePreprocessingView(String pattern, List<LastOccurrenceEntry> lastOccurrence,
			int[] borderPositions, int[] goodSuffixShift, int patternBorder, int matchShift) {

		/**
		 * Copia la tabla del carácter malo como lista, conservando el orden del mapa.
		 *
		 * <p>Complejidad: O(m) en tiempo y espacio.
		 *
		 * @param preprocessing preprocesamiento devuelto por {@code BoyerMoore.preprocess}
		 * @return la vista JSON del preprocesamiento
		 */
		public static BoyerMoorePreprocessingView of(BoyerMoorePreprocessing preprocessing) {
			List<LastOccurrenceEntry> entries = new ArrayList<>(preprocessing.lastOccurrence().size());
			for (Map.Entry<Character, Integer> entry : preprocessing.lastOccurrence().entrySet()) {
				entries.add(new LastOccurrenceEntry(entry.getKey(), entry.getValue()));
			}
			return new BoyerMoorePreprocessingView(preprocessing.pattern(), List.copyOf(entries),
					preprocessing.borderPositions(), preprocessing.goodSuffixShift(), preprocessing.patternBorder(),
					preprocessing.matchShift());
		}
	}

	/**
	 * Preprocesamiento como lo expone la API: {@link BoyerMoorePreprocessingView} para Boyer-Moore y el mismo
	 * objeto para los demás algoritmos.
	 *
	 * <p>Complejidad: O(m) en tiempo y espacio con Boyer-Moore; O(1) con los demás.
	 *
	 * @param preprocessing preprocesamiento devuelto por {@code preprocess}; puede ser nulo
	 * @return el objeto a serializar
	 */
	static Object preprocessingJson(Object preprocessing) {
		if (preprocessing instanceof BoyerMoorePreprocessing boyerMoore) {
			return BoyerMoorePreprocessingView.of(boyerMoore);
		}
		return preprocessing;
	}

	/**
	 * Preprocesamiento, traza y posiciones de un algoritmo sobre el abstract de un artículo (REQ-22, REQ-23, REQ-24,
	 * REQ-25, REQ-28).
	 *
	 * <p>Complejidad: construcción O(k + c) en tiempo y espacio por las copias, con k = posiciones y c = colisiones
	 * (el preprocesamiento y la traza no se copian: ya son inmodificables).
	 *
	 * @param algorithm       slug del algoritmo
	 * @param label           nombre legible
	 * @param articleId       id del artículo
	 * @param title           título del artículo
	 * @param abstractText    abstract tal como quedó almacenado
	 * @param pattern         patrón buscado, tal como llegó
	 * @param q               módulo de Rabin-Karp usado
	 * @param status          {@code CALCULADO} o {@code PENDIENTE}
	 * @param pendingLocation {@code <archivo>#<método>} del stub si está PENDIENTE; {@code null} si no
	 * @param positions       posiciones validadas; {@code null} si está PENDIENTE
	 * @param collisions      desplazamientos con colisión de hash en Rabin-Karp; {@code null} en los demás
	 *                        algoritmos o si está PENDIENTE
	 * @param preprocessing   {@code KmpPreprocessing}, {@link BoyerMoorePreprocessingView} o
	 *                        {@code RabinKarpPreprocessing}; {@code null} si está PENDIENTE
	 * @param trace           {@code KmpTrace}, {@code BoyerMooreTrace} o {@code RabinKarpTrace}; {@code null} si
	 *                        está PENDIENTE
	 */
	public record PatternTraceResponse(String algorithm, String label, long articleId, String title,
			String abstractText, String pattern, long q, String status, String pendingLocation, List<Integer> positions,
			List<Integer> collisions, Object preprocessing, Object trace) {

		/**
		 * Copia defensiva de las listas, si las hay. Las colisiones no pasan por el validador de posiciones, así que
		 * su copia conserva un elemento nulo en lugar de fallar.
		 *
		 * <p>Complejidad: O(k + c) en tiempo y espacio.
		 */
		public PatternTraceResponse {
			positions = positions == null ? null : List.copyOf(positions);
			collisions = collisions == null ? null : Collections.unmodifiableList(new ArrayList<>(collisions));
		}
	}

}
