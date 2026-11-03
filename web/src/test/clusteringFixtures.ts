/**
 * Datos de prueba de la spec 003 compartidos por los tests de la interfaz de clustering: los 4 artículos y la
 * matriz D₄ del caso a mano de la guía, con los dendrogramas esperados de cada algoritmo y las trazas de Single
 * Linkage y DIANA.
 */
import {
  CLUSTERING_ALGORITHM_LABELS,
  type ClusteringAlgorithmSlug,
  type ClusteringResponse,
  type ClusteringTraceResponse,
  type DendrogramResult,
  type DianaTrace,
  type LinkageTrace,
  type Merge,
} from '@/types/clustering'
import type { ArticleSummary } from '@/types/similarity'

/** Directorio de los stubs de clustering, como lo informa `pendingLocation`. */
export const CLUSTERING_STUB_DIRECTORY = 'src/main/java/co/edu/uniquindio/bibliomtr/algorithms/clustering/'

/** Cuatro artículos; el último con un título de más de 40 caracteres. */
export const CLUSTERING_ARTICLES: ArticleSummary[] = [
  { id: 11, title: 'Generative AI in education', authors: ['Ana Gómez'], fileName: '1. a.pdf', specter2Truncated: false },
  { id: 12, title: 'Learning analytics review', authors: ['Luis Pérez'], fileName: '2. b.pdf', specter2Truncated: false },
  { id: 13, title: 'Chatbots for tutoring', authors: [], fileName: '3. c.pdf', specter2Truncated: false },
  {
    id: 14,
    title: 'Large language models as teaching assistants in programming courses',
    authors: [],
    fileName: '4. d.pdf',
    specter2Truncated: false,
  },
]

/** Matriz D₄ del caso a mano (plan §6). */
export const D4: number[][] = [
  [0, 0.2, 0.5, 0.7],
  [0.2, 0, 0.3, 0.6],
  [0.5, 0.3, 0, 0.45],
  [0.7, 0.6, 0.45, 0],
]

/** Fusión `(left, right, height, size)`. */
function merge(left: number, right: number, height: number, size: number): Merge {
  return { left, right, height, size }
}

/** Fusiones esperadas sobre D₄ de cada algoritmo (plan §6). */
export const D4_MERGES: Record<ClusteringAlgorithmSlug, Merge[]> = {
  'single-linkage': [merge(0, 1, 0.2, 2), merge(2, 4, 0.3, 3), merge(3, 5, 0.45, 4)],
  'complete-linkage': [merge(0, 1, 0.2, 2), merge(2, 3, 0.45, 2), merge(4, 5, 0.7, 4)],
  'average-linkage': [merge(0, 1, 0.2, 2), merge(2, 4, 0.4, 3), merge(3, 5, 1.75 / 3, 4)],
  diana: [merge(0, 1, 0.2, 2), merge(2, 4, 0.5, 3), merge(3, 5, 0.7, 4)],
}

/** Resultado CALCULADO del algoritmo `slug` sobre D₄, con tiempo `elapsedMs`. */
export function calculatedDendrogram(slug: ClusteringAlgorithmSlug, elapsedMs: number): DendrogramResult {
  return {
    algorithm: slug,
    label: CLUSTERING_ALGORITHM_LABELS[slug],
    status: 'CALCULADO',
    merges: D4_MERGES[slug],
    elapsedMs,
    pendingLocation: null,
  }
}

/** Resultado PENDIENTE del algoritmo `slug`, con el stub `stubClass`. */
export function pendingDendrogram(slug: ClusteringAlgorithmSlug, stubClass: string): DendrogramResult {
  return {
    algorithm: slug,
    label: CLUSTERING_ALGORITHM_LABELS[slug],
    status: 'PENDIENTE',
    merges: null,
    elapsedMs: null,
    pendingLocation: `${CLUSTERING_STUB_DIRECTORY}${stubClass}.java#cluster`,
  }
}

/** Clustering de los 4 artículos con SPECTER2 (coseno) y los 4 algoritmos calculados. */
export const CLUSTERING_D4: ClusteringResponse = {
  articles: CLUSTERING_ARTICLES,
  source: { source: 'specter2-cosine', label: 'SPECTER2 (coseno)', status: 'CALCULADO', pendingLocation: null },
  preparationMs: 2.5,
  distances: D4,
  results: [
    calculatedDendrogram('single-linkage', 0.25),
    calculatedDendrogram('complete-linkage', 0.5),
    calculatedDendrogram('average-linkage', 0.75),
    calculatedDendrogram('diana', 1),
  ],
}

