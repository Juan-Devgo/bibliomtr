import { useQuery } from '@tanstack/react-query'
import type { SortAlgorithmSlug, SortTraceResponse } from '@/types/ranking'
import { apiGet } from './client'

/**
 * Traza completa de un algoritmo sobre la lista del corpus completo:
 * `GET /api/ranking/trace/{algorithm}?bucketSize=…` (REQ-16). Solo consulta cuando hay un algoritmo
 * elegido; `bucketSize` solo afecta a Bucket Sort.
 */
export function useSortTrace(algorithm: SortAlgorithmSlug | null, bucketSize: number) {
  return useQuery({
    queryKey: ['ranking-trace', algorithm, bucketSize],
    queryFn: () => {
      const params = new URLSearchParams({ bucketSize: String(bucketSize) })
      return apiGet<SortTraceResponse>(`/api/ranking/trace/${algorithm}?${params.toString()}`)
    },
    enabled: algorithm !== null,
  })
}
