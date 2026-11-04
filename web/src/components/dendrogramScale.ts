import { scaleLinear } from 'd3'
import type { Merge } from '@/types/clustering'

/** Ancho del lienzo del dendrograma en unidades del viewBox; el SVG se adapta al ancho del contenedor. */
export const DENDROGRAM_WIDTH = 580

/** Espacio de las etiquetas de las hojas, a la izquierda. */
const LABEL_WIDTH = 290

/** Margen derecho, para que la última marca del eje no se corte. */
const RIGHT_MARGIN = 24

/** Cantidad aproximada de marcas del eje de alturas. */
const TICK_COUNT = 5

/** Altura máxima del eje cuando ninguna fusión tiene altura positiva (distancias todas 0). */
const DEFAULT_MAX_HEIGHT = 1

/**
 * Altura máxima del eje, común a los dendrogramas de una misma vista (spec 003, REQ-14): la mayor altura de sus
 * fusiones redondeada hacia arriba a un valor "limpio" (p. ej. 0.047 → 0.05). Sin fusiones con altura positiva
 * devuelve 1.
 */
export function sharedMaxHeight(mergeLists: readonly (readonly Merge[])[]): number {
  const max = Math.max(0, ...mergeLists.flatMap((merges) => merges.map((merge) => merge.height)))
  return max > 0 ? scaleLinear().domain([0, max]).nice(TICK_COUNT - 1).domain()[1] : DEFAULT_MAX_HEIGHT
}

/** Escala de alturas [0, maxHeight] → posición horizontal. */
export function createHeightScale(maxHeight: number) {
  return scaleLinear()
    .domain([0, maxHeight])
    .range([LABEL_WIDTH, DENDROGRAM_WIDTH - RIGHT_MARGIN])
}

/** Marcas del eje de alturas para [0, maxHeight]. */
export function heightTicks(maxHeight: number): number[] {
  return createHeightScale(maxHeight).ticks(TICK_COUNT)
}
