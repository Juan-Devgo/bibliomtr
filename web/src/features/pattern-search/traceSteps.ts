/**
 * Conversión de la traza de un algoritmo de búsqueda de patrones en pasos uniformes del visor (spec 004, REQ-26).
 *
 * Solo reorganiza los pasos que devuelve la API: no busca ni compara el patrón con el texto (REQ-21). Los índices,
 * los desplazamientos, los caracteres, los saltos y los hashes salen tal cual de la traza.
 */
import type {
  BoyerMooreTraceResponse,
  KmpTraceResponse,
  PatternTraceResponse,
  RabinKarpTraceResponse,
} from '@/types/patternSearch'

/** Un carácter comparado en un paso: T[textIndex] contra P[patternIndex] y si coincidieron según la traza. */
export interface ComparedChar {
  textIndex: number
  patternIndex: number
  matched: boolean
}

/** Hash de la ventana de un paso de Rabin-Karp, tal como lo trae la traza. */
export interface WindowHash {
  hash: number
  patternHash: number
  hashMatches: boolean
  /** Resultado de la verificación carácter a carácter; `null` si el hash no coincidió. */
  verified: boolean | null
  collision: boolean
}

/** Paso uniforme del visor: el patrón alineado en `shift` con lo comparado en ese paso. */
export interface TraceFrame {
  /** Desplazamiento s del patrón bajo el texto en este paso. */
  shift: number
  /** Caracteres comparados en el paso, en el orden de la traza (vacío en Rabin-Karp). */
  compared: ComparedChar[]
  /** Índice j del patrón donde hubo desajuste, si lo hubo. */
  mismatchIndex?: number
  /** El paso completa (KMP, Boyer-Moore) o verifica (Rabin-Karp) una ocurrencia. */
  occurrence: boolean
  /** Desplazamiento del paso siguiente; `undefined` en el último paso de KMP. */
  nextShift?: number
  /** Descripción del paso en español. */
  label: string
  /** Hash de la ventana; solo en Rabin-Karp. */
  window?: WindowHash
}

/**
 * KMP: un frame por comparación T[i] vs P[j], con s = i − j de la traza y el s de la comparación siguiente.
 *
 * Complejidad: O(c) en tiempo y espacio, con c = número de comparaciones.
 */
function kmpFrames(response: KmpTraceResponse): TraceFrame[] {
  const comparisons = response.trace?.comparisons ?? []
  return comparisons.map((comparison, index) => {
    let label =
      `Comparación ${index + 1}: T[${comparison.i}] = '${comparison.textChar}' y P[${comparison.j}] = ` +
      `'${comparison.patternChar}' ${comparison.matched ? 'coinciden' : 'no coinciden'} (s = ${comparison.shift})`
    if (comparison.occurrence !== null) {
      label += `; ocurrencia en s = ${comparison.occurrence}`
    }
    if (comparison.fallbackTo !== null) {
      // Tras una ocurrencia j llegó a m y se usa π[m − 1] = π[j]; tras un desajuste, π[j − 1].
      const piIndex = comparison.occurrence !== null ? comparison.j : comparison.j - 1
      label += `; retroceso j ← π[${piIndex}] = ${comparison.fallbackTo}`
    } else if (!comparison.matched) {
      label += '; j = 0: avanza i'
    }
    return {
      shift: comparison.shift,
      compared: [{ textIndex: comparison.i, patternIndex: comparison.j, matched: comparison.matched }],
      mismatchIndex: comparison.matched ? undefined : comparison.j,
      occurrence: comparison.occurrence !== null,
      nextShift: index + 1 < comparisons.length ? comparisons[index + 1].shift : undefined,
      label,
    }
  })
}

/**
 * Boyer-Moore: un frame por alineación, con las comparaciones de derecha a izquierda y s + salto aplicado.
 *
 * Complejidad: O(c) en tiempo y espacio, con c = número de comparaciones de todas las alineaciones.
 */
