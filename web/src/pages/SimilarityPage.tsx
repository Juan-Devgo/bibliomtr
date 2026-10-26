import Divider from '@mui/material/Divider'
import Stack from '@mui/material/Stack'
import Typography from '@mui/material/Typography'
import { useRef, useState } from 'react'
import { useComparison } from '@/api/useComparison'
import ArticleSelector from '@/features/article-selection/ArticleSelector'
import SimilarityResults from '@/features/similarity-matrix/SimilarityResults'
import SimilarityTraceView, { type TraceSelection } from '@/features/similarity-trace/SimilarityTraceView'
import type { SimilarityAlgorithmSlug } from '@/types/similarity'

/** Selección de traza vacía. */
const NO_SELECTION: TraceSelection = { algorithm: null, a: null, b: null }

/**
 * Página del Requerimiento 1: similitud textual con algoritmos clásicos y embeddings (spec 001, REQ-17).
 *
 * Reúne el selector de artículos, las 8 matrices de similitud con sus tiempos y el visor de traza. Un clic en
 * una celda de un mapa de calor elige ese algoritmo y ese par en el visor y lo desplaza a la vista. Cada nueva
 * comparación conserva el algoritmo elegido y limpia el par, porque los artículos pueden cambiar.
 */
function SimilarityPage() {
  const comparison = useComparison()
  const [selection, setSelection] = useState<TraceSelection>(NO_SELECTION)
  const traceRef = useRef<HTMLDivElement>(null)
  const response = comparison.data

  const compare = (articleIds: number[]) => {
    comparison.mutate(articleIds, {
      onSuccess: () => setSelection((current) => ({ ...current, a: null, b: null })),
    })
  }

  const selectCell = (algorithm: SimilarityAlgorithmSlug, row: number, column: number) => {
    if (!response) {
      return
    }
    setSelection({ algorithm, a: response.articles[row].id, b: response.articles[column].id })
    traceRef.current?.scrollIntoView?.({ behavior: 'smooth', block: 'start' })
  }

  return (
    <Stack component="main" spacing={3} sx={{ mx: 'auto', maxWidth: 1200, p: 3 }}>
      <Typography variant="h4" component="h1">
        Requerimiento 1: Similitud textual
      </Typography>
      <Typography color="text.secondary">
        Compara los abstracts de 2 a 20 artículos con 4 algoritmos clásicos (Levenshtein, Needleman-Wunsch, coseno
        TF-IDF y Jaccard) y con los embeddings de SPECTER2 y nomic-embed-text (coseno y euclidiana).
      </Typography>
      <ArticleSelector onCompare={compare} comparing={comparison.isPending} error={comparison.error} />
      {response && (
        <>
          <Divider />
          <SimilarityResults response={response} onSelectCell={selectCell} />
          <Divider />
          <div ref={traceRef}>
            <SimilarityTraceView articles={response.articles} value={selection} onChange={setSelection} />
          </div>
        </>
      )}
    </Stack>
  )
}

export default SimilarityPage
