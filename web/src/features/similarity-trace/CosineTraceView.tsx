import Typography from '@mui/material/Typography'
import PagedArray from '@/components/PagedArray'
import TraceStep from '@/components/TraceStep'
import type { CosineTrace } from '@/types/similarity'
import StepList from './StepList'
import { decimal } from './traceFormat'

interface CosineTraceViewProps {
  trace: CosineTrace
  /** Coseno crudo en [−1, 1] que devolvió el algoritmo. */
  value: number
  /** Similitud en [0,1] que devolvió el algoritmo. */
  similarity: number
}

/**
 * Traza de la similitud coseno entre embeddings paso a paso (REQ-16, REQ-17, AC-15): productos por componente
 * con `PagedArray`, producto punto, normas, coseno y similitud, cada paso con su fórmula en KaTeX
 * (constitución P6, P7).
 *
 * Complejidad por render: O(min(d, 50)), con d = dimensión de los vectores (768).
 */
function CosineTraceView({ trace, value, similarity }: CosineTraceViewProps) {
  return (
    <StepList>
      <TraceStep
        number={1}
        title="Productos por componente"
        formula={String.raw`p_i = a_i\,b_i,\quad i = 1, \dots, d,\quad d = ${trace.dimension}`}
      >
        <PagedArray items={trace.products} renderItem={decimal} label="Productos p" />
      </TraceStep>
      <TraceStep
        number={2}
        title="Producto punto"
        formula={String.raw`a \cdot b = \sum_{i=1}^{d} p_i = ${decimal(trace.dotProduct)}`}
      >
        <Typography variant="body2" color="text.secondary">
          Suma de los productos por componente.
        </Typography>
      </TraceStep>
      <TraceStep
        number={3}
        title="Normas"
        formula={String.raw`\lVert a \rVert = \sqrt{\textstyle\sum_i a_i^2} = ${decimal(trace.normA)},\qquad \lVert b \rVert = \sqrt{\textstyle\sum_i b_i^2} = ${decimal(trace.normB)}`}
      >
        <Typography variant="body2" color="text.secondary">
          Normas euclidianas de los dos embeddings.
        </Typography>
      </TraceStep>
      <TraceStep
        number={4}
        title="Coseno"
        formula={String.raw`\cos(a, b) = \frac{a \cdot b}{\lVert a \rVert\,\lVert b \rVert} = ${decimal(value)}`}
      >
        <Typography variant="body2">{`Coseno en [−1, 1]: ${decimal(value)}`}</Typography>
      </TraceStep>
      <TraceStep
        number={5}
        title="Similitud"
        formula={String.raw`\text{sim} = \frac{1 + \cos(a, b)}{2} = ${decimal(similarity)}`}
      >
        <Typography variant="body2">{`Similitud en [0,1]: ${decimal(similarity)}`}</Typography>
      </TraceStep>
    </StepList>
  )
}

export default CosineTraceView
