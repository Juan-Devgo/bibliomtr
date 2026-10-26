package co.edu.uniquindio.bibliomtr.api.similarity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import co.edu.uniquindio.bibliomtr.algorithms.similarity.SimilarityResult;
import co.edu.uniquindio.bibliomtr.api.corpus.ArticleSummary;
import co.edu.uniquindio.bibliomtr.api.similarity.ComparisonDtos.AlgorithmMatrix;
import co.edu.uniquindio.bibliomtr.api.similarity.ComparisonDtos.ComparisonRequest;
import co.edu.uniquindio.bibliomtr.api.similarity.ComparisonDtos.ComparisonResponse;
import co.edu.uniquindio.bibliomtr.api.similarity.ComparisonDtos.PairTraceResponse;
import co.edu.uniquindio.bibliomtr.config.BibliomtrProperties;
import co.edu.uniquindio.bibliomtr.evaluation.Stopwatch;
import co.edu.uniquindio.bibliomtr.evaluation.Stopwatch.Timed;
import co.edu.uniquindio.bibliomtr.persistence.article.Article;
import co.edu.uniquindio.bibliomtr.persistence.article.ArticleRepository;
import co.edu.uniquindio.bibliomtr.preprocessing.EditGranularity;
import co.edu.uniquindio.bibliomtr.preprocessing.TextPreprocessor;

/**
 * Comparación de artículos con los 8 resultados del Requerimiento 1 y traza de un par (spec 001, REQ-13, REQ-14,
 * REQ-16, REQ-20, REQ-22).
 *
 * <p><b>Comparación</b> ({@link #compare}):
 * <ol>
 *   <li>{@link ArticleSelectionValidator} revisa la selección (400 si falla).</li>
 *   <li>Preprocesamiento, cronometrado como {@code preprocessingMs}: carga de todos los abstracts seleccionables,
 *       tokens de cada uno ({@link TextPreprocessor#tokenize}), secuencias de edición según
 *       {@code bibliomtr.similarity.edit-granularity} (WORDS: los tokens; CHARACTERS:
 *       {@link TextPreprocessor#characters}) y el corpus de tokens de todos los seleccionables para el IDF, para
 *       que el valor de un par no dependa de la selección.</li>
 *   <li>Por cada {@link ComparisonAlgorithm} se cronometra el recorrido de los pares i ≤ j; cada similitud se
 *       refleja en (j, i), porque los 6 algoritmos son simétricos. La diagonal también se calcula: fijarla en 1
 *       metería lógica de los algoritmos en el andamiaje (constitución P1).</li>
 * </ol>
 * Si un stub lanza {@link UnsupportedOperationException} con prefijo {@value #PENDING_PREFIX}, ese resultado
 * queda PENDIENTE, sin matriz ni tiempo, y los demás siguen (REQ-20). Cualquier otra excepción se propaga
 * (HTTP 500). Una similitud dentro de [0,1] con margen {@link #SIMILARITY_TOLERANCE} se recorta a [0,1]; una NaN
 * o fuera de ese margen lanza {@link IllegalStateException} (HTTP 500).
 *
 * <p><b>Traza</b> ({@link #trace}): los mismos pasos 1 y 2 para el par, y un solo algoritmo que devuelve
 * {@code value}, {@code similarity} y {@code trace}, o PENDIENTE.
 */
@Service
public class ComparisonService {

	/** Prefijo del mensaje de un stub sin implementar (REQ-1). */
	public static final String PENDING_PREFIX = "TODO estudiante:";

	/**
	 * Margen de redondeo de punto flotante aceptado alrededor de [0,1] para la similitud que devuelve un algoritmo.
	 *
	 * <p><b>Por qué.</b> Una implementación correcta puede salirse de [0,1] solo por redondeo: el coseno de dos
	 * vectores idénticos puede dar 1 + 2⁻⁵² ≈ 1.0000000000000002. Una similitud s ∈ [−1e-9, 1 + 1e-9] se devuelve
	 * como min(1, max(0, s)); fuera de ese intervalo, o NaN, es un error del algoritmo.
	 *
	 * <p>Complejidad: O(1) en tiempo y espacio por comprobación.
	 */
	public static final double SIMILARITY_TOLERANCE = 1e-9;

