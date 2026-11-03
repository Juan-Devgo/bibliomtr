package co.edu.uniquindio.bibliomtr.api.stringmatching;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import co.edu.uniquindio.bibliomtr.algorithms.stringmatching.InvalidMatchPositionsException;
import co.edu.uniquindio.bibliomtr.algorithms.stringmatching.MatchOptions;
import co.edu.uniquindio.bibliomtr.algorithms.stringmatching.MatchPositionsValidator;
import co.edu.uniquindio.bibliomtr.algorithms.stringmatching.MatchResult;
import co.edu.uniquindio.bibliomtr.algorithms.stringmatching.StringMatcher;
import co.edu.uniquindio.bibliomtr.api.PendingStubs;
import co.edu.uniquindio.bibliomtr.api.similarity.AlgorithmStatus;
import co.edu.uniquindio.bibliomtr.api.stringmatching.PatternSearchDtos.AlgorithmOccurrences;
import co.edu.uniquindio.bibliomtr.api.stringmatching.PatternSearchDtos.ArticleOccurrences;
import co.edu.uniquindio.bibliomtr.api.stringmatching.PatternSearchDtos.ArticleText;
import co.edu.uniquindio.bibliomtr.api.stringmatching.PatternSearchDtos.PatternSearchResponse;
import co.edu.uniquindio.bibliomtr.api.stringmatching.PatternSearchDtos.PatternTraceResponse;
import co.edu.uniquindio.bibliomtr.evaluation.Stopwatch;
import co.edu.uniquindio.bibliomtr.evaluation.Stopwatch.Timed;
import co.edu.uniquindio.bibliomtr.persistence.article.Article;
import co.edu.uniquindio.bibliomtr.persistence.article.ArticleRepository;

/**
 * Busca un patrón en los abstracts seleccionables con KMP, Boyer-Moore y Rabin-Karp, y da la traza de un algoritmo
 * sobre un abstract (spec 004, REQ-1, REQ-8, REQ-15, REQ-16, REQ-18, REQ-22, REQ-28, REQ-29).
 *
 * <p><b>Búsqueda</b> ({@link #search}):
 * <ol>
 *   <li>Valida {@code pattern} y {@code q} con {@link PatternSearchValidator} (HTTP 400 si fallan).</li>
 *   <li>Lee los artículos con abstract detectado, ordenados por nombre de archivo (spec 001); el texto buscado es
 *       {@link Article#getAbstractText()} tal como quedó almacenado, nunca el título (constitución P5).</li>
 *   <li>Por cada algoritmo, en el orden de {@link StringMatchingAlgorithm} y con {@code MatchOptions(q, false)}:
 *       cronometra un solo {@code preprocess} y, por cada abstract, un {@code search} con el mismo preprocesado,
 *       sumando sus tiempos (REQ-1, REQ-29). Un "TODO estudiante:" en {@code preprocess} o en {@code search} deja
 *       el algoritmo PENDIENTE con {@code <archivo>#<método>} (REQ-28).</li>
 *   <li>Cada resultado pasa por {@link MatchPositionsValidator}: posiciones nulas, desordenadas, repetidas o fuera
 *       de [0, n − m] lanzan {@link InvalidMatchPositionsException} (HTTP 500, REQ-8).</li>
 *   <li>{@code allMatch} es {@code null} con menos de 2 algoritmos CALCULADO y, si no, indica si todos dan listas
 *       iguales en cada artículo (REQ-16).</li>
 * </ol>
 *
 * <p><b>Traza</b> ({@link #trace}): valida patrón, q, algoritmo y artículo, en ese orden; corre un
 * {@code preprocess} y un {@code search} sobre ese abstract con {@code MatchOptions(q, true)} y valida las
 * posiciones (REQ-22).
 *
 * <p><b>Qué no hace.</b> No compara el patrón con el texto: esa es la tarea de los algoritmos (constitución P1,
 * REQ-7). Los tiempos son de una sola corrida con {@link Stopwatch}; la medición con calentamiento la hace JMH.
 */
