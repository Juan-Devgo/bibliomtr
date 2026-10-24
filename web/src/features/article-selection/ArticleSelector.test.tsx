import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { useComparison } from '@/api/useComparison'
import { renderWithQuery } from '@/test/setup'
import type { ArticleSummary } from '@/types/similarity'
import ArticleSelector from './ArticleSelector'

const ARTICLES: ArticleSummary[] = [
  { id: 1, title: 'Generative AI in education', authors: ['Ana Gómez'], fileName: '1. a.pdf', specter2Truncated: false },
  { id: 2, title: 'Learning analytics review', authors: ['Luis Pérez'], fileName: '2. b.pdf', specter2Truncated: false },
  { id: 3, title: 'Chatbots for tutoring', authors: [], fileName: '3. c.pdf', specter2Truncated: true },
]

/** Mensaje 400 del backend cuando un id deja de ser seleccionable entre la carga y la comparación. */
const NOT_SELECTABLE = 'Artículos inexistentes o no seleccionables: [2]'

/** API simulada: `GET /api/articles` devuelve la lista y el `POST` de la comparación responde 400. */
function mockApi() {
  const fetchMock = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
    if (String(input) === '/api/articles') {
      return Response.json(ARTICLES)
    }
    if (String(input) === '/api/similarity/comparisons' && init?.method === 'POST') {
      return Response.json({ status: 400, title: 'Bad Request', detail: NOT_SELECTABLE }, { status: 400 })
    }
    return new Response(null, { status: 404 })
  })
  vi.stubGlobal('fetch', fetchMock)
  return fetchMock
}

/** El selector conectado a `useComparison`, como en `SimilarityPage`. */
function SelectorWithComparison() {
  const comparison = useComparison()
  return (
    <ArticleSelector
      onCompare={(ids) => comparison.mutate(ids)}
      comparing={comparison.isPending}
      error={comparison.error}
    />
  )
}

afterEach(() => {
  vi.unstubAllGlobals()
})

describe('ArticleSelector (REQ-13, REQ-15, REQ-17, AC-14)', () => {
  it('deshabilita "Comparar" con 1 artículo y lo habilita con 2', async () => {
    const user = userEvent.setup()
    mockApi()
    const onCompare = vi.fn()
    renderWithQuery(<ArticleSelector onCompare={onCompare} />)

    const first = await screen.findByRole('checkbox', { name: '#1 Generative AI in education' })
    const compare = screen.getByRole('button', { name: 'Comparar' })
    expect(compare).toBeDisabled()

    await user.click(first)
    expect(first).toBeChecked()
    expect(compare).toBeDisabled()
    expect(screen.getByText('1 seleccionados')).toBeInTheDocument()

    await user.click(screen.getByRole('checkbox', { name: '#3 Chatbots for tutoring' }))
    expect(compare).toBeEnabled()
    expect(screen.getByText('2 seleccionados')).toBeInTheDocument()

    await user.click(compare)
    // Ids en el orden de la lista, que es el orden de las matrices.
    expect(onCompare).toHaveBeenCalledWith([1, 3])
  })

  it('muestra en un Alert el mensaje 400 en español del backend', async () => {
    const user = userEvent.setup()
    const fetchMock = mockApi()
    renderWithQuery(<SelectorWithComparison />)

    await user.click(await screen.findByRole('checkbox', { name: '#1 Generative AI in education' }))
    await user.click(screen.getByRole('checkbox', { name: '#2 Learning analytics review' }))
    await user.click(screen.getByRole('button', { name: 'Comparar' }))

    expect(await screen.findByRole('alert')).toHaveTextContent(NOT_SELECTABLE)
    const [, init] = fetchMock.mock.calls.find(([url]) => String(url) === '/api/similarity/comparisons') ?? []
    expect(JSON.parse(String(init?.body))).toEqual({ articleIds: [1, 2] })
  })
})
