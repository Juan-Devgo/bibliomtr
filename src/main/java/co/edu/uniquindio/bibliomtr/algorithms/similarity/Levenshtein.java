package co.edu.uniquindio.bibliomtr.algorithms.similarity;

import java.util.List;

import co.edu.uniquindio.bibliomtr.trace.similarity.LevenshteinTrace;

/**
 * Distancia de Levenshtein entre dos secuencias de símbolos (Requerimiento 1, algoritmo clásico).
 * STUB: el cuerpo de {@link #compare} lo escribe el estudiante (constitución P1; ver
 * {@code docs/guia-implementacion.md}).
 *
 * <p><b>Qué calcula.</b> El número mínimo de operaciones de edición (inserción, eliminación o sustitución de
 * un símbolo, cada una de costo 1) que transforman la secuencia {@code a} en la secuencia {@code b}.
 *
 * <p><b>Fórmula.</b> Con m = |a|, n = |b| y {@code D} una matriz de (m + 1) × (n + 1):
 * <pre>
 *   D[i][0] = i                       para 0 ≤ i ≤ m
 *   D[0][j] = j                       para 0 ≤ j ≤ n
 *   D[i][j] = min( D[i−1][j] + 1,                 (eliminar aᵢ)
 *                  D[i][j−1] + 1,                 (insertar bⱼ)
 *                  D[i−1][j−1] + [aᵢ ≠ bⱼ] )      (sustituir o coincidir)
 * </pre>
 * donde {@code [aᵢ ≠ bⱼ]} vale 1 si los símbolos difieren y 0 si son iguales. El valor crudo es
 * {@code D[m][n]}.
 *
 * <p><b>Normalización sugerida.</b> {@code similarity = 1 − D[m][n] / max(m, n)}, y {@code similarity = 1}
 * si m = n = 0.
 *
 * <p><b>Traza.</b> {@link LevenshteinTrace}: las dos secuencias, la matriz {@code D} completa, la distancia
 * {@code D[m][n]} y {@code max(m, n)}.
 *
 * <p><b>Rango.</b> {@code value} ∈ [0, max(m, n)] (entero); {@code similarity} ∈ [0, 1].
 *
 * <p><b>Casos de borde.</b> Ambas vacías: distancia 0 y similitud 1. Una vacía: distancia igual a la longitud
 * de la otra y similitud 0. Idénticas: distancia 0 y similitud 1. Disjuntas de igual longitud
 * ([a,b,c] y [x,y,z]): distancia 3 y similitud 0. Longitudes muy distintas ([a] y 50 × a): distancia 49 y
 * similitud 0.02.
 *
 * <p><b>Granularidad.</b> Los símbolos pueden ser palabras o caracteres: es una decisión del estudiante que se
 * configura en {@code bibliomtr.similarity.edit-granularity} de {@code application.properties}
 * ({@code WORDS} o {@code CHARACTERS}, ver {@code EditGranularity}). El algoritmo no cambia: recibe una
 * secuencia genérica de símbolos.
 *
 * <p><b>Complejidad: TODO estudiante</b> (tiempo y espacio en función de m y n).
 */
public final class Levenshtein implements SequenceSimilarity<LevenshteinTrace> {

	/**
	 * Calcula la distancia de Levenshtein entre {@code a} y {@code b}, su similitud normalizada y la traza.
	 *
	 * <p>Complejidad: TODO estudiante.
	 *
	 * @param a primera secuencia de símbolos (no nula; puede ser vacía)
	 * @param b segunda secuencia de símbolos (no nula; puede ser vacía)
	 * @return {@code value} = D[m][n], {@code similarity} en [0,1] y la traza {@link LevenshteinTrace}
	 */
	@Override
	public SimilarityResult<LevenshteinTrace> compare(List<String> a, List<String> b) {
		throw new UnsupportedOperationException(
				"TODO estudiante: implementar Levenshtein.compare (ver docs/guia-implementacion.md)");
	}

}
