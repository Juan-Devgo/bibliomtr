import { useMutation, useQueryClient } from '@tanstack/react-query'
import type { BenchmarkRun } from '@/types/benchmark'
import { apiPost } from './client'
import { BENCHMARKS_QUERY_KEY } from './useBenchmarks'

/**
 * Lanza una medición JMH de los tres algoritmos sobre el corpus completo: `POST /api/benchmarks`
 * (REQ-17, REQ-19).
 *
 * Al terminar, con éxito (202, EN_CURSO) o con error (p. ej. 409 "Ya hay una medición en curso"), invalida
 * `['benchmarks']` para que el historial muestre el estado actual y el refresco de `useBenchmarks` siga la
 * medición en curso. El error llega como `ApiError` con el mensaje en español del backend.
 */
export function useStartBenchmark() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: () => apiPost<BenchmarkRun>('/api/benchmarks'),
    onSettled: () => queryClient.invalidateQueries({ queryKey: BENCHMARKS_QUERY_KEY }),
  })
}
