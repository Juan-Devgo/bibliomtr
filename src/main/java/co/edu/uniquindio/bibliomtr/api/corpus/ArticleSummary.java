package co.edu.uniquindio.bibliomtr.api.corpus;

import java.util.List;

import co.edu.uniquindio.bibliomtr.persistence.article.Article;

/**
 * Artículo seleccionable tal como lo ve la interfaz (spec 001, REQ-13): sin abstract ni vectores, que no hacen
 * falta para elegirlo.
 *
 * <p>Complejidad: O(a) en tiempo y espacio al construirse, con a = número de autores.
 *
 * @param id                identificador para pedir comparaciones y trazas
 * @param title             título del artículo
 * @param authors           autores en orden; vacía si no hay
 * @param fileName          nombre del PDF
 * @param specter2Truncated {@code true} si su texto de SPECTER2 se truncó a 512 tokens (REQ-9a)
 */
public record ArticleSummary(long id, String title, List<String> authors, String fileName,
		boolean specter2Truncated) {

	/**
	 * Resumen de un artículo guardado.
	 *
	 * <p>Complejidad: O(a) en tiempo y espacio, con a = número de autores.
	 *
	 * @param article artículo con id
	 * @return su resumen
	 */
	public static ArticleSummary from(Article article) {
		return new ArticleSummary(article.getId(), article.getTitle(), article.getAuthors(), article.getFileName(),
				article.isSpecter2Truncated());
	}

}
