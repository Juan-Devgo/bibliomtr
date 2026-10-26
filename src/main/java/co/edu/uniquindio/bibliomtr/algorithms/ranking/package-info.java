/**
 * Requerimiento 2: ranking de términos mediante algoritmos de ordenamiento no basados en comparaciones
 * (spec {@code docs/specs/002-ranking-terminos}).
 *
 * <p><b>Qué hace.</b> Cuenta la frecuencia de cada término en los abstracts seleccionables del corpus y
 * ordena la lista de pares (término, frecuencia) con tres algoritmos escritos a mano (constitución P1):
 * <ul>
 *   <li><b>Counting Sort</b> (REQ-6): arreglo de conteo {@code C} de tamaño {@code max + 1} con
 *       {@code C[v]} = número de elementos con frecuencia {@code v}; acumulado {@code C[v] += C[v − 1]};
 *       construcción de la salida recorriendo la entrada de derecha a izquierda, lo que lo hace estable.
 *       Tiempo y espacio O(n + k), con n = número de términos y k = frecuencia máxima.</li>
 *   <li><b>Radix Sort</b> (REQ-7): LSD en base b = 10; una pasada de Counting Sort estable por dígito,
 *       con dígito {@code ⌊v / exp⌋ mod 10} y {@code exp} = 1, 10, 100, … mientras {@code ⌊max / exp⌋ > 0}.
 *       Tiempo O(d · (n + b)) y espacio O(n + b), con d = dígitos del máximo.</li>
 *   <li><b>Bucket Sort</b> (REQ-8): mínimo y máximo, {@code m = ⌊(max − min) / bucketSize⌋ + 1} cubetas,
 *       distribución por el índice {@code ⌊(v − min) / bucketSize⌋}, insertion sort estable escrito a mano
 *       en cada cubeta y concatenación. Tiempo O(n + m) promedio y O(n²) en el peor caso, espacio O(n + m),
 *       con m = número de cubetas; {@code bucketSize} es un parámetro (default 5, REQ-9).</li>
 * </ul>
 *
 * <p><b>Por qué así.</b> Los tres algoritmos ordenan ascendente y de forma estable por frecuencia, igual que
 * el código de referencia. Para obtener el ranking (frecuencia descendente y empates en orden alfabético
 * ascendente, REQ-2), la entrada se arma en orden alfabético descendente recorriendo un
 * {@code TreeMap<String,Integer>} (orden natural de {@code String}) y la salida se lee de derecha a izquierda.
 * Cada algoritmo devuelve, además del resultado, una traza con sus pasos intermedios (constitución P7,
 * paquete {@code co.edu.uniquindio.bibliomtr.trace.ranking}).
 *
 * <p><b>Código de referencia.</b> Las implementaciones adaptan, sin cambiar su lógica, el código entregado
 * por el usuario en {@code docs/referencias/ordenamiento/} ({@code CountingSort.java}, {@code RadixSort.java}
 * y {@code BucketSort.java}), que queda fuera de {@code src/} y no se compila. Los cambios son: trabajar con
 * pares (término, frecuencia), registrar la traza, proteger la lista vacía y, por el conflicto con la
 * constitución P1, reemplazar {@code Collections.sort} de cada cubeta por un insertion sort escrito a mano.
 * El uso de IA generativa en esa adaptación se declara en el documento técnico (constitución P4).
 *
 * <p>Este paquete no depende de {@code api} ni de {@code persistence} (constitución P13). En producción no se
 * usan {@code Collections.sort}, {@code Arrays.sort}, {@code List.sort}, {@code Stream.sorted} ni librerías
 * de ordenamiento (REQ-12).
 */
package co.edu.uniquindio.bibliomtr.algorithms.ranking;
