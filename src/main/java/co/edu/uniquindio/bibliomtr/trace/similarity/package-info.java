/**
 * Trazas inmutables de los algoritmos de similitud del Requerimiento 1 (constitución P7; spec 001, REQ-1 y
 * REQ-16).
 *
 * <p>Cada record es el contrato de los resultados intermedios que debe llenar el stub correspondiente de
 * {@code co.edu.uniquindio.bibliomtr.algorithms.similarity}: matrices de programación dinámica, conjuntos,
 * vectores TF-IDF, productos y diferencias por componente. La API los expone como JSON en
 * {@code GET /api/similarity/trace}, la interfaz los muestra paso a paso y el documento técnico los reproduce a
 * mano. Listas y arreglos se copian al construir y al leer, de modo que la traza no cambia después de creada.
 */
package co.edu.uniquindio.bibliomtr.trace.similarity;
