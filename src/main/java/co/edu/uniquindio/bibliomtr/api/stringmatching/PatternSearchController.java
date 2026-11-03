package co.edu.uniquindio.bibliomtr.api.stringmatching;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import co.edu.uniquindio.bibliomtr.api.stringmatching.PatternSearchDtos.PatternSearchResponse;
import co.edu.uniquindio.bibliomtr.api.stringmatching.PatternSearchDtos.PatternTraceResponse;

/**
 * Endpoints de la búsqueda de patrones y de la traza de un algoritmo (spec 004, REQ-15, REQ-22).
 *
 * <p><b>Parámetros.</b> {@code pattern} y {@code q} son opcionales para Spring y llegan como texto, sin recortar:
 * {@link PatternSearchValidator} da el mensaje propio de un patrón ausente o vacío y de un q que no sea un primo
 * válido. {@code algorithm} y {@code articleId} son obligatorios.
 *
 * <p><b>Errores 400</b> (mensajes en español, {@code ApiExceptionHandler}): patrón vacío o de más de 200
 * {@code char}, q inválido, algoritmo ausente o desconocido, {@code articleId} ausente o no entero, y artículo
 * inexistente o sin abstract. <b>Error 500</b>: posiciones inválidas de un algoritmo implementado.
 */
@RestController
@RequestMapping("/api/pattern-search")
public class PatternSearchController {

	private final PatternSearchService patternSearchService;

	/**
	 * Controlador sobre el servicio de búsqueda de patrones.
	 *
	 * <p>Complejidad: O(1) en tiempo y espacio.
	 *
	 * @param patternSearchService servicio que ejecuta los algoritmos
	 */
	public PatternSearchController(PatternSearchService patternSearchService) {
		this.patternSearchService = patternSearchService;
	}

	/**
	 * {@code GET /api/pattern-search}: ocurrencias del patrón en todos los abstracts con los 3 algoritmos, con los
	 * tiempos de preprocesamiento y de búsqueda (REQ-15, REQ-16, REQ-18, REQ-28, REQ-29).
	 *
	 * <p>Complejidad: la de {@link PatternSearchService#search(String, String)}.
	 *
	 * @param pattern patrón de 1 a 200 {@code char}, usado tal como llega
	 * @param q       módulo primo de Rabin-Karp en [2, 1 000 000 007]; por defecto 101
	 * @return la búsqueda
	 */
	@GetMapping
	public PatternSearchResponse search(@RequestParam(name = "pattern", required = false) String pattern,
			@RequestParam(name = "q", required = false) String q) {
		return patternSearchService.search(pattern, q);
	}

	/**
	 * {@code GET /api/pattern-search/trace}: preprocesamiento, traza y posiciones de un algoritmo sobre el abstract
	 * de un artículo (REQ-22).
	 *
	 * <p>Complejidad: la de {@link PatternSearchService#trace(String, long, String, String)}.
	 *
	 * @param algorithm slug del algoritmo: {@code kmp}, {@code boyer-moore} o {@code rabin-karp}
	 * @param articleId id del artículo
	 * @param pattern   patrón de 1 a 200 {@code char}, usado tal como llega
	 * @param q         módulo primo de Rabin-Karp en [2, 1 000 000 007]; por defecto 101
	 * @return la traza, o PENDIENTE
	 */
	@GetMapping("/trace")
	public PatternTraceResponse trace(@RequestParam(name = "algorithm") String algorithm,
			@RequestParam(name = "articleId") long articleId,
			@RequestParam(name = "pattern", required = false) String pattern,
			@RequestParam(name = "q", required = false) String q) {
		return patternSearchService.trace(algorithm, articleId, pattern, q);
	}

}
