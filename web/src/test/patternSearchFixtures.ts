/**
 * Respuestas de ejemplo de `GET /api/pattern-search` y `GET /api/pattern-search/trace` (spec 004) compartidas por
 * los tests de la pestaña "Búsqueda Patrones": los 3 algoritmos implementados, solo KMP implementado, el corpus sin
 * abstracts, una respuesta con posiciones que no son ocurrencias reales del patrón (la interfaz debe resaltar esas
 * mismas, REQ-21) y las trazas de los casos a mano del plan §6.
 *
 * Patrón "abab" con q = 101 sobre dos abstracts: "abababca" (caso a mano del plan §6: [0, 2]) y "abab abab"
 * ([0, 5]). Con q = 101 (h = 25, hash(P) = 48) las ventanas de "abababca" tienen hashes [48, 92, 48, 94, 21]
 * y las de "abab abab" [48, 27, 84, 29, 58, 48]: solo coinciden con hash(P) en las ocurrencias, así que
 * Rabin-Karp no tiene colisiones.
 */
import type {
  AlgorithmOccurrences,
  ArticleOccurrences,
  ArticleText,
  BoyerMooreTraceResponse,
  KmpTraceResponse,
  PatternSearchResponse,
  PatternTraceResponse,
  RabinKarpTraceResponse,
  StringMatchingSlug,
} from '@/types/patternSearch'
import { STRING_MATCHING_ALGORITHMS } from '@/types/patternSearch'

/** Directorio de los stubs de búsqueda de patrones, como lo informa `pendingLocation`. */
export const STRING_MATCHING_STUB_DIRECTORY = 'src/main/java/co/edu/uniquindio/bibliomtr/algorithms/stringmatching/'

/** Archivo del stub de cada algoritmo. */
const STUB_FILES: Record<StringMatchingSlug, string> = {
  kmp: 'KnuthMorrisPratt.java',
  'boyer-moore': 'BoyerMoore.java',
  'rabin-karp': 'RabinKarp.java',
}

/** Patrón de las respuestas de ejemplo. */
export const PATTERN = 'abab'

/** Los dos artículos buscados, en el orden de la respuesta. */
export const PATTERN_ARTICLES: ArticleText[] = [
  { articleId: 1, title: 'Búsqueda exacta en cadenas', abstractText: 'abababca' },
  { articleId: 2, title: 'Ocurrencias repetidas', abstractText: 'abab abab' },
]

/** Etiqueta de un algoritmo, la misma que devuelve la API. */
function labelOf(slug: StringMatchingSlug): string {
  return STRING_MATCHING_ALGORITHMS.find((algorithm) => algorithm.slug === slug)?.label ?? slug
}

/** `pendingLocation` del método `method` del stub de `slug`. */
export function pendingLocation(slug: StringMatchingSlug, method: 'preprocess' | 'search'): string {
  return `${STRING_MATCHING_STUB_DIRECTORY}${STUB_FILES[slug]}#${method}`
}

/**
 * Resultado CALCULADO de `slug` con las posiciones de cada artículo de `PATTERN_ARTICLES`, en orden; `collisions`
 * por artículo solo en Rabin-Karp.
 */
export function calculatedOccurrences(
  slug: StringMatchingSlug,
  positionsByArticle: number[][],
  times: { preprocessingMs: number; searchMs: number },
  collisionsByArticle: number[] = [],
): AlgorithmOccurrences {
  const articles: ArticleOccurrences[] = positionsByArticle.map((positions, index) => ({
    articleId: PATTERN_ARTICLES[index].articleId,
    title: PATTERN_ARTICLES[index].title,
    positions,
    count: positions.length,
    collisions: slug === 'rabin-karp' ? (collisionsByArticle[index] ?? 0) : null,
  }))
  return {
    algorithm: slug,
    label: labelOf(slug),
    status: 'CALCULADO',
    pendingLocation: null,
    preprocessingMs: times.preprocessingMs,
    searchMs: times.searchMs,
    totalOccurrences: articles.reduce((total, article) => total + article.count, 0),
    articles,
  }
}

