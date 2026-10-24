package co.edu.uniquindio.bibliomtr.algorithms.ranking;

/**
 * Parámetros propios de cada algoritmo que aparecen en su complejidad teórica (REQ-22).
 *
 * <p><b>Qué es.</b> Notación separada por algoritmo (spec, aclaración de notación):
 * <ul>
 *   <li>Counting Sort, O(n + k): {@code k} = frecuencia máxima.</li>
 *   <li>Radix Sort, O(d · (n + b)): {@code d} = dígitos decimales del máximo y {@code b} = 10, la base.</li>
 *   <li>Bucket Sort, O(n + m): {@code m} = número de cubetas, {@code ⌊(max − min) / bucketSize⌋ + 1}.</li>
 * </ul>
 * Los parámetros que no aplican a un algoritmo van en {@code null}. Con una lista vacía, {@code k},
 * {@code d} y {@code m} valen 0; {@code b} sigue siendo 10 porque es la base fija, no depende de la entrada.
 *
 * <p><b>Por qué.</b> La interfaz muestra la medición JMH junto a n y estos valores, para contrastar el
 * tiempo práctico con la fórmula teórica.
 *
 * <p>Complejidad: construcción O(1) en tiempo y espacio.
 *
 * @param k frecuencia máxima (solo Counting Sort)
 * @param d dígitos del máximo (solo Radix Sort)
 * @param b base de Radix Sort, siempre 10 (solo Radix Sort)
 * @param m número de cubetas (solo Bucket Sort)
 */
public record AlgorithmParameters(Integer k, Integer d, Integer b, Integer m) {
}
