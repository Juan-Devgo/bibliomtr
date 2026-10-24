package co.edu.uniquindio.bibliomtr.persistence.article;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio de los artículos del corpus (spec 001, REQ-8, REQ-10, REQ-13). Solo almacenamiento: métodos
 * derivados de Spring Data sin {@code Like}, {@code Containing} ni operadores de pgvector (AGENTS.md).
 *
 * <p>La spec 002 usa {@link #findAllByAbstractDetectedTrueOrderByFileNameAsc()} en su adaptador
 * {@code SelectableAbstractTokenSource}, para el ranking de términos sobre los mismos abstracts.
 */
public interface ArticleRepository extends JpaRepository<Article, Long> {

	/**
	 * Indica si ya se ingirió un PDF con este contenido; así un reinicio no duplica artículos (AC-11).
	 *
	 * <p>Complejidad: O(log A) en tiempo en la base (índice único de {@code file_sha256}) y O(1) en espacio, con
	 * A = número de artículos.
	 *
	 * @param fileSha256 SHA-256 del contenido en hexadecimal
	 * @return {@code true} si existe
	 */
	boolean existsByFileSha256(String fileSha256);

	/**
	 * Artículos seleccionables (con abstract detectado), ordenados por nombre de archivo (REQ-10, REQ-13). El
	 * orden lo da {@code ORDER BY file_name} en la consulta, no un ordenamiento en Java.
	 *
	 * <p>Complejidad: O(A log A) en tiempo en la base y O(A · d) en espacio, con d = 768 por los vectores.
	 *
	 * @return los artículos seleccionables; vacía si no hay
	 */
	List<Article> findAllByAbstractDetectedTrueOrderByFileNameAsc();

	/**
	 * Artículos seleccionables entre los ids dados; los que falten no existen o no tienen abstract (REQ-13,
	 * REQ-15).
	 *
	 * <p>Complejidad: O(k log A) en tiempo en la base (índice de la clave primaria) y O(k · d) en espacio, con
	 * k = número de ids.
	 *
	 * @param ids identificadores pedidos
	 * @return los artículos seleccionables encontrados, sin orden garantizado
	 */
	List<Article> findAllByIdInAndAbstractDetectedTrue(Collection<Long> ids);

}
