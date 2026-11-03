package co.edu.uniquindio.bibliomtr.trace.clustering;

import java.util.ArrayList;
import java.util.List;

/**
 * Traza de un clustering aglomerativo (Single, Complete o Average Linkage): un paso por fusión (REQ-16).
 *
 * <p><b>Ejemplo</b> (matriz D₄ de la guía, Single Linkage). El paso 1 tiene {@code clusters = [0, 1, 2, 3]},
 * {@code distances = D₄}, fusiona {@code left = 0} y {@code right = 1} a distancia 0.2 en el grupo 4, deja
 * {@code remaining = [2, 3]} y recalcula {@code updatedRow = [min(0.5, 0.3), min(0.7, 0.6)] = [0.3, 0.6]}. El
 * paso 2 empieza con {@code clusters = [2, 3, 4]}.
 *
 * <p>Complejidad: construcción O(N³) en tiempo y espacio en total por las copias defensivas (N pasos con
 * matrices de hasta N×N).
 *
 * @param steps un paso por fusión, en orden
 */
public record LinkageTrace(List<LinkageStep> steps) {

	/**
	 * Copia defensiva de la lista.
	 *
	 * <p>Complejidad: O(N) en tiempo y espacio.
	 */
	public LinkageTrace {
		steps = List.copyOf(steps);
	}

	/**
	 * Una fusión: los grupos vigentes antes de fusionar, su matriz de distancias, el par elegido y la fila
	 * recalculada con la fórmula de Lance-Williams del algoritmo.
	 *
	 * <p>Complejidad: construcción O(k²) en tiempo y espacio por las copias, con k = grupos vigentes.
	 *
	 * @param clusters   ids de los grupos vigentes antes de fusionar, en orden creciente (los que quedan
	 *                   conservan su orden y el grupo nuevo va al final)
	 * @param members    artículos de cada grupo de {@code clusters}, en orden creciente
	 * @param distances  matriz k×k de distancias entre los grupos de {@code clusters}
	 * @param left       id del primer grupo fusionado ({@code left < right})
	 * @param right      id del segundo grupo fusionado
	 * @param distance   distancia entre {@code left} y {@code right}: la altura de la fusión
	 * @param newCluster id del grupo nuevo, N + número de la fusión
	 * @param remaining  {@code clusters} sin {@code left} ni {@code right}, en el mismo orden
	 * @param updatedRow distancia del grupo nuevo a cada grupo de {@code remaining}, alineada
	 */
	public record LinkageStep(List<Integer> clusters, List<List<Integer>> members, double[][] distances, int left,
			int right, double distance, int newCluster, List<Integer> remaining, double[] updatedRow) {

		/**
		 * Copia defensiva de listas y arreglos.
		 *
		 * <p>Complejidad: O(k²) en tiempo y espacio.
		 */
		public LinkageStep {
			clusters = List.copyOf(clusters);
			members = copyMembers(members);
			distances = copy(distances);
			remaining = List.copyOf(remaining);
			updatedRow = updatedRow.clone();
		}

		/**
		 * Matriz de distancias entre los grupos vigentes.
		 *
		 * <p>Complejidad: O(k²) en tiempo y espacio (copia).
		 *
		 * @return copia de la matriz
		 */
		@Override
		public double[][] distances() {
			return copy(distances);
		}

		/**
		 * Distancias del grupo nuevo a los grupos que quedan.
		 *
		 * <p>Complejidad: O(k) en tiempo y espacio (copia).
		 *
		 * @return copia de la fila
		 */
		@Override
		public double[] updatedRow() {
			return updatedRow.clone();
		}

	}

	/** Copia de una lista de grupos; O(N) en tiempo y espacio. */
	static List<List<Integer>> copyMembers(List<List<Integer>> members) {
		List<List<Integer>> copy = new ArrayList<>(members.size());
		for (List<Integer> group : members) {
			copy.add(List.copyOf(group));
		}
		return List.copyOf(copy);
	}

	/** Copia profunda de una matriz; O(k²) en tiempo y espacio. */
	static double[][] copy(double[][] source) {
		double[][] target = new double[source.length][];
		for (int i = 0; i < source.length; i++) {
			target[i] = source[i].clone();
		}
		return target;
	}

}
