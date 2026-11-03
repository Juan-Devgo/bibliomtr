/**
 * Tipos de las respuestas de `POST /api/clustering` y `GET /api/clustering/trace` (spec 003, REQ-11, REQ-16,
 * REQ-18, REQ-19, REQ-22). Reflejan los records Java de `api/clustering/ClusteringDtos`,
 * `algorithms/clustering/Merge` y `trace/clustering`.
 */
import type { AlgorithmStatus, ArticleSummary, SimilarityAlgorithmSlug } from './similarity'

/** Slug de cada uno de los 4 algoritmos, en el orden de `ClusteringAlgorithm`. */
export type ClusteringAlgorithmSlug = 'single-linkage' | 'complete-linkage' | 'average-linkage' | 'diana'

/** Los 4 algoritmos en el orden de la respuesta (REQ-11). */
export const CLUSTERING_ALGORITHMS: readonly ClusteringAlgorithmSlug[] = [
  'single-linkage',
  'complete-linkage',
  'average-linkage',
  'diana',
]

/** Nombre legible de cada algoritmo; el mismo `label` que devuelve la API. */
export const CLUSTERING_ALGORITHM_LABELS: Record<ClusteringAlgorithmSlug, string> = {
  'single-linkage': 'Single Linkage',
  'complete-linkage': 'Complete Linkage',
  'average-linkage': 'Average Linkage (UPGMA)',
  diana: 'DIANA (divisivo)',
}

/** Fuente de similitud por defecto de la sección de clustering (REQ-13). */
export const DEFAULT_SOURCE: SimilarityAlgorithmSlug = 'specter2-cosine'

/**
 * Una fusión del dendrograma, estilo SciPy (`Merge`): con N hojas (0..N−1), la fusión k crea el grupo N + k
 * uniendo `left` < `right` a la altura `height` ∈ [0,1]; `size` = artículos del grupo nuevo.
 */
export interface Merge {
  left: number
  right: number
  height: number
  size: number
}

/** Fuente de similitud usada y su estado (`SourceSummary`, REQ-19). */
export interface SourceSummary {
  source: SimilarityAlgorithmSlug
  label: string
  status: AlgorithmStatus
  /** `<archivo>#<método>` del stub del Requerimiento 1 si está PENDIENTE; `null` si no. */
  pendingLocation: string | null
}

/** Dendrograma de un algoritmo (`DendrogramResult`, REQ-11, REQ-18, REQ-22). */
export interface DendrogramResult {
  algorithm: ClusteringAlgorithmSlug
  label: string
  status: AlgorithmStatus
  /** N − 1 fusiones; `null` si está PENDIENTE. */
  merges: Merge[] | null
  /** Tiempo en ms del algoritmo; `null` si está PENDIENTE. */
  elapsedMs: number | null
  /** `<archivo>#<método>` del stub si está PENDIENTE; `null` si no. */
  pendingLocation: string | null
}

/** Respuesta de un clustering (`ClusteringResponse`, REQ-11, REQ-19, REQ-22). */
export interface ClusteringResponse {
  /** Artículos agrupados, en el orden de la solicitud: la hoja i es el artículo i. */
  articles: ArticleSummary[]
  source: SourceSummary
  /** Tiempo en ms del preprocesamiento y la matriz de similitud; `null` si la fuente está PENDIENTE. */
  preparationMs: number | null
  /** Distancias N×N d = 1 − s con diagonal 0; `null` si la fuente está PENDIENTE. */
  distances: number[][] | null
  /** Los 4 resultados en el orden de `CLUSTERING_ALGORITHMS`; vacía si la fuente está PENDIENTE. */
  results: DendrogramResult[]
}

/** Una fusión de un algoritmo aglomerativo (`LinkageTrace.LinkageStep`, REQ-16). */
export interface LinkageStep {
  /** Ids de los grupos activos antes de fusionar. */
  clusters: number[]
  /** Artículos (índices de hoja) de cada grupo, alineados con `clusters`. */
  members: number[][]
  /** Distancias entre los grupos activos, alineadas con `clusters`. */
  distances: number[][]
  left: number
  right: number
  /** Distancia entre `left` y `right`: la altura de la fusión. */
  distance: number
  newCluster: number
  /** `clusters` sin `left` ni `right`. */
  remaining: number[]
  /** d(newCluster, h) para cada h de `remaining`, alineado. */
  updatedRow: number[]
}

/** Traza de Single, Complete y Average Linkage (`LinkageTrace`): un paso por fusión. */
export interface LinkageTrace {
  steps: LinkageStep[]
}

/** Una iteración de la búsqueda del grupo disidente de DIANA (`DianaTrace.DianaIteration`). */
export interface DianaIteration {
  splinter: number[]
  remainder: number[]
  /** Media de d(i, R \ {i}) para cada i de `remainder`, alineada. */
  averageToRemainder: number[]
  /** Media de d(i, S) para cada i de `remainder`, alineada (0 en la primera iteración). */
  averageToSplinter: number[]
  /** D(i) = averageToRemainder − averageToSplinter, alineada. */
  differences: number[]
  /** Artículo movido a S, o −1 al terminar. */
  moved: number
}

/** Una división de DIANA (`DianaTrace.DianaStep`, REQ-16). */
export interface DianaStep {
  /** Grupos activos antes de dividir, con sus artículos en orden creciente. */
  clusters: number[][]
  /** Diámetro de cada grupo, alineado con `clusters`. */
  diameters: number[]
  /** Posición en `clusters` del grupo dividido. */
  splitIndex: number
  diameter: number
  iterations: DianaIteration[]
  splinter: number[]
  remainder: number[]
}

/** Traza de DIANA (`DianaTrace`): un paso por división. */
export interface DianaTrace {
  steps: DianaStep[]
}

/** Traza que devuelve cada algoritmo según su slug. */
export interface ClusteringTraceBySlug {
  'single-linkage': LinkageTrace
  'complete-linkage': LinkageTrace
  'average-linkage': LinkageTrace
  diana: DianaTrace
}

/** Dendrograma y traza de un algoritmo `A` (`ClusteringTraceResponse`). */
interface ClusteringTraceOf<A extends ClusteringAlgorithmSlug> {
  algorithm: A
  label: string
  articles: ArticleSummary[]
  source: SourceSummary
  preparationMs: number | null
  distances: number[][] | null
  /** `null` si la fuente está PENDIENTE. */
  status: AlgorithmStatus | null
  merges: Merge[] | null
  trace: ClusteringTraceBySlug[A] | null
  elapsedMs: number | null
  pendingLocation: string | null
}

/**
 * Respuesta de `GET /api/clustering/trace` (REQ-16, REQ-18, REQ-19). Es una unión discriminada por
 * `algorithm`, así un `switch` sobre el slug estrecha el tipo de `trace`.
 */
export type ClusteringTraceResponse = {
  [A in ClusteringAlgorithmSlug]: ClusteringTraceOf<A>
}[ClusteringAlgorithmSlug]
