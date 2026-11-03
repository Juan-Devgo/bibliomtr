/**
 * Trazas inmutables de los algoritmos de clustering jerárquico del Requerimiento 3 (constitución P7; spec 003,
 * REQ-16).
 *
 * <p>Cada record es el contrato de los resultados intermedios que debe llenar el stub correspondiente de
 * {@code co.edu.uniquindio.bibliomtr.algorithms.clustering}: la matriz de distancias entre los grupos vigentes
 * en cada fusión ({@link co.edu.uniquindio.bibliomtr.trace.clustering.LinkageTrace}, común a Single, Complete y
 * Average Linkage) o los diámetros y las medias de cada división de DIANA
 * ({@link co.edu.uniquindio.bibliomtr.trace.clustering.DianaTrace}). La API los expone como JSON en
 * {@code GET /api/clustering/trace}, la interfaz los muestra paso a paso y el documento técnico los reproduce a
 * mano. Listas y arreglos se copian al construir y al leer, de modo que la traza no cambia después de creada.
 */
package co.edu.uniquindio.bibliomtr.trace.clustering;
