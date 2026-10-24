import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { renderWithQuery } from '@/test/setup'
import type { SortTraceResponse, TermFrequency } from '@/types/ranking'
import SortTraceView from './SortTraceView'

/** API simulada: responde la traza según la ruta pedida. */
function mockTraceApi(responses: Record<string, SortTraceResponse>) {
  const fetchMock = vi.fn(async (input: RequestInfo | URL) => {
    const url = String(input)
    const slug = url.substring('/api/ranking/trace/'.length, url.indexOf('?'))
    return new Response(JSON.stringify(responses[slug]), {
      status: 200,
      headers: { 'Content-Type': 'application/json' },
    })
  })
  vi.stubGlobal('fetch', fetchMock)
  return fetchMock
}

/** Títulos de los pasos de la traza, en el orden en que aparecen. */
function stepTitles(): string[] {
  return screen.getAllByRole('heading', { level: 3 }).map((heading) => heading.textContent ?? '')
}

/** 88 términos con frecuencia 1, en orden alfabético descendente (t87, …, t00). */
const INPUT_88: TermFrequency[] = Array.from({ length: 88 }, (_, i) => ({
  term: `t${String(87 - i).padStart(2, '0')}`,
  frequency: 1,
}))

const COUNTING_88: SortTraceResponse = {
  algorithm: 'counting',
  bucketSize: 5,
  input: INPUT_88,
  sorted: INPUT_88,
  ranking: [...INPUT_88].reverse(),
  trace: {
    input: INPUT_88,
    max: 1,
    count: [0, 88],
    cumulative: [0, 88],
    placements: INPUT_88.map((element, i) => ({ inputIndex: 87 - i, element, outputIndex: 87 - i })),
    output: INPUT_88,
  },
}

// Ejemplo de AC-11 reducido a dos pasadas: [17, 4] → exp = 1: [4, 17] → exp = 10: [4, 17].
const RADIX_INPUT = [
  { term: 'b', frequency: 17 },
  { term: 'a', frequency: 4 },
]
const RADIX_OUTPUT = [RADIX_INPUT[1], RADIX_INPUT[0]]
const RADIX: SortTraceResponse = {
  algorithm: 'radix',
  bucketSize: 5,
  input: RADIX_INPUT,
  sorted: RADIX_OUTPUT,
  ranking: RADIX_INPUT,
  trace: {
    input: RADIX_INPUT,
    max: 17,
    passes: [
      { exp: 1, digits: [7, 4], count: [0, 0, 0, 0, 1, 0, 0, 1, 0, 0], cumulative: [0, 0, 0, 0, 1, 1, 1, 2, 2, 2], result: RADIX_OUTPUT },
      { exp: 10, digits: [0, 1], count: [1, 1, 0, 0, 0, 0, 0, 0, 0, 0], cumulative: [1, 2, 2, 2, 2, 2, 2, 2, 2, 2], result: RADIX_OUTPUT },
    ],
    output: RADIX_OUTPUT,
  },
}

// Ejemplo de AC-9: [5, 3, 8, 3] con bucketSize = 2 → 3 cubetas: [3, 3], [5], [8].
const BUCKET_INPUT = [
  { term: 'd', frequency: 5 },
  { term: 'c', frequency: 3 },
  { term: 'b', frequency: 8 },
  { term: 'a', frequency: 3 },
]
const BUCKET_OUTPUT = [BUCKET_INPUT[1], BUCKET_INPUT[3], BUCKET_INPUT[0], BUCKET_INPUT[2]]
const BUCKET: SortTraceResponse = {
  algorithm: 'bucket',
  bucketSize: 2,
  input: BUCKET_INPUT,
  sorted: BUCKET_OUTPUT,
  ranking: [...BUCKET_OUTPUT].reverse(),
  trace: {
    input: BUCKET_INPUT,
    min: 3,
    max: 8,
    bucketSize: 2,
    bucketCount: 3,
    buckets: [
      { index: 0, before: [BUCKET_INPUT[1], BUCKET_INPUT[3]], after: [BUCKET_INPUT[1], BUCKET_INPUT[3]] },
      { index: 1, before: [BUCKET_INPUT[0]], after: [BUCKET_INPUT[0]] },
      { index: 2, before: [BUCKET_INPUT[2]], after: [BUCKET_INPUT[2]] },
    ],
    concatenated: BUCKET_OUTPUT,
  },
}

afterEach(() => {
  vi.unstubAllGlobals()
})