/** Traza de Single Linkage sobre D₄ (plan §6). */
export const SINGLE_TRACE_D4: LinkageTrace = {
  steps: [
    {
      clusters: [0, 1, 2, 3],
      members: [[0], [1], [2], [3]],
      distances: D4,
      left: 0,
      right: 1,
      distance: 0.2,
      newCluster: 4,
      remaining: [2, 3],
      updatedRow: [0.3, 0.6],
    },
    {
      clusters: [2, 3, 4],
      members: [[2], [3], [0, 1]],
      distances: [
        [0, 0.45, 0.3],
        [0.45, 0, 0.6],
        [0.3, 0.6, 0],
      ],
      left: 2,
      right: 4,
      distance: 0.3,
      newCluster: 5,
      remaining: [3],
      updatedRow: [0.45],
    },
    {
      clusters: [3, 5],
      members: [[3], [0, 1, 2]],
      distances: [
        [0, 0.45],
        [0.45, 0],
      ],
      left: 3,
      right: 5,
      distance: 0.45,
      newCluster: 6,
      remaining: [],
      updatedRow: [],
    },
  ],
}

/** Traza de DIANA sobre D₄ (plan §6). */
export const DIANA_TRACE_D4: DianaTrace = {
  steps: [
    {
      clusters: [[0, 1, 2, 3]],
      diameters: [0.7],
      splitIndex: 0,
      diameter: 0.7,
      iterations: [
        {
          splinter: [],
          remainder: [0, 1, 2, 3],
          averageToRemainder: [1.4 / 3, 1.1 / 3, 1.25 / 3, 1.75 / 3],
          averageToSplinter: [0, 0, 0, 0],
          differences: [1.4 / 3, 1.1 / 3, 1.25 / 3, 1.75 / 3],
          moved: 3,
        },
        {
          splinter: [3],
          remainder: [0, 1, 2],
          averageToRemainder: [0.35, 0.25, 0.4],
          averageToSplinter: [0.7, 0.6, 0.45],
          differences: [-0.35, -0.35, -0.05],
          moved: -1,
        },
      ],
      splinter: [3],
      remainder: [0, 1, 2],
    },
    {
      clusters: [[0, 1, 2], [3]],
      diameters: [0.5, 0],
      splitIndex: 0,
      diameter: 0.5,
      iterations: [
        {
          splinter: [],
          remainder: [0, 1, 2],
          averageToRemainder: [0.35, 0.25, 0.4],
          averageToSplinter: [0, 0, 0],
          differences: [0.35, 0.25, 0.4],
          moved: 2,
        },
        {
          splinter: [2],
          remainder: [0, 1],
          averageToRemainder: [0.2, 0.2],
          averageToSplinter: [0.5, 0.3],
          differences: [-0.3, -0.1],
          moved: -1,
        },
      ],
      splinter: [2],
      remainder: [0, 1],
    },
    {
      clusters: [[0, 1], [2], [3]],
      diameters: [0.2, 0, 0],
      splitIndex: 0,
      diameter: 0.2,
      iterations: [
        {
          splinter: [],
          remainder: [0, 1],
          averageToRemainder: [0.2, 0.2],
          averageToSplinter: [0, 0],
          differences: [0.2, 0.2],
          moved: 0,
        },
      ],
      splinter: [0],
      remainder: [1],
    },
  ],
}

/** Respuesta de `GET /api/clustering/trace` de `algorithm` sobre D₄ con su traza. */
export function traceResponse(algorithm: ClusteringAlgorithmSlug, trace: LinkageTrace | DianaTrace): ClusteringTraceResponse {
  return {
    algorithm,
    label: CLUSTERING_ALGORITHM_LABELS[algorithm],
    articles: CLUSTERING_ARTICLES,
    source: CLUSTERING_D4.source,
    preparationMs: 2.5,
    distances: D4,
    status: 'CALCULADO',
    merges: D4_MERGES[algorithm],
    trace,
    elapsedMs: 0.125,
    pendingLocation: null,
  } as ClusteringTraceResponse
}
