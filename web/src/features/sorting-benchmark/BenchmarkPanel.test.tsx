import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { renderWithQuery } from '@/test/setup'
import { benchmarkRun, jsonResponse, mockBenchmarkApi } from '@/test/benchmarkFixtures'
import type { BenchmarkRun } from '@/types/benchmark'
import BenchmarkPanel from './BenchmarkPanel'

const LAUNCH = 'Lanzar medición JMH'

afterEach(() => {
  vi.unstubAllGlobals()
})

/** Filas de la tabla de resultados, sin el encabezado, como texto de cada celda. */
function resultRows(): (string | null)[][] {
  const table = screen.getByRole('table', { name: 'Resultados JMH por algoritmo' })
  return within(table)
    .getAllByRole('row')
    .slice(1)
    .map((row) => within(row).getAllByRole('cell').map((cell) => cell.textContent))
}

/** Filas esperadas de los resultados de `benchmarkFixtures` (n = 88). */
const EXPECTED_ROWS = [
  ['Counting Sort', '0.0123 ± 0.0004 ms', '88', 'k = 42', 'O(n+k)'],
  ['Radix Sort', '0.0456 ± 0.0012 ms', '88', 'd = 2, b = 10', 'O(d·(n+b))'],
  ['Bucket Sort', '0.0789 ± 0.0021 ms', '88', 'm = 9', 'O(n+m) promedio, O(n²) peor caso'],
]

describe('BenchmarkPanel (REQ-20, REQ-21, REQ-22, REQ-23)', () => {
  it('sin mediciones COMPLETADAS muestra "Sin mediciones JMH" y el botón habilitado (AC-16)', async () => {
    mockBenchmarkApi(() => [])
    renderWithQuery(<BenchmarkPanel />)

    expect(await screen.findByText('Sin mediciones JMH')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: LAUNCH })).toBeEnabled()
    expect(screen.queryByRole('table')).not.toBeInTheDocument()
  })

  it('deshabilita el botón mientras hay una medición EN_CURSO (AC-13)', async () => {
    mockBenchmarkApi(() => [benchmarkRun(1, 'EN_CURSO', '2026-10-23T15:00:00Z')])
    renderWithQuery(<BenchmarkPanel />)

    expect(await screen.findByText('EN_CURSO')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: LAUNCH })).toBeDisabled()
  })

  it(
    'al lanzar pasa a EN_CURSO con el botón deshabilitado y al completar muestra ms ± error, n, parámetros y complejidad (AC-13)',
    async () => {
      // El historial cambia según avanza la medición: vacío, EN_CURSO tras el POST y luego COMPLETADA.
      let history: BenchmarkRun[] = []
      const fetchMock = mockBenchmarkApi(
        () => history,
        () => {
          history = [benchmarkRun(1, 'EN_CURSO', '2026-10-23T15:00:00Z')]
          return jsonResponse(history[0], 202)
        },
      )
      renderWithQuery(<BenchmarkPanel />)
      const user = userEvent.setup()

      await screen.findByText('Sin mediciones JMH')
      await user.click(screen.getByRole('button', { name: LAUNCH }))

      // Tras el POST se invalida ['benchmarks'] y el GET trae la medición EN_CURSO.
      expect(await screen.findByText('EN_CURSO')).toBeInTheDocument()
      expect(screen.getByRole('button', { name: LAUNCH })).toBeDisabled()
      expect(fetchMock).toHaveBeenCalledWith('/api/benchmarks', expect.objectContaining({ method: 'POST' }))

      // El refresco cada 2 s trae la medición ya COMPLETADA.
      history = [benchmarkRun(1, 'COMPLETADA', '2026-10-23T15:00:00Z')]
      expect(await screen.findByText('COMPLETADA', {}, { timeout: 4000 })).toBeInTheDocument()

      expect(resultRows()).toEqual(EXPECTED_ROWS)
      expect(screen.getByRole('button', { name: LAUNCH })).toBeEnabled()
      expect(screen.queryByText('Sin mediciones JMH')).not.toBeInTheDocument()
    },
    10_000,
  )

  it('una medición FALLIDA muestra su mensaje y la última COMPLETADA sigue visible (AC-15)', async () => {
    const message = 'La medición superó el límite de 10 minutos'
    mockBenchmarkApi(() => [
      benchmarkRun(2, 'FALLIDA', '2026-10-23T16:00:00Z', message),
      benchmarkRun(1, 'COMPLETADA', '2026-10-23T15:00:00Z'),
    ])
    renderWithQuery(<BenchmarkPanel />)

    expect(await screen.findByRole('alert')).toHaveTextContent(message)
    expect(screen.getByText('FALLIDA')).toBeInTheDocument()
    expect(screen.getByRole('region', { name: 'Última medición COMPLETADA' })).toBeInTheDocument()
    expect(resultRows()).toEqual(EXPECTED_ROWS)
    expect(screen.getByRole('button', { name: LAUNCH })).toBeEnabled()
  })

  it('muestra en un Alert el 409 "Ya hay una medición en curso" (REQ-19)', async () => {
    const detail = 'Ya hay una medición en curso'
    mockBenchmarkApi(
      () => [],
      () => jsonResponse({ status: 409, title: 'Conflict', detail }, 409),
    )
    renderWithQuery(<BenchmarkPanel />)
    const user = userEvent.setup()

    await user.click(await screen.findByRole('button', { name: LAUNCH }))

    expect(await screen.findByText(detail)).toBeInTheDocument()
  })
})
