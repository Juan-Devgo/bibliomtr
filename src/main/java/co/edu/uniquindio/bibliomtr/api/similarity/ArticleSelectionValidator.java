package co.edu.uniquindio.bibliomtr.api.similarity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

import org.springframework.stereotype.Component;

import co.edu.uniquindio.bibliomtr.persistence.article.Article;
import co.edu.uniquindio.bibliomtr.persistence.article.ArticleRepository;

/**
 * Valida la selección de artículos de una comparación o de una traza (spec 001, REQ-13, REQ-15, AC-14).
 *
 * <p><b>Reglas, en este orden.</b> La primera que falla lanza {@link InvalidSelectionException} (HTTP 400):
 * <ol>
 *   <li>Identificadores nulos dentro de la lista, p. ej. {@code [1, null]}: "El cuerpo debe ser un JSON con
 *       articleIds: lista de enteros".</li>
 *   <li>Cantidad: entre {@value #MIN_ARTICLES} y {@value #MAX_ARTICLES}; una lista nula cuenta como 0.
 *       "Debe seleccionar entre 2 y 20 artículos; se recibieron &lt;n&gt;".</li>
 *   <li>Repetidos: "Los identificadores de artículo no deben repetirse: &lt;ids&gt;", con cada id repetido una
 *       vez, en el orden de su primera repetición.</li>
 *   <li>Existencia: cada id debe existir y tener abstract detectado (REQ-10). "Artículos inexistentes o no
 *       seleccionables: &lt;ids&gt;", en el orden de la solicitud.</li>
 * </ol>
 * Los ids de los mensajes se separan con ", ".
 */
@Component
public class ArticleSelectionValidator {

	/** Mínimo de artículos de una comparación (REQ-13). */
	public static final int MIN_ARTICLES = 2;

	/** Máximo de artículos de una comparación: el corpus tiene 20 (REQ-13; constitución P5). */
	public static final int MAX_ARTICLES = 20;

	/** Mensaje de un cuerpo que no es una lista de enteros (también lo usa {@code ApiExceptionHandler}). */
	public static final String INVALID_BODY_MESSAGE = "El cuerpo debe ser un JSON con articleIds: lista de enteros";

	private final ArticleRepository repository;

	/**
	 * Validador sobre el repositorio de artículos.
	 *
	 * <p>Complejidad: O(1) en tiempo y espacio.
	 *
	 * @param repository artículos guardados
	 */
	public ArticleSelectionValidator(ArticleRepository repository) {
		this.repository = repository;
	}

	/**
	 * Valida los ids de una comparación y devuelve sus artículos en el orden de la solicitud.
	 *
	 * <p>Complejidad: O(k) en tiempo esperado en Java (conjuntos y mapas hash) más la consulta
	 * O(k log A) en la base, y O(k · d) en espacio por los vectores cargados, con k = número de ids, A = número de
	 * artículos y d = 768.
	 *
	 * @param articleIds ids pedidos; puede ser nula
	 * @return los artículos, en el orden de {@code articleIds}
	 * @throws InvalidSelectionException si falla alguna de las reglas
	 */
	public List<Article> validateComparison(List<Long> articleIds) {
		int count = articleIds == null ? 0 : articleIds.size();
		if (articleIds != null) {
			requireNoNulls(articleIds);
		}
		if (count < MIN_ARTICLES || count > MAX_ARTICLES) {
			throw new InvalidSelectionException("Debe seleccionar entre %d y %d artículos; se recibieron %d"
					.formatted(MIN_ARTICLES, MAX_ARTICLES, count));
		}
		// Repetidos: un id es repetido si ya estaba en "seen"; LinkedHashSet conserva el orden y no duplica.
		Set<Long> seen = new HashSet<>();
		Set<Long> repeated = new LinkedHashSet<>();
		for (Long id : articleIds) {
			if (!seen.add(id)) {
				repeated.add(id);
			}
		}
		if (!repeated.isEmpty()) {
			throw new InvalidSelectionException(
					"Los identificadores de artículo no deben repetirse: " + join(repeated));
		}
		return requireSelectable(articleIds);
	}

	/**
	 * Exige que cada id exista y sea seleccionable, sin regla de cantidad ni de repetidos; la usa la traza de un
	 * par, donde {@code a = b} está permitido.
	 *
	 * <p>Complejidad: O(k) en tiempo esperado en Java más la consulta O(k log A) en la base, y O(k · d) en
	 * espacio, con k = número de ids.
	 *
	 * @param articleIds ids pedidos, no nulos; puede haber repetidos
	 * @return los artículos, en el orden de {@code articleIds} (repetidos incluidos)
	 * @throws InvalidSelectionException si algún id no existe o no tiene abstract detectado
	 */
	public List<Article> requireSelectable(List<Long> articleIds) {
		requireNoNulls(articleIds);
		Map<Long, Article> found = new HashMap<>();
		for (Article article : repository.findAllByIdInAndAbstractDetectedTrue(new LinkedHashSet<>(articleIds))) {
			found.put(article.getId(), article);
		}
		// La consulta no garantiza orden: se reconstruye el de la solicitud y se anotan los que faltan.
		List<Article> ordered = new ArrayList<>(articleIds.size());
		Set<Long> missing = new LinkedHashSet<>();
		for (Long id : articleIds) {
			Article article = found.get(id);
			if (article == null) {
				missing.add(id);
			} else {
				ordered.add(article);
			}
		}
		if (!missing.isEmpty()) {
			throw new InvalidSelectionException("Artículos inexistentes o no seleccionables: " + join(missing));
		}
		return ordered;
	}

	/**
	 * Rechaza una lista con ids nulos ({@code [1, null]}), que Jackson acepta pero no son enteros.
	 *
	 * <p>Complejidad: O(k) en tiempo y O(1) en espacio.
	 */
	private static void requireNoNulls(List<Long> articleIds) {
		for (Long id : articleIds) {
			if (id == null) {
				throw new InvalidSelectionException(INVALID_BODY_MESSAGE);
			}
		}
	}

	/**
	 * Ids separados por ", ".
	 *
	 * <p>Complejidad: O(k) en tiempo y espacio.
	 */
	private static String join(Iterable<Long> ids) {
		StringJoiner joiner = new StringJoiner(", ");
		for (Long id : ids) {
			joiner.add(String.valueOf(id));
		}
		return joiner.toString();
	}

}
