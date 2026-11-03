import { scaleLinear } from 'd3'

/** Ancho del lienzo del dendrograma en unidades del viewBox; el SVG se adapta al ancho del contenedor. */
export const DENDROGRAM_WIDTH = 640

/** Espacio de las etiquetas de las hojas, a la izquierda. */
const LABEL_WIDTH = 300

/** Margen derecho, para que la marca 1 del eje no se corte. */
const RIGHT_MARGIN = 24

/** Marcas del eje de alturas: la misma escala [0,1] en todos los dendrogramas (spec 003, REQ-14). */
export const HEIGHT_TICKS: readonly number[] = [0, 0.25, 0.5, 0.75, 1]

/** Escala de alturas [0,1] → posición horizontal, compartida por todos los dendrogramas (REQ-14). */
export const heightScale = scaleLinear()
  .domain([0, 1])
  .range([LABEL_WIDTH, DENDROGRAM_WIDTH - RIGHT_MARGIN])
