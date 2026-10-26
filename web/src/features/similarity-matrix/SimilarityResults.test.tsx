import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { ALL_CALCULATED, pending, STUB_DIRECTORY } from '@/test/similarityFixtures'
import { SIMILARITY_ALGORITHM_LABELS, type ComparisonResponse } from '@/types/similarity'
import SimilarityResults from './SimilarityResults'

describe('SimilarityResults (REQ-17, REQ-21, REQ-22, AC-13, AC-18, AC-19)', () => {
  it('dibuja 8 mapas de calor 3×3 en el orden recibido', () => {
    render(<SimilarityResults response={ALL_CALCULATED} />)

    const tables = screen.getAllByRole('table', { name: /^Mapa de calor: / })
    expect(tables.map((table) => table.getAttribute('aria-label'))).toEqual(
      ALL_CALCULATED.results.map((result) => `Mapa de calor: ${SIMILARITY_ALGORITHM_LABELS[result.algorithm]}`),
    )
    for (const table of tables) {
      expect(within(table).getAllByRole('cell')).toHaveLength(9)
      expect(within(table).getAllByRole('rowheader').map((th) => th.textContent)).toEqual(['#1', '#2', '#3'])
    }
    // Jaccard (cuarto resultado, k = 4): s(#1, #2) = 0.4.
    const jaccard = screen.getByRole('table', { name: 'Mapa de calor: Jaccard' })
    expect(within(jaccard).getByRole('cell', { name: 'fila #2, columna #1: 0.400' })).toBeInTheDocument()
  })

  it('muestra el tiempo de preprocesamiento y el de cada algoritmo en ms, por separado', () => {
    render(<SimilarityResults response={ALL_CALCULATED} />)

    expect(screen.getByText('Preprocesamiento: 1.250 ms')).toBeInTheDocument()
    const levenshtein = screen.getByRole('region', { name: 'Levenshtein' })
    expect(within(levenshtein).getByText('Ejecución: 0.500 ms')).toBeInTheDocument()
    const nomicEuclidean = screen.getByRole('region', { name: 'nomic-embed-text (euclidiana)' })
    expect(within(nomicEuclidean).getByText('Ejecución: 4.000 ms')).toBeInTheDocument()
    expect(screen.getAllByText(/^Ejecución: \d+\.\d{3} ms$/)).toHaveLength(8)
  })

  it('muestra "Pendiente de implementar: <archivo>#<método>" para un algoritmo PENDIENTE, sin mapa de calor', () => {
    const response: ComparisonResponse = {
      ...ALL_CALCULATED,
      results: [pending('levenshtein', 'Levenshtein'), ...ALL_CALCULATED.results.slice(1)],
    }
    render(<SimilarityResults response={response} />)

    const levenshtein = screen.getByRole('region', { name: 'Levenshtein' })
    expect(
      within(levenshtein).getByText(`Pendiente de implementar: ${STUB_DIRECTORY}Levenshtein.java#compare`),
    ).toBeInTheDocument()
    expect(
      within(levenshtein).getByText('Pendiente de implementar: src/main/java/co/edu/uniquindio/bibliomtr/algorithms/similarity/Levenshtein.java#compare'),
    ).toBeInTheDocument()
    expect(within(levenshtein).queryByRole('table')).not.toBeInTheDocument()
    expect(within(levenshtein).queryByText(/^Ejecución:/)).not.toBeInTheDocument()
    // Los demás resultados se muestran con normalidad (REQ-20).
    expect(screen.getAllByRole('table', { name: /^Mapa de calor: / })).toHaveLength(7)
  })

  it('informa el algoritmo y la celda elegida para pedir su traza', async () => {
    const user = userEvent.setup()
    const onSelectCell = vi.fn()
    render(<SimilarityResults response={ALL_CALCULATED} onSelectCell={onSelectCell} />)

    const jaccard = screen.getByRole('table', { name: 'Mapa de calor: Jaccard' })
    await user.click(within(jaccard).getByRole('button', { name: 'fila #1, columna #3: 0.200' }))

    expect(onSelectCell).toHaveBeenCalledWith('jaccard', 0, 2)
  })
})
