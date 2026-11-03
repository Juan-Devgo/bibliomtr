import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'
import {
  ALL_IMPLEMENTED,
  EMPTY_CORPUS,
  FIXTURE_POSITIONS,
  ONLY_KMP,
  STRING_MATCHING_STUB_DIRECTORY,
} from '@/test/patternSearchFixtures'
import { renderWithQuery } from '@/test/setup'
import type { PatternSearchResponse } from '@/types/patternSearch'
import PatternSearchView from './PatternSearchView'

/** API simulada: cada llamada a `fetch` responde `body` con el código `status`. */
function mockFetch(body: PatternSearchResponse | { status: number; detail: string }, status = 200) {
  const fetchMock = vi.fn(async (_input: RequestInfo | URL, _init?: RequestInit) => Response.json(body, { status }))
  vi.stubGlobal('fetch', fetchMock)
  return fetchMock
}

/** Parámetros de la última consulta a `GET /api/pattern-search`, ya decodificados. */
function lastQuery(fetchMock: ReturnType<typeof mockFetch>): URLSearchParams {
  const calls = fetchMock.mock.calls
  const url = new URL(String(calls[calls.length - 1][0]), 'http://localhost')
  expect(url.pathname).toBe('/api/pattern-search')
  return url.searchParams
}

/** Escribe `pattern` (y `q` si viene) y pulsa "Buscar". */
async function searchFor(user: ReturnType<typeof userEvent.setup>, pattern: string, q?: string) {
  if (pattern.length > 0) {
    await user.type(screen.getByLabelText('Patrón'), pattern)
  }
  if (q !== undefined) {
    await user.type(screen.getByLabelText('q de Rabin-Karp (opcional)'), q)
  }
  await user.click(screen.getByRole('button', { name: 'Buscar' }))
}

/** Filas de datos (sin encabezado) de la tabla de ocurrencias de `label`. */
function occurrenceRows(label: string): HTMLElement[] {
  const section = screen.getByRole('region', { name: label })
  return within(within(section).getByRole('table', { name: `Ocurrencias de ${label}` }))
    .getAllByRole('row')
    .slice(1)
}

/** Texto de cada celda de cada fila. */
function cellTexts(rows: HTMLElement[]): (string | null)[][] {
  return rows.map((row) => within(row).getAllByRole('cell').map((cell) => cell.textContent))
}

/** Tramos resaltados del abstract y las posiciones de sus marcas de inicio. */
function highlighted(): { text: string | null; starts: (string | null)[] }[] {
  const abstract = screen.getByLabelText('Texto del abstract')
  return Array.from(abstract.querySelectorAll('mark')).map((mark) => ({
    text: mark.textContent,
    starts: Array.from(mark.querySelectorAll('[aria-label^="Inicio de ocurrencia en "]')).map((marker) =>
      marker.getAttribute('aria-label'),
    ),
  }))
}

afterEach(() => {
  vi.unstubAllGlobals()
})

