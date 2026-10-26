import { useQuery } from '@tanstack/react-query'
import type { ArticleSummary } from '@/types/similarity'
import { apiGet } from './client'

/** Clave de la consulta de artículos seleccionables. */
export const ARTICLES_QUERY_KEY = ['articles'] as const

/**
 * Artículos seleccionables del corpus, ordenados por nombre de archivo: `GET /api/articles` (REQ-13).
 * Los artículos sin abstract detectado no aparecen (REQ-10).
 */
export function useArticles() {
  return useQuery({
    queryKey: ARTICLES_QUERY_KEY,
    queryFn: () => apiGet<ArticleSummary[]>('/api/articles'),
  })
}