	private final ArticleSelectionValidator validator;
	private final ArticleRepository repository;
	private final SimilarityCatalog catalog;
	private final EditGranularity editGranularity;
	private final TextPreprocessor preprocessor = new TextPreprocessor();

	/**
	 * Servicio sobre el validador, los artículos guardados y el catálogo de algoritmos.
	 *
	 * <p>Complejidad: O(1) en tiempo y espacio, más la carga de las 179 stopwords.
	 *
	 * @param validator  validación de la selección
	 * @param repository artículos guardados
	 * @param catalog    las 6 estrategias de similitud
	 * @param properties propiedades {@code bibliomtr.*}; se usa {@code similarity.edit-granularity}
	 */
	public ComparisonService(ArticleSelectionValidator validator, ArticleRepository repository,
			SimilarityCatalog catalog, BibliomtrProperties properties) {
		this.validator = validator;
		this.repository = repository;
		this.catalog = catalog;
		this.editGranularity = properties.similarity().editGranularity();
	}

	/**
	 * Compara los artículos pedidos con los 8 resultados (REQ-14, REQ-20, REQ-22; AC-13, AC-17, AC-19).
	 *
	 * <p>Complejidad: O(A · L) en tiempo y espacio de preprocesamiento, con A = artículos seleccionables y
	 * L = longitud de un abstract, más (N(N+1)/2) llamadas por algoritmo, con N = artículos pedidos, cada una con la
	 * complejidad del algoritmo; O(N²) en espacio por cada matriz.
	 *
	 * @param request ids de los artículos; puede ser nula (cuenta como 0 ids)
	 * @return artículos en el orden de la solicitud, tiempos y los 8 resultados
	 * @throws InvalidSelectionException si la selección es inválida (HTTP 400)
	 * @throws IllegalStateException     si un algoritmo devuelve una similitud NaN o fuera de [0,1] más el margen
	 *                                   {@link #SIMILARITY_TOLERANCE} (HTTP 500)
	 */
	public ComparisonResponse compare(ComparisonRequest request) {
		List<Article> articles = validator.validateComparison(request == null ? null : request.articleIds());
		Timed<PreparedArticles> prepared = Stopwatch.time(() -> prepare(articles));
		List<AlgorithmMatrix> results = new ArrayList<>(ComparisonAlgorithm.values().length);
		for (ComparisonAlgorithm algorithm : ComparisonAlgorithm.values()) {
			results.add(matrix(algorithm, prepared.value()));
		}
		return new ComparisonResponse(summaries(articles), editGranularity.name(), prepared.millis(), results);
	}

	/**
	 * Resultado y traza de un algoritmo para el par (a, b); {@code a = b} está permitido (REQ-16, REQ-20; AC-15).
	 *
	 * <p>Complejidad: O(A · L) en tiempo y espacio de preprocesamiento, más una llamada al algoritmo.
	 *
	 * @param algorithmSlug slug del resultado, p. ej. {@code levenshtein}
	 * @param a             id del primer artículo
	 * @param b             id del segundo artículo
	 * @return valor, similitud y traza, o PENDIENTE con la ubicación del stub
	 * @throws IllegalArgumentException si el algoritmo es desconocido o algún artículo no es seleccionable
	 *                                  (HTTP 400)
	 * @throws IllegalStateException    si la similitud es NaN o está fuera de [0,1] más el margen
	 *                                  {@link #SIMILARITY_TOLERANCE} (HTTP 500)
	 */
	public PairTraceResponse trace(String algorithmSlug, long a, long b) {
		ComparisonAlgorithm algorithm = ComparisonAlgorithm.fromSlug(algorithmSlug);
		List<Article> pair = validator.requireSelectable(List.of(a, b));
		PreparedArticles prepared = prepare(pair);
		ArticleSummary summaryA = ArticleSummary.from(pair.get(0));
		ArticleSummary summaryB = ArticleSummary.from(pair.get(1));
		try {
			Timed<SimilarityResult<?>> timed = Stopwatch.time(() -> run(algorithm, prepared, 0, 1));
			SimilarityResult<?> result = timed.value();
			double similarity = clampToUnitInterval(algorithm, result.similarity(), a, b);
			return new PairTraceResponse(algorithm.slug(), algorithm.label(), summaryA, summaryB,
					editGranularity.name(), AlgorithmStatus.CALCULADO.name(), result.value(), similarity,
					result.trace(), timed.millis(), null);
		} catch (UnsupportedOperationException e) {
			requirePending(e);
			return new PairTraceResponse(algorithm.slug(), algorithm.label(), summaryA, summaryB,
					editGranularity.name(), AlgorithmStatus.PENDIENTE.name(), null, null, null, null,
					algorithm.pendingLocation());
		}
	}

