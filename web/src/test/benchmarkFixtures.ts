/**
 * Datos de prueba de las mediciones JMH (spec 002, REQ-22, REQ-27) y una API simulada de
 * `/api/benchmarks`, compartidos por los tests de `features/sorting-benchmark`.
 */
import { vi } from 'vitest'
import type { BenchmarkRun, BenchmarkStatus } from '@/types/benchmark'

/** Parámetros JMH fijos de REQ-18, tal como los envía el backend. */
export const FIXED_PARAMETERS: BenchmarkRun['parameters'] = {
  forks: 1,
  warmupIterations: 5,
  warmupSeconds: 1,
  measurementIterations: 10,
  measurementSeconds: 1,
  mode: 'avgt',
  timeUnit: 'ms',
  bucketSize: 5,
  traceEnabled: false,
}

/** Resultados de una medición COMPLETADA con n = 88, k = 42, d = 2, b = 10 y m = 9. */
export const COMPLETED_RESULTS: BenchmarkRun['results'] = [
  { algorithm: 'counting', scoreMs: 0.0123, scoreErrorMs: 0.0004, parameters: { k: 42, d: null, b: null, m: null }, complexity: 'O(n+k)' },
  { algorithm: 'radix', scoreMs: 0.0456, scoreErrorMs: 0.0012, parameters: { k: null, d: 2, b: 10, m: null }, complexity: 'O(d·(n+b))' },
  { algorithm: 'bucket', scoreMs: 0.0789, scoreErrorMs: 0.0021, parameters: { k: null, d: null, b: null, m: 9 }, complexity: 'O(n+m) promedio, O(n²) peor caso' },
]

/**
 * Crea una medición con el estado dado. Una COMPLETADA trae los tres resultados; una FALLIDA, `message`;
 * una EN_CURSO, `finishedAt = null`.
 */
export function benchmarkRun(id: number, status: BenchmarkStatus, startedAt: string, message: string | null = null): BenchmarkRun {
  return {
    id,
    startedAt,
    finishedAt: status === 'EN_CURSO' ? null : startedAt,
    status,
    message,
    parameters: FIXED_PARAMETERS,
    termCount: 88,
    results: status === 'COMPLETADA' ? COMPLETED_RESULTS : [],
  }
}

/** Respuesta JSON simulada. */
export function jsonResponse(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } })
}

/**
 * Reemplaza `fetch` por una API simulada de `/api/benchmarks`. Cada GET devuelve el historial que entrega
 * `history()` en ese momento (así el test puede cambiarlo entre consultas) y cada POST devuelve `post()`.
 */
export function mockBenchmarkApi(history: () => BenchmarkRun[], post: () => Response = () => jsonResponse({}, 202)) {
  const fetchMock = vi.fn(async (_path: string, init?: RequestInit) => {
    const method = init?.method ?? 'GET'
    return method === 'POST' ? post() : jsonResponse(history())
  })
  vi.stubGlobal('fetch', fetchMock)
  return fetchMock
}
