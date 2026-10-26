package co.edu.uniquindio.bibliomtr.algorithms.ranking;

/**
 * Opciones de una ejecución de un {@link TermSorter} (REQ-9, REQ-11).
 *
 * <p><b>Qué es.</b> {@code bucketSize} es el ancho de rango de cada cubeta de Bucket Sort, de modo que el
 * número de cubetas es {@code m = ⌊(max − min) / bucketSize⌋ + 1}; Counting Sort y Radix Sort lo ignoran.
 * {@code recordTrace} indica si se registran los pasos intermedios.
 *
 * <p><b>Por qué una bandera.</b> Un solo cuerpo por algoritmo garantiza que lo que se mide (sin traza, como
 * en JMH y en los tiempos del ranking) y lo que se muestra paso a paso (con traza) sea el mismo código.
 *
 * <p>Complejidad: construcción O(1) en tiempo y espacio.
 *
 * @param bucketSize  ancho de cada cubeta de Bucket Sort, entero mayor o igual a 1 (default
 *                    {@link #DEFAULT_BUCKET_SIZE})
 * @param recordTrace {@code true} para registrar la traza, {@code false} para medir tiempos
 */
public record SortOptions(int bucketSize, boolean recordTrace) {

	/** Ancho de cubeta por defecto de la API, la traza y JMH (REQ-9, REQ-18). */
	public static final int DEFAULT_BUCKET_SIZE = 5;

	/**
	 * Valida que {@code bucketSize} sea al menos 1, porque Bucket Sort divide por él.
	 *
	 * <p>Complejidad: O(1) en tiempo y espacio.
	 */
	public SortOptions {
		if (bucketSize < 1) {
			throw new IllegalArgumentException("El parámetro bucketSize debe ser un entero mayor o igual a 1");
		}
	}

}
