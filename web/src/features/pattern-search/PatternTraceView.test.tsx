import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'
import {
  ALL_IMPLEMENTED,
  BOYER_MOORE_TRACE_ABAB,
  KMP_TRACE_ABAB,
  PENDING_BOYER_MOORE_TRACE,
  RABIN_KARP_COLLISION_TRACE,
  RABIN_KARP_TRACE_ABAB,
  STRING_MATCHING_STUB_DIRECTORY,
} from '@/test/patternSearchFixtures'
import { renderWithQuery } from '@/test/setup'
import type { PatternTraceResponse, StringMatchingSlug } from '@/types/patternSearch'
import PatternSearchView from './PatternSearchView'
import PatternTraceView from './PatternTraceView'

/** API simulada: la búsqueda responde `ALL_IMPLEMENTED` y la traza, la de `traces` según `algorithm`. */
function mockApi(traces: Partial<Record<StringMatchingSlug, PatternTraceResponse | { status: number; detail: string }>>) {
  const fetchMock = vi.fn(async (input: RequestInfo | URL, _init?: RequestInit) => {
    const url = new URL(String(input), 'http://localhost')
    if (url.pathname === '/api/pattern-search') {
      return Response.json(ALL_IMPLEMENTED)
    }
    const body = traces[url.searchParams.get('algorithm') as StringMatchingSlug]
    if (!body) {
      return new Response(null, { status: 404 })
    }
    return Response.json(body, { status: 'detail' in body ? body.status : 200 })
  })
  vi.stubGlobal('fetch', fetchMock)
  return fetchMock
}

/** Parámetros de las consultas a `GET /api/pattern-search/trace`, en orden. */
function traceQueries(fetchMock: ReturnType<typeof mockApi>): URLSearchParams[] {
  return fetchMock.mock.calls
    .map((call) => new URL(String(call[0]), 'http://localhost'))
    .filter((url) => url.pathname === '/api/pattern-search/trace')
    .map((url) => url.searchParams)
}

/** Texto de las celdas de datos de cada fila (sin la fila de índices) de la tabla `label`. */
function tableRows(label: string): string[][] {
  const table = screen.getByRole('table', { name: label })
  return within(table)
    .getAllByRole('row')
    .slice(1)
    .map((row) => [
      within(row).getByRole('rowheader').textContent ?? '',
      ...within(row)
        .getAllByRole('cell')
        .map((cell) => cell.textContent ?? ''),
    ])
}

/** Fuente LaTeX de la fórmula KaTeX de un paso de la traza. */
function formulaOf(step: string): string {
  const item = screen.getByRole('listitem', { name: step })
  return item.querySelector('annotation[encoding="application/x-tex"]')?.textContent ?? ''
}

/** Tabla del texto con el patrón alineado en el paso actual. */
function alignedTable(): HTMLElement {
  return within(screen.getByRole('region', { name: 'Alineación del paso' })).getByRole('table')
}

afterEach(() => {
  vi.unstubAllGlobals()
})

