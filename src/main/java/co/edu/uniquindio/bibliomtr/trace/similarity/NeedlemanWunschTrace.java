package co.edu.uniquindio.bibliomtr.trace.similarity;

import java.util.List;

import co.edu.uniquindio.bibliomtr.algorithms.similarity.NeedlemanWunschScoring;

/**
 * Traza de Needleman-Wunsch: puntajes, matriz F, movimientos, alineamiento y cotas de la normalización.
 *
 * <p><b>Qué guarda.</b> Con m = |a| y n = |b|:
 * <ul>
 *   <li>{@code scores}: la matriz F de (m + 1) × (n + 1), con {@code F[i][j]} = mejor puntaje de alinear los
 *       prefijos {@code a[0..i)} y {@code b[0..j)}.</li>
 *   <li>{@code moves}: de (m + 1) × (n + 1), el movimiento elegido en cada celda: {@code "DIAG"} (alinear
 *       {@code aᵢ} con {@code bⱼ}), {@code "UP"} ({@code aᵢ} con gap) o {@code "LEFT"} (gap con {@code bⱼ}).
 *       La celda [0][0] no tiene movimiento y puede ser {@code null}.</li>
 *   <li>{@code alignment}: el alineamiento óptimo reconstruido desde [m][n], en orden de izquierda a
 *       derecha.</li>
 *   <li>{@code score} = {@code F[m][n]}; {@code minScore} y {@code maxScore}: las cotas de la normalización
 *       sugerida en {@code NeedlemanWunsch}.</li>
 * </ul>
 * Ejemplo (AC-7): g,a,t,o y p,a,t,o con (1, −1, −1) dan {@code score = 2}, {@code maxScore = 4} y
 * {@code minScore = −4}.
 *
 * <p>Complejidad: construcción O(m · n) en tiempo y espacio por las copias defensivas.
 *
 * @param a         primera secuencia
 * @param b         segunda secuencia
 * @param scoring   esquema de puntajes usado
 * @param scores    matriz F de (m + 1) × (n + 1)
 * @param moves     movimientos por celda, de (m + 1) × (n + 1)
 * @param alignment alineamiento óptimo de izquierda a derecha
 * @param score     puntaje óptimo F[m][n]
 * @param minScore  cota inferior del puntaje usada para normalizar
 * @param maxScore  cota superior del puntaje usada para normalizar
 */
public record NeedlemanWunschTrace(List<String> a, List<String> b, NeedlemanWunschScoring scoring, int[][] scores,
		String[][] moves, List<AlignedPair> alignment, int score, int minScore, int maxScore) {

	/**
	 * Copia defensiva de listas y matrices para que la traza sea inmutable.
	 *
	 * <p>Complejidad: O(m · n) en tiempo y espacio.
	 */
	public NeedlemanWunschTrace {
		a = List.copyOf(a);
		b = List.copyOf(b);
		scores = copy(scores);
		moves = copy(moves);
		alignment = List.copyOf(alignment);
	}

	/**
	 * Matriz F de puntajes.
	 *
	 * <p>Complejidad: O(m · n) en tiempo y espacio (copia).
	 *
	 * @return copia de la matriz
	 */
	@Override
	public int[][] scores() {
		return copy(scores);
	}

	/**
	 * Movimientos elegidos por celda.
	 *
	 * <p>Complejidad: O(m · n) en tiempo y espacio (copia).
	 *
	 * @return copia de la matriz de movimientos
	 */
	@Override
	public String[][] moves() {
		return copy(moves);
	}

	/** Copia profunda de una matriz de enteros; O(m · n) en tiempo y espacio. */
	private static int[][] copy(int[][] source) {
		int[][] target = new int[source.length][];
		for (int i = 0; i < source.length; i++) {
			target[i] = source[i].clone();
		}
		return target;
	}

	/** Copia profunda de una matriz de cadenas (admite celdas nulas); O(m · n) en tiempo y espacio. */
	private static String[][] copy(String[][] source) {
		String[][] target = new String[source.length][];
		for (int i = 0; i < source.length; i++) {
			target[i] = source[i].clone();
		}
		return target;
	}

	/**
	 * Una columna del alineamiento: el símbolo de cada secuencia, o {@code null} si es un gap.
	 *
	 * <p>Complejidad: construcción O(1) en tiempo y espacio.
	 *
	 * @param fromA símbolo de {@code a}, o {@code null} si es un gap
	 * @param fromB símbolo de {@code b}, o {@code null} si es un gap
	 */
	public record AlignedPair(String fromA, String fromB) {
	}

}