@Service
public class PatternSearchService {

	private final ArticleRepository repository;
	private final StringMatchingCatalog catalog;

	/**
	 * Servicio sobre el repositorio de artículos y el catálogo de algoritmos.
	 *
	 * <p>Complejidad: O(1) en tiempo y espacio.
	 *
	 * @param repository artículos del corpus (spec 001)
	 * @param catalog    los 3 algoritmos de búsqueda de patrones
	 */
	public PatternSearchService(ArticleRepository repository, StringMatchingCatalog catalog) {
		this.repository = repository;
		this.catalog = catalog;
	}

	/**
	 * Ocurrencias del patrón en todos los abstracts seleccionables, con los 3 algoritmos (REQ-15, REQ-16, REQ-18,
	 * REQ-28, REQ-29; AC-12, AC-14, AC-20, AC-21, AC-25, AC-27).
	 *
	 * <p>Complejidad: O(√q) por la validación de q, más O(A) lecturas de la base, más, por algoritmo, su
	 * preprocesamiento una vez y su búsqueda en cada abstract, más O(R · (A + K)) en tiempo para validar y comparar
	 * posiciones, con A = artículos, K = ocurrencias totales y R = 3; O(Σ n + R · K) en espacio.
	 *
	 * @param rawPattern patrón recibido, sin recortar
	 * @param rawQ       q recibido; {@code null} si no vino
	 * @return resultados por algoritmo, indicador de coincidencia y abstracts
	 * @throws IllegalArgumentException       si el patrón o q son inválidos (HTTP 400)
	 * @throws InvalidMatchPositionsException si un algoritmo devuelve posiciones inválidas (HTTP 500)
	 */
	public PatternSearchResponse search(String rawPattern, String rawQ) {
		String pattern = PatternSearchValidator.pattern(rawPattern);
		long q = PatternSearchValidator.q(rawQ);
		List<Article> articles = repository.findAllByAbstractDetectedTrueOrderByFileNameAsc();
		MatchOptions options = new MatchOptions(q, false);

		List<AlgorithmOccurrences> results = new ArrayList<>(StringMatchingAlgorithm.values().length);
		for (StringMatchingAlgorithm algorithm : StringMatchingAlgorithm.values()) {
			results.add(searchCorpus(algorithm, catalog.strategy(algorithm), pattern, articles, options));
		}
		List<ArticleText> texts = new ArrayList<>(articles.size());
		for (Article article : articles) {
			texts.add(new ArticleText(article.getId(), article.getTitle(), article.getAbstractText()));
		}
		return new PatternSearchResponse(pattern, q, MatchOptions.D, articles.size(), allMatch(results), results,
				texts);
	}

	/**
	 * Preprocesamiento, traza y posiciones de un algoritmo sobre el abstract de un artículo (REQ-22, REQ-28;
	 * AC-19, AC-26). Valida en este orden: patrón, q, algoritmo y artículo.
	 *
	 * <p>Complejidad: O(√q) por la validación de q, más una lectura de la base, más el preprocesamiento y la
	 * búsqueda del algoritmo con la traza activada, más O(k) para validar las posiciones.
	 *
	 * @param algorithmSlug slug del algoritmo, p. ej. {@code kmp}
	 * @param articleId     id del artículo
	 * @param rawPattern    patrón recibido, sin recortar
	 * @param rawQ          q recibido; {@code null} si no vino
	 * @return la traza, o PENDIENTE con la ubicación del método sin implementar
	 * @throws IllegalArgumentException       si el patrón, q o el algoritmo son inválidos, o si el artículo no existe
	 *                                        o no tiene abstract (HTTP 400)
	 * @throws InvalidMatchPositionsException si el algoritmo devuelve posiciones inválidas (HTTP 500)
	 */
	public PatternTraceResponse trace(String algorithmSlug, long articleId, String rawPattern, String rawQ) {
		String pattern = PatternSearchValidator.pattern(rawPattern);
		long q = PatternSearchValidator.q(rawQ);
		StringMatchingAlgorithm algorithm = StringMatchingAlgorithm.fromSlug(algorithmSlug);
		List<Article> found = repository.findAllByIdInAndAbstractDetectedTrue(List.of(articleId));
		if (found.isEmpty()) {
			throw new IllegalArgumentException("El artículo %d no existe o no tiene abstract".formatted(articleId));
		}
		return traceArticle(algorithm, catalog.strategy(algorithm), found.getFirst(), pattern, q);
	}

