package co.edu.uniquindio.bibliomtr.trace.ranking;

import java.util.List;

import co.edu.uniquindio.bibliomtr.algorithms.ranking.TermFrequency;

/**
 * Traza de Bucket Sort (REQ-15): mínimo, máximo, número de cubetas, contenido de cada cubeta antes y después
 * del insertion sort y arreglo concatenado.
 *
 * <p><b>Qué guarda.</b> {@code bucketCount = m = ⌊(max − min) / bucketSize⌋ + 1}; cada elemento con
 * frecuencia {@code v} cae en la cubeta {@code ⌊(v − min) / bucketSize⌋} (AC-9: [5, 3, 8, 3] con
 * {@code bucketSize = 2} da 3 cubetas: [3, 3], [5] y [8]). Se registran todas las cubetas, incluidas las
 * vacías. Lista vacía: listas vacías, {@code min = max = 0} y {@code bucketCount = 0}.
 *
 * <p>Complejidad: construcción O(n + m) en tiempo y espacio por las copias defensivas, con m = número de
 * cubetas.
 *
 * @param input        entrada recibida por el algoritmo
 * @param min          frecuencia mínima
 * @param max          frecuencia máxima
 * @param bucketSize   ancho de cada cubeta
 * @param bucketCount  número de cubetas m
 * @param buckets      cubetas en orden de índice
 * @param concatenated concatenación de las cubetas ordenadas, que es la salida
 */
public record BucketSortTrace(List<TermFrequency> input, int min, int max, int bucketSize, int bucketCount,
		List<Bucket> buckets, List<TermFrequency> concatenated) {

	/**
	 * Copia defensiva de las listas para que la traza sea inmutable.
	 *
	 * <p>Complejidad: O(n + m) en tiempo y espacio.
	 */
	public BucketSortTrace {
		input = List.copyOf(input);
		buckets = List.copyOf(buckets);
		concatenated = List.copyOf(concatenated);
	}

	/**
	 * Contenido de una cubeta antes y después de ordenarla con insertion sort estable.
	 *
	 * <p>Complejidad: construcción O(tamaño de la cubeta) en tiempo y espacio.
	 *
	 * @param index  índice de la cubeta, de 0 a {@code bucketCount − 1}
	 * @param before elementos en el orden en que se distribuyeron
	 * @param after  elementos tras el insertion sort
	 */
	public record Bucket(int index, List<TermFrequency> before, List<TermFrequency> after) {

		/**
		 * Copia defensiva de las listas.
		 *
		 * <p>Complejidad: O(tamaño de la cubeta) en tiempo y espacio.
		 */
		public Bucket {
			before = List.copyOf(before);
			after = List.copyOf(after);
		}

	}

}
