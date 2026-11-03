import { useQuery } from '@tanstack/react-query'
import type { ClusteringResponse } from '@/types/clustering'
import { MIN_SELECTION, type SimilarityAlgorithmSlug } from '@/types/similarity'
import { apiPost } from './client'

/**
 * Dendrogramas de los 4 algoritmos: `POST /api/clustering` con `{ articleIds, source }` (spec 003, REQ-11,
 * REQ-13). Las hojas siguen el orden de `articleIds`. Solo consulta con 2 o más ids; un cambio de fuente es una
 * consulta nueva, guardada en caché por ids y fuente.
 */
export function useClustering(articleIds: readonly number[], source: SimilarityAlgorithmSlug) {
  return useQuery({
    queryKey: ['clustering', articleIds, source],
    queryFn: () => apiPost<ClusteringResponse>('/api/clustering', { articleIds, source }),
    enabled: articleIds.length >= MIN_SELECTION,
  })
}
