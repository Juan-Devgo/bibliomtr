import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { useState } from 'react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { renderWithQuery } from '@/test/setup'
import { ARTICLES, STUB_DIRECTORY } from '@/test/similarityFixtures'
import {
  SIMILARITY_ALGORITHM_LABELS,
  type PairTraceResponse,
  type SimilarityAlgorithmSlug,
  type SimilarityTraceBySlug,
} from '@/types/similarity'
import SimilarityTraceView, { type TraceSelection } from './SimilarityTraceView'

/** Respuesta de la traza del par (#1, #2) para `algorithm`. */
function traceResponse<S extends SimilarityAlgorithmSlug>(
  algorithm: S,
  value: number,
  similarity: number,
  trace: SimilarityTraceBySlug[S],
): PairTraceResponse {
  return {
    algorithm,
    label: SIMILARITY_ALGORITHM_LABELS[algorithm],
    articleA: ARTICLES[0],
    articleB: ARTICLES[1],
    editGranularity: 'WORDS',
    status: 'CALCULADO',
    value,
    similarity,
    trace,
    elapsedMs: 0.042,
    pendingLocation: null,
  } as PairTraceResponse
}

// Caso a mano de AC-7: g,a,t,o frente a p,a,t,o → D[4][4] = 1 y sim = 1 − 1/4 = 0.75.
const LEVENSHTEIN_GATO_PATO = traceResponse('levenshtein', 1, 0.75, {
  a: ['g', 'a', 't', 'o'],
  b: ['p', 'a', 't', 'o'],
  matrix: [
    [0, 1, 2, 3, 4],
    [1, 1, 2, 3, 4],
    [2, 2, 1, 2, 3],
    [3, 3, 2, 1, 2],
    [4, 4, 3, 2, 1],
  ],
  distance: 1,
  maxLength: 4,
})

const LEVENSHTEIN_PENDING: PairTraceResponse = {
  ...LEVENSHTEIN_GATO_PATO,
  status: 'PENDIENTE',
  value: null,
  similarity: null,
  trace: null,
  elapsedMs: null,
  pendingLocation: `${STUB_DIRECTORY}Levenshtein.java#compare`,
} as PairTraceResponse

// Mismo caso con (1, −1, −1): score 2, maxScore 4, minScore −4 y sim 0.75.
const NEEDLEMAN_WUNSCH_GATO_PATO = traceResponse('needleman-wunsch', 2, 0.75, {
  a: ['g', 'a', 't', 'o'],
  b: ['p', 'a', 't', 'o'],
  scoring: { match: 1, mismatch: -1, gap: -1 },
  scores: [
    [0, -1, -2, -3, -4],
    [-1, -1, -2, -3, -4],
    [-2, -2, 0, -1, -2],
    [-3, -3, -1, 1, 0],
    [-4, -4, -2, 0, 2],
  ],
  moves: [
    [null, 'LEFT', 'LEFT', 'LEFT', 'LEFT'],
    ['UP', 'DIAG', 'DIAG', 'DIAG', 'DIAG'],
    ['UP', 'DIAG', 'DIAG', 'LEFT', 'LEFT'],
    ['UP', 'DIAG', 'UP', 'DIAG', 'LEFT'],
    ['UP', 'DIAG', 'UP', 'UP', 'DIAG'],
  ],
  alignment: [
    { fromA: 'g', fromB: 'p' },
    { fromA: 'a', fromB: 'a' },
    { fromA: 't', fromB: 't' },
    { fromA: 'o', fromB: 'o' },
  ],
  score: 2,
  minScore: -4,
  maxScore: 4,
})

const JACCARD = traceResponse('jaccard', 0.5, 0.5, {
  setA: ['a', 'b', 'c'],
  setB: ['b', 'c', 'd'],
  intersection: ['b', 'c'],
  union: ['a', 'b', 'c', 'd'],
})

const TFIDF = traceResponse('tfidf-cosine', 0.24483, 0.24483, {
  vocabulary: ['a', 'b', 'c'],
  documentCount: 3,
  documentFrequency: [1, 2, 2],
  idf: [1.098612, 0.405465, 0.405465],
  tfA: [0.5, 0.5, 0],
  tfB: [0, 0.5, 0.5],
  weightsA: [0.549306, 0.202733, 0],
  weightsB: [0, 0.202733, 0.202733],
  dotProduct: 0.041101,
  normA: 0.585523,
  normB: 0.286707,
})

