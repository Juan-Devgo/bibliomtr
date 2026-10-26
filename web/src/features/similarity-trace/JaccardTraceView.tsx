import Typography from '@mui/material/Typography'
import PagedArray from '@/components/PagedArray'
import TraceStep from '@/components/TraceStep'
import type { JaccardTrace } from '@/types/similarity'
import StepList from './StepList'
import { decimal, symbolLabel } from './traceFormat'

interface JaccardTraceViewProps {
  trace: JaccardTrace
  /** Índice de Jaccard en [0,1] que devolvió el algoritmo. */
  similarity: number
}

/**
 * Traza de Jaccard paso a paso (REQ-16, REQ-17, AC-15): conjuntos, intersección, unión e índice J, cada paso
 * con su fórmula en KaTeX (constitución P6, P7). Los conjuntos van en orden de primera aparición.
 *
 * Complejidad por render: O(min(|A ∪ B|, 50)).
 */
function JaccardTraceView({ trace, similarity }: JaccardTraceViewProps) {
  const intersection = trace.intersection.length
  const union = trace.union.length
  const jaccardFormula =
    union === 0
      ? String.raw`J = 1 \quad (A = B = \emptyset)`
      : String.raw`J = \frac{|A \cap B|}{|A \cup B|} = \frac{${intersection}}{${union}} = ${decimal(similarity)}`

  return (
    <StepList>
      <TraceStep
        number={1}
        title="Conjuntos"
        formula={String.raw`A = \{\, a_i \,\},\ |A| = ${trace.setA.length};\qquad B = \{\, b_j \,\},\ |B| = ${trace.setB.length}`}
      >
        <PagedArray items={trace.setA} renderItem={symbolLabel} label="Conjunto A" />
        <PagedArray items={trace.setB} renderItem={symbolLabel} label="Conjunto B" />
      </TraceStep>
      <TraceStep number={2} title="Intersección" formula={String.raw`|A \cap B| = ${intersection}`}>
        <PagedArray items={trace.intersection} renderItem={symbolLabel} label="Intersección A ∩ B" />
      </TraceStep>
      <TraceStep number={3} title="Unión" formula={String.raw`|A \cup B| = ${union}`}>
        <PagedArray items={trace.union} renderItem={symbolLabel} label="Unión A ∪ B" />
      </TraceStep>
      <TraceStep number={4} title="Índice de Jaccard" formula={jaccardFormula}>
        <Typography variant="body2">{`Similitud en [0,1]: ${decimal(similarity)}`}</Typography>
      </TraceStep>
    </StepList>
  )
}

export default JaccardTraceView
