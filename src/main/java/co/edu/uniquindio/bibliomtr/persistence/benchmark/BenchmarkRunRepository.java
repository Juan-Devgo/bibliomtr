package co.edu.uniquindio.bibliomtr.persistence.benchmark;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio del historial de mediciones JMH (REQ-19, REQ-21, REQ-28). Solo almacenamiento: métodos derivados
 * de Spring Data sin funciones de búsqueda de texto ni operadores de pgvector (AGENTS.md).
 */
public interface BenchmarkRunRepository extends JpaRepository<BenchmarkRun, Long> {

	/**
	 * Indica si hay alguna medición con el estado dado; con EN_CURSO decide el HTTP 409 (REQ-19).
	 *
	 * <p>Complejidad: O(R) en tiempo en la base (sin índice sobre {@code status}) y O(1) en espacio, con
	 * R = número de mediciones.
	 *
	 * @param status estado buscado
	 * @return {@code true} si existe al menos una
	 */
	boolean existsByStatus(BenchmarkStatus status);

	/**
	 * Mediciones con el estado dado; al arrancar, las EN_CURSO pasan a FALLIDA (REQ-21).
	 *
	 * <p>Complejidad: O(R) en tiempo en la base y O(r) en espacio, con r = mediciones devueltas.
	 *
	 * @param status estado buscado
	 * @return las mediciones con ese estado
	 */
	List<BenchmarkRun> findAllByStatus(BenchmarkStatus status);

	/**
	 * Historial completo, de la más reciente a la más antigua por {@code started_at} (REQ-28). El orden lo da
	 * {@code ORDER BY started_at DESC} en la consulta, no un ordenamiento en Java. Los resultados de cada
	 * medición se cargan en la misma consulta.
	 *
	 * <p>Complejidad: O(R log R) en tiempo en la base y O(R · a) en espacio, con a = resultados por medición.
	 *
	 * @return todas las mediciones con sus resultados
	 */
	@EntityGraph(attributePaths = "results")
	List<BenchmarkRun> findAllByOrderByStartedAtDesc();

}
