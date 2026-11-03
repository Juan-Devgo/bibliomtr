package co.edu.uniquindio.bibliomtr.api.clustering;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import co.edu.uniquindio.bibliomtr.algorithms.clustering.ClusteringResult;
import co.edu.uniquindio.bibliomtr.algorithms.clustering.DendrogramValidator;
import co.edu.uniquindio.bibliomtr.algorithms.clustering.InvalidDendrogramException;
import co.edu.uniquindio.bibliomtr.algorithms.clustering.Merge;
import co.edu.uniquindio.bibliomtr.api.clustering.ClusteringDtos.ClusteringRequest;
import co.edu.uniquindio.bibliomtr.api.clustering.ClusteringDtos.ClusteringResponse;
import co.edu.uniquindio.bibliomtr.api.clustering.ClusteringDtos.ClusteringTraceResponse;
import co.edu.uniquindio.bibliomtr.api.clustering.ClusteringDtos.DendrogramResult;
import co.edu.uniquindio.bibliomtr.api.clustering.ClusteringDtos.SourceSummary;
import co.edu.uniquindio.bibliomtr.api.corpus.ArticleSummary;
import co.edu.uniquindio.bibliomtr.api.similarity.AlgorithmStatus;
import co.edu.uniquindio.bibliomtr.api.similarity.ComparisonAlgorithm;
import co.edu.uniquindio.bibliomtr.api.similarity.ComparisonDtos.AlgorithmMatrix;
import co.edu.uniquindio.bibliomtr.api.similarity.ComparisonService;
import co.edu.uniquindio.bibliomtr.api.similarity.ComparisonService.SourceMatrix;
import co.edu.uniquindio.bibliomtr.api.similarity.InvalidSelectionException;
import co.edu.uniquindio.bibliomtr.evaluation.Stopwatch;
import co.edu.uniquindio.bibliomtr.evaluation.Stopwatch.Timed;

/**
 * Agrupa los artículos pedidos con los 4 algoritmos de clustering jerárquico (spec 003, REQ-7, REQ-10, REQ-11,
 * REQ-16, REQ-18, REQ-19, REQ-22).
 *
 * <p><b>Flujo.</b>
 * <ol>
 *   <li>Resuelve la fuente con {@link ComparisonAlgorithm#fromSource} (400 si es nula, vacía o desconocida).</li>
 *   <li>Pide a {@link ComparisonService#similarityMatrix} la matriz de esa fuente: misma validación de la
 *       selección, preprocesamiento y similitud que el Requerimiento 1, sin reimplementarlos.</li>
 *   <li>Si la fuente está PENDIENTE, responde sin distancias ni dendrogramas (REQ-19).</li>
 *   <li>Si no, arma D = 1 − S con la diagonal en 0 (REQ-10) y ejecuta cada algoritmo sobre una copia de D, de
 *       modo que un algoritmo que modifique su entrada no afecte a los demás.</li>
 *   <li>Un "TODO estudiante:" deja el algoritmo PENDIENTE (REQ-18); un resultado pasa por
 *       {@link DendrogramValidator#validate}, que lanza {@link InvalidDendrogramException} (HTTP 500) si el
 *       dendrograma es inválido (REQ-7).</li>
 * </ol>
 *
 * <p><b>Tiempos</b> (REQ-22): {@code preparationMs} = preprocesamiento + matriz de similitud; {@code elapsedMs}
 * = solo {@code cluster(D)}, sin la copia de D ni la validación.
 */
@Service
public class ClusteringService {

	private final ComparisonService comparisonService;
	private final ClusteringCatalog catalog;

	/**
	 * Servicio sobre la comparación del Requerimiento 1 y el catálogo de algoritmos.
	 *
	 * <p>Complejidad: O(1) en tiempo y espacio.
	 *
	 * @param comparisonService servicio que da la matriz de similitud de una fuente
	 * @param catalog           los 4 algoritmos de clustering
	 */
	public ClusteringService(ComparisonService comparisonService, ClusteringCatalog catalog) {
		this.comparisonService = comparisonService;
		this.catalog = catalog;
	}

