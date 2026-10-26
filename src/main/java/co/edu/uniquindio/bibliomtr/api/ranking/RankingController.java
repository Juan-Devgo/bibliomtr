package co.edu.uniquindio.bibliomtr.api.ranking;

import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import co.edu.uniquindio.bibliomtr.algorithms.ranking.SortAlgorithm;
import co.edu.uniquindio.bibliomtr.algorithms.ranking.SortOptions;
import co.edu.uniquindio.bibliomtr.api.ranking.RankingDtos.RankingResponse;
import co.edu.uniquindio.bibliomtr.api.ranking.RankingDtos.SortTraceResponse;

/**
 * Endpoints del ranking de términos y de la traza de cada algoritmo (REQ-3, REQ-4, REQ-9, REQ-16).
 *
 * <p><b>Validación.</b> {@code n} y {@code bucketSize} son enteros mayores o iguales a 1, validados con
 * {@code @Min(1)} por la validación de métodos de Spring MVC; un valor no entero o menor que 1 da HTTP 400
 * con un mensaje en español ({@code ApiExceptionHandler}). Un algoritmo desconocido también da HTTP 400.
 */
@RestController
@RequestMapping("/api/ranking")
public class RankingController {

	/** Tamaño por defecto del top N (REQ-3). */
	static final String DEFAULT_N = "20";

	/** Ancho de cubeta por defecto, tomado de {@link SortOptions#DEFAULT_BUCKET_SIZE} (REQ-9). */
	static final String DEFAULT_BUCKET_SIZE = "" + SortOptions.DEFAULT_BUCKET_SIZE;

	private final RankingService rankingService;

	/**
	 * Controlador sobre el servicio de ranking.
	 *
	 * <p>Complejidad: O(1) en tiempo y espacio.
	 *
	 * @param rankingService servicio que ejecuta los algoritmos
	 */
	public RankingController(RankingService rankingService) {
		this.rankingService = rankingService;
	}

	/**
	 * {@code GET /api/ranking}: top N de los tres algoritmos sobre el corpus completo, indicador de
	 * coincidencia y tiempos en ms medidos sin traza (REQ-3, REQ-11). Si {@code n} supera el número de
	 * términos devuelve todos sin error.
	 *
	 * <p>Complejidad: la de {@link RankingService#rank(int, int)}.
	 *
	 * @param n          número de términos del top, entero mayor o igual a 1 (default 20)
	 * @param bucketSize ancho de cubeta de Bucket Sort, entero mayor o igual a 1 (default 5)
	 * @return ranking de los tres algoritmos
	 */
	@GetMapping
	public RankingResponse ranking(
			@RequestParam(name = "n", defaultValue = DEFAULT_N) @Min(1) int n,
			@RequestParam(name = "bucketSize", defaultValue = DEFAULT_BUCKET_SIZE) @Min(1) int bucketSize) {
		return rankingService.rank(n, bucketSize);
	}

	/**
	 * {@code GET /api/ranking/trace/{algorithm}}: traza completa de un algoritmo sobre la lista de
	 * frecuencias del corpus completo (REQ-16, AC-23, AC-25). {@code bucketSize} solo afecta a Bucket Sort.
	 *
	 * <p>Complejidad: la de {@link RankingService#trace(SortAlgorithm, int)}.
	 *
	 * @param algorithm  {@code counting}, {@code radix} o {@code bucket}
	 * @param bucketSize ancho de cubeta de Bucket Sort, entero mayor o igual a 1 (default 5)
	 * @return entrada, salida, ranking y traza del algoritmo
	 * @throws IllegalArgumentException si el algoritmo es desconocido (HTTP 400)
	 */
	@GetMapping("/trace/{algorithm}")
	public SortTraceResponse trace(
			@PathVariable("algorithm") String algorithm,
			@RequestParam(name = "bucketSize", defaultValue = DEFAULT_BUCKET_SIZE) @Min(1) int bucketSize) {
		return rankingService.trace(SortAlgorithm.fromSlug(algorithm), bucketSize);
	}

}
