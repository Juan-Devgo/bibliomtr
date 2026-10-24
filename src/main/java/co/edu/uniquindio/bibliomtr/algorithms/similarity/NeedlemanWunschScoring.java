package co.edu.uniquindio.bibliomtr.algorithms.similarity;

/**
 * Esquema de puntajes de Needleman-Wunsch: coincidencia, sustitución y gap.
 *
 * <p><b>Qué es.</b> Con símbolos {@code x}, {@code y}: la función de sustitución es
 * {@code s(x, y) = match} si {@code x = y} y {@code s(x, y) = mismatch} si {@code x ≠ y}; cada gap (símbolo
 * alineado con un hueco) suma {@code gap}.
 *
 * <p><b>Por qué fijo.</b> La aplicación usa siempre {@link #DEFAULT} = (1, −1, −1). Se rechazaron puntajes
 * configurables porque cambiarían los valores esperados de los tests y las cotas {@code minScore} y
 * {@code maxScore} de la normalización documentada en {@code NeedlemanWunsch}.
 *
 * <p>Complejidad: construcción O(1) en tiempo y espacio.
 *
 * @param match    puntaje de alinear dos símbolos iguales
 * @param mismatch puntaje de alinear dos símbolos distintos
 * @param gap      puntaje de alinear un símbolo con un hueco
 */
public record NeedlemanWunschScoring(int match, int mismatch, int gap) {

	/** Esquema por defecto: match = 1, mismatch = −1, gap = −1. */
	public static final NeedlemanWunschScoring DEFAULT = new NeedlemanWunschScoring(1, -1, -1);

}
