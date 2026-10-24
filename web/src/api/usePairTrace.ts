import { useQuery } from '@tanstack/react-query'
import type { PairTraceResponse, SimilarityAlgorithmSlug } from '@/types/similarity'
import { apiGet } from './client'

/**
 * Resultado y traza de un algoritmo para un par de artículos:
 * `GET /api/similarity/trace?algorithm=…&a=…&b=…` (REQ-16). Solo consulta cuando el algoritmo y los dos
 * artículos están definidos; `a = b` está permitido.
 */
export function usePairTrace(algorithm: SimilarityAlgorithmSlug | null, a: number | null, b: number | null) {
  return useQuery({
    queryKey: ['similarity-trace', algorithm, a, b],
    queryFn: () => {
      const params = new URLSearchParams({ algorithm: String(algorithm), a: String(a), b: String(b) })
      return apiGet<PairTraceResponse>(`/api/similarity/trace?${params.toString()}`)
    },
    enabled: algorithm !== null && a !== null && b !== null,
  })
}
