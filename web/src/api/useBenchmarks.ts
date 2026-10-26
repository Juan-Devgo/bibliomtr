import { useQuery } from '@tanstack/react-query'
import type { BenchmarkRun } from '@/types/benchmark'
import { apiGet } from './client'

/** Clave de TanStack Query del historial de mediciones JMH, compartida con `useStartBenchmark`. */
export const BENCHMARKS_QUERY_KEY = ['benchmarks'] as const

/** Intervalo de refresco, en ms, mientras alguna medición está EN_CURSO (REQ-20). */
export const BENCHMARKS_POLL_MS = 2000

/**
 * Historial completo de mediciones JMH, de la más reciente a la más antigua: `GET /api/benchmarks`
 * (REQ-20, REQ-28).
 *
 * Mientras alguna medición del historial está EN_CURSO vuelve a consultar cada 2 s, para que la interfaz
 * vea el paso a COMPLETADA o FALLIDA; sin mediciones en curso no refresca.
 *
 * Complejidad: O(r) por respuesta para revisar los estados, con r = número de mediciones.
 */
export function useBenchmarks() {
  return useQuery({
    queryKey: BENCHMARKS_QUERY_KEY,
    queryFn: () => apiGet<BenchmarkRun[]>('/api/benchmarks'),
    refetchInterval: (query) =>
      query.state.data?.some((run) => run.status === 'EN_CURSO') ? BENCHMARKS_POLL_MS : false,
  })
}
