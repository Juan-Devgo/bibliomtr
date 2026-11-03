/** Decimales con que se muestran las distancias y alturas del clustering. */
export const DISTANCE_DECIMALS = 3

/** Distancia o altura con 3 decimales, p. ej. `0.450`. */
export function formatDistance(value: number): string {
  return value.toFixed(DISTANCE_DECIMALS)
}

/** Conjunto de artículos (índices de hoja): `{0, 1, 2}`. */
export function memberSet(members: readonly number[]): string {
  return `{${members.join(', ')}}`
}

/** Encabezado de un grupo de la traza aglomerativa: `G4 {0, 1}`. */
export function groupLabel(id: number, members: readonly number[]): string {
  return `G${id} ${memberSet(members)}`
}
