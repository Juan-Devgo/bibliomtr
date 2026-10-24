import { screen, within } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { renderWithQuery } from '@/test/setup'
import { benchmarkRun, mockBenchmarkApi } from '@/test/benchmarkFixtures'
import { formatDateTime } from './benchmarkFormat'
import BenchmarkHistory from './BenchmarkHistory'

afterEach(() => {
  vi.unstubAllGlobals()
})

/** Elementos de la lista del historial, en el orden en que se muestran. */
async function historyItems(): Promise<HTMLElement[]> {
  const list = await screen.findByRole('list', { name: 'Mediciones JMH, de la más reciente a la más antigua' })
  return within(list).getAllByRole('listitem')
}

describe('BenchmarkHistory (REQ-28)', () => {
  it('lista 3 mediciones de la más reciente a la más antigua con fecha, estado y resultados (AC-20)', async () => {
    const failure = 'La medición falló: proceso JMH terminó con código 1'
    // El backend entrega el historial ya ordenado por fecha descendente; la vista no lo reordena.
    const runs = [
      benchmarkRun(3, 'COMPLETADA', '2026-10-23T17:00:00Z'),
      benchmarkRun(2, 'FALLIDA', '2026-10-23T16:00:00Z', failure),
      benchmarkRun(1, 'COMPLETADA', '2026-10-23T15:00:00Z'),
    ]
    mockBenchmarkApi(() => runs)
    renderWithQuery(<BenchmarkHistory />)

    const items = await historyItems()
    expect(items.map((item) => item.getAttribute('aria-label'))).toEqual(['Medición 3', 'Medición 2', 'Medición 1'])

    expect(items[0]).toHaveTextContent(`${formatDateTime('2026-10-23T17:00:00Z')} · COMPLETADA`)
    expect(items[1]).toHaveTextContent(`${formatDateTime('2026-10-23T16:00:00Z')} · FALLIDA`)
    expect(items[2]).toHaveTextContent(`${formatDateTime('2026-10-23T15:00:00Z')} · COMPLETADA`)

    for (const completed of [items[0], items[2]]) {
      expect(completed).toHaveTextContent('Counting Sort: 0.0123 ± 0.0004 ms · n = 88 · k = 42 · O(n+k)')
      expect(completed).toHaveTextContent('Radix Sort: 0.0456 ± 0.0012 ms · n = 88 · d = 2, b = 10 · O(d·(n+b))')
      expect(completed).toHaveTextContent(
        'Bucket Sort: 0.0789 ± 0.0021 ms · n = 88 · m = 9 · O(n+m) promedio, O(n²) peor caso',
      )
    }
    expect(items[1]).toHaveTextContent(failure)
    expect(items[1]).not.toHaveTextContent('Counting Sort')
  })

  it('muestra 30 mediciones en una sola lista, sin paginar, en orden descendente (AC-27)', async () => {
    // Ids 30..1 con fechas descendentes de hora en hora, como las entrega el backend.
    const runs = Array.from({ length: 30 }, (_, index) => {
      const id = 30 - index
      const startedAt = new Date(Date.UTC(2026, 9, 1) + id * 3_600_000).toISOString()
      return benchmarkRun(id, 'COMPLETADA', startedAt)
    })
    mockBenchmarkApi(() => runs)
    renderWithQuery(<BenchmarkHistory />)

    const items = await historyItems()
    expect(items).toHaveLength(30)
    expect(items.map((item) => item.getAttribute('aria-label'))).toEqual(
      runs.map((run) => `Medición ${run.id}`),
    )
    expect(screen.getAllByRole('list', { name: /Mediciones JMH/ })).toHaveLength(1)
    expect(screen.queryByRole('navigation')).not.toBeInTheDocument()
    expect(screen.queryByRole('button')).not.toBeInTheDocument()
  })
})
