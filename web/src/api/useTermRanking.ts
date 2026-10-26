import { useQuery } from '@tanstack/react-query'
import type { RankingResponse } from '@/types/ranking'
import { apiGet } from './client'

/**
 * Ranking de términos de los tres algoritmos: `GET /api/ranking?n=…&bucketSize=…` (REQ-3, REQ-5).
 * Un `n` o `bucketSize` inválido llega como `ApiError` 400 con el mensaje en español del backend.
 */
export function useTermRanking(n: number, bucketSize: number) {
  return useQuery({
    queryKey: ['ranking', n, bucketSize],
    queryFn: () => {
      const params = new URLSearchParams({ n: String(n), bucketSize: String(bucketSize) })
      return apiGet<RankingResponse>(`/api/ranking?${params.toString()}`)
    },
  })
}
