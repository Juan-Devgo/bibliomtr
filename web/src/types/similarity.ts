/**
 * Tipos de las respuestas de `GET /api/articles`, `POST /api/similarity/comparisons` y
 * `GET /api/similarity/trace` (spec 001, REQ-13, REQ-14, REQ-16). Reflejan los records Java de
 * `api/corpus/ArticleSummary`, `api/similarity/ComparisonDtos` y `trace/similarity`.
 */

/** Mínimo de artículos de una comparación (REQ-13). */
export const MIN_SELECTION = 2

/** Máximo de artículos de una comparación (REQ-13). */
export const MAX_SELECTION = 20

/** Artículo seleccionable, sin abstract ni vectores (`ArticleSummary`). */
export interface ArticleSummary {
  id: number
  title: string
  authors: string[]
  fileName: string
  /** `true` si su texto de SPECTER2 se truncó a 512 tokens (REQ-9a). */
  specter2Truncated: boolean
}

/** Slug de cada uno de los 8 resultados, en el orden de `ComparisonAlgorithm`. */
export type SimilarityAlgorithmSlug =
  | 'levenshtein'
  | 'needleman-wunsch'
  | 'tfidf-cosine'
  | 'jaccard'
  | 'specter2-cosine'
  | 'specter2-euclidean'
  | 'nomic-cosine'
  | 'nomic-euclidean'

/** Los 8 resultados en el orden de la respuesta (REQ-14). */
export const SIMILARITY_ALGORITHMS: readonly SimilarityAlgorithmSlug[] = [
  'levenshtein',
  'needleman-wunsch',
  'tfidf-cosine',
  'jaccard',
  'specter2-cosine',
  'specter2-euclidean',
  'nomic-cosine',
  'nomic-euclidean',
]

/** Nombre legible de cada resultado; el mismo `label` que devuelve la API. */
export const SIMILARITY_ALGORITHM_LABELS: Record<SimilarityAlgorithmSlug, string> = {
  levenshtein: 'Levenshtein',
  'needleman-wunsch': 'Needleman-Wunsch',
  'tfidf-cosine': 'Coseno TF-IDF',
  jaccard: 'Jaccard',
  'specter2-cosine': 'SPECTER2 (coseno)',
  'specter2-euclidean': 'SPECTER2 (euclidiana)',
  'nomic-cosine': 'nomic-embed-text (coseno)',
  'nomic-euclidean': 'nomic-embed-text (euclidiana)',
}

/** Estado de un resultado: calculado, o pendiente porque el stub aún lanza "TODO estudiante:" (REQ-20). */
export type AlgorithmStatus = 'CALCULADO' | 'PENDIENTE'

/** Granularidad de Levenshtein y Needleman-Wunsch (`bibliomtr.similarity.edit-granularity`, REQ-3). */
export type EditGranularity = 'WORDS' | 'CHARACTERS'

/** Matriz N×N de un resultado (`AlgorithmMatrix`, REQ-14, REQ-20, REQ-22). */
export interface AlgorithmMatrix {
  algorithm: SimilarityAlgorithmSlug
  label: string
  status: AlgorithmStatus
  /** Similitudes en [0,1], simétrica, en el orden de `articles`; `null` si está PENDIENTE. */
  matrix: number[][] | null
  /** Tiempo en ms de recorrer todos los pares; `null` si está PENDIENTE. */
  elapsedMs: number | null
  /** `<archivo>#<método>` del stub si está PENDIENTE; `null` si no. */
  pendingLocation: string | null
}

/** Respuesta de una comparación (`ComparisonResponse`, REQ-14, REQ-22). */
export interface ComparisonResponse {
  /** Artículos comparados: filas y columnas de cada matriz, en el orden de la solicitud. */
  articles: ArticleSummary[]
  editGranularity: EditGranularity
  /** Tiempo en ms del preprocesamiento, medido aparte de los algoritmos (P11). */
  preprocessingMs: number
  /** Los 8 resultados en el orden de `SIMILARITY_ALGORITHMS`. */
  results: AlgorithmMatrix[]
}