	/**
	 * Dendrogramas de los 4 algoritmos para los artículos y la fuente pedidos (REQ-11, REQ-18, REQ-19, REQ-22;
	 * AC-11, AC-16, AC-17, AC-19).
	 *
	 * <p>Complejidad: la de {@link ComparisonService#similarityMatrix} más O(N²) en tiempo y espacio para D y sus
	 * copias, más la de cada algoritmo.
	 *
	 * @param request ids y fuente; puede ser nula (cuenta como sin fuente)
	 * @return artículos, fuente, tiempos, distancias y los 4 resultados
	 * @throws IllegalArgumentException   si la fuente es nula, vacía o desconocida (HTTP 400)
	 * @throws InvalidSelectionException  si la selección es inválida (HTTP 400)
	 * @throws InvalidDendrogramException si un algoritmo devuelve un dendrograma inválido (HTTP 500)
	 */
	public ClusteringResponse cluster(ClusteringRequest request) {
		ComparisonAlgorithm source = ComparisonAlgorithm.fromSource(request == null ? null : request.source());
		Prepared prepared = prepare(request.articleIds(), source);
		if (prepared.distances() == null) {
			return new ClusteringResponse(prepared.articles(), prepared.source(), null, null, List.of());
		}
		List<DendrogramResult> results = new ArrayList<>(ClusteringAlgorithm.values().length);
		for (ClusteringAlgorithm algorithm : ClusteringAlgorithm.values()) {
			Outcome outcome = run(algorithm, prepared.distances());
			results.add(outcome == null
					? new DendrogramResult(algorithm.slug(), algorithm.label(), AlgorithmStatus.PENDIENTE.name(), null,
							null, algorithm.pendingLocation())
					: new DendrogramResult(algorithm.slug(), algorithm.label(), AlgorithmStatus.CALCULADO.name(),
							outcome.merges(), outcome.elapsedMs(), null));
		}
		return new ClusteringResponse(prepared.articles(), prepared.source(), prepared.preparationMs(),
				prepared.distances(), results);
	}

	/**
	 * Dendrograma y traza de un algoritmo para los artículos y la fuente pedidos (REQ-16, REQ-18, REQ-19; AC-15).
	 * Valida en este orden: fuente, selección de artículos y algoritmo.
	 *
	 * <p>Complejidad: la de {@link #cluster} con un solo algoritmo.
	 *
	 * @param algorithmSlug slug del algoritmo, p. ej. {@code average-linkage}
	 * @param sourceSlug    slug de la fuente, p. ej. {@code specter2-cosine}
	 * @param articleIds    ids de los artículos, en el orden de las hojas
	 * @return la traza, o PENDIENTE (del algoritmo o de la fuente)
	 * @throws IllegalArgumentException   si la fuente o el algoritmo son desconocidos (HTTP 400)
	 * @throws InvalidSelectionException  si la selección es inválida (HTTP 400)
	 * @throws InvalidDendrogramException si el algoritmo devuelve un dendrograma inválido (HTTP 500)
	 */
	public ClusteringTraceResponse trace(String algorithmSlug, String sourceSlug, List<Long> articleIds) {
		ComparisonAlgorithm source = ComparisonAlgorithm.fromSource(sourceSlug);
		Prepared prepared = prepare(articleIds, source);
		ClusteringAlgorithm algorithm = ClusteringAlgorithm.fromSlug(algorithmSlug);
		if (prepared.distances() == null) {
			return new ClusteringTraceResponse(algorithm.slug(), algorithm.label(), prepared.articles(),
					prepared.source(), null, null, null, null, null, null, null);
		}
		Outcome outcome = run(algorithm, prepared.distances());
		if (outcome == null) {
			return new ClusteringTraceResponse(algorithm.slug(), algorithm.label(), prepared.articles(),
					prepared.source(), prepared.preparationMs(), prepared.distances(), AlgorithmStatus.PENDIENTE.name(),
					null, null, null, algorithm.pendingLocation());
		}
		return new ClusteringTraceResponse(algorithm.slug(), algorithm.label(), prepared.articles(), prepared.source(),
				prepared.preparationMs(), prepared.distances(), AlgorithmStatus.CALCULADO.name(), outcome.merges(),
				outcome.trace(), outcome.elapsedMs(), null);
	}

