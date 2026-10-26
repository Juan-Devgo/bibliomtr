import Typography from '@mui/material/Typography'
import PagedArray from '@/components/PagedArray'
import TraceStep from '@/components/TraceStep'
import type { EuclideanTrace } from '@/types/similarity'
import StepList from './StepList'
import { decimal } from './traceFormat'

interface EuclideanTraceViewProps {
  trace: EuclideanTrace
  /** Distancia euclidiana que devolvió el algoritmo. */
  value: number
  /** Similitud en [0,1] que devolvió el algoritmo. */
  similarity: number
}

/**
 * Traza de la distancia euclidiana entre embeddings paso a paso (REQ-16, REQ-17, AC-15): diferencias al
 * cuadrado con `PagedArray`, suma, distancia y similitud, cada paso con su fórmula en KaTeX (constitución P6,
 * P7).
 *
 * Complejidad por render: O(min(d, 50)), con d = dimensión de los vectores (768).
 */
function EuclideanTraceView({ trace, value, similarity }: EuclideanTraceViewProps) {
  return (
    <StepList>
      <TraceStep
        number={1}
        title="Diferencias al cuadrado"
        formula={String.raw`q_i = (a_i - b_i)^2,\quad i = 1, \dots, d,\quad d = ${trace.dimension}`}
      >
        <PagedArray items={trace.squaredDifferences} renderItem={decimal} label="Diferencias al cuadrado q" />
      </TraceStep>
      <TraceStep number={2} title="Suma" formula={String.raw`S = \sum_{i=1}^{d} q_i = ${decimal(trace.sumOfSquares)}`}>
        <Typography variant="body2" color="text.secondary">
          Suma de las diferencias al cuadrado.
        </Typography>
      </TraceStep>
      <TraceStep number={3} title="Distancia" formula={String.raw`d(a, b) = \sqrt{S} = ${decimal(value)}`}>
        <Typography variant="body2">{`Distancia: ${decimal(value)}`}</Typography>
      </TraceStep>
      <TraceStep
        number={4}
        title="Similitud"
        formula={String.raw`\text{sim} = \frac{1}{1 + d(a, b)} = ${decimal(similarity)}`}
      >
        <Typography variant="body2">{`Similitud en [0,1]: ${decimal(similarity)}`}</Typography>
      </TraceStep>
    </StepList>
  )
}

export default EuclideanTraceView
