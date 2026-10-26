package co.edu.uniquindio.bibliomtr.algorithms.ranking;

import java.util.List;

/**
 * Los tres algoritmos de ordenamiento del Requerimiento 2, con su slug de la API, su complejidad teórica y
 * el cálculo de sus parámetros propios (REQ-22).
 *
 * <p><b>Por qué.</b> Centraliza en un solo lugar el nombre que usa la API ({@code counting}, {@code radix},
 * {@code bucket}) y la notación de complejidad que muestran la interfaz y el documento técnico, para que
 * no diverjan.
 */
public enum SortAlgorithm {

	/** Counting Sort: O(n + k), k = frecuencia máxima. */
	COUNTING("counting", "O(n+k)"),

	/** Radix Sort LSD en base 10: O(d·(n+b)), d = dígitos del máximo, b = 10. */
	RADIX("radix", "O(d·(n+b))"),

	/** Bucket Sort con insertion sort por cubeta: O(n+m) promedio y O(n²) peor caso, m = número de cubetas. */
	BUCKET("bucket", "O(n+m) promedio, O(n²) peor caso");

	/** Base de Radix Sort (REQ-7). */
	public static final int RADIX_BASE = 10;

	private final String slug;
	private final String complexity;

	SortAlgorithm(String slug, String complexity) {
		this.slug = slug;
		this.complexity = complexity;
	}

	/**
	 * Nombre del algoritmo en la API ({@code /api/ranking/trace/{algorithm}}).
	 *
	 * <p>Complejidad: O(1) en tiempo y espacio.
	 *
	 * @return slug en minúsculas
	 */
	public String slug() {
		return slug;
	}

	/**
	 * Complejidad teórica en tiempo con la notación propia del algoritmo.
	 *
	 * <p>Complejidad: O(1) en tiempo y espacio.
	 *
	 * @return complejidad, p. ej. {@code O(n+k)}
	 */
	public String complexity() {
		return complexity;
	}

	/**
	 * Busca el algoritmo por su slug.
	 *
	 * <p>Complejidad: O(1) en tiempo y espacio (tres valores).
	 *
	 * @param slug {@code counting}, {@code radix} o {@code bucket}
	 * @return el algoritmo correspondiente
	 * @throws IllegalArgumentException si el slug es desconocido, con el mensaje en español de la API
	 */
	public static SortAlgorithm fromSlug(String slug) {
		for (SortAlgorithm algorithm : values()) {
			if (algorithm.slug.equals(slug)) {
				return algorithm;
			}
		}
		throw new IllegalArgumentException(
				"Algoritmo desconocido: " + slug + ". Valores válidos: counting, radix, bucket");
	}

	/**
	 * Calcula los parámetros propios del algoritmo sobre la entrada (REQ-22):
	 * Counting {@code k = max}; Radix {@code d} = número de dígitos decimales de {@code max} (igual al número
	 * de pasadas, pues se itera mientras {@code ⌊max / exp⌋ > 0}) y {@code b = 10}; Bucket
	 * {@code m = ⌊(max − min) / bucketSize⌋ + 1}. Lista vacía: {@code k = d = m = 0}.
	 *
	 * <p>Complejidad: O(n) en tiempo (un recorrido para mínimo y máximo) y O(1) en espacio.
	 *
	 * @param input      lista de frecuencias que ordenará el algoritmo
	 * @param bucketSize ancho de cubeta (solo afecta a Bucket Sort), entero mayor o igual a 1
	 * @return parámetros del algoritmo; los que no aplican van en {@code null}
	 */
	public AlgorithmParameters parameters(List<TermFrequency> input, int bucketSize) {
		if (bucketSize < 1) {
			throw new IllegalArgumentException("El parámetro bucketSize debe ser un entero mayor o igual a 1");
		}
		int min = 0;
		int max = 0;
		if (!input.isEmpty()) {
			min = input.getFirst().frequency();
			max = min;
			for (TermFrequency element : input) {
				if (element.frequency() < min) {
					min = element.frequency();
				}
				if (element.frequency() > max) {
					max = element.frequency();
				}
			}
		}
		return switch (this) {
			case COUNTING -> new AlgorithmParameters(max, null, null, null);
			case RADIX -> new AlgorithmParameters(null, decimalDigits(max), RADIX_BASE, null);
			case BUCKET -> new AlgorithmParameters(null, null, null,
					input.isEmpty() ? 0 : (max - min) / bucketSize + 1);
		};
	}

	/**
	 * Número de dígitos decimales de {@code value}, con 0 para {@code value = 0} (Radix Sort no hace
	 * pasadas en ese caso). Se calcula dividiendo por 10, sin riesgo de desbordamiento.
	 *
	 * <p>Complejidad: O(d) en tiempo y O(1) en espacio.
	 */
	private static int decimalDigits(int value) {
		int digits = 0;
		for (int rest = value; rest > 0; rest /= RADIX_BASE) {
			digits++;
		}
		return digits;
	}

}
