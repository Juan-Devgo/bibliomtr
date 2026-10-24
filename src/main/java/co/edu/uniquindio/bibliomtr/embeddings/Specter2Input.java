package co.edu.uniquindio.bibliomtr.embeddings;

import java.util.Objects;

/**
 * Texto de entrada de SPECTER2: {@code título [SEP] abstract} (spec 001, REQ-9; constitución 1.1.0, P5).
 *
 * <p><b>Qué hace.</b> Une el título y el abstract con el token separador de BERT, que el tokenizer reconoce
 * como token especial (id 103) y no como texto.
 *
 * <p><b>Por qué.</b> SPECTER2 se entrenó con ese formato, por lo que es el único modelo autorizado a recibir el
 * título (excepción de la constitución 1.1.0). nomic-embed-text y los algoritmos clásicos usan solo el
 * abstract.
 */
public final class Specter2Input {

	/** Separador entre título y abstract; el tokenizer lo convierte en el token especial [SEP]. */
	public static final String SEPARATOR = " [SEP] ";

	private Specter2Input() {
	}

	/**
	 * Arma {@code title + " [SEP] " + abstractText}.
	 *
	 * <p>Complejidad: O(t + a) en tiempo y espacio, con t y a = longitudes del título y del abstract.
	 *
	 * @param title        título del artículo; no nulo
	 * @param abstractText abstract del artículo; no nulo
	 * @return el texto que se tokeniza para SPECTER2
	 * @throws NullPointerException si algún argumento es nulo
	 */
	public static String of(String title, String abstractText) {
		Objects.requireNonNull(title, "title");
		Objects.requireNonNull(abstractText, "abstractText");
		return title + SEPARATOR + abstractText;
	}
}
