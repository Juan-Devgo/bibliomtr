import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { axisMaxHeight, heightTicks } from '@/components/dendrogramScale'
import {
  calculatedDendrogram,
  CLUSTERING_ARTICLES,
  CLUSTERING_D4,
  CLUSTERING_STUB_DIRECTORY,
  pendingDendrogram,
} from '@/test/clusteringFixtures'
import { renderWithQuery } from '@/test/setup'
import { CLUSTERING_ALGORITHM_LABELS, type ClusteringResponse } from '@/types/clustering'
import ClusteringSection from './ClusteringSection'

/** Cuerpo de `POST /api/clustering`. */
interface ClusteringBody {
  articleIds: number[]
  source: string
}

/** API simulada: responde el clustering según la fuente del cuerpo, o un ProblemDetail con `status`. */
function mockClusteringApi(responses: Record<string, ClusteringResponse | { status: number; detail: string }>) {
  const fetchMock = vi.fn(async (_input: RequestInfo | URL, init?: RequestInit) => {
    const body = JSON.parse(String(init?.body)) as ClusteringBody
    const response = responses[body.source]
    if (!response) {
      return new Response(null, { status: 404 })
    }
    if ('detail' in response) {
      return Response.json({ detail: response.detail }, { status: response.status })
    }
    return Response.json(response)
  })
  vi.stubGlobal('fetch', fetchMock)
  return fetchMock
}

/** Cuerpos enviados a `POST /api/clustering`, en orden. */
function sentBodies(fetchMock: ReturnType<typeof mockClusteringApi>): ClusteringBody[] {
  return fetchMock.mock.calls
    .filter(([url]) => String(url) === '/api/clustering')
    .map(([, init]) => JSON.parse(String(init?.body)) as ClusteringBody)
}

afterEach(() => {
  vi.unstubAllGlobals()
})

