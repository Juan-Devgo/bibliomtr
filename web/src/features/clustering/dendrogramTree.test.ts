import { describe, expect, it } from 'vitest'
import { CLUSTERING_ARTICLES, D4_MERGES } from '@/test/clusteringFixtures'
import { leafLabel, toHierarchy, type DendrogramNode } from './dendrogramTree'

/** Ids de las hojas en el orden en que se recorren, izquierda primero. */
function leafOrder(node: DendrogramNode): number[] {
  return node.children ? [...leafOrder(node.children[0]), ...leafOrder(node.children[1])] : [node.id]
}

describe('toHierarchy (REQ-15, AC-14)', () => {
  it('arma el árbol de Single Linkage sobre D₄ con los hijos en el orden de la API', () => {
    const root = toHierarchy(D4_MERGES['single-linkage'], ['a', 'b', 'c', 'd'])

    expect(root).toMatchObject({ id: 6, height: 0.45, size: 4 })
    expect(root.children?.map((child) => child.id)).toEqual([3, 5])
    expect(root.children?.[1].children?.map((child) => child.id)).toEqual([2, 4])
    expect(root.children?.[1].children?.[1]).toMatchObject({ id: 4, height: 0.2, size: 2 })
    expect(leafOrder(root)).toEqual([3, 2, 0, 1])
  })

  it('las hojas tienen altura 0, tamaño 1 y su etiqueta', () => {
    const root = toHierarchy(D4_MERGES.diana, ['a', 'b', 'c', 'd'])
    const leaf = root.children?.[0]

    expect(leaf).toEqual({ id: 3, label: 'd', height: 0, size: 1 })
  })

  it('2 hojas dan un solo nodo interno', () => {
    const root = toHierarchy([{ left: 0, right: 1, height: 0.35, size: 2 }], ['a', 'b'])

    expect(root).toMatchObject({ id: 2, height: 0.35, size: 2 })
    expect(leafOrder(root)).toEqual([0, 1])
  })

  it('rechaza fusiones que no forman un solo árbol', () => {
    expect(() => toHierarchy(D4_MERGES['single-linkage'].slice(0, 2), ['a', 'b', 'c', 'd'])).toThrow(
      'Se esperaban 3 fusiones para 4 hojas; se recibieron 2',
    )
    expect(() =>
      toHierarchy(
        [
          { left: 0, right: 1, height: 0.1, size: 2 },
          { left: 0, right: 2, height: 0.2, size: 2 },
        ],
        ['a', 'b', 'c'],
      ),
    ).toThrow('La fusión 1 usa un grupo inválido: 0')
    expect(() => toHierarchy([{ left: 0, right: 2, height: 0.1, size: 2 }], ['a', 'b'])).toThrow(
      'La fusión 0 usa un grupo inválido: 2',
    )
  })
})

describe('leafLabel (REQ-14, AC-14)', () => {
  it('muestra #id y el título, recortado a 40 caracteres', () => {
    expect(leafLabel(CLUSTERING_ARTICLES[0])).toBe('#11 Generative AI in education')
    const long = leafLabel(CLUSTERING_ARTICLES[3])
    expect(long.startsWith('#14 Large language models as teaching')).toBe(true)
    expect(long.endsWith('…')).toBe(true)
    expect(long.length - '#14 '.length).toBeLessThanOrEqual(40)
  })
})
