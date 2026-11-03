import { render, screen, within } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { D4_MERGES } from '@/test/clusteringFixtures'
import Dendrogram from './Dendrogram'
import { HEIGHT_TICKS, heightScale } from './dendrogramScale'

const LABELS = ['#1 Alfa', '#2 Beta', '#3 Gamma', '#4 Delta']

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

  it('ubica cada fusión según su altura en la escala [0,1] y conecta los 2N − 2 enlaces', () => {
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

  it('todos los dendrogramas comparten el eje de altura 0–1', () => {
    const { container } = render(
      <>
        <Dendrogram merges={D4_MERGES['single-linkage']} labels={LABELS} ariaLabel="A" />
        <Dendrogram merges={D4_MERGES.diana} labels={LABELS} ariaLabel="B" />
      </>,
    )

    for (const name of ['A', 'B']) {
      const axis = within(screen.getByRole('img', { name })).getByLabelText('Eje de altura')
      expect(HEIGHT_TICKS.map((tick) => within(axis).getByText(String(tick)))).toHaveLength(5)
    }
    expect(heightScale.domain()).toEqual([0, 1])
    expect(container.querySelectorAll('svg')).toHaveLength(2)
  })
})
