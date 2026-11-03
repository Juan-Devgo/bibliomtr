/**
 * Fórmulas en LaTeX de la traza del clustering jerárquico (spec 003, REQ-17); KaTeX solo las dibuja
 * (constitución P2, P6). Son las mismas del Javadoc de cada stub y de la guía.
 */
import type { ClusteringAlgorithmSlug } from '@/types/clustering'

/** Distancia de entrada a partir de la similitud de la fuente (REQ-10). */
export const INPUT_DISTANCE_FORMULA = String.raw`d(i, j) = 1 - s(i, j), \qquad d(i, i) = 0`

/** Recálculo de Lance-Williams del grupo nuevo i ∪ j frente a otro grupo h, por algoritmo aglomerativo. */
export const LINKAGE_FORMULAS: Record<Exclude<ClusteringAlgorithmSlug, 'diana'>, string> = {
  'single-linkage': String.raw`d(i \cup j,\, h) = \min\big(d(i, h),\ d(j, h)\big)`,
  'complete-linkage': String.raw`d(i \cup j,\, h) = \max\big(d(i, h),\ d(j, h)\big)`,
  'average-linkage': String.raw`d(i \cup j,\, h) = \frac{|i|\, d(i, h) + |j|\, d(j, h)}{|i| + |j|}`,
}

/** Elección del par: la menor distancia entre grupos activos; empate, el par (p, q) lexicográficamente menor. */
export const PAIR_FORMULA = String.raw`(p, q) = \operatorname*{arg\,min}_{p < q} d(p, q)`

/** Diámetro de un grupo y diferencia D(i) que decide qué artículo pasa al grupo disidente S en DIANA. */
export const DIANA_FORMULA = String.raw`\begin{gathered}
\operatorname{diam}(C) = \max_{a, b \in C} d(a, b) \\
D(i) = \frac{1}{|R| - 1} \sum_{j \in R \setminus \{i\}} d(i, j) \;-\; \frac{1}{|S|} \sum_{j \in S} d(i, j)
\end{gathered}`
