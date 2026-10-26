package co.edu.uniquindio.bibliomtr.pdf;

import java.util.List;
import java.util.Objects;

/**
 * Datos que la ingesta toma de un PDF del corpus: título, autores y abstract (spec 001, REQ-8 y REQ-10).
 *
 * <p><b>Por qué.</b> Separa la lectura del PDF (PDFBox, constitución P2) de la ingesta y la persistencia: el
 * resto de la aplicación solo ve estos tres valores, nunca el documento.
 *
 * <p>Complejidad: O(a) en tiempo y espacio al construirse, con a = número de autores (copia inmutable).
 *
 * @param title título del artículo; no nulo ni vacío
 * @param authors autores en el orden de los metadatos; lista inmutable, vacía si no hay
 * @param abstractText abstract detectado, o {@code null} si no se detecta (el artículo queda como no
 *        seleccionable, REQ-10)
 */
public record ExtractedArticle(String title, List<String> authors, String abstractText) {

	/**
	 * Valida el título y copia los autores en una lista inmutable.
	 *
	 * @throws NullPointerException si {@code title} o {@code authors} son nulos
	 * @throws IllegalArgumentException si {@code title} está vacío
	 */
	public ExtractedArticle {
		Objects.requireNonNull(title, "title");
		if (title.isBlank()) {
			throw new IllegalArgumentException("El título no puede estar vacío");
		}
		authors = List.copyOf(Objects.requireNonNull(authors, "authors"));
	}

	/**
	 * Indica si se detectó el abstract.
	 *
	 * <p>Complejidad: O(1).
	 *
	 * @return {@code true} si {@code abstractText} no es nulo
	 */
	public boolean hasAbstract() {
		return abstractText != null;
	}

}
