import { describe, expect, it } from 'vitest'
import { highlightSegments } from './highlightSegments'

describe('highlightSegments (REQ-20, REQ-21, AC-15)', () => {
  it('une las ocurrencias solapadas en un tramo continuo con una marca en cada inicio ("aaaa", "aa")', () => {
    expect(highlightSegments(4, [0, 1, 2], 2)).toEqual([{ start: 0, end: 4, highlighted: true, starts: [0, 1, 2] }])
  })

  it('une las solapadas y rellena el resto con un tramo sin resaltar ("abababca", "abab")', () => {
    expect(highlightSegments(8, [0, 2], 4)).toEqual([
      { start: 0, end: 6, highlighted: true, starts: [0, 2] },
      { start: 6, end: 8, highlighted: false, starts: [] },
    ])
  })

  it('une las ocurrencias que se tocan sin solaparse', () => {
    expect(highlightSegments(6, [0, 2], 2)).toEqual([
      { start: 0, end: 4, highlighted: true, starts: [0, 2] },
      { start: 4, end: 6, highlighted: false, starts: [] },
    ])
  })

  it('separa las ocurrencias con huecos entre ellas y al inicio', () => {
    expect(highlightSegments(9, [1, 5], 2)).toEqual([
      { start: 0, end: 1, highlighted: false, starts: [] },
      { start: 1, end: 3, highlighted: true, starts: [1] },
      { start: 3, end: 5, highlighted: false, starts: [] },
      { start: 5, end: 7, highlighted: true, starts: [5] },
      { start: 7, end: 9, highlighted: false, starts: [] },
    ])
  })

  it('termina en una ocurrencia al final del texto sin tramo vacío', () => {
    expect(highlightSegments(5, [0, 3], 2)).toEqual([
      { start: 0, end: 2, highlighted: true, starts: [0] },
      { start: 2, end: 3, highlighted: false, starts: [] },
      { start: 3, end: 5, highlighted: true, starts: [3] },
    ])
  })

  it('recorta el fin del tramo a la longitud del texto', () => {
    expect(highlightSegments(3, [2], 2)).toEqual([
      { start: 0, end: 2, highlighted: false, starts: [] },
      { start: 2, end: 3, highlighted: true, starts: [2] },
    ])
  })

  it('sin posiciones devuelve un solo tramo sin resaltar, y con el texto vacío ninguno', () => {
    expect(highlightSegments(5, [], 2)).toEqual([{ start: 0, end: 5, highlighted: false, starts: [] }])
    expect(highlightSegments(0, [], 2)).toEqual([])
  })

  it('usa las posiciones recibidas tal cual, aunque no sean ocurrencias del patrón (REQ-21)', () => {
    // La función no recibe el texto ni el patrón: solo reorganiza las posiciones de la API.
    expect(highlightSegments(8, [1, 4], 4)).toEqual([
      { start: 0, end: 1, highlighted: false, starts: [] },
      { start: 1, end: 8, highlighted: true, starts: [1, 4] },
    ])
  })
})
