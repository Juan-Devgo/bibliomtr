package co.edu.uniquindio.bibliomtr.preprocessing;

/**
 * Granularidad de los símbolos que reciben las distancias de edición (Levenshtein y Needleman-Wunsch).
 *
 * <p><b>Qué es.</b> Indica cómo se convierte un abstract en la secuencia de símbolos que comparan los
 * algoritmos de edición:
 * <ul>
 *   <li>{@link #WORDS}: cada símbolo es un token de {@code TextPreprocessor.tokenize}.</li>
 *   <li>{@link #CHARACTERS}: cada símbolo es un carácter de {@code TextPreprocessor.characters},
 *       espacios incluidos.</li>
 * </ul>
 *
 * <p><b>Por qué.</b> La elección entre palabras o caracteres es una decisión de los estudiantes
 * (spec 001, REQ-3): los stubs operan sobre secuencias genéricas y la granularidad se configura en
 * {@code bibliomtr.similarity.edit-granularity} de {@code application.properties}, sin recompilar.
 *
 * <p>Complejidad: O(1) en tiempo y espacio.
 */
public enum EditGranularity {

	/** Cada símbolo de la secuencia es una palabra (token sin stopwords). */
	WORDS,

	/** Cada símbolo de la secuencia es un carácter del texto normalizado. */
	CHARACTERS
}