/** Resultado PENDIENTE de `slug`: sin tiempos, total ni artículos. */
export function pendingOccurrences(slug: StringMatchingSlug, method: 'preprocess' | 'search'): AlgorithmOccurrences {
  return {
    algorithm: slug,
    label: labelOf(slug),
    status: 'PENDIENTE',
    pendingLocation: pendingLocation(slug, method),
    preprocessingMs: null,
    searchMs: null,
    totalOccurrences: null,
    articles: [],
  }
}

/** Posiciones reales de "abab" en los dos abstracts. */
const REAL_POSITIONS = [
  [0, 2],
  [0, 5],
]

/** Los 3 algoritmos implementados, con las mismas posiciones y `allMatch` verdadero. */
export const ALL_IMPLEMENTED: PatternSearchResponse = {
  pattern: PATTERN,
  q: 101,
  d: 65536,
  articleCount: 2,
  allMatch: true,
  algorithms: [
    calculatedOccurrences('kmp', REAL_POSITIONS, { preprocessingMs: 0.125, searchMs: 0.5 }),
    calculatedOccurrences('boyer-moore', REAL_POSITIONS, { preprocessingMs: 0.25, searchMs: 0.75 }),
    calculatedOccurrences('rabin-karp', REAL_POSITIONS, { preprocessingMs: 0.0625, searchMs: 1 }, [0, 0]),
  ],
  articles: PATTERN_ARTICLES,
}

/** Solo KMP implementado: Boyer-Moore y Rabin-Karp pendientes en `preprocess` y `allMatch` nulo (AC-20). */
export const ONLY_KMP: PatternSearchResponse = {
  ...ALL_IMPLEMENTED,
  allMatch: null,
  algorithms: [
    calculatedOccurrences('kmp', REAL_POSITIONS, { preprocessingMs: 0.125, searchMs: 0.5 }),
    pendingOccurrences('boyer-moore', 'preprocess'),
    pendingOccurrences('rabin-karp', 'preprocess'),
  ],
}

/** Corpus sin abstracts seleccionables: resultados vacíos sin error (REQ-18, AC-14). */
export const EMPTY_CORPUS: PatternSearchResponse = {
  pattern: PATTERN,
  q: 101,
  d: 65536,
  articleCount: 0,
  allMatch: true,
  algorithms: STRING_MATCHING_ALGORITHMS.map(({ slug }) =>
    calculatedOccurrences(slug, [], { preprocessingMs: 0.01, searchMs: 0 }),
  ),
  articles: [],
}

/**
 * Posiciones que no son ocurrencias reales de "abab": KMP y Boyer-Moore [1, 4] y [2], Rabin-Karp [3] y [];
 * `allMatch` falso. La interfaz resalta exactamente estas posiciones, porque no busca el patrón (REQ-21).
 */
export const FIXTURE_POSITIONS: PatternSearchResponse = {
  ...ALL_IMPLEMENTED,
  allMatch: false,
  algorithms: [
    calculatedOccurrences('kmp', [[1, 4], [2]], { preprocessingMs: 0.125, searchMs: 0.5 }),
    calculatedOccurrences('boyer-moore', [[1, 4], [2]], { preprocessingMs: 0.25, searchMs: 0.75 }),
    calculatedOccurrences('rabin-karp', [[3], []], { preprocessingMs: 0.0625, searchMs: 1 }, [2, 1]),
  ],
}

/** Artículo de las trazas de "abababca"/"abab": el primero de `PATTERN_ARTICLES`. */
const TRACE_ARTICLE = PATTERN_ARTICLES[0]

/** Artículo de la traza de colisión de Rabin-Karp: "abba" con el patrón "ab" y q = 3. */
export const COLLISION_ARTICLE: ArticleText = { articleId: 3, title: 'Colisión de hash', abstractText: 'abba' }

/**
 * Traza de KMP sobre "abababca"/"abab" (plan §6, REQ-23, AC-16): π = [0, 0, 1, 2] con un paso por i = 1 … 3 y
 * 9 comparaciones con (i, j, s) = (0,0,0) (1,1,0) (2,2,0) (3,3,0) (4,2,2) (5,3,2) (6,2,4) (6,0,6) (7,0,7);
 * retroceso a 2 tras las ocurrencias en s = 0 y s = 2 y a 0 tras el desajuste de 'c' con j = 2.
 */