	/**
	 * Artículos, fuente y distancias listos para agrupar.
	 *
	 * @param articles      artículos en el orden de la solicitud
	 * @param source        fuente y su estado
	 * @param distances     D = 1 − S con diagonal 0; {@code null} si la fuente está PENDIENTE
	 * @param preparationMs preprocesamiento + matriz; {@code null} si la fuente está PENDIENTE
	 */
	private record Prepared(List<ArticleSummary> articles, SourceSummary source, double[][] distances,
			Double preparationMs) {
	}

	/**
	 * Fusiones validadas, traza y tiempo de un algoritmo con resultado.
	 *
	 * @param merges    fusiones con las alturas recortadas a [0,1]
	 * @param trace     traza del algoritmo
	 * @param elapsedMs tiempo en ms de {@code cluster}
	 */
	private record Outcome(List<Merge> merges, Object trace, double elapsedMs) {
	}

	/**
	 * Valida la selección, calcula la matriz de la fuente y la convierte en distancias.
	 *
	 * <p>Complejidad: la de {@link ComparisonService#similarityMatrix} más O(N²) en tiempo y espacio.
	 */
	private Prepared prepare(List<Long> articleIds, ComparisonAlgorithm source) {
		SourceMatrix sourceMatrix = comparisonService.similarityMatrix(articleIds, source);
		AlgorithmMatrix matrix = sourceMatrix.matrix();
		SourceSummary summary = new SourceSummary(source.slug(), source.label(), matrix.status(),
				matrix.pendingLocation());
		if (matrix.matrix() == null) {
			return new Prepared(sourceMatrix.articles(), summary, null, null);
		}
		return new Prepared(sourceMatrix.articles(), summary, distances(matrix.matrix()),
				sourceMatrix.preprocessingMs() + matrix.elapsedMs());
	}

	/**
	 * D[i][j] = 1 − S[i][j] para i ≠ j y D[i][i] = 0 (REQ-10). S ya está recortada a [0,1], así que D también.
	 *
	 * <p>Complejidad: O(N²) en tiempo y espacio.
	 */
	private static double[][] distances(double[][] similarities) {
		int n = similarities.length;
		double[][] distances = new double[n][n];
		for (int i = 0; i < n; i++) {
			for (int j = 0; j < n; j++) {
				distances[i][j] = i == j ? 0.0 : 1.0 - similarities[i][j];
			}
		}
		return distances;
	}

	/**
	 * Ejecuta un algoritmo sobre una copia de D y valida su dendrograma.
	 *
	 * <p>Complejidad: O(N²) en tiempo y espacio por la copia más la del algoritmo.
	 *
	 * @return el resultado, o {@code null} si el stub no está implementado
	 * @throws InvalidDendrogramException si el dendrograma es inválido
	 */
	private Outcome run(ClusteringAlgorithm algorithm, double[][] distances) {
		double[][] input = copy(distances);
		Timed<? extends ClusteringResult<?>> timed;
		try {
			timed = Stopwatch.time(() -> catalog.strategy(algorithm).cluster(input));
		} catch (UnsupportedOperationException e) {
			requirePending(e);
			return null;
		}
		ClusteringResult<?> result = timed.value();
		if (result == null) {
			throw new InvalidDendrogramException(
					"El algoritmo %s devolvió un dendrograma inválido: el resultado es nulo".formatted(algorithm.slug()));
		}
		List<Merge> merges = DendrogramValidator.validate(algorithm.slug(), distances.length, result.merges());
		return new Outcome(merges, result.trace(), timed.millis());
	}

	/**
	 * Propaga la excepción si no es la de un stub sin implementar.
	 *
	 * <p>Complejidad: O(p) en tiempo, con p = longitud del prefijo, y O(1) en espacio.
	 */
	private static void requirePending(UnsupportedOperationException e) {
		String message = e.getMessage();
		if (message == null || !message.startsWith(ComparisonService.PENDING_PREFIX)) {
			throw e;
		}
	}

	/**
	 * Copia profunda de una matriz.
	 *
	 * <p>Complejidad: O(N²) en tiempo y espacio.
	 */
	private static double[][] copy(double[][] matrix) {
		double[][] copy = new double[matrix.length][];
		for (int i = 0; i < matrix.length; i++) {
			copy[i] = matrix[i].clone();
		}
		return copy;
	}

}
