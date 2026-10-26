package co.edu.uniquindio.bibliomtr.api.corpus;

import java.util.ArrayList;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import co.edu.uniquindio.bibliomtr.persistence.article.Article;
import co.edu.uniquindio.bibliomtr.persistence.article.ArticleRepository;

/**
 * {@code GET /api/articles}: artículos que se pueden seleccionar para comparar (spec 001, REQ-10, REQ-13; AC-9,
 * AC-10).
 *
 * <p>Solo devuelve los artículos con abstract detectado, ordenados por nombre de archivo; el orden lo da la
 * consulta ({@code ORDER BY file_name}). Con el corpus vacío devuelve {@code []}.
 */
@RestController
@RequestMapping("/api/articles")
public class ArticleController {

	private final ArticleRepository repository;

	/**
	 * Controlador sobre el repositorio de artículos.
	 *
	 * <p>Complejidad: O(1) en tiempo y espacio.
	 *
	 * @param repository artículos guardados
	 */
	public ArticleController(ArticleRepository repository) {
		this.repository = repository;
	}

	/**
	 * Lista los artículos seleccionables.
	 *
	 * <p>Complejidad: O(A log A) en tiempo en la base más O(A · a) en Java, y O(A · d) en espacio por los vectores
	 * cargados, con A = artículos seleccionables, a = autores por artículo y d = 768.
	 *
	 * @return los resúmenes en orden de {@code file_name}; vacía si no hay
	 */
	@GetMapping
	public List<ArticleSummary> list() {
		List<Article> articles = repository.findAllByAbstractDetectedTrueOrderByFileNameAsc();
		List<ArticleSummary> summaries = new ArrayList<>(articles.size());
		for (Article article : articles) {
			summaries.add(ArticleSummary.from(article));
		}
		return summaries;
	}

}
