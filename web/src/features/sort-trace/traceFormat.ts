import type { TermFrequency } from '@/types/ranking'

/** Texto de un par (término, frecuencia) en los arreglos de la traza: `término (frecuencia)`. */
export function termLabel(entry: TermFrequency): string {
  return `${entry.term} (${entry.frequency})`
}

/** Texto de un entero de la traza (conteos, dígitos). */
export function numberLabel(value: number): string {
  return String(value)
}
