import { articleLabel } from '@/features/similarity-matrix/resultFormat'
import type { Merge } from '@/types/clustering'
import type { ArticleSummary } from '@/types/similarity'

/** Máximo de caracteres del título en la etiqueta de una hoja (REQ-14). */
export const TITLE_LENGTH = 40

/**
 * Nodo del árbol que dibuja D3. Las hojas tienen `id` = índice del artículo, altura 0 y tamaño 1; el grupo
 * N + k toma la altura y el tamaño de la fusión k.
 */
export interface DendrogramNode {
  id: number
  label: string
  height: number
  size: number
  children?: [DendrogramNode, DendrogramNode]
}

/**
 * Convierte las fusiones de la API en el árbol que dibuja D3 (REQ-15): solo reorganiza los datos, no calcula
 * fusiones, alturas ni grupos. Los hijos del grupo N + k son `left` y `right` de `merges[k]`, en ese orden.
 *
 * Complejidad: O(N) en tiempo y espacio.
 *
 * @throws Error si las fusiones no forman un solo árbol con las N hojas
 */
export function toHierarchy(merges: readonly Merge[], leafLabels: readonly string[]): DendrogramNode {
  const n = leafLabels.length
  if (n === 0 || merges.length !== n - 1) {
    throw new Error(`Se esperaban ${Math.max(0, n - 1)} fusiones para ${n} hojas; se recibieron ${merges.length}`)
  }
  const nodes: DendrogramNode[] = leafLabels.map((label, id) => ({ id, label, height: 0, size: 1 }))
  const used = new Set<number>()
  merges.forEach((merge, k) => {
    const id = n + k
    for (const child of [merge.left, merge.right]) {
      if (!Number.isInteger(child) || child < 0 || child >= id || used.has(child)) {
        throw new Error(`La fusión ${k} usa un grupo inválido: ${child}`)
      }
      used.add(child)
    }
    nodes.push({
      id,
      label: `G${id}`,
      height: merge.height,
      size: merge.size,
      children: [nodes[merge.left], nodes[merge.right]],
    })
  })
  return nodes[nodes.length - 1]
}

/** Etiqueta visible de una hoja: `#id` y el título recortado a `TITLE_LENGTH` caracteres. */
export function leafLabel(article: ArticleSummary): string {
  const title =
    article.title.length > TITLE_LENGTH ? `${article.title.slice(0, TITLE_LENGTH - 1).trimEnd()}…` : article.title
  return `${articleLabel(article.id)} ${title}`
}