describe('PatternSearchView (REQ-17, REQ-18, REQ-19, REQ-20, REQ-21, REQ-28, REQ-29)', () => {
  it('muestra el campo del patrón, el q opcional y "Buscar", y no consulta hasta buscar (REQ-19)', () => {
    const fetchMock = mockFetch(ALL_IMPLEMENTED)
    renderWithQuery(<PatternSearchView />)

    expect(screen.getByLabelText('Patrón')).toHaveValue('')
    expect(screen.getByLabelText('q de Rabin-Karp (opcional)')).toHaveValue('')
    expect(screen.getByRole('button', { name: 'Buscar' })).toBeInTheDocument()
    expect(fetchMock).not.toHaveBeenCalled()
  })

  it('envía " " como patrón de 1 carácter, sin recortar, y omite q si está vacío (AC-25)', async () => {
    const user = userEvent.setup()
    const fetchMock = mockFetch(ALL_IMPLEMENTED)
    renderWithQuery(<PatternSearchView />)

    await searchFor(user, ' ')

    await screen.findByRole('region', { name: 'Knuth-Morris-Pratt (KMP)' })
    const query = lastQuery(fetchMock)
    expect(query.get('pattern')).toBe(' ')
    expect(query.has('q')).toBe(false)
  })

  it('conserva los espacios de los extremos del patrón y envía q como se escribió', async () => {
    const user = userEvent.setup()
    const fetchMock = mockFetch(ALL_IMPLEMENTED)
    renderWithQuery(<PatternSearchView />)

    await searchFor(user, ' abab ', '3')

    await screen.findByRole('region', { name: 'Rabin-Karp' })
    const query = lastQuery(fetchMock)
    expect(query.get('pattern')).toBe(' abab ')
    expect(query.get('q')).toBe('3')
  })

  it('muestra tal cual el mensaje en español de un 400 por patrón vacío (AC-13)', async () => {
    const user = userEvent.setup()
    const detail = 'El patrón no puede estar vacío'
    const fetchMock = mockFetch({ status: 400, detail }, 400)
    renderWithQuery(<PatternSearchView />)

    await searchFor(user, '')

    expect(await screen.findByText(detail)).toBeInTheDocument()
    expect(lastQuery(fetchMock).get('pattern')).toBe('')
  })

  it('muestra tal cual el mensaje en español de un 400 por q inválido (AC-13)', async () => {
    const user = userEvent.setup()
    const detail = 'El parámetro q debe ser un número primo entre 2 y 1000000007; se recibió: abc'
    const fetchMock = mockFetch({ status: 400, detail }, 400)
    renderWithQuery(<PatternSearchView />)

    await searchFor(user, 'abab', 'abc')

    expect(await screen.findByText(detail)).toBeInTheDocument()
    expect(lastQuery(fetchMock).get('q')).toBe('abc')
  })

  it('muestra por algoritmo los tiempos por fase, el total, la tabla y el indicador de coincidencia (AC-21)', async () => {
    const user = userEvent.setup()
    mockFetch(ALL_IMPLEMENTED)
    renderWithQuery(<PatternSearchView />)

    await searchFor(user, 'abab')

    const kmp = await screen.findByRole('region', { name: 'Knuth-Morris-Pratt (KMP)' })
    expect(within(kmp).getByText('Preprocesamiento: 0.125 ms')).toBeInTheDocument()
    expect(within(kmp).getByText('Búsqueda: 0.500 ms')).toBeInTheDocument()
    expect(within(kmp).getByText('Total: 4')).toBeInTheDocument()
    const boyerMoore = screen.getByRole('region', { name: 'Boyer-Moore' })
    expect(within(boyerMoore).getByText('Preprocesamiento: 0.250 ms')).toBeInTheDocument()
    expect(within(boyerMoore).getByText('Búsqueda: 0.750 ms')).toBeInTheDocument()
    const rabinKarp = screen.getByRole('region', { name: 'Rabin-Karp' })
    expect(within(rabinKarp).getByText('Preprocesamiento: 0.063 ms')).toBeInTheDocument()
    expect(within(rabinKarp).getByText('Búsqueda: 1.000 ms')).toBeInTheDocument()

    // Las colisiones solo aparecen en la tabla de Rabin-Karp.
    expect(cellTexts(occurrenceRows('Knuth-Morris-Pratt (KMP)'))).toEqual([
      ['#1 Búsqueda exacta en cadenas', '2', '0, 2'],
      ['#2 Ocurrencias repetidas', '2', '0, 5'],
    ])
    expect(cellTexts(occurrenceRows('Rabin-Karp'))).toEqual([
      ['#1 Búsqueda exacta en cadenas', '2', '0, 2', '0'],
      ['#2 Ocurrencias repetidas', '2', '0, 5', '0'],
    ])
    expect(within(rabinKarp).getByRole('columnheader', { name: 'Colisiones' })).toBeInTheDocument()
    expect(within(kmp).queryByRole('columnheader', { name: 'Colisiones' })).not.toBeInTheDocument()

    expect(screen.getByText('Coinciden todas las posiciones: Sí')).toBeInTheDocument()
  })

  it('con solo KMP implementado muestra sus posiciones, los pendientes y ningún indicador (AC-20)', async () => {
    const user = userEvent.setup()
    mockFetch(ONLY_KMP)
    renderWithQuery(<PatternSearchView />)

    await searchFor(user, 'abab')

    await screen.findByRole('region', { name: 'Knuth-Morris-Pratt (KMP)' })
    expect(cellTexts(occurrenceRows('Knuth-Morris-Pratt (KMP)'))).toEqual([
      ['#1 Búsqueda exacta en cadenas', '2', '0, 2'],
      ['#2 Ocurrencias repetidas', '2', '0, 5'],
    ])
    const boyerMoore = screen.getByRole('region', { name: 'Boyer-Moore' })
    expect(within(boyerMoore).getByText(
      `Pendiente de implementar: ${STRING_MATCHING_STUB_DIRECTORY}BoyerMoore.java#preprocess`,
    )).toBeInTheDocument()
    expect(within(boyerMoore).queryByRole('table')).not.toBeInTheDocument()
    expect(within(boyerMoore).queryByText(/^Preprocesamiento:/)).not.toBeInTheDocument()
    const rabinKarp = screen.getByRole('region', { name: 'Rabin-Karp' })
    expect(within(rabinKarp).getByText(
      `Pendiente de implementar: ${STRING_MATCHING_STUB_DIRECTORY}RabinKarp.java#preprocess`,
    )).toBeInTheDocument()
    expect(screen.queryByText(/^Coinciden todas las posiciones/)).not.toBeInTheDocument()

    // El selector solo ofrece el algoritmo implementado.
    await user.click(occurrenceRows('Knuth-Morris-Pratt (KMP)')[0])
    const selector = screen.getByLabelText('Algoritmo resaltado')
    expect(within(selector).getAllByRole('option').map((option) => option.textContent)).toEqual([
      'Knuth-Morris-Pratt (KMP)',
    ])
  })

  it('con el corpus sin abstracts muestra "Sin abstracts en el corpus" (AC-14)', async () => {
    const user = userEvent.setup()
    mockFetch(EMPTY_CORPUS)
    renderWithQuery(<PatternSearchView />)

    await searchFor(user, 'abab')

    expect(await screen.findByText('Sin abstracts en el corpus')).toBeInTheDocument()
    expect(screen.queryByRole('table')).not.toBeInTheDocument()
  })

  it('al elegir un artículo resalta las posiciones de KMP por defecto, con las solapadas en un tramo (AC-15)', async () => {
    const user = userEvent.setup()
    mockFetch(ALL_IMPLEMENTED)
    renderWithQuery(<PatternSearchView />)

    await searchFor(user, 'abab')
    await screen.findByRole('region', { name: 'Knuth-Morris-Pratt (KMP)' })
    expect(screen.queryByLabelText('Texto del abstract')).not.toBeInTheDocument()

    await user.click(occurrenceRows('Boyer-Moore')[0])

    expect(screen.getByLabelText('Algoritmo resaltado')).toHaveValue('kmp')
    expect(screen.getByLabelText('Texto del abstract')).toHaveTextContent('abababca')
    // "abababca" con [0, 2] y m = 4: un tramo continuo [0, 6) con marcas en 0 y 2, y "ca" sin resaltar.
    expect(highlighted()).toEqual([
      { text: 'ababab', starts: ['Inicio de ocurrencia en 0', 'Inicio de ocurrencia en 2'] },
    ])
    for (const label of ['Knuth-Morris-Pratt (KMP)', 'Boyer-Moore', 'Rabin-Karp']) {
      expect(occurrenceRows(label)[0]).toHaveAttribute('aria-selected', 'true')
    }

    await user.click(occurrenceRows('Knuth-Morris-Pratt (KMP)')[1])

    expect(screen.getByLabelText('Texto del abstract')).toHaveTextContent('abab abab')
    expect(highlighted()).toEqual([
      { text: 'abab', starts: ['Inicio de ocurrencia en 0'] },
      { text: 'abab', starts: ['Inicio de ocurrencia en 5'] },
    ])
  })

  it('resalta exactamente las posiciones de la API del algoritmo elegido, aunque no sean ocurrencias (AC-15, REQ-21)', async () => {
    const user = userEvent.setup()
    mockFetch(FIXTURE_POSITIONS)
    renderWithQuery(<PatternSearchView />)

    await searchFor(user, 'abab')
    await screen.findByRole('region', { name: 'Knuth-Morris-Pratt (KMP)' })
    expect(screen.getByText('Coinciden todas las posiciones: No')).toBeInTheDocument()

    await user.click(occurrenceRows('Knuth-Morris-Pratt (KMP)')[0])

    // KMP por defecto: [1, 4] con m = 4 en "abababca" → tramo [1, 8) con marcas en 1 y 4.
    expect(highlighted()).toEqual([
      { text: 'bababca', starts: ['Inicio de ocurrencia en 1', 'Inicio de ocurrencia en 4'] },
    ])

    await user.selectOptions(screen.getByLabelText('Algoritmo resaltado'), 'rabin-karp')

    // Rabin-Karp: [3] → tramo [3, 7).
    expect(screen.getByLabelText('Algoritmo resaltado')).toHaveValue('rabin-karp')
    expect(highlighted()).toEqual([{ text: 'babc', starts: ['Inicio de ocurrencia en 3'] }])
    expect(screen.getByText('Posiciones de Rabin-Karp: 3')).toBeInTheDocument()

    // En el segundo artículo Rabin-Karp no tiene posiciones: nada resaltado.
    await user.click(occurrenceRows('Rabin-Karp')[1])
    expect(highlighted()).toEqual([])
    expect(screen.getByText('Posiciones de Rabin-Karp: ninguna')).toBeInTheDocument()
  })
})
