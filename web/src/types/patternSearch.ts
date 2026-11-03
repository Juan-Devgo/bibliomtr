/**
 * Tipos de las respuestas de `GET /api/pattern-search` y `GET /api/pattern-search/trace` (spec 004, REQ-15,
 * REQ-16, REQ-22, REQ-23, REQ-24, REQ-25, REQ-28, REQ-29). Reflejan los records Java de
 * `api/stringmatching/PatternSearchDtos` y `trace/stringmatching`.
 *
 * Las posiciones y los índices son base 0 en `char` de Java, que coinciden con las unidades UTF-16 de una cadena
 * de JavaScript: la interfaz los usa con `text.length`, `text[i]` y `slice`. Un `char` de Java llega como una
 * cadena de un carácter.
 */
import type { AlgorithmStatus } from './similarity'

/** Slug de cada uno de los 3 algoritmos, en el orden de `StringMatchingAlgorithm`. */
export type StringMatchingSlug = 'kmp' | 'boyer-moore' | 'rabin-karp'

/** Los 3 algoritmos con su etiqueta, en el orden de la respuesta: KMP, Boyer-Moore y Rabin-Karp (REQ-15). */
export const STRING_MATCHING_ALGORITHMS: readonly { slug: StringMatchingSlug; label: string }[] = [
  { slug: 'kmp', label: 'Knuth-Morris-Pratt (KMP)' },
  { slug: 'boyer-moore', label: 'Boyer-Moore' },
  { slug: 'rabin-karp', label: 'Rabin-Karp' },
]

/** q por defecto de Rabin-Karp cuando el campo se deja vacío (`MatchOptions.DEFAULT_Q`, REQ-6). */
export const DEFAULT_Q = 101

/** Artículo buscado con su abstract tal como quedó almacenado (`ArticleText`, REQ-15, REQ-20). */
export interface ArticleText {
  articleId: number
  title: string
  abstractText: string
}

/** Ocurrencias de un algoritmo en un artículo (`ArticleOccurrences`, REQ-15). */
export interface ArticleOccurrences {
  articleId: number
  title: string
  /** Posiciones iniciales base 0, ascendentes, con las solapadas. */
  positions: number[]
  /** Número de posiciones. */
  count: number
  /** Ventanas con hash igual y texto distinto; solo en `rabin-karp`, `null` en los demás. */
  collisions: number | null
}

/** Resultado de un algoritmo sobre el corpus (`AlgorithmOccurrences`, REQ-15, REQ-16, REQ-28, REQ-29). */
export interface AlgorithmOccurrences {
  algorithm: StringMatchingSlug
  label: string
  status: AlgorithmStatus
  /** `<archivo>#<método>` del stub si está PENDIENTE; `null` si no. */
  pendingLocation: string | null
  /** Tiempo en ms del único preprocesamiento del patrón; `null` si está PENDIENTE. */
  preprocessingMs: number | null
  /** Suma en ms de las búsquedas en cada abstract; `null` si está PENDIENTE. */
  searchMs: number | null
  /** Suma de `count` de todos los artículos; `null` si está PENDIENTE. */
  totalOccurrences: number | null
  /** Un elemento por artículo, en el orden de `articles` de la respuesta; vacía si está PENDIENTE. */
  articles: ArticleOccurrences[]
}

/** Respuesta de una búsqueda (`PatternSearchResponse`, REQ-15, REQ-16, REQ-18). */
export interface PatternSearchResponse {
  /** Patrón buscado, exactamente como llegó (sin recortar). */
  pattern: string
  q: number
  d: number
  /** Número de abstracts seleccionables; 0 con el corpus vacío. */
  articleCount: number
  /** `null` con menos de 2 algoritmos CALCULADO; si no, si todos dan las mismas posiciones en cada artículo. */
  allMatch: boolean | null
  /** Los 3 resultados en el orden de `STRING_MATCHING_ALGORITHMS`. */
  algorithms: AlgorithmOccurrences[]
  articles: ArticleText[]
}

/** Paso de la construcción de π para la posición i (`KmpPreprocessing.PrefixStep`, REQ-23). */
export interface KmpPrefixStep {
  i: number
  character: string
  /** Valores de k probados en orden: π[i − 1] y luego cada π[k − 1]. */
  candidates: number[]
  /** π[i]. */
  value: number
}

/** Preprocesamiento de KMP: la función de prefijo π (`KmpPreprocessing`, REQ-23). */
export interface KmpPreprocessing {
  pattern: string
  pi: number[]
  steps: KmpPrefixStep[]
}

/** Una comparación de la búsqueda de KMP (`KmpTrace.KmpComparison`, REQ-23). */
export interface KmpComparison {
  i: number
  j: number
  /** s = i − j, calculado por el backend. */
  shift: number
  textChar: string
  patternChar: string
  matched: boolean
  /** j nuevo tras j ← π[j − 1]; `null` si no hubo retroceso. */
  fallbackTo: number | null
  /** s de la ocurrencia que completa esta comparación; `null` si no completa ninguna. */
  occurrence: number | null
}

/** Traza de la búsqueda de KMP (`KmpTrace`, REQ-23). */
export interface KmpTrace {
  comparisons: KmpComparison[]
  occurrences: number[]
}

