/**
 * Tipos de las respuestas de `POST /api/benchmarks` y `GET /api/benchmarks` (REQ-22, REQ-27).
 * Reflejan los records Java de `api/benchmark/BenchmarkDtos`, `evaluation/JmhParameters` y
 * `algorithms/ranking/AlgorithmParameters`.
 */
import type { SortAlgorithmSlug } from './ranking'

/** Estado de una medición JMH (REQ-20). */
export type BenchmarkStatus = 'EN_CURSO' | 'COMPLETADA' | 'FALLIDA'

/** Parámetros fijos con que se midió (REQ-18): 1 fork, 5 × 1 s, 10 × 1 s, avgt, ms, bucketSize 5, sin traza. */
export interface JmhParameters {
  forks: number
  warmupIterations: number
  warmupSeconds: number
  measurementIterations: number
  measurementSeconds: number
  mode: string
  timeUnit: string
  bucketSize: number
  traceEnabled: boolean
}

/**
 * Parámetros propios de cada algoritmo en su complejidad teórica (REQ-22). Los que no aplican llegan en
 * `null`: Counting usa k (frecuencia máxima); Radix, d (dígitos del máximo) y b = 10; Bucket, m (cubetas).
 */
export interface AlgorithmParameters {
  k: number | null
  d: number | null
  b: number | null
  m: number | null
}

/** Resultado JMH de un algoritmo: tiempo promedio ± scoreError en ms y su complejidad teórica (REQ-22). */
export interface BenchmarkResult {
  algorithm: SortAlgorithmSlug
  /** Tiempo promedio por operación, en ms. */
  scoreMs: number
  /** scoreError de JMH: semiamplitud del intervalo de confianza al 99,9 %, en ms. */
  scoreErrorMs: number
  parameters: AlgorithmParameters
  /** Complejidad teórica, p. ej. `O(n+k)`. */
  complexity: string
}

/** Una medición JMH guardada en la base de datos (REQ-27). */
export interface BenchmarkRun {
  id: number
  /** Instante de inicio en ISO 8601. */
  startedAt: string
  /** Instante de fin en ISO 8601; `null` mientras está EN_CURSO. */
  finishedAt: string | null
  status: BenchmarkStatus
  /** Mensaje en español de una medición FALLIDA; `null` en otro caso. */
  message: string | null
  parameters: JmhParameters
  /** Número de términos n de la lista medida. */
  termCount: number | null
  /** Un resultado por algoritmo si está COMPLETADA; vacío en otro caso. */
  results: BenchmarkResult[]
}
