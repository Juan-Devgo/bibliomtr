import { screen, within } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { renderWithQuery } from '@/test/setup'
import type { RankingResponse, TermFrequency } from '@/types/ranking'
import RankingView from './RankingView'

/** API simulada: cada llamada a `fetch` responde `body` con el código `status`. */
function mockFetch(body: unknown, status = 200) {
  const fetchMock = vi.fn(async () =>
    new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } }),
  )
  vi.stubGlobal('fetch', fetchMock)
  return fetchMock
}

/** Respuesta con el mismo top en los tres algoritmos. */
function rankingResponse(top: TermFrequency[]): RankingResponse {
  return {
    termCount: top.length,
    n: 20,
    bucketSize: 5,
    preprocessingMs: 1.25,
    allMatch: true,
    algorithms: [
      { algorithm: 'counting', top, elapsedMs: 0.1 },
      { algorithm: 'radix', top, elapsedMs: 0.2 },
      { algorithm: 'bucket', top, elapsedMs: 0.3 },
    ],
  }
}

afterEach(() => {
  vi.unstubAllGlobals()
})

describe('RankingView (REQ-5, AC-4, AC-5)', () => {
  it('muestra el top N de cada algoritmo como tabla de 3 filas y gráfico de 3 barras', async () => {
    const top = [
      { term: 'model', frequency: 42 },
      { term: 'data', frequency: 30 },
      { term: 'graph', frequency: 30 },
    ]
    const fetchMock = mockFetch(rankingResponse(top))
    renderWithQuery(<RankingView />)

    // La primera petición usa los valores por defecto N = 20 y bucketSize = 5.
    await screen.findByRole('region', { name: 'Counting Sort' })
    expect(fetchMock).toHaveBeenCalledWith('/api/ranking?n=20&bucketSize=5', expect.anything())

    for (const name of ['Counting Sort', 'Radix Sort', 'Bucket Sort']) {
      const section = screen.getByRole('region', { name })

      const table = within(section).getByRole('table', { name: `Tabla del ranking de ${name}` })
      const rows = within(table).getAllByRole('row').slice(1) // sin la fila de encabezado
      expect(rows).toHaveLength(3)
      expect(rows.map((row) => within(row).getAllByRole('cell').map((cell) => cell.textContent))).toEqual([
        ['1', 'model', '42'],
        ['2', 'data', '30'],
        ['3', 'graph', '30'],
      ])

      const chart = within(section).getByRole('figure', { name: `Gráfico de barras del ranking de ${name}` })
      expect(chart.querySelectorAll('.recharts-bar-rectangle')).toHaveLength(3)
    }

    expect(screen.getByText('Tiempo de preprocesamiento: 1.250 ms')).toBeInTheDocument()
    expect(screen.getByText('Los tres rankings completos coinciden.')).toBeInTheDocument()
  })

  it('muestra "Sin términos en el corpus" cuando el ranking está vacío', async () => {
    mockFetch(rankingResponse([]))
    renderWithQuery(<RankingView />)

    expect(await screen.findByText('Sin términos en el corpus')).toBeInTheDocument()
    expect(screen.queryByRole('table')).not.toBeInTheDocument()
  })

  it('muestra tal cual el mensaje en español de un error 400 de la API', async () => {
    const detail = 'El parámetro n debe ser un entero mayor o igual a 1'
    mockFetch({ status: 400, title: 'Bad Request', detail }, 400)
    renderWithQuery(<RankingView />)

    expect(await screen.findByText(detail)).toBeInTheDocument()
  })
})
