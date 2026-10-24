/** Decimales con que se muestran los reales de la traza (pesos, normas, cosenos, similitudes). */
export const TRACE_DECIMALS = 6

/** Real de la traza con 6 decimales, p. ej. `0.750000`. */
export function decimal(value: number): string {
  return value.toFixed(TRACE_DECIMALS)
}

/**
 * Texto de un símbolo de una secuencia: el espacio, que aparece con granularidad de caracteres, se muestra
 * como `␣` para que se vea.
 */
export function symbolLabel(symbol: string): string {
  return symbol === ' ' ? '␣' : symbol
}

/** Encabezados de una matriz de programación dinámica: `ε` para la fila o columna 0 y luego los símbolos. */
export function dpLabels(sequence: readonly string[]): string[] {
  return ['ε', ...sequence.map(symbolLabel)]
}
