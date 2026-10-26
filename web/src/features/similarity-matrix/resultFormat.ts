/** Tiempo en milisegundos con 3 decimales: `1.250 ms` (REQ-22). */
export function formatMs(ms: number): string {
  return `${ms.toFixed(3)} ms`
}

/** Encabezado de un artículo en filas y columnas de las matrices: `#id`. */
export function articleLabel(id: number): string {
  return `#${id}`
}