	/**
	 * Corre un algoritmo sobre todo el corpus: un preprocesamiento y una búsqueda por abstract.
	 *
	 * <p>Complejidad: la del preprocesamiento una vez y la de la búsqueda en cada abstract, más O(A + K) en tiempo
	 * para validar las posiciones; O(A + K) en espacio.
	 *
	 * @param <P> tipo del preprocesamiento del algoritmo
	 * @param <T> tipo de la traza del algoritmo
	 */
	private static <P, T> AlgorithmOccurrences searchCorpus(StringMatchingAlgorithm algorithm,
			StringMatcher<P, T> matcher, String pattern, List<Article> articles, MatchOptions options) {
		Timed<P> preprocessed;
		try {
			preprocessed = Stopwatch.time(() -> matcher.preprocess(pattern, options));
		} catch (UnsupportedOperationException e) {
			requirePending(e);
			return pending(algorithm, StringMatchingAlgorithm.PREPROCESS_METHOD);
		}
		P tables = preprocessed.value();

		double searchMs = 0.0;
		int total = 0;
		List<ArticleOccurrences> occurrences = new ArrayList<>(articles.size());
		for (Article article : articles) {
			String text = article.getAbstractText();
			Timed<MatchResult<T>> timed;
			try {
				timed = Stopwatch.time(() -> matcher.search(text, tables, options));
			} catch (UnsupportedOperationException e) {
				requirePending(e);
				return pending(algorithm, StringMatchingAlgorithm.SEARCH_METHOD);
			}
			searchMs += timed.millis();
			List<Integer> positions = validPositions(algorithm, article.getId(), text, pattern, timed.value());
			Integer collisions = algorithm == StringMatchingAlgorithm.RABIN_KARP
					? timed.value().collisions().size()
					: null;
			occurrences.add(new ArticleOccurrences(article.getId(), article.getTitle(), positions, positions.size(),
					collisions));
			total += positions.size();
		}
		return new AlgorithmOccurrences(algorithm.slug(), algorithm.label(), AlgorithmStatus.CALCULADO.name(), null,
				preprocessed.millis(), searchMs, total, occurrences);
	}

	/**
	 * Corre un algoritmo con la traza activada sobre un abstract.
	 *
	 * <p>Complejidad: la del preprocesamiento y la búsqueda del algoritmo, más O(k) en tiempo para validar.
	 *
	 * @param <P> tipo del preprocesamiento del algoritmo
	 * @param <T> tipo de la traza del algoritmo
	 */
	private static <P, T> PatternTraceResponse traceArticle(StringMatchingAlgorithm algorithm,
			StringMatcher<P, T> matcher, Article article, String pattern, long q) {
		MatchOptions options = MatchOptions.tracing(q);
		String text = article.getAbstractText();
		P tables;
		try {
			tables = matcher.preprocess(pattern, options);
		} catch (UnsupportedOperationException e) {
			requirePending(e);
			return pendingTrace(algorithm, article, pattern, q, StringMatchingAlgorithm.PREPROCESS_METHOD);
		}
		MatchResult<T> result;
		try {
			result = matcher.search(text, tables, options);
		} catch (UnsupportedOperationException e) {
			requirePending(e);
			return pendingTrace(algorithm, article, pattern, q, StringMatchingAlgorithm.SEARCH_METHOD);
		}
		List<Integer> positions = validPositions(algorithm, article.getId(), text, pattern, result);
		List<Integer> collisions = algorithm == StringMatchingAlgorithm.RABIN_KARP ? result.collisions() : null;
		return new PatternTraceResponse(algorithm.slug(), algorithm.label(), article.getId(), article.getTitle(),
				text, pattern, q, AlgorithmStatus.CALCULADO.name(), null, positions, collisions, tables,
				result.trace());
	}

