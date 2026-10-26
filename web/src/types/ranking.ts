/**
 * Tipos de las respuestas de `GET /api/ranking` y `GET /api/ranking/trace/{algorithm}` (REQ-3, REQ-16).
 * Reflejan los records Java de `api/ranking/RankingDtos` y `trace/ranking`.
 */

/** N por defecto de `GET /api/ranking` (REQ-3). */
export const DEFAULT_N = 20

/** `bucketSize` por defecto de la API, solo usado por Bucket Sort (REQ-9). */
export const DEFAULT_BUCKET_SIZE = 5

/** Slug de cada algoritmo del Requerimiento 2. */
export type SortAlgorithmSlug = 'counting' | 'radix' | 'bucket'

/** Algoritmos en el orden de la respuesta: Counting, Radix, Bucket. */
export const SORT_ALGORITHMS: readonly SortAlgorithmSlug[] = ['counting', 'radix', 'bucket']

/** Nombre legible de cada algoritmo. */
export const SORT_ALGORITHM_LABELS: Record<SortAlgorithmSlug, string> = {
  counting: 'Counting Sort',
  radix: 'Radix Sort',
  bucket: 'Bucket Sort',
}

/** Par (término, frecuencia); la frecuencia es la clave de ordenamiento. */
export interface TermFrequency {
  term: string
  frequency: number
}

/** Top N y tiempo de un algoritmo, medido sin traza (AC-7). */
export interface AlgorithmRanking {
  algorithm: SortAlgorithmSlug
  top: TermFrequency[]
  elapsedMs: number
}

/** Ranking de los tres algoritmos sobre la misma lista (REQ-3, REQ-11). */
export interface RankingResponse {
  termCount: number
  n: number
  bucketSize: number
  preprocessingMs: number
  allMatch: boolean
  algorithms: AlgorithmRanking[]
}

/** Traza de Counting Sort (REQ-13). */
export interface CountingSortTrace {
  input: TermFrequency[]
  max: number
  /** Arreglo C tras contar: C[v] = número de elementos con frecuencia v. */
  count: number[]
  /** Arreglo C tras acumular: C[v] += C[v − 1]. */
  cumulative: number[]
  /** Colocaciones en el orden del recorrido de derecha a izquierda. */
  placements: CountingPlacement[]
  output: TermFrequency[]
}

/** Colocación de un elemento de la entrada en la salida de Counting Sort. */
export interface CountingPlacement {
  inputIndex: number
  element: TermFrequency
  outputIndex: number
}

/** Traza de Radix Sort (REQ-14). */
export interface RadixSortTrace {
  input: TermFrequency[]
  max: number
  passes: RadixPass[]
  output: TermFrequency[]
}

/** Una pasada de Counting Sort estable por el dígito ⌊v/exp⌋ mod 10. */
export interface RadixPass {
  exp: number
  digits: number[]
  count: number[]
  cumulative: number[]
  result: TermFrequency[]
}

/** Traza de Bucket Sort (REQ-15). */
export interface BucketSortTrace {
  input: TermFrequency[]
  min: number
  max: number
  bucketSize: number
  bucketCount: number
  buckets: SortBucket[]
  concatenated: TermFrequency[]
}

/** Contenido de una cubeta antes y después del insertion sort. */
export interface SortBucket {
  index: number
  before: TermFrequency[]
  after: TermFrequency[]
}

/** Campos comunes de la respuesta de traza. */
interface SortTraceBase {
  bucketSize: number
  input: TermFrequency[]
  /** Salida ascendente y estable por frecuencia. */
  sorted: TermFrequency[]
  /** Salida leída de derecha a izquierda. */
  ranking: TermFrequency[]
}

/** Respuesta de la traza, discriminada por `algorithm` (REQ-16). */
export type SortTraceResponse =
  | (SortTraceBase & { algorithm: 'counting'; trace: CountingSortTrace })
  | (SortTraceBase & { algorithm: 'radix'; trace: RadixSortTrace })
  | (SortTraceBase & { algorithm: 'bucket'; trace: BucketSortTrace })
