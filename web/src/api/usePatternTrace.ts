import { useQuery } from '@tanstack/react-query'
import type { PatternTraceResponse, StringMatchingSlug } from '@/types/patternSearch'
import { apiGet } from './client'

/**
 * Preprocesamiento y traza de un algoritmo sobre el abstract de un artículo:
 * `GET /api/pattern-search/trace?algorithm=…&articleId=…&pattern=…&q=…` (spec 004, REQ-22).
 *
 * El patrón se envía sin recortar, igual que en la búsqueda. Un algoritmo PENDIENTE responde 200 con
 * `status` PENDIENTE y sin traza (REQ-28); un artículo inexistente, un algoritmo desconocido o un patrón o q
 * inválidos llegan como `ApiError` 400 con el mensaje en español. Solo consulta con algoritmo y artículo elegidos.
 */
export function usePatternTrace(
  algorithm: StringMatchingSlug | null,
  articleId: number | null,
  pattern: string,
  q?: string,
) {
  return useQuery({
    queryKey: ['pattern-trace', algorithm, articleId, pattern, q],
    queryFn: () => {
      const query = new URLSearchParams({
        algorithm: String(algorithm),
        articleId: String(articleId),
        pattern,
      })
      if (q !== undefined) {
        query.set('q', q)
      }
      return apiGet<PatternTraceResponse>(`/api/pattern-search/trace?${query.toString()}`)
    },
    enabled: algorithm !== null && articleId !== null,
  })
}
