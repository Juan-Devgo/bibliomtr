import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { useState } from 'react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import {
  CLUSTERING_ARTICLES,
  CLUSTERING_D4,
  CLUSTERING_STUB_DIRECTORY,
  DIANA_TRACE_D4,
  SINGLE_TRACE_D4,
  traceResponse,
} from '@/test/clusteringFixtures'
import { renderWithQuery } from '@/test/setup'
import type { ClusteringAlgorithmSlug, ClusteringTraceResponse, LinkageTrace } from '@/types/clustering'
import ClusteringSection from './ClusteringSection'
import ClusteringTraceView from './ClusteringTraceView'

/** Traza de Average Linkage sobre D₄: la misma estructura que Single con los valores promedio. */
const AVERAGE_TRACE_D4: LinkageTrace = {
  steps: [
    { ...SINGLE_TRACE_D4.steps[0], updatedRow: [0.4, 0.65] },
    {
      ...SINGLE_TRACE_D4.steps[1],
      distances: [
        [0, 0.45, 0.4],
        [0.45, 0, 0.65],
        [0.4, 0.65, 0],
      ],
      distance: 0.4,
      updatedRow: [1.75 / 3],
    },
    { ...SINGLE_TRACE_D4.steps[2], distance: 1.75 / 3 },
  ],
}

const RESPONSES: Partial<Record<ClusteringAlgorithmSlug, ClusteringTraceResponse>> = {
  'single-linkage': traceResponse('single-linkage', SINGLE_TRACE_D4),
  'average-linkage': traceResponse('average-linkage', AVERAGE_TRACE_D4),
  diana: traceResponse('diana', DIANA_TRACE_D4),
  'complete-linkage': {
    ...traceResponse('complete-linkage', SINGLE_TRACE_D4),
    status: 'PENDIENTE',
    merges: null,
    trace: null,
    elapsedMs: null,
    pendingLocation: `${CLUSTERING_STUB_DIRECTORY}CompleteLinkage.java#cluster`,
  } as ClusteringTraceResponse,
}

/** API simulada: el clustering para el POST y la traza según el parámetro `algorithm`. */
function mockApi() {
  const fetchMock = vi.fn(async (input: RequestInfo | URL) => {
    const url = new URL(String(input), 'http://localhost')
    if (url.pathname === '/api/clustering') {
      return Response.json(CLUSTERING_D4)
    }
    const response = RESPONSES[url.searchParams.get('algorithm') as ClusteringAlgorithmSlug]
    return response ? Response.json(response) : new Response(null, { status: 404 })
  })
  vi.stubGlobal('fetch', fetchMock)
  return fetchMock
}

/** El visor con el algoritmo en estado local, como en `ClusteringSection`. */
function TraceViewWithState({ initial }: { initial: ClusteringAlgorithmSlug | null }) {
  const [algorithm, setAlgorithm] = useState(initial)
  return (
    <ClusteringTraceView
      articles={CLUSTERING_ARTICLES}
      source="specter2-cosine"
      algorithm={algorithm}
      onAlgorithmChange={setAlgorithm}
    />
  )
}

/** Títulos de los pasos de la traza, en el orden en que aparecen. */
function stepTitles(): string[] {
  return screen.getAllByRole('listitem', { name: /./ }).map((item) => item.querySelector('h3')?.textContent ?? '')
}

/** Fuente LaTeX de la fórmula KaTeX de un paso. */
function formulaOf(step: string): string {
  const item = screen.getByRole('listitem', { name: step })
  return item.querySelector('annotation[encoding="application/x-tex"]')?.textContent ?? ''
}

afterEach(() => {
  vi.unstubAllGlobals()
})

