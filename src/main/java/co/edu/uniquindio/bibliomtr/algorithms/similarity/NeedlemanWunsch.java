package co.edu.uniquindio.bibliomtr.algorithms.similarity;

import java.util.List;
import java.util.Objects;

import co.edu.uniquindio.bibliomtr.trace.similarity.NeedlemanWunschTrace;

/**
 * Alineamiento global de Needleman-Wunsch entre dos secuencias de símbolos (Requerimiento 1, algoritmo
 * clásico). STUB: el cuerpo de {@link #compare} lo escribe el estudiante (constitución P1; ver
 * {@code docs/guia-implementacion.md}).
 *
 * <p><b>Qué calcula.</b> El puntaje del mejor alineamiento global de {@code a} y {@code b}, en el que cada
 * columna alinea dos símbolos o un símbolo con un hueco (gap), y el alineamiento que lo alcanza. Los puntajes
 * vienen de {@link NeedlemanWunschScoring}: la aplicación usa {@link NeedlemanWunschScoring#DEFAULT} =
 * (match = 1, mismatch = −1, gap = −1).
 *
 * <p><b>Fórmula.</b> Con m = |a|, n = |b|, g = gap, s(x, y) = match si x = y y mismatch si x ≠ y, y {@code F}
 * una matriz de (m + 1) × (n + 1):
 * <pre>
 *   F[i][0] = i · g                   para 0 ≤ i ≤ m
 *   F[0][j] = j · g                   para 0 ≤ j ≤ n
 *   F[i][j] = max( F[i−1][j−1] + s(aᵢ, bⱼ),       (DIAG: alinear aᵢ con bⱼ)
 *                  F[i−1][j] + g,                 (UP: aᵢ con gap)
 *                  F[i][j−1] + g )                (LEFT: gap con bⱼ)
 * </pre>
 * El valor crudo es {@code score = F[m][n]}. El alineamiento se reconstruye desde [m][n] siguiendo los
 * movimientos hasta [0][0].
 *
 * <p><b>Normalización sugerida.</b> Con (1, −1, −1):
 * <ul>
 *   <li>{@code maxScore = 2 · min(m, n) − max(m, n)}: min(m, n) coincidencias y max(m, n) − min(m, n)
 *       gaps.</li>
 *   <li>{@code minScore = −max(m, n)}: min(m, n) sustituciones más max(m, n) − min(m, n) gaps ya alcanzan
 *       −max(m, n), y el óptimo nunca queda por debajo.</li>
 *   <li>{@code similarity = (score − minScore) / (maxScore − minScore)}; {@code similarity = 1} si
 *       m = n = 0 y {@code similarity = 0} si solo una secuencia es vacía (ahí maxScore = minScore).</li>
 * </ul>
 *
 * <p><b>Traza.</b> {@link NeedlemanWunschTrace}: las secuencias, el esquema de puntajes, la matriz {@code F},
 * la matriz de movimientos ({@code "DIAG"}, {@code "UP"}, {@code "LEFT"}), el alineamiento (con {@code null}
 * en los gaps), {@code score}, {@code minScore} y {@code maxScore}.
 *
 * <p><b>Rango.</b> {@code value} ∈ [minScore, maxScore] (entero); {@code similarity} ∈ [0, 1].
 *
 * <p><b>Casos de borde.</b> Ambas vacías: score 0 y similitud 1. Solo una vacía: score = longitud · g y
 * similitud 0. Idénticas: score = m y similitud 1. Disjuntas ([a,b,c] y [x,y,z]): score −3 y similitud 0.
 * Longitudes muy distintas ([a,b] y [a, 9 × x]): score −8, maxScore −6, minScore −10 y similitud 0.5.
 *
 * <p><b>Granularidad.</b> Los símbolos pueden ser palabras o caracteres: es una decisión del estudiante que se
 * configura en {@code bibliomtr.similarity.edit-granularity} de {@code application.properties}
 * ({@code WORDS} o {@code CHARACTERS}, ver {@code EditGranularity}). El algoritmo no cambia: recibe una
 * secuencia genérica de símbolos.
 *
 * <p><b>Complejidad: TODO estudiante</b> (tiempo y espacio en función de m y n).
 */
public final class NeedlemanWunsch implements SequenceSimilarity<NeedlemanWunschTrace> {

	/** Esquema de puntajes con el que se llena la matriz F. */
	private final NeedlemanWunschScoring scoring;

	/**
	 * Crea el algoritmo con un esquema de puntajes.
	 *
	 * <p>Complejidad: O(1) en tiempo y espacio.
	 *
	 * @param scoring puntajes de match, mismatch y gap (la aplicación usa {@link NeedlemanWunschScoring#DEFAULT})
	 */
	public NeedlemanWunsch(NeedlemanWunschScoring scoring) {
		this.scoring = Objects.requireNonNull(scoring, "scoring");
	}

	/**
	 * Calcula el puntaje del alineamiento global de {@code a} y {@code b}, su similitud normalizada y la traza.
	 *
	 * <p>Complejidad: TODO estudiante.
	 *
	 * @param a primera secuencia de símbolos (no nula; puede ser vacía)
	 * @param b segunda secuencia de símbolos (no nula; puede ser vacía)
	 * @return {@code value} = F[m][n], {@code similarity} en [0,1] y la traza {@link NeedlemanWunschTrace}
	 */
	@Override
	public SimilarityResult<NeedlemanWunschTrace> compare(List<String> a, List<String> b) {
		throw new UnsupportedOperationException(
				"TODO estudiante: implementar NeedlemanWunsch.compare (ver docs/guia-implementacion.md)");
	}

}