function boyerMooreFrames(response: BoyerMooreTraceResponse): TraceFrame[] {
  const alignments = response.trace?.alignments ?? []
  return alignments.map((alignment, index) => {
    const prefix = `Alineación ${index + 1} (s = ${alignment.shift}): `
    let label: string
    if (alignment.occurrence || alignment.mismatchIndex === null) {
      label = `${prefix}ocurrencia completa; salto tras ocurrencia ${alignment.appliedShift}`
    } else {
      // La comparación del desajuste es la última: se compara de derecha a izquierda y se para al fallar.
      const failed = alignment.comparisons[alignment.comparisons.length - 1]
      label =
        `${prefix}desajuste en j = ${alignment.mismatchIndex} con T[${failed?.textIndex}] = '${failed?.textChar}'; ` +
        `carácter malo ${alignment.badCharacterShift}, sufijo bueno ${alignment.goodSuffixShift}, ` +
        `salto aplicado ${alignment.appliedShift}`
    }
    return {
      shift: alignment.shift,
      compared: alignment.comparisons.map((comparison) => ({
        textIndex: comparison.textIndex,
        patternIndex: comparison.j,
        matched: comparison.matched,
      })),
      mismatchIndex: alignment.mismatchIndex ?? undefined,
      occurrence: alignment.occurrence,
      nextShift: alignment.shift + alignment.appliedShift,
      label,
    }
  })
}

/**
 * Rabin-Karp: un frame por ventana [s, s + m), sin comparaciones de caracteres, con su hash, la verificación y si
 * es colisión; el paso siguiente es s + 1.
 *
 * Complejidad: O(w) en tiempo y espacio, con w = número de ventanas.
 */
function rabinKarpFrames(response: RabinKarpTraceResponse): TraceFrame[] {
  const trace = response.trace
  if (!trace) {
    return []
  }
  return trace.windows.map((window) => {
    let label = `Ventana s = ${window.shift}: t(${window.shift}) = ${window.hash}`
    if (!window.hashMatches) {
      label += ` ≠ h(P) = ${trace.patternHash}`
    } else if (window.collision) {
      label += ' = h(P); verificación: no coincide, colisión'
    } else {
      label += ' = h(P); verificación: coincide, ocurrencia'
    }
    return {
      shift: window.shift,
      compared: [],
      occurrence: window.verified === true,
      nextShift: window.shift + 1,
      label,
      window: {
        hash: window.hash,
        patternHash: trace.patternHash,
        hashMatches: window.hashMatches,
        verified: window.verified,
        collision: window.collision,
      },
    }
  })
}

/**
 * Muestra un carácter de la traza de forma visible en una celda: el espacio como "␣" y el salto de línea como
 * "↵"; `undefined` (fuera del texto) como cadena vacía. Solo cambia cómo se dibuja, no compara con el patrón.
 *
 * Complejidad: O(1).
 */
export function visibleChar(character: string | undefined): string {
  if (character === undefined) {
    return ''
  }
  if (character === ' ') {
    return '␣'
  }
  return character === '\n' ? '↵' : character
}

/**
 * Pasos del visor a partir de la respuesta de traza (REQ-26): KMP, uno por comparación; Boyer-Moore, uno por
 * alineación; Rabin-Karp, uno por ventana. Una respuesta PENDIENTE o sin traza no tiene pasos.
 *
 * Complejidad: O(c) en tiempo y espacio, con c = tamaño de la traza.
 */
export function toFrames(response: PatternTraceResponse): TraceFrame[] {
  if (response.status !== 'CALCULADO') {
    return []
  }
  switch (response.algorithm) {
    case 'kmp':
      return kmpFrames(response)
    case 'boyer-moore':
      return boyerMooreFrames(response)
    case 'rabin-karp':
      return rabinKarpFrames(response)
    default:
      return []
  }
}