describe('ClusteringTraceView (REQ-16, REQ-17, REQ-18, AC-15)', () => {
  it('no pide la traza hasta elegir el algoritmo y la pide con la fuente y los artículos', async () => {
    const user = userEvent.setup()
    const fetchMock = mockApi()
    renderWithQuery(<TraceViewWithState initial={null} />)

    expect(screen.getByText('Elige un algoritmo o pulsa "Ver traza" en su dendrograma.')).toBeInTheDocument()
    expect(fetchMock).not.toHaveBeenCalled()

    await user.selectOptions(screen.getByLabelText('Algoritmo de clustering'), 'single-linkage')

    await screen.findByText('Single Linkage · SPECTER2 (coseno)')
    expect(fetchMock).toHaveBeenCalledWith(
      '/api/clustering/trace?algorithm=single-linkage&source=specter2-cosine&articleIds=11%2C12%2C13%2C14',
      expect.anything(),
    )
  })

  it('muestra la traza de Single Linkage sobre D₄ con 3 fusiones en orden y la fórmula \\min en KaTeX', async () => {
    mockApi()
    const { container } = renderWithQuery(<TraceViewWithState initial="single-linkage" />)

    await screen.findByText('Single Linkage · SPECTER2 (coseno)')
    expect(stepTitles()).toEqual([
      'Paso 1. Fusión de G0 y G1 en G4',
      'Paso 2. Fusión de G2 y G4 en G5',
      'Paso 3. Fusión de G3 y G5 en G6',
    ])
    expect(screen.getByText('Ejecución: 0.125 ms · Preparación: 2.500 ms')).toBeInTheDocument()

    // Entrada: hojas y matriz D de 4×4.
    expect(screen.getByRole('region', { name: 'Hojas del dendrograma' })).toHaveTextContent('0 = #11 Generative AI in education')
    const input = screen.getByRole('region', { name: 'Matriz de distancias D' })
    expect(within(input).getAllByRole('cell')).toHaveLength(16)
    expect(within(input).getAllByRole('rowheader').map((th) => th.textContent)).toEqual(['#11', '#12', '#13', '#14'])

    // Paso 1: matriz entre grupos, par elegido y fila recalculada [0.3, 0.6].
    const step1 = screen.getByRole('listitem', { name: 'Fusión de G0 y G1 en G4' })
    const groups = within(step1).getByRole('region', { name: 'Distancias entre grupos, paso 1' })
    expect(within(groups).getAllByRole('rowheader').map((th) => th.textContent)).toEqual([
      'G0 {0}',
      'G1 {1}',
      'G2 {2}',
      'G3 {3}',
    ])
    expect(within(groups).getAllByRole('row')[3]).toHaveTextContent('G2 {2}0.5000.3000.0000.450')
    expect(within(step1).getByText('Par elegido: G0 y G1 · distancia 0.200 · nuevo grupo G4 {0, 1}')).toBeInTheDocument()
    const row = within(step1).getByRole('list', { name: 'Fila recalculada, paso 1' })
    expect(within(row).getAllByRole('listitem').map((li) => li.textContent)).toEqual([
      'd(G4, G2) = 0.300',
      'd(G4, G3) = 0.600',
    ])

    // Paso 2: los grupos que quedan conservan su orden y el nuevo va al final.
    const step2 = screen.getByRole('listitem', { name: 'Fusión de G2 y G4 en G5' })
    expect(
      within(within(step2).getByRole('region', { name: 'Distancias entre grupos, paso 2' }))
        .getAllByRole('rowheader')
        .map((th) => th.textContent),
    ).toEqual(['G2 {2}', 'G3 {3}', 'G4 {0, 1}'])

    // Último paso: no hay fila que recalcular.
    const step3 = screen.getByRole('listitem', { name: 'Fusión de G3 y G5 en G6' })
    expect(within(step3).getByText('No quedan grupos por recalcular: es la última fusión.')).toBeInTheDocument()

    expect(formulaOf('Fusión de G0 y G1 en G4')).toContain(String.raw`\min\big(d(i, h),\ d(j, h)\big)`)
    expect(container.querySelectorAll('.katex').length).toBeGreaterThanOrEqual(4)
  })

  it('muestra la fórmula de Average Linkage y su fila recalculada con el promedio ponderado', async () => {
    mockApi()
    renderWithQuery(<TraceViewWithState initial="average-linkage" />)

    await screen.findByText('Average Linkage (UPGMA) · SPECTER2 (coseno)')
    expect(formulaOf('Fusión de G0 y G1 en G4')).toContain(String.raw`\frac{|i|\, d(i, h) + |j|\, d(j, h)}{|i| + |j|}`)
    const step2 = screen.getByRole('listitem', { name: 'Fusión de G2 y G4 en G5' })
    expect(within(step2).getByText('d(G5, G3) = 0.583')).toBeInTheDocument()
  })

  it('muestra las 3 divisiones de DIANA con diámetros, iteraciones y partes', async () => {
    mockApi()
    renderWithQuery(<TraceViewWithState initial="diana" />)

    await screen.findByText('DIANA (divisivo) · SPECTER2 (coseno)')
    expect(stepTitles()).toEqual([
      'Paso 1. División de {0, 1, 2, 3} con diámetro 0.700',
      'Paso 2. División de {0, 1, 2} con diámetro 0.500',
      'Paso 3. División de {0, 1} con diámetro 0.200',
    ])

    const step2 = screen.getByRole('listitem', { name: 'División de {0, 1, 2} con diámetro 0.500' })
    const groups = within(step2).getByRole('table', { name: 'Grupos y diámetros, paso 2' })
    expect(within(groups).getAllByRole('rowheader').map((th) => th.textContent)).toEqual([
      '{0, 1, 2} (se divide)',
      '{3}',
    ])
    const first = within(step2).getByRole('region', { name: 'Paso 2, iteración 1' })
    expect(within(first).getByText('Paso 2, iteración 1: S = {}, R = {0, 1, 2}')).toBeInTheDocument()
    expect(within(first).getAllByRole('row')[3]).toHaveTextContent('20.4000.0000.400')
    expect(within(first).getByText('Se mueve 2 a S.')).toBeInTheDocument()
    const second = within(step2).getByRole('region', { name: 'Paso 2, iteración 2' })
    expect(within(second).getAllByRole('row')[1]).toHaveTextContent('00.2000.500-0.300')
    expect(within(second).getByText('Fin: ningún D(i) es positivo.')).toBeInTheDocument()
    expect(within(step2).getByText('Partes: S = {2} y C \\ S = {0, 1}')).toBeInTheDocument()

    expect(formulaOf('División de {0, 1, 2, 3} con diámetro 0.700')).toContain(String.raw`\operatorname{diam}(C)`)
  })

  it('muestra "Pendiente de implementar" para un algoritmo sin implementar, sin pasos', async () => {
    mockApi()
    renderWithQuery(<TraceViewWithState initial="complete-linkage" />)

    expect(
      await screen.findByText(`Pendiente de implementar: ${CLUSTERING_STUB_DIRECTORY}CompleteLinkage.java#cluster`),
    ).toBeInTheDocument()
    expect(screen.queryByRole('listitem', { name: /./ })).not.toBeInTheDocument()
  })
})

describe('ClusteringSection con el visor de traza (REQ-16, REQ-18)', () => {
  it('"Ver traza" en una tarjeta elige ese algoritmo; cambiar la fuente lo limpia', async () => {
    const user = userEvent.setup()
    mockApi()
    renderWithQuery(<ClusteringSection articles={CLUSTERING_ARTICLES} />)

    await user.click(await screen.findByRole('button', { name: 'Ver traza: DIANA (divisivo)' }))

    await screen.findByText('DIANA (divisivo) · SPECTER2 (coseno)')
    expect(screen.getByLabelText('Algoritmo de clustering')).toHaveValue('diana')

    await user.selectOptions(screen.getByLabelText('Fuente de similitud'), 'jaccard')
    expect(await screen.findByLabelText('Algoritmo de clustering')).toHaveValue('')
  })
})