	/**
	 * Entradas ya preprocesadas de los artículos, alineadas por índice con la selección.
	 *
	 * @param ids           id de cada artículo, para los mensajes de error
	 * @param tokens        tokens de cada abstract (TF-IDF y Jaccard)
	 * @param editSequences símbolos de Levenshtein y Needleman-Wunsch según la granularidad
	 * @param corpus        tokens de todos los abstracts seleccionables, para el IDF
	 * @param specter2      embedding de SPECTER2 de cada artículo
	 * @param nomic         embedding de nomic-embed-text de cada artículo
	 */
	private record PreparedArticles(List<Long> ids, List<List<String>> tokens, List<List<String>> editSequences,
			List<List<String>> corpus, List<float[]> specter2, List<float[]> nomic) {
	}

	/**
	 * Preprocesamiento de la selección y del corpus de seleccionables. Los tokens de un artículo seleccionado se
	 * reutilizan del corpus, porque todo artículo seleccionado es seleccionable.
	 *
	 * <p>Complejidad: O(A · L) en tiempo y espacio, más O(N · d) por las copias de los vectores, con d = 768.
	 */
	private PreparedArticles prepare(List<Article> articles) {
		List<Article> selectable = repository.findAllByAbstractDetectedTrueOrderByFileNameAsc();
		Map<Long, List<String>> tokensById = new HashMap<>();
		List<List<String>> corpus = new ArrayList<>(selectable.size());
		for (Article article : selectable) {
			List<String> tokens = preprocessor.tokenize(article.getAbstractText());
			tokensById.put(article.getId(), tokens);
			corpus.add(tokens);
		}
		List<Long> ids = new ArrayList<>(articles.size());
		List<List<String>> tokens = new ArrayList<>(articles.size());
		List<List<String>> editSequences = new ArrayList<>(articles.size());
		List<float[]> specter2 = new ArrayList<>(articles.size());
		List<float[]> nomic = new ArrayList<>(articles.size());
		for (Article article : articles) {
			List<String> articleTokens = tokensById.computeIfAbsent(article.getId(),
					id -> preprocessor.tokenize(article.getAbstractText()));
			ids.add(article.getId());
			tokens.add(articleTokens);
			editSequences.add(switch (editGranularity) {
				case WORDS -> articleTokens;
				case CHARACTERS -> preprocessor.characters(article.getAbstractText());
			});
			specter2.add(article.getSpecter2Embedding());
			nomic.add(article.getNomicEmbedding());
		}
		return new PreparedArticles(ids, tokens, editSequences, List.copyOf(corpus), specter2, nomic);
	}

