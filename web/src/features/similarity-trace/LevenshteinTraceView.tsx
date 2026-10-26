import Typography from '@mui/material/Typography'
import PagedArray from '@/components/PagedArray'
import PagedMatrix from '@/components/PagedMatrix'
import TraceStep from '@/components/TraceStep'
import type { LevenshteinTrace } from '@/types/similarity'
import StepList from './StepList'
import { decimal, dpLabels, symbolLabel } from './traceFormat'

interface LevenshteinTraceViewProps {
  trace: LevenshteinTrace
  /** Similitud en [0,1] que devolvió el algoritmo. */
  similarity: number
}

/**
 * Traza de Levenshtein paso a paso (REQ-16, REQ-17, AC-15): secuencias, matriz D con `PagedMatrix`, distancia
 * y similitud, cada paso con su fórmula en KaTeX (constitución P6, P7). La similitud usa la normalización
 * sugerida del stub. Complejidad del algoritmo trazado: la declara el estudiante en el stub.
 *
 * Complejidad por render: O(min(m, 25)·min(n, 25) + min(m, 50) + min(n, 50)).
 */
function LevenshteinTraceView({ trace, similarity }: LevenshteinTraceViewProps) {
  const m = trace.a.length
  const n = trace.b.length
  const similarityFormula =
    trace.maxLength === 0
      ? String.raw`\text{sim} = 1 \quad (m = n = 0)`
      : String.raw`\text{sim} = 1 - \frac{d}{\max(m, n)} = 1 - \frac{${trace.distance}}{${trace.maxLength}} = ${decimal(similarity)}`

  return (
    <StepList>
      <TraceStep
        number={1}
        title="Secuencias"
        formula={String.raw`a = (a_1, \dots, a_m),\ m = ${m};\qquad b = (b_1, \dots, b_n),\ n = ${n}`}
      >
        <PagedArray items={trace.a} renderItem={symbolLabel} label="Secuencia a" />
        <PagedArray items={trace.b} renderItem={symbolLabel} label="Secuencia b" />
      </TraceStep>
      <TraceStep
        number={2}
        title="Matriz de distancias D"
        formula={String.raw`D_{i,0} = i,\quad D_{0,j} = j,\quad D_{i,j} = \min\big(D_{i-1,j} + 1,\ D_{i,j-1} + 1,\ D_{i-1,j-1} + [a_i \ne b_j]\big)`}
      >
        <Typography variant="body2" color="text.secondary">
          D tiene (m + 1)×(n + 1) celdas; la fila y la columna ε corresponden al prefijo vacío.
        </Typography>
        <PagedMatrix matrix={trace.matrix} rowLabels={dpLabels(trace.a)} colLabels={dpLabels(trace.b)} label="Matriz D" />
      </TraceStep>
      <TraceStep number={3} title="Distancia" formula={String.raw`d = D_{m,n} = ${trace.distance}`}>
        <Typography variant="body2">Número mínimo de inserciones, borrados y sustituciones.</Typography>
      </TraceStep>
      <TraceStep number={4} title="Similitud" formula={similarityFormula}>
        <Typography variant="body2">{`Similitud en [0,1]: ${decimal(similarity)}`}</Typography>
      </TraceStep>
    </StepList>
  )
}

export default LevenshteinTraceView
