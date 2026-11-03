package co.edu.uniquindio.bibliomtr.algorithms.clustering;

/**
 * Estrategia de clustering jerárquico sobre una matriz de distancias (Single, Complete y Average Linkage, y
 * DIANA).
 *
 * <p><b>Qué recibe.</b> La matriz N×N {@code D = 1 − S}, con N ≥ 2, simétrica, de diagonal 0 y valores en [0,1];
 * la fila y la columna i corresponden al artículo i de la solicitud (REQ-3, REQ-10). El algoritmo no necesita
 * saber de qué fuente de similitud viene.
 *
 * <p><b>Por qué.</b> Una sola firma (patrón Strategy) permite que el servicio de clustering y los tests llamen a
 * cualquier implementación, y que los tests de la API la reemplacen por un algoritmo falso.
 *
 * <p>Complejidad: la declara cada implementación.
 *
 * @param <T> tipo de la traza que devuelve la implementación
 */
public interface HierarchicalClustering<T> {

	/**
	 * Agrupa los N artículos de la matriz.
	 *
	 * <p>Complejidad: la declara cada implementación.
	 *
	 * @param distances matriz N×N de distancias; el algoritmo puede modificar esta copia
	 * @return las N − 1 fusiones del dendrograma y la traza
	 */
	ClusteringResult<T> cluster(double[][] distances);

}
