import { render, screen, within } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { D4_MERGES } from '@/test/clusteringFixtures'
import Dendrogram from './Dendrogram'
import { axisMaxHeight, createHeightScale, heightTicks } from './dendrogramScale'

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

  it('ubica cada fusión según su altura en una escala [0,1] y conecta los 2N − 2 enlaces', () => {
    const { container } = render(
      <Dendrogram merges={D4_MERGES['complete-linkage']} labels={LABELS} ariaLabel="Dendrograma" maxHeight={1} />,
    )

    const nodes = [...container.querySelectorAll('circle')]
    expect(nodes.map((node) => Number(node.getAttribute('cx')))).toEqual(
      expect.arrayContaining([heightScale(0.2), heightScale(0.45), heightScale(0.7)]),
    )
    expect(nodes).toHaveLength(3)
    expect(container.querySelectorAll('path')).toHaveLength(6)
    expect(screen.getByText('G6: altura 0.700, 4 artículos')).toBeInTheDocument()
  })

  it('cada dendrograma ajusta su eje a sus propias alturas', () => {
    const small = [{ left: 0, right: 1, height: 0.047, size: 2 }]
    const large = [{ left: 0, right: 1, height: 0.8, size: 2 }]
    render(
      <>
        <Dendrogram merges={small} labels={['#1 Alfa', '#2 Beta']} ariaLabel="A" />
        <Dendrogram merges={large} labels={['#1 Alfa', '#2 Beta']} ariaLabel="B" />
      </>,
    )

    for (const [name, merges] of [['A', small], ['B', large]] as const) {
      const maxHeight = axisMaxHeight(merges)
      const axis = within(screen.getByRole('img', { name })).getByLabelText('Eje de altura')
      expect(heightTicks(maxHeight).map((tick) => within(axis).getByText(String(tick)))).not.toHaveLength(0)
    }
    expect(heightTicks(axisMaxHeight(small))).not.toEqual(heightTicks(axisMaxHeight(large)))
  })

  it('sin maxHeight la fusión más alta llega al final del eje del propio dendrograma', () => {
    const maxHeight = axisMaxHeight([{ left: 0, right: 1, height: 0.047, size: 2 }])
    const scale = createHeightScale(maxHeight)
    const { container } = render(
      <Dendrogram
        merges={[{ left: 0, right: 1, height: 0.047, size: 2 }]}
        labels={['#1 Alfa', '#2 Beta']}
        ariaLabel="Dendrograma"
      />,
    )

    expect(maxHeight).toBe(0.05)
    expect(Number(container.querySelector('circle')?.getAttribute('cx'))).toBeCloseTo(scale(0.047))
    expect(scale(0.047)).toBeGreaterThan(createHeightScale(1)(0.5))
  })
})

describe('axisMaxHeight (REQ-14)', () => {
  it('redondea hacia arriba la mayor altura de las fusiones', () => {
    expect(axisMaxHeight([{ left: 0, right: 1, height: 0.03, size: 2 }, { left: 2, right: 3, height: 0.047, size: 3 }])).toBe(0.05)
  })

  it('vale 1 sin fusiones o con todas las alturas en 0', () => {
    expect(axisMaxHeight([])).toBe(1)
    expect(axisMaxHeight([{ left: 0, right: 1, height: 0, size: 2 }])).toBe(1)
  })
})
