import { useQuery } from '@tanstack/react-query'
import type { PatternSearchResponse } from '@/types/patternSearch'
import { apiGet } from './client'

/** Parámetros de una búsqueda: el patrón exactamente como se escribió y q opcional de Rabin-Karp. */
export interface PatternSearchParams {
  pattern: string
  /** Texto de q tal como se escribió; si falta, el backend usa 101. */
  q?: string
}

/**
 * Ocurrencias del patrón en los abstracts del corpus con KMP, Boyer-Moore y Rabin-Karp:
 * `GET /api/pattern-search?pattern=…&q=…` (spec 004, REQ-15, REQ-16).
 *
 * El patrón se envía sin recortar (`URLSearchParams` codifica " " como "+", que el backend decodifica como un
 * espacio, AC-25). No valida nada: un patrón vacío, uno de más de 200 caracteres o un q inválido llegan como
 * `ApiError` 400 con el mensaje en español del backend (REQ-17). Solo consulta con `params` no nulo.
 */
export function usePatternSearch(params: PatternSearchParams | null) {
  return useQuery({
    queryKey: ['pattern-search', params?.pattern, params?.q],
    queryFn: () => {
      const query = new URLSearchParams({ pattern: params?.pattern ?? '' })
      if (params?.q !== undefined) {
        query.set('q', params.q)
      }
      return apiGet<PatternSearchResponse>(`/api/pattern-search?${query.toString()}`)
    },
    enabled: params !== null,
  })
}
