package co.edu.uniquindio.bibliomtr.api.similarity;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import co.edu.uniquindio.bibliomtr.api.similarity.ComparisonDtos.ComparisonRequest;
import co.edu.uniquindio.bibliomtr.api.similarity.ComparisonDtos.ComparisonResponse;
import co.edu.uniquindio.bibliomtr.api.similarity.ComparisonDtos.PairTraceResponse;

/**
 * Endpoints de la comparación de artículos y de la traza de un par (spec 001, REQ-13, REQ-14, REQ-16).
 *
 * <p><b>Errores 400</b> (mensajes en español, {@code ApiExceptionHandler}): selección inválida
 * ({@link ArticleSelectionValidator}), cuerpo ausente o que no es una lista de enteros, parámetro de la traza
 * ausente o no numérico, y algoritmo desconocido.
 */
@RestController
@RequestMapping("/api/similarity")
public class ComparisonController {

	private final ComparisonService comparisonService;

	/**
	 * Controlador sobre el servicio de comparación.
	 *
	 * <p>Complejidad: O(1) en tiempo y espacio.
	 *
	 * @param comparisonService servicio que ejecuta los algoritmos
	 */
	public ComparisonController(ComparisonService comparisonService) {
		this.comparisonService = comparisonService;
	}

	/**
	 * {@code POST /api/similarity/comparisons}: una matriz N×N por cada uno de los 8 resultados, con los tiempos
	 * de preprocesamiento y de cada algoritmo (REQ-13, REQ-14, REQ-20, REQ-22).
	 *
	 * <p>Complejidad: la de {@link ComparisonService#compare(ComparisonRequest)}.
	 *
	 * @param request {@code {"articleIds": [..]}} con 2 a 20 ids distintos
	 * @return la comparación
	 */
	@PostMapping("/comparisons")
	public ComparisonResponse compare(@RequestBody ComparisonRequest request) {
		return comparisonService.compare(request);
	}

	/**
	 * {@code GET /api/similarity/trace}: resultado y traza de un algoritmo para un par de artículos (REQ-16,
	 * REQ-20). {@code a = b} está permitido.
	 *
	 * <p>Complejidad: la de {@link ComparisonService#trace(String, long, long)}.
	 *
	 * @param algorithm slug del resultado, p. ej. {@code levenshtein}
	 * @param a         id del primer artículo
	 * @param b         id del segundo artículo
	 * @return la traza del par, o PENDIENTE
	 */
	@GetMapping("/trace")
	public PairTraceResponse trace(@RequestParam(name = "algorithm") String algorithm,
			@RequestParam(name = "a") long a, @RequestParam(name = "b") long b) {
		return comparisonService.trace(algorithm, a, b);
	}

}