	/**
	 * Posiciones de un resultado, revisadas con {@link MatchPositionsValidator} (REQ-8).
	 *
	 * <p>Complejidad: O(k) en tiempo y O(1) en espacio.
	 *
	 * @throws InvalidMatchPositionsException si el resultado es nulo o sus posiciones son inválidas
	 */
	private static List<Integer> validPositions(StringMatchingAlgorithm algorithm, long articleId, String text,
			String pattern, MatchResult<?> result) {
		if (result == null) {
			throw MatchPositionsValidator.nullResult(algorithm.slug(), articleId);
		}
		MatchPositionsValidator.validate(algorithm.slug(), articleId, text.length(), pattern.length(),
				result.positions());
		return result.positions();
	}

	/**
	 * {@code null} con menos de 2 algoritmos CALCULADO; si no, {@code true} cuando todos dan las mismas posiciones
	 * en cada artículo (REQ-16). Todos los CALCULADO recorren los artículos en el mismo orden, así que se comparan
	 * por índice. Con el corpus vacío y 2 o más CALCULADO es {@code true}: no hay artículo en desacuerdo.
	 *
	 * <p>Complejidad: O(R · (A + K)) en tiempo y O(R) en espacio.
	 */
	private static Boolean allMatch(List<AlgorithmOccurrences> results) {
		List<AlgorithmOccurrences> computed = new ArrayList<>(results.size());
		for (AlgorithmOccurrences result : results) {
			if (AlgorithmStatus.CALCULADO.name().equals(result.status())) {
				computed.add(result);
			}
		}
		if (computed.size() < 2) {
			return null;
		}
		List<ArticleOccurrences> reference = computed.getFirst().articles();
		for (int r = 1; r < computed.size(); r++) {
			List<ArticleOccurrences> other = computed.get(r).articles();
			for (int a = 0; a < reference.size(); a++) {
				if (!reference.get(a).positions().equals(other.get(a).positions())) {
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * Resultado PENDIENTE de la búsqueda: sin tiempos, total ni artículos (REQ-28).
	 *
	 * <p>Complejidad: O(1) en tiempo y espacio.
	 */
	private static AlgorithmOccurrences pending(StringMatchingAlgorithm algorithm, String method) {
		return new AlgorithmOccurrences(algorithm.slug(), algorithm.label(), AlgorithmStatus.PENDIENTE.name(),
				algorithm.pendingLocation(method), null, null, null, List.of());
	}

	/**
	 * Traza PENDIENTE: sin posiciones, colisiones, preprocesamiento ni traza (REQ-22, REQ-28).
	 *
	 * <p>Complejidad: O(1) en tiempo y espacio.
	 */
	private static PatternTraceResponse pendingTrace(StringMatchingAlgorithm algorithm, Article article,
			String pattern, long q, String method) {
		return new PatternTraceResponse(algorithm.slug(), algorithm.label(), article.getId(), article.getTitle(),
				article.getAbstractText(), pattern, q, AlgorithmStatus.PENDIENTE.name(),
				algorithm.pendingLocation(method), null, null, null, null);
	}

	/**
	 * Propaga la excepción si no es la de un stub sin implementar.
	 *
	 * <p>Complejidad: O(p) en tiempo, con p = longitud del prefijo, y O(1) en espacio.
	 */
	private static void requirePending(UnsupportedOperationException e) {
		if (!PendingStubs.isPending(e)) {
			throw e;
		}
	}

}