describe('SortTraceView (REQ-16, AC-12, AC-25)', () => {
  it('no pide la traza hasta elegir un algoritmo', () => {
    const fetchMock = mockTraceApi({ counting: COUNTING_88 })
    renderWithQuery(<SortTraceView />)

    expect(screen.getByText('Elige un algoritmo para ver su traza sobre el corpus completo.')).toBeInTheDocument()
    expect(fetchMock).not.toHaveBeenCalled()
  })

  it('muestra los pasos de Counting Sort en orden y pagina el arreglo de 88 en 0–49 y 50–87', async () => {
    const user = userEvent.setup()
    const fetchMock = mockTraceApi({ counting: COUNTING_88 })
    renderWithQuery(<SortTraceView />)

    await user.click(screen.getByRole('button', { name: 'Counting Sort' }))
    await screen.findByRole('heading', { name: 'Paso 1. Entrada' })
    expect(fetchMock).toHaveBeenCalledWith('/api/ranking/trace/counting?bucketSize=5', expect.anything())

    expect(stepTitles()).toEqual([
      'Paso 1. Entrada',
      'Paso 2. Máximo',
      'Paso 3. Conteo',
      'Paso 4. Acumulado',
      'Paso 5. Colocaciones',
      'Paso 6. Salida',
      'Paso 7. Ranking',
    ])

    const input = screen.getByRole('region', { name: 'Entrada A' })
    expect(within(input).getByText('0–49')).toBeInTheDocument()
    expect(within(input).getByText('longitud 88')).toBeInTheDocument()
    expect(within(input).getByText('t87 (1)')).toBeInTheDocument()
    expect(within(input).queryByText('t37 (1)')).not.toBeInTheDocument()

    await user.click(within(input).getByRole('button', { name: 'Entrada A: página siguiente' }))
    expect(within(input).getByText('50–87')).toBeInTheDocument()
    expect(within(input).getByText('longitud 88')).toBeInTheDocument()
    expect(within(input).getByText('t00 (1)')).toBeInTheDocument()

    // El arreglo de conteo, de 2 elementos, se muestra completo sin paginar.
    const count = screen.getByRole('region', { name: 'Conteo C' })
    expect(within(count).queryByText(/longitud/)).not.toBeInTheDocument()
    expect(within(count).getByText('88')).toBeInTheDocument()
  })

  it('muestra los pasos de Radix Sort en orden, con una tarjeta por pasada', async () => {
    const user = userEvent.setup()
    mockTraceApi({ radix: RADIX })
    renderWithQuery(<SortTraceView />)

    await user.click(screen.getByRole('button', { name: 'Radix Sort' }))
    await screen.findByRole('heading', { name: 'Paso 1. Entrada' })

    expect(stepTitles()).toEqual(['Paso 1. Entrada', 'Paso 2. Máximo', 'Paso 3. Pasadas por dígito', 'Paso 4. Ranking'])
    const passes = screen.getAllByRole('heading', { level: 4, name: /^Pasada/ }).map((h) => h.textContent)
    expect(passes).toEqual(['Pasada 1 (exp = 1)', 'Pasada 2 (exp = 10)'])
  })

  it('muestra los pasos de Bucket Sort en orden y pide la traza con el bucketSize elegido', async () => {
    const user = userEvent.setup()
    const fetchMock = mockTraceApi({ bucket: BUCKET })
    renderWithQuery(<SortTraceView />)

    await user.clear(screen.getByLabelText('bucketSize de la traza'))
    await user.type(screen.getByLabelText('bucketSize de la traza'), '2')
    await user.click(screen.getByRole('button', { name: 'Aplicar' }))
    await user.click(screen.getByRole('button', { name: 'Bucket Sort' }))
    await screen.findByRole('heading', { name: 'Paso 1. Entrada' })
    expect(fetchMock).toHaveBeenCalledWith('/api/ranking/trace/bucket?bucketSize=2', expect.anything())

    expect(stepTitles()).toEqual([
      'Paso 1. Entrada',
      'Paso 2. Mínimo y máximo',
      'Paso 3. Número de cubetas',
      'Paso 4. Cubetas antes y después del insertion sort',
      'Paso 5. Concatenado',
      'Paso 6. Ranking',
    ])
    const bucket0 = screen.getByRole('region', { name: 'Cubeta 0 antes' })
    expect(within(bucket0).getByText('c (3)')).toBeInTheDocument()
    expect(within(bucket0).getByText('a (3)')).toBeInTheDocument()
    expect(screen.getByRole('region', { name: 'Cubeta 2 después' })).toHaveTextContent('b (8)')
  })
})