/** Traza de Levenshtein (`LevenshteinTrace`). */
export interface LevenshteinTrace {
  a: string[]
  b: string[]
  /** Matriz D de (m + 1)×(n + 1). */
  matrix: number[][]
  distance: number
  /** max(m, n), denominador de la normalización sugerida. */
  maxLength: number
}

/** Puntajes de Needleman-Wunsch (`NeedlemanWunschScoring`). */
export interface NeedlemanWunschScoring {
  match: number
  mismatch: number
  gap: number
}

/** Movimiento de la matriz F: diagonal, arriba o izquierda; `null` en la celda (0, 0). */
export type NeedlemanWunschMove = 'DIAG' | 'UP' | 'LEFT'

/** Par del alineamiento; `null` es un gap. */
export interface AlignedPair {
  fromA: string | null
  fromB: string | null
}

/** Traza de Needleman-Wunsch (`NeedlemanWunschTrace`). */
export interface NeedlemanWunschTrace {
  a: string[]
  b: string[]
  scoring: NeedlemanWunschScoring
  /** Matriz F de (m + 1)×(n + 1). */
  scores: number[][]
  /** Movimiento elegido en cada celda de F. */
  moves: (NeedlemanWunschMove | null)[][]
  alignment: AlignedPair[]
  score: number
  minScore: number
  maxScore: number
}

/** Traza de Jaccard (`JaccardTrace`); los conjuntos van en orden de primera aparición. */
export interface JaccardTrace {
  setA: string[]
  setB: string[]
  intersection: string[]
  union: string[]
}

/** Traza del coseno TF-IDF (`TfIdfCosineTrace`); los arreglos van alineados con `vocabulary`. */
export interface TfIdfCosineTrace {
  vocabulary: string[]
  /** N = número de documentos del corpus (todos los seleccionables). */
  documentCount: number
  documentFrequency: number[]
  idf: number[]
  tfA: number[]
  tfB: number[]
  weightsA: number[]
  weightsB: number[]
  dotProduct: number
  normA: number
  normB: number
}

/** Traza de la similitud coseno entre vectores (`CosineTrace`). */
export interface CosineTrace {
  dimension: number
  /** aᵢ·bᵢ por componente. */
  products: number[]
  dotProduct: number
  normA: number
  normB: number
}

/** Traza de la distancia euclidiana entre vectores (`EuclideanTrace`). */
export interface EuclideanTrace {
  dimension: number
  /** (aᵢ − bᵢ)² por componente. */
  squaredDifferences: number[]
  sumOfSquares: number
}

/** Traza que devuelve cada resultado según su slug. */
export interface SimilarityTraceBySlug {
  levenshtein: LevenshteinTrace
  'needleman-wunsch': NeedlemanWunschTrace
  'tfidf-cosine': TfIdfCosineTrace
  jaccard: JaccardTrace
  'specter2-cosine': CosineTrace
  'specter2-euclidean': EuclideanTrace
  'nomic-cosine': CosineTrace
  'nomic-euclidean': EuclideanTrace
}

/** Resultado y traza de un resultado `S` para un par (`PairTraceResponse`). */
interface PairTraceOf<S extends SimilarityAlgorithmSlug> {
  algorithm: S
  label: string
  articleA: ArticleSummary
  articleB: ArticleSummary
  editGranularity: EditGranularity
  status: AlgorithmStatus
  /** Valor crudo (distancia, puntaje, coseno…); `null` si está PENDIENTE. */
  value: number | null
  /** Similitud en [0,1]; `null` si está PENDIENTE. */
  similarity: number | null
  /** Traza del algoritmo; `null` si está PENDIENTE. */
  trace: SimilarityTraceBySlug[S] | null
  elapsedMs: number | null
  pendingLocation: string | null
}

/**
 * Respuesta de `GET /api/similarity/trace` (REQ-16, REQ-20). Es una unión discriminada por `algorithm`,
 * así un `switch` sobre el slug estrecha el tipo de `trace`.
 */
export type PairTraceResponse = { [S in SimilarityAlgorithmSlug]: PairTraceOf<S> }[SimilarityAlgorithmSlug]