	/**
	 * Matriz N×N de un resultado, o PENDIENTE si su stub no está implementado.
	 *
	 * <p>Complejidad: N(N+1)/2 llamadas al algoritmo en tiempo y O(N²) en espacio.
	 */
	private AlgorithmMatrix matrix(ComparisonAlgorithm algorithm, PreparedArticles prepared) {
		int n = prepared.tokens().size();
		try {
			Timed<double[][]> timed = Stopwatch.time(() -> {
				double[][] matrix = new double[n][n];
				// Solo los pares i ≤ j: el valor se refleja en (j, i).
				for (int i = 0; i < n; i++) {
					for (int j = i; j < n; j++) {
						double similarity = clampToUnitInterval(algorithm, run(algorithm, prepared, i, j).similarity(),
								prepared.ids().get(i), prepared.ids().get(j));
						matrix[i][j] = similarity;
						matrix[j][i] = similarity;
					}
				}
				return matrix;
			});
			return new AlgorithmMatrix(algorithm.slug(), algorithm.label(), AlgorithmStatus.CALCULADO.name(),
					timed.value(), timed.millis(), null);
		} catch (UnsupportedOperationException e) {
			requirePending(e);
			return new AlgorithmMatrix(algorithm.slug(), algorithm.label(), AlgorithmStatus.PENDIENTE.name(), null,
					null, algorithm.pendingLocation());
		}
	}

	/**
	 * Ejecuta la estrategia del resultado sobre los artículos i y j.
	 *
	 * <p>Complejidad: la del algoritmo.
	 */
	private SimilarityResult<?> run(ComparisonAlgorithm algorithm, PreparedArticles prepared, int i, int j) {
		return switch (algorithm) {
			case LEVENSHTEIN -> catalog.levenshtein()
					.compare(prepared.editSequences().get(i), prepared.editSequences().get(j));
			case NEEDLEMAN_WUNSCH -> catalog.needlemanWunsch()
					.compare(prepared.editSequences().get(i), prepared.editSequences().get(j));
			case TFIDF_COSINE -> catalog.tfIdfCosine()
					.compare(prepared.tokens().get(i), prepared.tokens().get(j), prepared.corpus());
			case JACCARD -> catalog.jaccard().compare(prepared.tokens().get(i), prepared.tokens().get(j));
			case SPECTER2_COSINE -> catalog.cosine().compare(prepared.specter2().get(i), prepared.specter2().get(j));
			case SPECTER2_EUCLIDEAN -> catalog.euclidean()
					.compare(prepared.specter2().get(i), prepared.specter2().get(j));
			case NOMIC_COSINE -> catalog.cosine().compare(prepared.nomic().get(i), prepared.nomic().get(j));
			case NOMIC_EUCLIDEAN -> catalog.euclidean().compare(prepared.nomic().get(i), prepared.nomic().get(j));
		};
	}

	/**
	 * Recorta la similitud a [0,1] si está dentro del margen {@link #SIMILARITY_TOLERANCE}; la normalización la
	 * escribe el estudiante dentro del stub.
	 *
	 * <p>Complejidad: O(1) en tiempo y espacio.
	 *
	 * @throws IllegalStateException si la similitud es NaN o está fuera de [−tolerancia, 1 + tolerancia]
	 */
	private static double clampToUnitInterval(ComparisonAlgorithm algorithm, double similarity, long a, long b) {
		if (Double.isNaN(similarity) || similarity < -SIMILARITY_TOLERANCE || similarity > 1.0 + SIMILARITY_TOLERANCE) {
			throw new IllegalStateException("El algoritmo %s devolvió %s fuera de [0,1] para los artículos %d y %d"
					.formatted(algorithm.slug(), similarity, a, b));
		}
		return Math.min(1.0, Math.max(0.0, similarity));
	}

	/**
	 * Propaga la excepción si no es la de un stub sin implementar.
	 *
	 * <p>Complejidad: O(p) en tiempo, con p = longitud del prefijo, y O(1) en espacio.
	 */
	private static void requirePending(UnsupportedOperationException e) {
		String message = e.getMessage();
		if (message == null || !message.startsWith(PENDING_PREFIX)) {
			throw e;
		}
	}

	/**
	 * Resúmenes de los artículos en el mismo orden.
	 *
	 * <p>Complejidad: O(N · a) en tiempo y espacio, con a = autores por artículo.
	 */
	private static List<ArticleSummary> summaries(List<Article> articles) {
		List<ArticleSummary> summaries = new ArrayList<>(articles.size());
		for (Article article : articles) {
			summaries.add(ArticleSummary.from(article));
		}
		return summaries;
	}

}
