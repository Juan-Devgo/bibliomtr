import { useQuery } from '@tanstack/react-query'
import type { ClusteringAlgorithmSlug, ClusteringTraceResponse } from '@/types/clustering'
import type { SimilarityAlgorithmSlug } from '@/types/similarity'
import { apiGet } from './client'

/**
 * Dendrograma y traza de un algoritmo:
 * `GET /api/clustering/trace?algorithm=…&source=…&articleIds=1,2,3` (spec 003, REQ-16). Solo consulta cuando
 * el algoritmo está elegido.
 */
export function useClusteringTrace(
  algorithm: ClusteringAlgorithmSlug | null,
  source: SimilarityAlgorithmSlug,
  articleIds: readonly number[],
) {
  return useQuery({
    queryKey: ['clustering-trace', algorithm, source, articleIds],
    queryFn: () => {
      const params = new URLSearchParams({
        algorithm: String(algorithm),
        source,
        articleIds: articleIds.join(','),
      })
      return apiGet<ClusteringTraceResponse>(`/api/clustering/trace?${params.toString()}`)
    },
    enabled: algorithm !== null,
  })
}
