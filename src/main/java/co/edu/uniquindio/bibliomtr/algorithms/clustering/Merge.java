package co.edu.uniquindio.bibliomtr.algorithms.clustering;

/**
 * Una fusión del dendrograma (Requerimiento 3, REQ-3), con la convención de identificadores de SciPy.
 *
 * <p><b>Convención.</b> Con N artículos, las hojas son los grupos 0..N−1 (el artículo i de la solicitud es el
 * grupo i) y la fusión k, con 0 ≤ k &lt; N − 1, crea el grupo N + k. Así un dendrograma completo es la lista de
 * sus N − 1 fusiones, en el orden en que se crean los grupos.
 *
 * <p><b>Ejemplo.</b> Con la matriz D₄ de la guía, la primera fusión de Single Linkage es
 * {@code Merge(0, 1, 0.2, 2)} y crea el grupo 4; la segunda, {@code Merge(2, 4, 0.3, 3)}, une el artículo 2 con
 * ese grupo.
 *
 * <p>Complejidad: construcción O(1) en tiempo y espacio.
 *
 * @param left   identificador del primer grupo fusionado; siempre {@code left < right}
 * @param right  identificador del segundo grupo fusionado
 * @param height altura de la fusión: la distancia entre los dos grupos, en [0,1]
 * @param size   número de artículos del grupo nuevo
 */
public record Merge(int left, int right, double height, int size) {
}
