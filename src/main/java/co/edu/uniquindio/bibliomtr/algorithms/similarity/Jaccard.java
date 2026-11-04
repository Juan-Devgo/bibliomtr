package co.edu.uniquindio.bibliomtr.algorithms.similarity;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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
	  // Conjuntos A y B en orden de primera aparición (las repeticiones no cuentan).
		Set<String> setA = new LinkedHashSet<>(a);
		Set<String> setB = new LinkedHashSet<>(b);
	
		// A ∩ B: los símbolos de A que también están en B.
		List<String> intersection = new ArrayList<>();
		for (String symbol : setA) {
			if (setB.contains(symbol)) {
				intersection.add(symbol);
			}
		}
	
		// A ∪ B: primero los de A y luego los de B que no estaban en A.
		List<String> union = new ArrayList<>(setA);
		for (String symbol : setB) {
			if (!setA.contains(symbol)) {
				union.add(symbol);
			}
		}
	
		// J = |A ∩ B| / |A ∪ B|; dos conjuntos vacíos son idénticos.
		double jaccard = union.isEmpty() ? 1.0 : (double) intersection.size() / union.size();
		JaccardTrace trace = new JaccardTrace(new ArrayList<>(setA), new ArrayList<>(setB), intersection, union);
		return new SimilarityResult<>(jaccard, jaccard, trace);
	}

}
