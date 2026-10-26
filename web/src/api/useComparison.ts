import { useMutation } from '@tanstack/react-query'
import type { ComparisonResponse } from '@/types/similarity'
import { apiPost } from './client'

/**
 * Compara de 2 a 20 artículos: `POST /api/similarity/comparisons` con `{ articleIds }` (REQ-13, REQ-14).
 * Las matrices siguen el orden de `articleIds`. Una selección inválida llega como `ApiError` 400 con el
 * mensaje en español del backend (REQ-15), que la interfaz muestra tal cual.
 */
export function useComparison() {
  return useMutation({
    mutationFn: (articleIds: readonly number[]) =>
      apiPost<ComparisonResponse>('/api/similarity/comparisons', { articleIds }),
  })
}
