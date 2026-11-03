package co.edu.uniquindio.bibliomtr.trace.clustering;

import java.util.List;

/**
 * Traza de DIANA (divisive analysis): un paso por división (REQ-16).
 *
 * <p><b>Ejemplo</b> (matriz D₄ de la guía). El paso 1 tiene {@code clusters = [[0, 1, 2, 3]]},
 * {@code diameters = [0.7]} y divide ese grupo. Su primera iteración tiene {@code splinter = []},
 * {@code averageToRemainder = [0.466667, 0.366667, 0.416667, 0.583333]} y mueve el artículo 3; la segunda tiene
 * {@code differences = [−0.35, −0.35, −0.05]} y termina con {@code moved = −1}. Las partes son
 * {@code splinter = [3]} y {@code remainder = [0, 1, 2]}.
 *
 * <p>Complejidad: construcción O(N³) en tiempo y espacio en total por las copias defensivas.
 *
 * @param steps un paso por división, en orden
 */
public record DianaTrace(List<DianaStep> steps) {

	/**
	 * Copia defensiva de la lista.
	 *
	 * <p>Complejidad: O(N) en tiempo y espacio.
	 */
	public DianaTrace {
		steps = List.copyOf(steps);
	}

	/**
	 * Una división: los grupos vigentes y sus diámetros, el grupo elegido, las iteraciones que arman el grupo
	 * disidente y las dos partes.
	 *
	 * <p>Complejidad: construcción O(N²) en tiempo y espacio por las copias.
	 *
	 * @param clusters   grupos vigentes antes de dividir, cada uno con sus artículos en orden creciente; tras
	 *                   dividir, el grupo se reemplaza en su posición por la parte con el menor artículo seguida
	 *                   de la otra
	 * @param diameters  diámetro de cada grupo de {@code clusters}, alineado (0 en los de un artículo)
	 * @param splitIndex posición en {@code clusters} del grupo dividido
	 * @param diameter   diámetro del grupo dividido: la altura de la fusión que le corresponde
	 * @param iterations iteraciones del grupo disidente, en orden
	 * @param splinter   grupo disidente S final, en orden de entrada
	 * @param remainder  resto R final, en orden creciente
	 */
	public record DianaStep(List<List<Integer>> clusters, double[] diameters, int splitIndex, double diameter,
			List<DianaIteration> iterations, List<Integer> splinter, List<Integer> remainder) {

		/**
		 * Copia defensiva de listas y arreglos.
		 *
		 * <p>Complejidad: O(N²) en tiempo y espacio.
		 */
		public DianaStep {
			clusters = LinkageTrace.copyMembers(clusters);
			diameters = diameters.clone();
			iterations = List.copyOf(iterations);
			splinter = List.copyOf(splinter);
			remainder = List.copyOf(remainder);
		}

		/**
		 * Diámetros de los grupos vigentes.
		 *
		 * <p>Complejidad: O(k) en tiempo y espacio (copia).
		 *
		 * @return copia de los diámetros
		 */
		@Override
		public double[] diameters() {
			return diameters.clone();
		}

	}

	/**
	 * Una iteración del grupo disidente: medias de cada artículo del resto R frente a R y frente a S, sus
	 * diferencias y el artículo que se mueve a S.
	 *
	 * <p>En la primera iteración {@code splinter} está vacío, {@code averageToSplinter} es 0 y
	 * {@code differences} = {@code averageToRemainder}: se mueve el de mayor distancia media al resto.
	 *
	 * <p>Complejidad: construcción O(|R|) en tiempo y espacio por las copias.
	 *
	 * @param splinter           grupo disidente S antes de la iteración
	 * @param remainder          resto R antes de la iteración
	 * @param averageToRemainder media de d(i, R \ {i}) por cada i de {@code remainder}, alineada
	 * @param averageToSplinter  media de d(i, S) por cada i de {@code remainder}, alineada
	 * @param differences        D(i) = averageToRemainder − averageToSplinter, alineada
	 * @param moved              artículo movido a S, o −1 si ningún D(i) es positivo y la división termina
	 */
	public record DianaIteration(List<Integer> splinter, List<Integer> remainder, double[] averageToRemainder,
			double[] averageToSplinter, double[] differences, int moved) {

		/**
		 * Copia defensiva de listas y arreglos.
		 *
		 * <p>Complejidad: O(|R| + |S|) en tiempo y espacio.
		 */
		public DianaIteration {
			splinter = List.copyOf(splinter);
			remainder = List.copyOf(remainder);
			averageToRemainder = averageToRemainder.clone();
			averageToSplinter = averageToSplinter.clone();
			differences = differences.clone();
		}

		/**
		 * Medias frente al resto.
		 *
		 * <p>Complejidad: O(|R|) en tiempo y espacio (copia).
		 *
		 * @return copia del arreglo
		 */
		@Override
		public double[] averageToRemainder() {
			return averageToRemainder.clone();
		}

		/**
		 * Medias frente al grupo disidente.
		 *
		 * <p>Complejidad: O(|R|) en tiempo y espacio (copia).
		 *
		 * @return copia del arreglo
		 */
		@Override
		public double[] averageToSplinter() {
			return averageToSplinter.clone();
		}

		/**
		 * Diferencias D(i).
		 *
		 * <p>Complejidad: O(|R|) en tiempo y espacio (copia).
		 *
		 * @return copia del arreglo
		 */
		@Override
		public double[] differences() {
			return differences.clone();
		}

	}

}
