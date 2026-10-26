package co.edu.uniquindio.bibliomtr.api.benchmark;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import co.edu.uniquindio.bibliomtr.api.benchmark.BenchmarkDtos.BenchmarkRunDto;

/**
 * Endpoints de las mediciones JMH del Requerimiento 2 (REQ-17, REQ-19, REQ-28).
 */
@RestController
@RequestMapping("/api/benchmarks")
public class BenchmarkController {

	private final BenchmarkService benchmarkService;

	/**
	 * Controlador sobre el servicio de mediciones.
	 *
	 * <p>Complejidad: O(1) en tiempo y espacio.
	 *
	 * @param benchmarkService servicio que lanza y lista las mediciones
	 */
	public BenchmarkController(BenchmarkService benchmarkService) {
		this.benchmarkService = benchmarkService;
	}

	/**
	 * {@code POST /api/benchmarks}: lanza una medición JMH de los tres algoritmos sobre la lista de frecuencias
	 * del corpus completo. Responde 202 porque la medición sigue corriendo; si ya hay una EN_CURSO, 409 con
	 * "Ya hay una medición en curso" (REQ-17, REQ-19).
	 *
	 * <p>Complejidad: la de {@link BenchmarkService#start()}.
	 *
	 * @return la medición EN_CURSO
	 */
	@PostMapping
	@ResponseStatus(HttpStatus.ACCEPTED)
	public BenchmarkRunDto start() {
		return benchmarkService.start();
	}

	/**
	 * {@code GET /api/benchmarks}: historial completo, sin paginar, de la más reciente a la más antigua
	 * (REQ-28).
	 *
	 * <p>Complejidad: la de {@link BenchmarkService#history()}.
	 *
	 * @return todas las mediciones
	 */
	@GetMapping
	public List<BenchmarkRunDto> history() {
		return benchmarkService.history();
	}

}