export const KMP_TRACE_ABAB: KmpTraceResponse = {
  algorithm: 'kmp',
  label: 'Knuth-Morris-Pratt (KMP)',
  articleId: TRACE_ARTICLE.articleId,
  title: TRACE_ARTICLE.title,
  abstractText: TRACE_ARTICLE.abstractText,
  pattern: PATTERN,
  q: 101,
  status: 'CALCULADO',
  pendingLocation: null,
  positions: [0, 2],
  collisions: null,
  preprocessing: {
    pattern: PATTERN,
    pi: [0, 0, 1, 2],
    steps: [
      { i: 1, character: 'b', candidates: [0], value: 0 },
      { i: 2, character: 'a', candidates: [0], value: 1 },
      { i: 3, character: 'b', candidates: [1], value: 2 },
    ],
  },
  trace: {
    comparisons: [
      { i: 0, j: 0, shift: 0, textChar: 'a', patternChar: 'a', matched: true, fallbackTo: null, occurrence: null },
      { i: 1, j: 1, shift: 0, textChar: 'b', patternChar: 'b', matched: true, fallbackTo: null, occurrence: null },
      { i: 2, j: 2, shift: 0, textChar: 'a', patternChar: 'a', matched: true, fallbackTo: null, occurrence: null },
      { i: 3, j: 3, shift: 0, textChar: 'b', patternChar: 'b', matched: true, fallbackTo: 2, occurrence: 0 },
      { i: 4, j: 2, shift: 2, textChar: 'a', patternChar: 'a', matched: true, fallbackTo: null, occurrence: null },
      { i: 5, j: 3, shift: 2, textChar: 'b', patternChar: 'b', matched: true, fallbackTo: 2, occurrence: 2 },
      { i: 6, j: 2, shift: 4, textChar: 'c', patternChar: 'a', matched: false, fallbackTo: 0, occurrence: null },
      { i: 6, j: 0, shift: 6, textChar: 'c', patternChar: 'a', matched: false, fallbackTo: null, occurrence: null },
      { i: 7, j: 0, shift: 7, textChar: 'a', patternChar: 'a', matched: true, fallbackTo: null, occurrence: null },
    ],
    occurrences: [0, 2],
  },
}

/**
 * Traza de Boyer-Moore sobre "abababca"/"abab" (plan §6, REQ-24, AC-17, AC-24): last = {a: 2, b: 3},
 * f = [2, 3, 4, 4, 5], sufijo bueno [2, 2, 4, 1], borde(P) = 2 y salto tras ocurrencia 2; alineaciones s = 0
 * (ocurrencia, salto 2), s = 2 (ocurrencia, salto 2) y s = 4 (desajuste en j = 3 con 'a': carácter malo
 * 3 − 2 = 1, sufijo bueno 1, salto 1).
 */
export const BOYER_MOORE_TRACE_ABAB: BoyerMooreTraceResponse = {
  algorithm: 'boyer-moore',
  label: 'Boyer-Moore',
  articleId: TRACE_ARTICLE.articleId,
  title: TRACE_ARTICLE.title,
  abstractText: TRACE_ARTICLE.abstractText,
  pattern: PATTERN,
  q: 101,
  status: 'CALCULADO',
  pendingLocation: null,
  positions: [0, 2],
  collisions: null,
  preprocessing: {
    pattern: PATTERN,
    lastOccurrence: [
      { character: 'a', index: 2 },
      { character: 'b', index: 3 },
    ],
    borderPositions: [2, 3, 4, 4, 5],
    goodSuffixShift: [2, 2, 4, 1],
    patternBorder: 2,
    matchShift: 2,
  },
  trace: {
    alignments: [
      {
        shift: 0,
        comparisons: [
          { j: 3, textIndex: 3, textChar: 'b', patternChar: 'b', matched: true },
          { j: 2, textIndex: 2, textChar: 'a', patternChar: 'a', matched: true },
          { j: 1, textIndex: 1, textChar: 'b', patternChar: 'b', matched: true },
          { j: 0, textIndex: 0, textChar: 'a', patternChar: 'a', matched: true },
        ],
        mismatchIndex: null,
        occurrence: true,
        badCharacterShift: null,
        goodSuffixShift: 2,
        appliedShift: 2,
      },
      {
        shift: 2,
        comparisons: [
          { j: 3, textIndex: 5, textChar: 'b', patternChar: 'b', matched: true },
          { j: 2, textIndex: 4, textChar: 'a', patternChar: 'a', matched: true },
          { j: 1, textIndex: 3, textChar: 'b', patternChar: 'b', matched: true },
          { j: 0, textIndex: 2, textChar: 'a', patternChar: 'a', matched: true },
        ],
        mismatchIndex: null,
        occurrence: true,
        badCharacterShift: null,
        goodSuffixShift: 2,
        appliedShift: 2,
      },
      {
        shift: 4,
        comparisons: [{ j: 3, textIndex: 7, textChar: 'a', patternChar: 'b', matched: false }],
        mismatchIndex: 3,
        occurrence: false,
        badCharacterShift: 1,
        goodSuffixShift: 1,
        appliedShift: 1,
      },
    ],
    occurrences: [0, 2],
  },
}

