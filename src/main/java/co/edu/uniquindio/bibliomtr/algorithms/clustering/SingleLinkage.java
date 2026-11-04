package co.edu.uniquindio.bibliomtr.algorithms.clustering;

import java.util.List;
import java.util.ArrayList;

import co.edu.uniquindio.bibliomtr.trace.clustering.LinkageTrace;
import co.edu.uniquindio.bibliomtr.trace.clustering.LinkageTrace.LinkageStep;

/**
 * Single Linkage (enlace simple o vecino más cercano) (Requerimiento 3, algoritmo aglomerativo). STUB: el cuerpo de {@link #cluster} lo escribe el
 * estudiante (constitución P1; ver {@code docs/guia-implementacion.md}, § 10).
 *
 * <p><b>Qué calcula.</b> Un dendrograma aglomerativo (bottom-up): empieza con un grupo por artículo y en cada
 * paso fusiona los dos grupos más cercanos, donde la distancia entre dos grupos es la del par de artículos más
 * cercano entre ellos. Tiende a encadenar grupos alargados (efecto cadena).
 *
 * <p><b>Fórmula.</b>
 * <pre>
 *   d(A, B) = min { d(a, b) : a ∈ A, b ∈ B }
 *   Lance-Williams, grupo nuevo k = i ∪ j frente a otro grupo h:
 *   d(k, h) = min(d(i, h), d(j, h))
 * </pre>
 *
 * <p><b>Desempate.</b> Entre los pares de grupos vigentes (p, q), p &lt; q, de distancia mínima gana el de
 * menor p y, a igual p, el de menor q. Compare con {@code <} exacto, sin margen (REQ-5). La fusión se devuelve
 * como {@code Merge(p, q, d(p, q), |p| + |q|)} y crea el grupo N + k (convención de {@link Merge}).
 *
 * <p><b>Traza.</b> {@link LinkageTrace}, un {@code LinkageStep} por fusión: grupos vigentes en orden
 * creciente (los que quedan conservan su orden y el nuevo va al final), sus artículos, la matriz entre ellos, el
 * par fusionado y su distancia, el id del grupo nuevo, los grupos que quedan y la fila recalculada.
 *
 * <p><b>Rango.</b> Alturas en [0,1] y no decrecientes de una fusión a la siguiente.
 *
 * <p><b>Caso a mano</b> (matriz D₄ de la guía): (0, 1, 0.2, 2), (2, 4, 0.3, 3), (3, 5, 0.45, 4); fila recalculada del paso 1: [0.3, 0.6].
 *
 * <p><b>Casos de borde.</b> 2 artículos: una fusión {@code (0, 1, d, 2)}. Abstracts idénticos: se fusionan a
 * altura 0. Todas las distancias iguales a 0.5 con 4 artículos: {@code (0, 1)}, {@code (2, 3)}, {@code (4, 5)}.
 * 20 artículos: 19 fusiones y la última de tamaño 20.
 *
 * <p><b>Complejidad: TODO estudiante</b> (tiempo y espacio en función de N; compare la versión directa O(N³)
 * con la que mantiene el mínimo de cada fila).
 */
public final class SingleLinkage implements HierarchicalClustering<LinkageTrace> {

