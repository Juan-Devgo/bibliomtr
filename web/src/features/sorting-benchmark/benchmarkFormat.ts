import type { BenchmarkResult, JmhParameters } from '@/types/benchmark'
import type { SortAlgorithmSlug } from '@/types/ranking'

/** Decimales con que se muestran los tiempos JMH en ms. */
const SCORE_DECIMALS = 4

/** Formato de fecha y hora de las mediciones, en español. */
const DATE_TIME_FORMAT = new Intl.DateTimeFormat('es-CO', { dateStyle: 'medium', timeStyle: 'medium' })

/**
 * Tiempo promedio de un algoritmo como `score ± scoreError ms` (REQ-22). El error es el scoreError de JMH,
 * la semiamplitud del intervalo de confianza al 99,9 %.
 */
export function formatScore(result: Pick<BenchmarkResult, 'scoreMs' | 'scoreErrorMs'>): string {
  return `${result.scoreMs.toFixed(SCORE_DECIMALS)} ± ${result.scoreErrorMs.toFixed(SCORE_DECIMALS)} ms`
}

/**
 * Parámetros propios del algoritmo en su complejidad (REQ-22): Counting `k = …` (frecuencia máxima),
 * Radix `d = …, b = …` (dígitos del máximo y base 10), Bucket `m = …` (número de cubetas).
 */
export function formatAlgorithmParameters(result: Pick<BenchmarkResult, 'algorithm' | 'parameters'>): string {
  const { k, d, b, m } = result.parameters
  const byAlgorithm: Record<SortAlgorithmSlug, string> = {
    counting: `k = ${k ?? '—'}`,
    radix: `d = ${d ?? '—'}, b = ${b ?? '—'}`,
    bucket: `m = ${m ?? '—'}`,
  }
  return byAlgorithm[result.algorithm] ?? ''
}

/** Fecha y hora local de un instante ISO 8601. */
export function formatDateTime(iso: string): string {
  return DATE_TIME_FORMAT.format(new Date(iso))
}

/** Resumen de los parámetros JMH fijos de REQ-18. */
export function formatJmhParameters(parameters: JmhParameters): string {
  return (
    `${parameters.forks} fork · calentamiento ${parameters.warmupIterations} × ${parameters.warmupSeconds} s · ` +
    `medición ${parameters.measurementIterations} × ${parameters.measurementSeconds} s · ` +
    `modo ${parameters.mode} · unidad ${parameters.timeUnit} · bucketSize ${parameters.bucketSize} · ` +
    `traza ${parameters.traceEnabled ? 'activada' : 'desactivada'}`
  )
}