/**
 * Traza de Rabin-Karp sobre "abababca"/"abab" con q = 101 (plan §6, REQ-25): d = 65 536, h = 25, hash(P) = 48,
 * t(0) = 48 y ventanas s = 0 … 4 con hashes [48, 92, 48, 94, 21]; coincidencias de hash en s = 0 y 2, ambas
 * verificadas; sin colisiones.
 */
export const RABIN_KARP_TRACE_ABAB: RabinKarpTraceResponse = {
  algorithm: 'rabin-karp',
  label: 'Rabin-Karp',
  articleId: TRACE_ARTICLE.articleId,
  title: TRACE_ARTICLE.title,
  abstractText: TRACE_ARTICLE.abstractText,
  pattern: PATTERN,
  q: 101,
  status: 'CALCULADO',
  pendingLocation: null,
  positions: [0, 2],
  collisions: [],
  preprocessing: { pattern: PATTERN, d: 65536, q: 101, h: 25, patternHash: 48 },
  trace: {
    d: 65536,
    q: 101,
    h: 25,
    patternHash: 48,
    initialWindowHash: 48,
    windows: [
      { shift: 0, hash: 48, hashMatches: true, verified: true, collision: false },
      { shift: 1, hash: 92, hashMatches: false, verified: null, collision: false },
      { shift: 2, hash: 48, hashMatches: true, verified: true, collision: false },
      { shift: 3, hash: 94, hashMatches: false, verified: null, collision: false },
      { shift: 4, hash: 21, hashMatches: false, verified: null, collision: false },
    ],
    collisions: [],
    occurrences: [0, 2],
  },
}

/**
 * Traza de colisión de Rabin-Karp sobre "abba"/"ab" con q = 3 (plan §6, REQ-10, REQ-25, AC-18): h = 1,
 * hash(P) = 0, ventanas [0, 1, 0]; s = 2 ("ba") tiene el hash del patrón con texto distinto, así que es una
 * colisión y no una ocurrencia: posiciones [0], colisiones [2].
 */
export const RABIN_KARP_COLLISION_TRACE: RabinKarpTraceResponse = {
  algorithm: 'rabin-karp',
  label: 'Rabin-Karp',
  articleId: COLLISION_ARTICLE.articleId,
  title: COLLISION_ARTICLE.title,
  abstractText: COLLISION_ARTICLE.abstractText,
  pattern: 'ab',
  q: 3,
  status: 'CALCULADO',
  pendingLocation: null,
  positions: [0],
  collisions: [2],
  preprocessing: { pattern: 'ab', d: 65536, q: 3, h: 1, patternHash: 0 },
  trace: {
    d: 65536,
    q: 3,
    h: 1,
    patternHash: 0,
    initialWindowHash: 0,
    windows: [
      { shift: 0, hash: 0, hashMatches: true, verified: true, collision: false },
      { shift: 1, hash: 1, hashMatches: false, verified: null, collision: false },
      { shift: 2, hash: 0, hashMatches: true, verified: false, collision: true },
    ],
    collisions: [2],
    occurrences: [0],
  },
}

/** Traza PENDIENTE de Boyer-Moore: 200 sin posiciones, colisiones, preprocesamiento ni traza (REQ-28, AC-26). */
export const PENDING_BOYER_MOORE_TRACE: PatternTraceResponse = {
  ...BOYER_MOORE_TRACE_ABAB,
  status: 'PENDIENTE',
  pendingLocation: pendingLocation('boyer-moore', 'preprocess'),
  positions: null,
  collisions: null,
  preprocessing: null,
  trace: null,
}
