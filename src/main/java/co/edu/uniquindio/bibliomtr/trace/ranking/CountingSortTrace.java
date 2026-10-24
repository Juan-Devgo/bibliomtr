package co.edu.uniquindio.bibliomtr.trace.ranking;

import java.util.List;

import co.edu.uniquindio.bibliomtr.algorithms.ranking.TermFrequency;

/**
 * Traza de Counting Sort (REQ-13): máximo, arreglo de conteo, arreglo acumulado y colocación de cada elemento.
 *
 * <p><b>Qué guarda.</b> Con {@code v} = frecuencia:
 * <ul>
 *   <li>{@code count[v]} = número de elementos con frecuencia {@code v}, tras el recorrido de conteo
 *       (AC-10: [4, 1, 3, 1] da [0, 2, 0, 1, 1]).</li>
 *   <li>{@code cumulative[v] = Σ_{u ≤ v} count[u]}, tras {@code C[v] += C[v − 1]} (AC-10: [0, 2, 2, 3, 4]);
 *       es la posición, contada desde 1, del último elemento con frecuencia {@code v} en la salida.</li>
 *   <li>{@code placements}: en el orden del recorrido de derecha a izquierda, el índice de entrada, el
 *       elemento y el índice de salida {@code C[v] − 1} que recibió.</li>
 * </ul>
 * Lista vacía: listas vacías, {@code max = 0} y arreglos vacíos.
 *
 * <p>Complejidad: construcción O(n + k) en tiempo y espacio por las copias defensivas, con n = número de
 * términos y k = frecuencia máxima.
 *
 * @param input      entrada recibida por el algoritmo
 * @param max        frecuencia máxima k
 * @param count      arreglo de conteo, de tamaño {@code max + 1}
 * @param cumulative arreglo acumulado, de tamaño {@code max + 1}
 * @param placements colocaciones en el orden del recorrido de derecha a izquierda
 * @param output     salida ordenada ascendente y estable
 */
public record CountingSortTrace(List<TermFrequency> input, int max, int[] count, int[] cumulative,
		List<Placement> placements, List<TermFrequency> output) {

	/**
	 * Copia defensiva de listas y arreglos para que la traza sea inmutable.
	 *
	 * <p>Complejidad: O(n + k) en tiempo y espacio.
	 */
	public CountingSortTrace {
		input = List.copyOf(input);
		count = count.clone();
		cumulative = cumulative.clone();
		placements = List.copyOf(placements);
		output = List.copyOf(output);
	}

	/**
	 * Arreglo de conteo tras contar.
	 *
	 * <p>Complejidad: O(k) en tiempo y espacio (copia).
	 *
	 * @return copia del arreglo de conteo
	 */
	@Override
	public int[] count() {
		return count.clone();
	}

	/**
	 * Arreglo tras acumular.
	 *
	 * <p>Complejidad: O(k) en tiempo y espacio (copia).
	 *
	 * @return copia del arreglo acumulado
	 */
	@Override
	public int[] cumulative() {
		return cumulative.clone();
	}

	/**
	 * Colocación de un elemento en la salida: {@code output[outputIndex] = input[inputIndex]}, con
	 * {@code outputIndex = C[v] − 1} antes de decrementar {@code C[v]}.
	 *
	 * <p>Complejidad: construcción O(1) en tiempo y espacio.
	 *
	 * @param inputIndex  índice del elemento en la entrada
	 * @param element     elemento colocado
	 * @param outputIndex índice que ocupa en la salida
	 */
	public record Placement(int inputIndex, TermFrequency element, int outputIndex) {
	}

}