	/**
	 * Agrupa los N artículos de {@code distances} y devuelve las N − 1 fusiones y la traza.
	 *
	 * <p>Complejidad: TODO estudiante.
	 *
	 * @param distances matriz N×N simétrica, de diagonal 0 y valores en [0,1]; N ≥ 2
	 * @return las fusiones en el orden de creación de los grupos y la traza {@link LinkageTrace}
	 */
	@Override
	public ClusteringResult<LinkageTrace> cluster(double[][] distances) {
  	int n = distances.length;

  	// Matriz de trabajo entre los grupos vigentes (copia: la entrada no se modifica). Al inicio cada artículo es un grupo.
  	double[][] current = new double[n][];
  	List<Integer> clusters = new ArrayList<>(); // ids de los grupos vigentes
  	List<List<Integer>> members = new ArrayList<>(); // artículos de cada grupo, en orden creciente
  	for (int i = 0; i < n; i++) {
  		current[i] = distances[i].clone();
  		clusters.add(i);
  		members.add(new ArrayList<>(List.of(i)));
  	}

  	List<Merge> merges = new ArrayList<>();
  	List<LinkageStep> steps = new ArrayList<>();

  	// N − 1 fusiones: en cada una el número de grupos baja en 1.
  	for (int step = 0; step < n - 1; step++) {
  		int k = clusters.size();

  		// 1. Par (p, q), p < q, de menor distancia. Como el orden de clusters crece con los ids, recorrer p y q en
  		// orden y reemplazar solo con "<" estricto aplica el desempate lexicográfico.
  		int p = 0;
  		int q = 1;
  		for (int i = 0; i < k; i++) {
  			for (int j = i + 1; j < k; j++) {
  				if (current[i][j] < current[p][q]) {
  					p = i;
  					q = j;
  				}
  			}
  		}
  		double height = current[p][q];
  		int newCluster = n + step;

  		// 2. Grupos que quedan (todos menos p y q) y su distancia al grupo nuevo (Lance-Williams).
  		List<Integer> remaining = new ArrayList<>();
  		double[] updatedRow = new double[k - 2];
  		int sizeP = members.get(p).size();
  		int sizeQ = members.get(q).size();
  		int r = 0;
  		for (int h = 0; h < k; h++) {
  			if (h == p || h == q) {
  				continue;
  			}
  			remaining.add(clusters.get(h));
  			updatedRow[r] = Math.min(current[p][h], current[q][h]); // d(k, h) = min(d(i, h), d(j, h))
  			r++;
  		}

  		// 3. Se guarda el paso con la matriz ANTES de fusionar y se crea la fusión (left < right).
  		steps.add(new LinkageStep(clusters, members, current, clusters.get(p), clusters.get(q), height,
  				newCluster, remaining, updatedRow));
  		merges.add(new Merge(clusters.get(p), clusters.get(q), height, sizeP + sizeQ));

  		// 4. Nueva matriz de (k − 1) × (k − 1): los grupos que quedan conservan su orden y el nuevo va al final.
  		double[][] next = new double[k - 1][k - 1];
  		List<Integer> nextClusters = new ArrayList<>();
  		List<List<Integer>> nextMembers = new ArrayList<>();
  		int a = 0;
  		for (int h = 0; h < k; h++) {
  			if (h == p || h == q) {
  				continue;
  			}
  			int b = 0;
  			for (int g = 0; g < k; g++) {
  				if (g == p || g == q) {
  					continue;
  				}
  				next[a][b] = current[h][g];
  				b++;
  			}
  			next[a][k - 2] = updatedRow[a];
  			next[k - 2][a] = updatedRow[a];
  			nextClusters.add(clusters.get(h));
  			nextMembers.add(members.get(h));
  			a++;
  		}
  		nextClusters.add(newCluster);
  		nextMembers.add(mergeSorted(members.get(p), members.get(q)));
  		current = next;
  		clusters = nextClusters;
  		members = nextMembers;
  	}
   
  	return new ClusteringResult<>(merges, new LinkageTrace(steps));
  }

  private static List<Integer> mergeSorted(List<Integer> x, List<Integer> y) {
    
  	List<Integer> result = new ArrayList<>(x.size() + y.size());
  	int i = 0;
  	int j = 0;
   
  	while (i < x.size() && j < y.size()) {
  		if (x.get(i) <= y.get(j)) {
  			result.add(x.get(i++));
  		} else {
  			result.add(y.get(j++));
  		}
  	}
   
  	while (i < x.size()) {
  		result.add(x.get(i++));
  	}
   
  	while (j < y.size()) {
  		result.add(y.get(j++));
  	}
   
  	return result;
	}

}