const COSINE = traceResponse('specter2-cosine', 0.707107, 0.853553, {
  dimension: 2,
  products: [1, 0],
  dotProduct: 1,
  normA: 1,
  normB: 1.414214,
})

const EUCLIDEAN = traceResponse('nomic-euclidean', 5, 1 / 6, {
  dimension: 2,
  squaredDifferences: [9, 16],
  sumOfSquares: 25,
})

/** API simulada: responde la traza según el parámetro `algorithm`. */
function mockTraceApi(responses: Partial<Record<SimilarityAlgorithmSlug, PairTraceResponse>>) {
  const fetchMock = vi.fn(async (input: RequestInfo | URL) => {
    const url = new URL(String(input), 'http://localhost')
    const response = responses[url.searchParams.get('algorithm') as SimilarityAlgorithmSlug]
    return response ? Response.json(response) : new Response(null, { status: 404 })
  })
  vi.stubGlobal('fetch', fetchMock)
  return fetchMock
}

/** El visor con su selección en estado local, como en `SimilarityPage`. */
function TraceViewWithState({ initial }: { initial: TraceSelection }) {
  const [selection, setSelection] = useState(initial)
  return <SimilarityTraceView articles={ARTICLES} value={selection} onChange={setSelection} />
}

/** Títulos de los pasos de la traza, en el orden en que aparecen. */
function stepTitles(): string[] {
  return screen.getAllByRole('heading', { level: 3 }).map((heading) => heading.textContent ?? '')
}

/** Fuente LaTeX de la fórmula KaTeX de un paso. */
function formulaOf(step: string): string {
  const item = screen.getByRole('listitem', { name: step })
  return item.querySelector('annotation[encoding="application/x-tex"]')?.textContent ?? ''
}

afterEach(() => {
  vi.unstubAllGlobals()
})

