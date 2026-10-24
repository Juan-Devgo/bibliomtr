package co.edu.uniquindio.bibliomtr.trace.similarity;

/**
 * Traza de la similitud coseno entre dos vectores: productos por componente, producto punto y normas.
 *
 * <p><b>Qué guarda.</b> Con d = {@code dimension}: {@code products[i] = aᵢ · bᵢ},
 * {@code dotProduct} = Σ aᵢ bᵢ, {@code normA} = ‖a‖₂ y {@code normB} = ‖b‖₂. Ejemplo (AC-7): (1, 0) y (1, 1)
 * dan productos [1, 0], producto punto 1, normas 1 y √2.
 *
 * <p>Complejidad: construcción O(d) en tiempo y espacio por la copia defensiva.
 *
 * @param dimension  dimensión d de los vectores
 * @param products   productos por componente
 * @param dotProduct producto punto
 * @param normA      norma euclidiana de a
 * @param normB      norma euclidiana de b
 */
public record CosineTrace(int dimension, double[] products, double dotProduct, double normA, double normB) {

	/**
	 * Copia defensiva del arreglo para que la traza sea inmutable.
	 *
	 * <p>Complejidad: O(d) en tiempo y espacio.
	 */
	public CosineTrace {
		products = products.clone();
	}

	/**
	 * Productos por componente.
	 *
	 * <p>Complejidad: O(d) en tiempo y espacio (copia).
	 *
	 * @return copia de los productos
	 */
	@Override
	public double[] products() {
		return products.clone();
	}

}
