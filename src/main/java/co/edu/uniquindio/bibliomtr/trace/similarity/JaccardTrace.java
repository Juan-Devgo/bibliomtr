package co.edu.uniquindio.bibliomtr.trace.similarity;

import java.util.List;

/**
 * Traza del coeficiente de Jaccard: los dos conjuntos, su intersección y su unión.
 *
 * <p><b>Qué guarda.</b> {@code setA} y {@code setB} son los símbolos distintos de cada secuencia; la
 * intersección y la unión son las de esos conjuntos. Todas las listas van en orden de primera aparición
 * (la unión: primero los de {@code setA} y luego los nuevos de {@code setB}), para que la traza sea
 * determinista. Ejemplo (AC-7): {a,b,c} y {b,c,d} dan intersección [b, c] y unión [a, b, c, d].
 *
 * <p>Complejidad: construcción O(|A| + |B|) en tiempo y espacio por las copias defensivas.
 *
 * @param setA         símbolos distintos de la primera secuencia
 * @param setB         símbolos distintos de la segunda secuencia
 * @param intersection A ∩ B
 * @param union        A ∪ B
 */
public record JaccardTrace(List<String> setA, List<String> setB, List<String> intersection, List<String> union) {

	/**
	 * Copia defensiva de las listas para que la traza sea inmutable.
	 *
	 * <p>Complejidad: O(|A| + |B|) en tiempo y espacio.
	 */
	public JaccardTrace {
		setA = List.copyOf(setA);
		setB = List.copyOf(setB);
		intersection = List.copyOf(intersection);
		union = List.copyOf(union);
	}

}
