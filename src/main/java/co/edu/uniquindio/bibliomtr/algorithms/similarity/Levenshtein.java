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
	
		int m = a.size();
		int n = b.size();
	
		// Matriz D de (m + 1) × (n + 1): D[i][j] = distancia entre los primeros i símbolos de a y los primeros j de b.
		int[][] d = new int[m + 1][n + 1];
	
		// Casos base: transformar a[0..i) en la secuencia vacía cuesta i eliminaciones; la vacía en b[0..j) cuesta j inserciones.
		for (int i = 0; i <= m; i++) {
			d[i][0] = i;
		}
		for (int j = 0; j <= n; j++) {
			d[0][j] = j;
		}
	
		// Caso general: la mejor de las tres operaciones sobre el último símbolo de cada prefijo.
		for (int i = 1; i <= m; i++) {
			for (int j = 1; j <= n; j++) {
				int substitution = a.get(i - 1).equals(b.get(j - 1)) ? 0 : 1; // [aᵢ ≠ bⱼ]
				int delete = d[i - 1][j] + 1;
				int insert = d[i][j - 1] + 1;
				int replace = d[i - 1][j - 1] + substitution;
				d[i][j] = Math.min(Math.min(delete, insert), replace);
			}
		}
	
		int distance = d[m][n];
		int maxLength = Math.max(m, n);
		// similitud = 1 − D[m][n] / max(m, n); dos secuencias vacías son idénticas.
		double similarity = maxLength == 0 ? 1.0 : 1.0 - (double) distance / maxLength;
		return new SimilarityResult<>(distance, similarity, new LevenshteinTrace(a, b, d, distance, maxLength));
	}
}
