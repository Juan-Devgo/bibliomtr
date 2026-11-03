/**
 * Respuestas de ejemplo de `GET /api/pattern-search` (spec 004) compartidas por los tests de la pestaña
 * "Búsqueda Patrones": los 3 algoritmos implementados, solo KMP implementado, el corpus sin abstracts y una
 * respuesta con posiciones que no son ocurrencias reales del patrón (la interfaz debe resaltar esas mismas,
 * REQ-21).
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
  PatternSearchResponse,
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