describe('SimilarityTraceView (REQ-16, REQ-17, REQ-21, AC-15, AC-18)', () => {
  it('no pide la traza hasta elegir el algoritmo y los dos artículos', async () => {
    const user = userEvent.setup()
    const fetchMock = mockTraceApi({ levenshtein: LEVENSHTEIN_GATO_PATO })
    renderWithQuery(<TraceViewWithState initial={{ algorithm: null, a: null, b: null }} />)

    expect(screen.getByText('Elige un algoritmo y dos artículos, o haz clic en una celda de un mapa de calor.')).toBeInTheDocument()
    await user.selectOptions(screen.getByLabelText('Algoritmo'), 'levenshtein')
    await user.selectOptions(screen.getByLabelText('Artículo a'), '1')
    expect(fetchMock).not.toHaveBeenCalled()

    await user.selectOptions(screen.getByLabelText('Artículo b'), '2')
    await screen.findByRole('heading', { name: 'Paso 1. Secuencias' })
    expect(fetchMock).toHaveBeenCalledWith('/api/similarity/trace?algorithm=levenshtein&a=1&b=2', expect.anything())
  })

  it('muestra la traza de Levenshtein para gato/pato con los pasos en orden y la fórmula en KaTeX', async () => {
    mockTraceApi({ levenshtein: LEVENSHTEIN_GATO_PATO })
    const { container } = renderWithQuery(<TraceViewWithState initial={{ algorithm: 'levenshtein', a: 1, b: 2 }} />)

    await screen.findByRole('heading', { name: 'Paso 1. Secuencias' })
    expect(stepTitles()).toEqual([
      'Paso 1. Secuencias',
      'Paso 2. Matriz de distancias D',
      'Paso 3. Distancia',
      'Paso 4. Similitud',
    ])
    expect(screen.getByText('Levenshtein · #1 frente a #2')).toBeInTheDocument()

    // Secuencias g,a,t,o y p,a,t,o.
    expect(screen.getByRole('region', { name: 'Secuencia a' })).toHaveTextContent('0:g1:a2:t3:o')
    expect(screen.getByRole('region', { name: 'Secuencia b' })).toHaveTextContent('0:p1:a2:t3:o')

    // Matriz D de 5×5 con encabezados ε, g, a, t, o y D[4][4] = 1.
    const matrix = screen.getByRole('region', { name: 'Matriz D' })
    expect(within(matrix).getByText('filas 0–4, columnas 0–4 de 5×5')).toBeInTheDocument()
    expect(within(matrix).getAllByRole('cell')).toHaveLength(25)
    expect(within(matrix).getAllByRole('rowheader').map((th) => th.textContent)).toEqual(['ε', 'g', 'a', 't', 'o'])
    expect(within(matrix).getAllByRole('row')[5]).toHaveTextContent('o44321')

    // Fórmulas dibujadas por KaTeX, con la fuente LaTeX de cada paso.
    expect(container.querySelectorAll('.katex').length).toBeGreaterThanOrEqual(4)
    expect(formulaOf('Matriz de distancias D')).toContain(String.raw`D_{i,j} = \min`)
    expect(formulaOf('Distancia')).toContain('d = D_{m,n} = 1')
    expect(formulaOf('Similitud')).toContain(String.raw`1 - \frac{1}{4} = 0.750000`)
  })

  it('muestra "Pendiente de implementar: <archivo>#<método>" si el algoritmo está PENDIENTE', async () => {
    mockTraceApi({ levenshtein: LEVENSHTEIN_PENDING })
    renderWithQuery(<TraceViewWithState initial={{ algorithm: 'levenshtein', a: 1, b: 2 }} />)

    expect(
      await screen.findByText(
        'Pendiente de implementar: src/main/java/co/edu/uniquindio/bibliomtr/algorithms/similarity/Levenshtein.java#compare',
      ),
    ).toBeInTheDocument()
    expect(screen.queryAllByRole('heading', { level: 3 })).toHaveLength(0)
  })

  it.each([
    [
      'needleman-wunsch',
      NEEDLEMAN_WUNSCH_GATO_PATO,
      [
        'Paso 1. Secuencias y puntajes',
        'Paso 2. Matriz de puntajes F',
        'Paso 3. Movimientos',
        'Paso 4. Alineamiento',
        'Paso 5. Puntaje',
        'Paso 6. Puntajes mínimo y máximo',
        'Paso 7. Similitud',
      ],
    ],
    ['jaccard', JACCARD, ['Paso 1. Conjuntos', 'Paso 2. Intersección', 'Paso 3. Unión', 'Paso 4. Índice de Jaccard']],
    [
      'tfidf-cosine',
      TFIDF,
      [
        'Paso 1. Vocabulario',
        'Paso 2. Frecuencia de documento e IDF',
        'Paso 3. Frecuencia de término',
        'Paso 4. Pesos',
        'Paso 5. Producto punto y normas',
        'Paso 6. Coseno',
      ],
    ],
    [
      'specter2-cosine',
      COSINE,
      ['Paso 1. Productos por componente', 'Paso 2. Producto punto', 'Paso 3. Normas', 'Paso 4. Coseno', 'Paso 5. Similitud'],
    ],
    [
      'nomic-euclidean',
      EUCLIDEAN,
      ['Paso 1. Diferencias al cuadrado', 'Paso 2. Suma', 'Paso 3. Distancia', 'Paso 4. Similitud'],
    ],
  ] as const)('muestra los pasos de %s en orden', async (algorithm, response, titles) => {
    mockTraceApi({ [algorithm]: response })
    renderWithQuery(<TraceViewWithState initial={{ algorithm, a: 1, b: 2 }} />)

    await screen.findByRole('heading', { name: titles[0] })
    expect(stepTitles()).toEqual(titles)
  })

  it('muestra los valores intermedios de Needleman-Wunsch para gato/pato', async () => {
    mockTraceApi({ 'needleman-wunsch': NEEDLEMAN_WUNSCH_GATO_PATO })
    renderWithQuery(<TraceViewWithState initial={{ algorithm: 'needleman-wunsch', a: 1, b: 2 }} />)

    await screen.findByRole('heading', { name: 'Paso 7. Similitud' })
    expect(screen.getByRole('region', { name: 'Alineamiento a / b' })).toHaveTextContent('0:g / p1:a / a2:t / t3:o / o')
    expect(within(screen.getByRole('region', { name: 'Movimientos' })).getAllByRole('row')[1]).toHaveTextContent('ε·←←←←')
    expect(formulaOf('Puntajes mínimo y máximo')).toContain('= -4')
    expect(formulaOf('Similitud')).toContain(String.raw`\frac{2 - (-4)}{4 - (-4)} = 0.750000`)
  })
})