describe('ClusteringSection (REQ-13, REQ-14, REQ-18, REQ-19, REQ-22, AC-13, AC-14, AC-16, AC-17, AC-19)', () => {
  it('agrupa los artículos de la comparación con SPECTER2 (coseno) y dibuja los 4 dendrogramas', async () => {
    const fetchMock = mockClusteringApi({ 'specter2-cosine': CLUSTERING_D4 })
    renderWithQuery(<ClusteringSection articles={CLUSTERING_ARTICLES} />)

    await screen.findByText('Preparación: 2.500 ms')
    expect(sentBodies(fetchMock)).toEqual([{ articleIds: [11, 12, 13, 14], source: 'specter2-cosine' }])
    expect(screen.getByLabelText('Fuente de similitud')).toHaveValue('specter2-cosine')

    const svgs = screen.getAllByRole('img', { name: /^Dendrograma: / })
    expect(svgs.map((svg) => svg.getAttribute('aria-label'))).toEqual(
      Object.values(CLUSTERING_ALGORITHM_LABELS).map((label) => `Dendrograma: ${label}`),
    )
    const results = CLUSTERING_D4.results
    for (const [index, svg] of svgs.entries()) {
      const ticks = heightTicks(axisMaxHeight(results[index].merges ?? [])).join('')
      expect(within(svg).getByText('#11 Generative AI in education')).toBeInTheDocument()
      expect(within(svg).getByText('Large language models as teaching assistants in programming courses')).toBeInTheDocument()
      expect(within(svg).getByLabelText('Eje de altura')).toHaveTextContent(ticks)
    }
  })

  it('muestra el tiempo de preparación y el de cada algoritmo en ms, por separado', async () => {
    mockClusteringApi({ 'specter2-cosine': CLUSTERING_D4 })
    renderWithQuery(<ClusteringSection articles={CLUSTERING_ARTICLES} />)

    await screen.findByText('Preparación: 2.500 ms')
    expect(within(screen.getByRole('region', { name: 'Single Linkage' })).getByText('Ejecución: 0.250 ms')).toBeInTheDocument()
    expect(within(screen.getByRole('region', { name: 'DIANA (divisivo)' })).getByText('Ejecución: 1.000 ms')).toBeInTheDocument()
    expect(screen.getAllByText(/^Ejecución: \d+\.\d{3} ms$/)).toHaveLength(4)
  })

  it('al cambiar la fuente agrupa los mismos artículos con la nueva fuente', async () => {
    const user = userEvent.setup()
    const jaccard: ClusteringResponse = {
      ...CLUSTERING_D4,
      source: { source: 'jaccard', label: 'Jaccard', status: 'CALCULADO', pendingLocation: null },
      preparationMs: 7,
    }
    const fetchMock = mockClusteringApi({ 'specter2-cosine': CLUSTERING_D4, jaccard })
    renderWithQuery(<ClusteringSection articles={CLUSTERING_ARTICLES} />)
    await screen.findByText('Preparación: 2.500 ms')

    await user.selectOptions(screen.getByLabelText('Fuente de similitud'), 'jaccard')

    await screen.findByText('Preparación: 7.000 ms')
    expect(sentBodies(fetchMock)).toEqual([
      { articleIds: [11, 12, 13, 14], source: 'specter2-cosine' },
      { articleIds: [11, 12, 13, 14], source: 'jaccard' },
    ])
    expect(screen.getAllByRole('img', { name: /^Dendrograma: / })).toHaveLength(4)
  })

  it('muestra "Pendiente de implementar" para los algoritmos sin implementar y dibuja los demás', async () => {
    mockClusteringApi({
      'specter2-cosine': {
        ...CLUSTERING_D4,
        results: [
          pendingDendrogram('single-linkage', 'SingleLinkage'),
          pendingDendrogram('complete-linkage', 'CompleteLinkage'),
          calculatedDendrogram('average-linkage', 0.75),
          pendingDendrogram('diana', 'Diana'),
        ],
      },
    })
    renderWithQuery(<ClusteringSection articles={CLUSTERING_ARTICLES} />)

    await screen.findByText('Preparación: 2.500 ms')
    const single = screen.getByRole('region', { name: 'Single Linkage' })
    expect(
      within(single).getByText(
        'Pendiente de implementar: src/main/java/co/edu/uniquindio/bibliomtr/algorithms/clustering/SingleLinkage.java#cluster',
      ),
    ).toBeInTheDocument()
    expect(within(single).queryByRole('img')).not.toBeInTheDocument()
    expect(screen.getByText(`Pendiente de implementar: ${CLUSTERING_STUB_DIRECTORY}Diana.java#cluster`)).toBeInTheDocument()
    expect(screen.getAllByText(/^Pendiente de implementar: /)).toHaveLength(3)
    expect(screen.getAllByRole('img', { name: /^Dendrograma: / })).toHaveLength(1)
  })

  it('con la fuente pendiente muestra el aviso con su ubicación y ningún dendrograma', async () => {
    const location = 'src/main/java/co/edu/uniquindio/bibliomtr/algorithms/similarity/CosineSimilarity.java#compare'
    mockClusteringApi({
      'specter2-cosine': {
        ...CLUSTERING_D4,
        source: { source: 'specter2-cosine', label: 'SPECTER2 (coseno)', status: 'PENDIENTE', pendingLocation: location },
        preparationMs: null,
        distances: null,
        results: [],
      },
    })
    renderWithQuery(<ClusteringSection articles={CLUSTERING_ARTICLES} />)

    expect(
      await screen.findByText(`La fuente SPECTER2 (coseno) está pendiente de implementar: ${location}`),
    ).toBeInTheDocument()
    expect(screen.queryByRole('img')).not.toBeInTheDocument()
    expect(screen.queryByText(/^Preparación:/)).not.toBeInTheDocument()
  })

  it('muestra el mensaje en español de un error de la API', async () => {
    mockClusteringApi({
      'specter2-cosine': { status: 500, detail: 'El algoritmo diana devolvió un dendrograma inválido: el grupo 2 se fusiona dos veces' },
    })
    renderWithQuery(<ClusteringSection articles={CLUSTERING_ARTICLES} />)

    expect(
      await screen.findByText('El algoritmo diana devolvió un dendrograma inválido: el grupo 2 se fusiona dos veces'),
    ).toBeInTheDocument()
    expect(screen.queryByRole('img')).not.toBeInTheDocument()
  })
})
