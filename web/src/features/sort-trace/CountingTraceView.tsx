import Typography from '@mui/material/Typography'
import PagedArray from '@/components/PagedArray'
import TraceStep from '@/components/TraceStep'
import type { CountingPlacement, CountingSortTrace, TermFrequency } from '@/types/ranking'
import { numberLabel, termLabel } from './traceFormat'

interface CountingTraceViewProps {
  trace: CountingSortTrace
  /** Salida leída de derecha a izquierda (REQ-2). */
  ranking: readonly TermFrequency[]
}

/** Texto de una colocación: el elemento A[i] va a la posición B[j]. */
function placementLabel(placement: CountingPlacement): string {
  return `A[${placement.inputIndex}] = ${termLabel(placement.element)} → B[${placement.outputIndex}]`
}

/**
 * Traza de Counting Sort paso a paso (REQ-13, REQ-16): entrada, máximo k, conteo C, acumulado C,
 * colocaciones de derecha a izquierda, salida B y ranking. Cada paso muestra su fórmula y todo arreglo
 * pasa por `PagedArray`. Complejidad del algoritmo trazado: O(n + k) en tiempo y espacio.
 */
function CountingTraceView({ trace, ranking }: CountingTraceViewProps) {
  return (
    <ol style={{ display: 'flex', flexDirection: 'column', gap: 12, padding: 0, margin: 0 }}>
      <TraceStep number={1} title="Entrada" formula={String.raw`A = [a_0, \dots, a_{n-1}],\quad n = ${trace.input.length}`}>
        <Typography variant="body2" color="text.secondary">
          Orden alfabético descendente; la clave de cada elemento es su frecuencia.
        </Typography>
        <PagedArray items={trace.input} renderItem={termLabel} label="Entrada A" />
      </TraceStep>
      <TraceStep number={2} title="Máximo" formula={String.raw`k = \max_{i} a_i = ${trace.max}`}>
        <Typography variant="body2">El arreglo de conteo tiene k + 1 = {trace.max + 1} posiciones.</Typography>
      </TraceStep>
      <TraceStep number={3} title="Conteo" formula={String.raw`C[v] = \left|\{\, i : a_i = v \,\}\right|,\quad 0 \le v \le k`}>
        <PagedArray items={trace.count} renderItem={numberLabel} label="Conteo C" />
      </TraceStep>
      <TraceStep number={4} title="Acumulado" formula={String.raw`C[v] \mathrel{+}= C[v-1],\quad 1 \le v \le k`}>
        <PagedArray items={trace.cumulative} renderItem={numberLabel} label="Acumulado C" />
      </TraceStep>
      <TraceStep
        number={5}
        title="Colocaciones"
        formula={String.raw`B\big[C[a_i] - 1\big] = a_i,\quad C[a_i] \mathrel{-}= 1,\quad i = n-1, \dots, 0`}
      >
        <Typography variant="body2" color="text.secondary">
          Recorrido de derecha a izquierda: así los empates conservan su orden de entrada (estable).
        </Typography>
        <PagedArray items={trace.placements} renderItem={placementLabel} label="Colocaciones" />
      </TraceStep>
      <TraceStep number={6} title="Salida" formula={String.raw`B = [b_0, \dots, b_{n-1}],\quad b_0 \le b_1 \le \dots \le b_{n-1}`}>
        <PagedArray items={trace.output} renderItem={termLabel} label="Salida B" />
      </TraceStep>
      <TraceStep number={7} title="Ranking" formula={String.raw`R_j = b_{n-1-j},\quad 0 \le j \le n-1`}>
        <PagedArray items={ranking} renderItem={termLabel} label="Ranking R" />
      </TraceStep>
    </ol>
  )
}

export default CountingTraceView
