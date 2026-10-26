package co.edu.uniquindio.bibliomtr.algorithms.similarity;

import java.util.List;

import co.edu.uniquindio.bibliomtr.trace.similarity.JaccardTrace;

/**
 * Coeficiente de Jaccard entre los conjuntos de símbolos de dos secuencias (Requerimiento 1, algoritmo
 * clásico). STUB: el cuerpo de {@link #compare} lo escribe el estudiante (constitución P1; ver
 * {@code docs/guia-implementacion.md}).
 *
 * <p><b>Qué calcula.</b> La proporción de símbolos distintos compartidos entre las dos secuencias: las
 * repeticiones no cuentan, porque se comparan los conjuntos A = símbolos de {@code a} y B = símbolos de
 * {@code b}.
 *
 * <p><b>Fórmula.</b>
 * <pre>
 *   J(A, B) = |A ∩ B| / |A ∪ B|
 * </pre>
 *
 * <p><b>Normalización sugerida.</b> Ninguna: {@code value = similarity = J}, y {@code J = 1} si
 * A = B = ∅.
 *
 * <p><b>Traza.</b> {@link JaccardTrace}: A, B, A ∩ B y A ∪ B, en orden de primera aparición (la unión: primero
 * los de A y luego los nuevos de B).
 *
 * <p><b>Rango.</b> {@code value} = {@code similarity} ∈ [0, 1].
 *
 * <p><b>Casos de borde.</b> Ambas vacías: 1. Duplicados: [a,a,b] y [a,b,b] dan 1. Idénticas: 1. Disjuntas: 0.
 * Longitudes muy distintas ([a] y [a, b, …, j]): 0.1.
 *
 * <p><b>Complejidad: TODO estudiante</b> (tiempo y espacio en función de |a| y |b|).
 */
public final class Jaccard implements SequenceSimilarity<JaccardTrace> {

	/**
	 * Calcula el coeficiente de Jaccard entre los conjuntos de {@code a} y {@code b} y la traza.
	 *
	 * <p>Complejidad: TODO estudiante.
	 *
	 * @param a primera secuencia de símbolos (no nula; puede ser vacía)
	 * @param b segunda secuencia de símbolos (no nula; puede ser vacía)
	 * @return {@code value} = {@code similarity} = J(A, B) y la traza {@link JaccardTrace}
	 */
	@Override
	public SimilarityResult<JaccardTrace> compare(List<String> a, List<String> b) {
		throw new UnsupportedOperationException(
				"TODO estudiante: implementar Jaccard.compare (ver docs/guia-implementacion.md)");
	}

}