/** Entrada de la tabla del carácter malo (`LastOccurrenceEntry`, REQ-24). */
export interface LastOccurrenceEntry {
  character: string
  /** Última posición de `character` en el patrón. */
  index: number
}

/** Preprocesamiento de Boyer-Moore (`BoyerMoorePreprocessingView`, REQ-24). */
export interface BoyerMoorePreprocessing {
  pattern: string
  /**
   * Última posición de cada carácter del patrón, en orden de primera aparición; un ausente vale −1. Es una lista y
   * no un objeto porque un objeto JSON reordena las claves numéricas ("1", "2", …).
   */
  lastOccurrence: LastOccurrenceEntry[]
  /** Arreglo f de m + 1 posiciones del preprocesamiento de bordes. */
  borderPositions: number[]
  /** Salto por desajuste en la posición j (regla fuerte del sufijo bueno). */
  goodSuffixShift: number[]
  /** borde(P): longitud del borde propio más largo del patrón. */
  patternBorder: number
  /** m − borde(P): salto tras una ocurrencia completa. */
  matchShift: number
}

/** Comparación de un carácter en una alineación de Boyer-Moore (`BoyerMooreTrace.CharComparison`, REQ-24). */
export interface BoyerMooreCharComparison {
  j: number
  textIndex: number
  textChar: string
  patternChar: string
  matched: boolean
}

/** Una alineación de Boyer-Moore (`BoyerMooreTrace.Alignment`, REQ-24). */
export interface BoyerMooreAlignment {
  shift: number
  /** Comparaciones de derecha a izquierda. */
  comparisons: BoyerMooreCharComparison[]
  /** j del desajuste; `null` si fue ocurrencia. */
  mismatchIndex: number | null
  occurrence: boolean
  /** j − last(T[s + j]); `null` si fue ocurrencia. */
  badCharacterShift: number | null
  /** goodSuffixShift[j], o `matchShift` tras una ocurrencia. */
  goodSuffixShift: number | null
  /** max(las dos reglas, 1), o `matchShift` tras una ocurrencia. */
  appliedShift: number
}

/** Traza de la búsqueda de Boyer-Moore (`BoyerMooreTrace`, REQ-24). */
export interface BoyerMooreTrace {
  alignments: BoyerMooreAlignment[]
  occurrences: number[]
}

/** Preprocesamiento de Rabin-Karp (`RabinKarpPreprocessing`, REQ-25). */
export interface RabinKarpPreprocessing {
  pattern: string
  d: number
  q: number
  /** h = d^(m−1) mod q. */
  h: number
  patternHash: number
}

/** Una ventana de Rabin-Karp (`RabinKarpTrace.Window`, REQ-25). */
export interface RabinKarpWindow {
  shift: number
  hash: number
  hashMatches: boolean
  /** Resultado de la verificación carácter a carácter; `null` si el hash no coincidió. */
  verified: boolean | null
  /** Hash igual y texto distinto. */
  collision: boolean
}

/** Traza de la búsqueda de Rabin-Karp (`RabinKarpTrace`, REQ-25). */
export interface RabinKarpTrace {
  d: number
  q: number
  h: number
  patternHash: number
  /** Hash de la primera ventana; `null` si el texto es más corto que el patrón. */
  initialWindowHash: number | null
  windows: RabinKarpWindow[]
  collisions: number[]
  occurrences: number[]
}

/** Campos comunes de la respuesta de traza (`PatternTraceResponse`, REQ-22, REQ-28). */
interface PatternTraceBase {
  label: string
  articleId: number
  title: string
  abstractText: string
  pattern: string
  q: number
  status: AlgorithmStatus
  /** `<archivo>#<método>` del stub si está PENDIENTE; `null` si no. */
  pendingLocation: string | null
  /** `null` si está PENDIENTE. */
  positions: number[] | null
  /** Desplazamientos con colisión de Rabin-Karp; `null` en KMP y Boyer-Moore, y si está PENDIENTE. */
  collisions: number[] | null
}

/** Traza de KMP; `preprocessing` y `trace` son `null` si está PENDIENTE. */
export interface KmpTraceResponse extends PatternTraceBase {
  algorithm: 'kmp'
  preprocessing: KmpPreprocessing | null
  trace: KmpTrace | null
}

/** Traza de Boyer-Moore; `preprocessing` y `trace` son `null` si está PENDIENTE. */
export interface BoyerMooreTraceResponse extends PatternTraceBase {
  algorithm: 'boyer-moore'
  preprocessing: BoyerMoorePreprocessing | null
  trace: BoyerMooreTrace | null
}

/** Traza de Rabin-Karp; `preprocessing` y `trace` son `null` si está PENDIENTE. */
export interface RabinKarpTraceResponse extends PatternTraceBase {
  algorithm: 'rabin-karp'
  preprocessing: RabinKarpPreprocessing | null
  trace: RabinKarpTrace | null
}

/** Respuesta de `GET /api/pattern-search/trace`, discriminada por `algorithm` (REQ-22). */
export type PatternTraceResponse = KmpTraceResponse | BoyerMooreTraceResponse | RabinKarpTraceResponse