describe('PatternTraceView (REQ-23, REQ-24, REQ-25, REQ-26, REQ-27, REQ-28)', () => {
  it('pide la traza con el algoritmo, el artículo, el patrón sin recortar y q', async () => {
    const fetchMock = mockApi({ kmp: KMP_TRACE_ABAB })
    renderWithQuery(<PatternTraceView algorithm="kmp" articleId={1} pattern=" abab" q="101" />)

    await screen.findByText(/^Knuth-Morris-Pratt \(KMP\) · #1/)
    const [query] = traceQueries(fetchMock)
    expect(query.get('algorithm')).toBe('kmp')
    expect(query.get('articleId')).toBe('1')
    expect(query.get('pattern')).toBe(' abab')
    expect(query.get('q')).toBe('101')
  })

  it('KMP: muestra π, avanza paso a paso con el patrón en el s de la traza y grafica s (AC-16)', async () => {
    const user = userEvent.setup()
    mockApi({ kmp: KMP_TRACE_ABAB })
    const { container } = renderWithQuery(<PatternTraceView algorithm="kmp" articleId={1} pattern="abab" q="101" />)

    await screen.findByText('Knuth-Morris-Pratt (KMP) · #1 Búsqueda exacta en cadenas · m = 4 · n = 8')
    expect(tableRows('Tabla π')).toEqual([
      ['P[i]', 'a', 'b', 'a', 'b'],
      ['π[i]', '0', '0', '1', '2'],
    ])
    const construction = screen.getByRole('list', { name: 'Construcción de π' })
    expect(within(construction).getAllByRole('listitem').map((item) => item.textContent)).toEqual([
      "i = 1, P[1] = 'b': k probados 0 → π[1] = 0",
      "i = 2, P[2] = 'a': k probados 0 → π[2] = 1",
      "i = 3, P[3] = 'b': k probados 1 → π[3] = 2",
    ])
    expect(formulaOf('Función de prefijo π')).toContain(String.raw`\pi[i] = \max`)
    expect(formulaOf('Búsqueda con π')).toContain(String.raw`j \leftarrow \pi[j - 1]`)

    // Paso 1: s = 0 y la comparación T[0] vs P[0] resaltada como coincidencia.
    expect(screen.getByText('Paso 1 de 9')).toBeInTheDocument()
    expect(alignedTable()).toHaveAccessibleName('Texto y patrón alineados en s = 0')
    expect(within(alignedTable()).getByRole('cell', { name: 'P[0] bajo T[0]' })).toHaveAttribute('data-state', 'coincide')

    // Paso 5: s = 2 de la traza; P[0] queda bajo T[2] y se compara T[4] con P[2].
    for (let step = 0; step < 4; step++) {
      await user.click(screen.getByRole('button', { name: 'Siguiente' }))
    }
    expect(screen.getByText('Paso 5 de 9')).toBeInTheDocument()
    expect(alignedTable()).toHaveAccessibleName('Texto y patrón alineados en s = 2')
    expect(within(alignedTable()).getByRole('cell', { name: 'P[0] bajo T[2]' })).toHaveTextContent('a')
    expect(within(alignedTable()).getByRole('cell', { name: 'P[2] bajo T[4]' })).toHaveAttribute('data-state', 'coincide')

    // Paso 7: desajuste de 'c' con P[2] y retroceso j ← π[1] = 0.
    await user.click(screen.getByRole('button', { name: 'Siguiente' }))
    await user.click(screen.getByRole('button', { name: 'Siguiente' }))
    expect(within(alignedTable()).getByRole('cell', { name: 'P[2] bajo T[6]' })).toHaveAttribute('data-state', 'desajuste')
    expect(
      screen.getByText("Comparación 7: T[6] = 'c' y P[2] = 'a' no coinciden (s = 4); retroceso j ← π[1] = 0"),
    ).toBeInTheDocument()
    expect(screen.getByText('Salto al paso siguiente: s = 6 (+2)')).toBeInTheDocument()

    // Último paso: s = 7; "Siguiente" y "Último" quedan deshabilitados. "Primero" vuelve a s = 0.
    await user.click(screen.getByRole('button', { name: 'Último' }))
    expect(screen.getByText('Paso 9 de 9')).toBeInTheDocument()
    expect(alignedTable()).toHaveAccessibleName('Texto y patrón alineados en s = 7')
    expect(screen.getByRole('button', { name: 'Siguiente' })).toBeDisabled()
    expect(screen.getByText('Último paso de la traza.')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Primero' }))
    expect(screen.getByText('Paso 1 de 9')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Anterior' })).toBeDisabled()

    const chart = screen.getByRole('figure', { name: 'Gráfico de s por comparación' })
    expect(chart).toHaveTextContent('Desplazamiento s = i − j de cada una de las 9 comparaciones.')
    expect(chart.querySelector('.recharts-surface')).not.toBeNull()
    expect(screen.getByText('Ocurrencias: 0, 2')).toBeInTheDocument()
    expect(container.querySelectorAll('.katex').length).toBeGreaterThanOrEqual(2)
  })

  it('Boyer-Moore: muestra las tablas, la comparación de derecha a izquierda y el salto de cada regla (AC-17)', async () => {
    const user = userEvent.setup()
    mockApi({ 'boyer-moore': BOYER_MOORE_TRACE_ABAB })
    renderWithQuery(<PatternTraceView algorithm="boyer-moore" articleId={1} pattern="abab" q="101" />)

    await screen.findByText(/^Boyer-Moore · #1/)
    const badCharacter = screen.getByRole('table', { name: 'Tabla del carácter malo' })
    expect(within(badCharacter).getAllByRole('columnheader').map((th) => th.textContent)).toEqual(['a', 'b'])
    expect(tableRows('Tabla del carácter malo')).toEqual([['last(c)', '2', '3']])
    expect(tableRows('Tabla del sufijo bueno')).toEqual([
      ['P[j]', 'a', 'b', 'a', 'b'],
      ['salto_sb(j)', '2', '2', '4', '1'],
    ])
    expect(tableRows('Arreglo de bordes f')).toEqual([['f[i]', '2', '3', '4', '4', '5']])
    expect(screen.getByText('borde(P) = 2 · salto tras ocurrencia m − borde(P) = 4 − 2 = 2')).toBeInTheDocument()
    expect(formulaOf('Regla del carácter malo')).toContain(String.raw`\operatorname{last}(c)`)
    expect(formulaOf('Alineaciones')).toContain(String.raw`\max\big(\text{salto}_{cm}(j)`)

    // Alineación 1: ocurrencia en s = 0, comparada de derecha a izquierda.
    expect(screen.getByText('Paso 1 de 3')).toBeInTheDocument()
    const comparisons = screen.getByRole('list', { name: 'Comparaciones de derecha a izquierda' })
    expect(within(comparisons).getAllByRole('listitem').map((item) => item.textContent)).toEqual([
      "j = 3: T[3] = 'b' y P[3] = 'b' coinciden",
      "j = 2: T[2] = 'a' y P[2] = 'a' coinciden",
      "j = 1: T[1] = 'b' y P[1] = 'b' coinciden",
      "j = 0: T[0] = 'a' y P[0] = 'a' coinciden",
    ])
    expect(screen.getByText('Ocurrencia completa')).toBeInTheDocument()
    expect(screen.getByText('Salto aplicado: 2')).toBeInTheDocument()

    // Alineación 3: desajuste en j = 3 con 'a'; carácter malo 1, sufijo bueno 1, salto 1.
    await user.click(screen.getByRole('button', { name: 'Último' }))
    expect(alignedTable()).toHaveAccessibleName('Texto y patrón alineados en s = 4')
    expect(within(alignedTable()).getByRole('cell', { name: 'P[3] bajo T[7]' })).toHaveAttribute('data-state', 'desajuste')
    expect(screen.getByText('Desajuste en j = 3')).toBeInTheDocument()
    expect(screen.getByText('Salto del carácter malo: 1')).toBeInTheDocument()
    expect(screen.getByText('Salto del sufijo bueno: 1')).toBeInTheDocument()
    expect(screen.getByText('Salto aplicado: 1')).toBeInTheDocument()
    expect(screen.getByText('Salto al paso siguiente: s = 5 (+1); fin: s > n − m')).toBeInTheDocument()

    const chart = screen.getByRole('figure', { name: 'Gráfico del salto aplicado por alineación' })
    expect(chart).toHaveTextContent('Salto aplicado en cada una de las 3 alineaciones.')
    expect(chart.querySelectorAll('.recharts-bar-rectangle')).toHaveLength(3)
  })

  it('Rabin-Karp: muestra d, q, h, h(P), las ventanas, el gráfico y la colisión en s = 2 listada (AC-18)', async () => {
    const user = userEvent.setup()
    mockApi({ 'rabin-karp': RABIN_KARP_COLLISION_TRACE })
    renderWithQuery(<PatternTraceView algorithm="rabin-karp" articleId={3} pattern="ab" q="3" />)

    await screen.findByText('Rabin-Karp · #3 Colisión de hash · m = 2 · n = 4 · q = 3')
    const parameters = screen.getByRole('list', { name: 'Parámetros de Rabin-Karp' })
    expect(within(parameters).getAllByRole('listitem').map((item) => item.textContent)).toEqual([
      'd = 65536',
      'q = 3',
      'h = d^(1) mod q = 1',
      'h(P) = 0',
      't(0) = 0',
    ])
    expect(formulaOf('Hash del patrón')).toContain(String.raw`h = d^{\,m-1} \bmod q`)
    expect(formulaOf('Ventanas con rolling hash')).toContain(String.raw`t(s+1)`)

    expect(screen.getByText('Paso 1 de 3')).toBeInTheDocument()
    expect(screen.getByText('Ventana s = 0: t(0) = 0 = h(P); verificación: coincide, ocurrencia')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Último' }))
    expect(alignedTable()).toHaveAccessibleName('Texto y patrón alineados en s = 2')
    expect(screen.getByText('Ventana s = 2: t(2) = 0 = h(P); verificación: no coincide, colisión')).toBeInTheDocument()
    expect(within(alignedTable()).getByRole('cell', { name: 'T[2]' })).toHaveAttribute('data-state', 'colision')

    const collisions = screen.getByRole('list', { name: 'Colisiones' })
    expect(within(collisions).getAllByRole('listitem').map((item) => item.textContent)).toEqual([
      's = 2: t(2) = h(P) = 0 con texto distinto',
    ])
    const chart = screen.getByRole('figure', { name: 'Gráfico del hash por ventana' })
    expect(chart).toHaveTextContent('Hash t(s) de cada ventana frente a h(P) = 0; colisiones en s = 2.')
    expect(chart.querySelector('.recharts-reference-line')).not.toBeNull()
    expect(chart.querySelectorAll('.recharts-scatter-symbol')).toHaveLength(1)
    expect(screen.getByText('Ocurrencias: 0')).toBeInTheDocument()
  })

  it('Rabin-Karp sin colisiones: 5 ventanas con los hashes de la traza (AC-18)', async () => {
    mockApi({ 'rabin-karp': RABIN_KARP_TRACE_ABAB })
    renderWithQuery(<PatternTraceView algorithm="rabin-karp" articleId={1} pattern="abab" q="101" />)

    await screen.findByText(/^Rabin-Karp · #1/)
    expect(screen.getByText('Paso 1 de 5')).toBeInTheDocument()
    expect(screen.getByText('Sin colisiones: toda ventana con el hash del patrón fue una ocurrencia.')).toBeInTheDocument()
    expect(screen.getByRole('figure', { name: 'Gráfico del hash por ventana' })).toHaveTextContent(
      'Hash t(s) de cada ventana frente a h(P) = 48; sin colisiones.',
    )
  })

  it('con un algoritmo PENDIENTE muestra "Pendiente de implementar" sin pasos (AC-26)', async () => {
    mockApi({ 'boyer-moore': PENDING_BOYER_MOORE_TRACE })
    renderWithQuery(<PatternTraceView algorithm="boyer-moore" articleId={1} pattern="abab" q="101" />)

    expect(
      await screen.findByText(`Pendiente de implementar: ${STRING_MATCHING_STUB_DIRECTORY}BoyerMoore.java#preprocess`),
    ).toBeInTheDocument()
    expect(screen.queryByRole('group')).not.toBeInTheDocument()
    expect(screen.queryByRole('figure')).not.toBeInTheDocument()
  })

  it('muestra tal cual el mensaje en español de un 400', async () => {
    const detail = 'El artículo 99 no existe o no tiene abstract'
    mockApi({ kmp: { status: 400, detail } })
    renderWithQuery(<PatternTraceView algorithm="kmp" articleId={99} pattern="abab" q="101" />)

    expect(await screen.findByText(detail)).toBeInTheDocument()
  })
})

describe('PatternSearchView con el visor de traza (REQ-22, REQ-26)', () => {
  it('al elegir un artículo pide la traza del algoritmo resaltado y la cambia con el selector', async () => {
    const user = userEvent.setup()
    const fetchMock = mockApi({
      kmp: KMP_TRACE_ABAB,
      'boyer-moore': BOYER_MOORE_TRACE_ABAB,
      'rabin-karp': RABIN_KARP_TRACE_ABAB,
    })
    renderWithQuery(<PatternSearchView />)

    await user.type(screen.getByLabelText('Patrón'), 'abab')
    await user.click(screen.getByRole('button', { name: 'Buscar' }))
    await screen.findByRole('region', { name: 'Knuth-Morris-Pratt (KMP)' })
    expect(traceQueries(fetchMock)).toHaveLength(0)

    const table = screen.getByRole('table', { name: 'Ocurrencias de Knuth-Morris-Pratt (KMP)' })
    await user.click(within(table).getAllByRole('row')[1])

    await screen.findByText(/^Knuth-Morris-Pratt \(KMP\) · #1/)
    const first = traceQueries(fetchMock)[0]
    expect(first.get('algorithm')).toBe('kmp')
    expect(first.get('articleId')).toBe('1')
    expect(first.get('pattern')).toBe('abab')
    expect(first.get('q')).toBe('101')

    await user.selectOptions(screen.getByLabelText('Algoritmo resaltado'), 'rabin-karp')

    await screen.findByText(/^Rabin-Karp · #1/)
    const queries = traceQueries(fetchMock)
    expect(queries[queries.length - 1].get('algorithm')).toBe('rabin-karp')
    expect(screen.getByRole('list', { name: 'Parámetros de Rabin-Karp' })).toHaveTextContent('h(P) = 48')
  })
})
