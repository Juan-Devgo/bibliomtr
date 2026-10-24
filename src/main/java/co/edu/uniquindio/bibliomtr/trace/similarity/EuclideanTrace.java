package co.edu.uniquindio.bibliomtr.trace.similarity;

/**
 * Traza de la distancia euclidiana entre dos vectores: diferencias al cuadrado por componente y su suma.
 *
 * <p><b>Qué guarda.</b> Con d = {@code dimension}: {@code squaredDifferences[i] = (aᵢ − bᵢ)²} y
 * {@code sumOfSquares} = Σ (aᵢ − bᵢ)²; la distancia es su raíz cuadrada. Ejemplo (AC-7): (0, 0) y (3, 4) dan
 * [9, 16] y suma 25.
 *
 * <p>Complejidad: construcción O(d) en tiempo y espacio por la copia defensiva.
 *
 * @param dimension          dimensión d de los vectores
 * @param squaredDifferences diferencias al cuadrado por componente
 * @param sumOfSquares       suma de las diferencias al cuadrado
 */
public record EuclideanTrace(int dimension, double[] squaredDifferences, double sumOfSquares) {

	/**
	 * Copia defensiva del arreglo para que la traza sea inmutable.
	 *
	 * <p>Complejidad: O(d) en tiempo y espacio.
	 */
	public EuclideanTrace {
		squaredDifferences = squaredDifferences.clone();
	}

	/**
	 * Diferencias al cuadrado por componente.
	 *
	 * <p>Complejidad: O(d) en tiempo y espacio (copia).
	 *
	 * @return copia de las diferencias al cuadrado
	 */
	@Override
	public double[] squaredDifferences() {
		return squaredDifferences.clone();
	}

}
