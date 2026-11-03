package co.edu.uniquindio.bibliomtr.api.clustering;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import co.edu.uniquindio.bibliomtr.api.clustering.ClusteringDtos.ClusteringRequest;
import co.edu.uniquindio.bibliomtr.api.clustering.ClusteringDtos.ClusteringResponse;
import co.edu.uniquindio.bibliomtr.api.clustering.ClusteringDtos.ClusteringTraceResponse;

/**
 * Endpoints del clustering jerárquico y de la traza de un algoritmo (spec 003, REQ-11, REQ-16).
 *
 * <p><b>Errores 400</b> (mensajes en español, {@code ApiExceptionHandler}): fuente ausente o desconocida,
 * selección inválida (mismos mensajes que la comparación del Requerimiento 1), cuerpo ilegible, parámetro de la
 * traza ausente o no numérico y algoritmo desconocido. <b>Error 500</b>: dendrograma inválido.
 */
@RestController
@RequestMapping("/api/clustering")
public class ClusteringController {

	private final ClusteringService clusteringService;

	/**
	 * Controlador sobre el servicio de clustering.
	 *
	 * <p>Complejidad: O(1) en tiempo y espacio.
	 *
	 * @param clusteringService servicio que ejecuta los algoritmos
	 */
	public ClusteringController(ClusteringService clusteringService) {
		this.clusteringService = clusteringService;
	}

	/**
	 * {@code POST /api/clustering}: un dendrograma por cada uno de los 4 algoritmos, con las distancias y los
	 * tiempos (REQ-11, REQ-18, REQ-19, REQ-22).
	 *
	 * <p>Complejidad: la de {@link ClusteringService#cluster(ClusteringRequest)}.
	 *
	 * @param request {@code {"articleIds": [..], "source": "specter2-cosine"}} con 2 a 20 ids distintos
	 * @return el clustering
	 */
	@PostMapping
	public ClusteringResponse cluster(@RequestBody ClusteringRequest request) {
		return clusteringService.cluster(request);
	}

	/**
	 * {@code GET /api/clustering/trace}: dendrograma y traza de un algoritmo (REQ-16).
	 *
	 * <p>Complejidad: la de {@link ClusteringService#trace(String, String, List)}.
	 *
	 * @param algorithm  slug del algoritmo, p. ej. {@code single-linkage}
	 * @param source     slug de la fuente de similitud, p. ej. {@code specter2-cosine}
	 * @param articleIds ids separados por comas, en el orden de las hojas
	 * @return la traza, o PENDIENTE
	 */
	@GetMapping("/trace")
	public ClusteringTraceResponse trace(@RequestParam(name = "algorithm") String algorithm,
			@RequestParam(name = "source") String source, @RequestParam(name = "articleIds") List<Long> articleIds) {
		return clusteringService.trace(algorithm, source, articleIds);
	}

}
