/**
 * Preprocesamientos y trazas inmutables de los algoritmos de coincidencia exacta de cadenas del Requerimiento 4
 * (constitución P7; spec 004, REQ-23, REQ-24 y REQ-25).
 *
 * <p>Cada algoritmo tiene dos records que llena el stub correspondiente de
 * {@code co.edu.uniquindio.bibliomtr.algorithms.stringmatching}:
 * <ul>
 *   <li>El <b>preprocesamiento</b> ({@link co.edu.uniquindio.bibliomtr.trace.stringmatching.KmpPreprocessing},
 *       {@link co.edu.uniquindio.bibliomtr.trace.stringmatching.BoyerMoorePreprocessing},
 *       {@link co.edu.uniquindio.bibliomtr.trace.stringmatching.RabinKarpPreprocessing}): las tablas del patrón.
 *       Es a la vez la entrada de {@code search} y la traza del preprocesamiento, para no duplicar tipos.</li>
 *   <li>La <b>traza de la búsqueda</b> ({@link co.edu.uniquindio.bibliomtr.trace.stringmatching.KmpTrace},
 *       {@link co.edu.uniquindio.bibliomtr.trace.stringmatching.BoyerMooreTrace},
 *       {@link co.edu.uniquindio.bibliomtr.trace.stringmatching.RabinKarpTrace}): comparaciones, alineaciones,
 *       desplazamientos, hashes y colisiones.</li>
 * </ul>
 * La API los expone como JSON en {@code GET /api/pattern-search/trace}, la interfaz los muestra paso a paso y el
 * documento técnico los reproduce a mano. Listas, mapas y arreglos se copian al construir (y los arreglos también al
 * leer), de modo que nada cambia después de creado; los mapas conservan el orden de inserción.
 */
package co.edu.uniquindio.bibliomtr.trace.stringmatching;
