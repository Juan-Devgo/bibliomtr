/**
 * Datos de prueba de la spec 001 compartidos por los tests de la interfaz de similitud: tres artículos y una
 * comparación con los 8 resultados calculados, simétricos y en [0,1].
 */
import {
  SIMILARITY_ALGORITHM_LABELS,
  SIMILARITY_ALGORITHMS,
  type AlgorithmMatrix,
  type ArticleSummary,
  type ComparisonResponse,
  type SimilarityAlgorithmSlug,
} from '@/types/similarity'

/** Directorio de los stubs, como lo informa `pendingLocation`. */
export const STUB_DIRECTORY = 'src/main/java/co/edu/uniquindio/bibliomtr/algorithms/similarity/'

/** Tres artículos seleccionables. */
export const ARTICLES: ArticleSummary[] = [
  { id: 1, title: 'Generative AI in education', authors: ['Ana Gómez'], fileName: '1. a.pdf', specter2Truncated: false },
  { id: 2, title: 'Learning analytics review', authors: ['Luis Pérez'], fileName: '2. b.pdf', specter2Truncated: false },
  { id: 3, title: 'Chatbots for tutoring', authors: [], fileName: '3. c.pdf', specter2Truncated: false },
]

/** Matriz 3×3 simétrica con diagonal 1 y valores fuera de la diagonal que dependen de `k`. */
function matrix(k: number): number[][] {
  const ab = 0.1 * k
  const ac = 0.05 * k
  const bc = 0.02 * k
  return [
    [1, ab, ac],
    [ab, 1, bc],
    [ac, bc, 1],
  ]
}

/** Resultado CALCULADO del algoritmo `slug`, con tiempo `elapsedMs`. */
export function calculated(slug: SimilarityAlgorithmSlug, k: number, elapsedMs: number): AlgorithmMatrix {
  return {
    algorithm: slug,
    label: SIMILARITY_ALGORITHM_LABELS[slug],
    status: 'CALCULADO',
    matrix: matrix(k),
    elapsedMs,
    pendingLocation: null,
  }
}

/** Resultado PENDIENTE del algoritmo `slug`, cuyo stub es `stubClass`. */
export function pending(slug: SimilarityAlgorithmSlug, stubClass: string): AlgorithmMatrix {
  return {
    algorithm: slug,
    label: SIMILARITY_ALGORITHM_LABELS[slug],
    status: 'PENDIENTE',
    matrix: null,
    elapsedMs: null,
    pendingLocation: `${STUB_DIRECTORY}${stubClass}.java#compare`,
  }
}

/** Comparación de los 3 artículos con los 8 resultados calculados; el resultado i tarda (i + 1)·0.5 ms. */
export const ALL_CALCULATED: ComparisonResponse = {
  articles: ARTICLES,
  editGranularity: 'WORDS',
  preprocessingMs: 1.25,
  results: SIMILARITY_ALGORITHMS.map((slug, i) => calculated(slug, i + 1, (i + 1) * 0.5)),
}
