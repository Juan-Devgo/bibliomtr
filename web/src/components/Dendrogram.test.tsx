import { render, screen, within } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { D4_MERGES } from '@/test/clusteringFixtures'
import Dendrogram from './Dendrogram'
import { createHeightScale, heightTicks, sharedMaxHeight } from './dendrogramScale'

const LABELS = ['#1 Alfa', '#2 Beta', '#3 Gamma', '#4 Delta']

const heightScale = createHeightScale(1)

describe('Dendrogram (REQ-14, REQ-15, AC-14)', () => {
  it('dibuja una hoja por artículo con su etiqueta y el título completo', () => {
    render(
      <Dendrogram
        merges={D4_MERGES['single-linkage']}
        labels={LABELS}
        titles={['Alfa completo', 'Beta completo', 'Gamma completo', 'Delta completo']}
        ariaLabel="Dendrograma: Single Linkage"
      />,
    )

    const svg = screen.getByRole('img', { name: 'Dendrograma: Single Linkage' })
    for (const label of LABELS) {
      expect(within(svg).getByText(label)).toBeInTheDocument()
    }
    expect(within(svg).getByText('Delta completo')).toBeInTheDocument()
  })

  it('ubica cada fusión según su altura en la escala por defecto [0,1] y conecta los 2N − 2 enlaces', () => {
    const { container } = render(
      <Dendrogram merges={D4_MERGES['complete-linkage']} labels={LABELS} ariaLabel="Dendrograma" />,
    )

    const nodes = [...container.querySelectorAll('circle')]
    expect(nodes.map((node) => Number(node.getAttribute('cx')))).toEqual(
      expect.arrayContaining([heightScale(0.2), heightScale(0.45), heightScale(0.7)]),
    )
    expect(nodes).toHaveLength(3)
    expect(container.querySelectorAll('path')).toHaveLength(6)
    expect(screen.getByText('G6: altura 0.700, 4 artículos')).toBeInTheDocument()
  })

  it('todos los dendrogramas con el mismo maxHeight comparten el eje de altura', () => {
    const maxHeight = sharedMaxHeight([D4_MERGES['single-linkage'], D4_MERGES.diana])
    const { container } = render(
      <>
        <Dendrogram merges={D4_MERGES['single-linkage']} labels={LABELS} ariaLabel="A" maxHeight={maxHeight} />
        <Dendrogram merges={D4_MERGES.diana} labels={LABELS} ariaLabel="B" maxHeight={maxHeight} />
      </>,
    )

    for (const name of ['A', 'B']) {
      const axis = within(screen.getByRole('img', { name })).getByLabelText('Eje de altura')
      expect(heightTicks(maxHeight).map((tick) => within(axis).getByText(String(tick)))).not.toHaveLength(0)
    }
    expect(container.querySelectorAll('svg')).toHaveLength(2)
  })

  it('con maxHeight menor que 1 la fusión más alta llega al final del eje', () => {
    const maxHeight = sharedMaxHeight([[{ left: 0, right: 1, height: 0.047, size: 2 }]])
    const scale = createHeightScale(maxHeight)
    const { container } = render(
      <Dendrogram
        merges={[{ left: 0, right: 1, height: 0.047, size: 2 }]}
        labels={['#1 Alfa', '#2 Beta']}
        ariaLabel="Dendrograma"
        maxHeight={maxHeight}
      />,
    )

    expect(maxHeight).toBe(0.05)
    expect(Number(container.querySelector('circle')?.getAttribute('cx'))).toBeCloseTo(scale(0.047))
    expect(scale(0.047)).toBeGreaterThan(createHeightScale(1)(0.5))
  })
})

describe('sharedMaxHeight (REQ-14)', () => {
  it('redondea hacia arriba la mayor altura de todas las listas', () => {
    expect(sharedMaxHeight([[{ left: 0, right: 1, height: 0.03, size: 2 }], [{ left: 0, right: 1, height: 0.047, size: 2 }]])).toBe(0.05)
  })

  it('vale 1 sin fusiones o con todas las alturas en 0', () => {
    expect(sharedMaxHeight([])).toBe(1)
    expect(sharedMaxHeight([[{ left: 0, right: 1, height: 0, size: 2 }]])).toBe(1)
  })
})
