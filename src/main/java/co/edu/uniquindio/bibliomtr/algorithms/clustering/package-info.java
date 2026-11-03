/**
 * Requerimiento 3: clustering jerárquico de los abstracts y su dendrograma (spec
 * {@code docs/specs/003-clustering-jerarquico}).
 *
 * <p><b>Qué contiene.</b> El contrato {@link co.edu.uniquindio.bibliomtr.algorithms.clustering.HierarchicalClustering}
 * y los 4 stubs que completan los estudiantes (constitución P1):
 * <ul>
 *   <li>Aglomerativos (bottom-up): {@code SingleLinkage}, {@code CompleteLinkage} y {@code AverageLinkage}
 *       (UPGMA). Los tres recalculan la distancia del grupo nuevo con la fórmula de Lance-Williams.</li>
 *   <li>Divisivo (top-down, opcional): {@code Diana}, que divide recursivamente el grupo de mayor diámetro.</li>
 * </ul>
 *
 * <p><b>Entrada y salida.</b> Cada stub recibe la matriz de distancias N×N {@code D = 1 − S}, donde S es la
 * matriz de similitud de una de las 8 fuentes del Requerimiento 1, y devuelve un
 * {@link co.edu.uniquindio.bibliomtr.algorithms.clustering.ClusteringResult}: las N − 1 fusiones del dendrograma
 * ({@link co.edu.uniquindio.bibliomtr.algorithms.clustering.Merge}, convención de identificadores de SciPy) y una
 * traza con la matriz entre grupos de cada paso (constitución P7, paquete
 * {@code co.edu.uniquindio.bibliomtr.trace.clustering}). El andamiaje solo valida la estructura del dendrograma
 * ({@link co.edu.uniquindio.bibliomtr.algorithms.clustering.DendrogramValidator}); no contiene la lógica de
 * ningún algoritmo (REQ-6).
 *
 * <p><b>Guía.</b> Qué implementar, dónde, con qué fórmula, regla de desempate y tests se explica en
 * {@code docs/guia-implementacion.md}, § 10. Los tests de cada algoritmo se ejecutan con
 * {@code ./gradlew studentTest --tests <Clase>Test}.
 *
 * <p>Este paquete no depende de {@code api} ni de {@code persistence} (constitución P13), ni de librerías que
 * calculen clustering (constitución P1).
 */
package co.edu.uniquindio.bibliomtr.algorithms.clustering;
