/**
 * Tramos del abstract para resaltar las ocurrencias que devolvió la API (spec 004, REQ-20, REQ-21, AC-15).
 *
 * No recibe el texto ni el patrón y por eso no puede buscarlos ni compararlos: solo reorganiza las posiciones de
 * un algoritmo en intervalos [start, end) de índices UTF-16, iguales a los `char` de Java.
 */

/** Intervalo [start, end) del abstract; si está resaltado, `starts` trae las posiciones iniciales que contiene. */
export interface Segment {
  start: number
  end: number
  highlighted: boolean
  /** Posiciones iniciales de las ocurrencias del tramo, ascendentes; vacía si no está resaltado. */
  starts: number[]
}

/**
 * Convierte las posiciones de un algoritmo en tramos consecutivos que cubren [0, textLength).
 *
 * Recorre `positions` (ascendentes, como las valida el backend) una vez: la ocurrencia en p cubre
 * [p, min(p + m, n)); si p ≤ fin del tramo abierto (se solapa o se toca con él), lo extiende y agrega p a sus
 * inicios; si no, cierra el tramo y rellena el hueco con un tramo no resaltado. Ejemplo: n = 4, [0, 1, 2], m = 2
 * → [{0, 4, resaltado, [0, 1, 2]}].
 *
 * Complejidad: O(k) en tiempo y espacio, con k = número de posiciones; no ordena.
 *
 * @param textLength    longitud n del abstract
 * @param positions     posiciones iniciales de la API, ascendentes
 * @param patternLength longitud m del patrón
 */
export function highlightSegments(textLength: number, positions: readonly number[], patternLength: number): Segment[] {
  const segments: Segment[] = []
  // Fin del último tramo emitido: todo lo anterior ya está cubierto.
  let covered = 0
  let open: Segment | null = null
  for (const position of positions) {
    const end = Math.min(position + patternLength, textLength)
    if (open !== null && position <= open.end) {
      // Se solapa o se toca con el tramo abierto: un solo tramo continuo con una marca más.
      open.end = Math.max(open.end, end)
      open.starts.push(position)
      continue
    }
    if (open !== null) {
      segments.push(open)
      covered = open.end
    }
    if (position > covered) {
      segments.push({ start: covered, end: position, highlighted: false, starts: [] })
    }
    open = { start: position, end, highlighted: true, starts: [position] }
  }
  if (open !== null) {
    segments.push(open)
    covered = open.end
  }
  if (covered < textLength) {
    segments.push({ start: covered, end: textLength, highlighted: false, starts: [] })
  }
  return segments
}
