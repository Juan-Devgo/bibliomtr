import Typography from '@mui/material/Typography'
import PagedArray from '@/components/PagedArray'
import PagedMatrix from '@/components/PagedMatrix'
import TraceStep from '@/components/TraceStep'
import type { AlignedPair, NeedlemanWunschMove, NeedlemanWunschTrace } from '@/types/similarity'
import StepList from './StepList'
import { decimal, dpLabels, symbolLabel } from './traceFormat'

interface NeedlemanWunschTraceViewProps {
  trace: NeedlemanWunschTrace
  /** Similitud en [0,1] que devolvió el algoritmo. */
  similarity: number
}

/** Flecha de cada movimiento de la matriz F; `·` en la celda (0, 0), que no tiene movimiento. */
const MOVE_ARROWS: Record<NeedlemanWunschMove, string> = { DIAG: '↖', UP: '↑', LEFT: '←' }

/** Texto de un movimiento. */
function moveLabel(move: NeedlemanWunschMove | null): string {
  return move === null ? '·' : (MOVE_ARROWS[move] ?? move)
}

/** Texto de un par del alineamiento: `aᵢ / bⱼ`, con `−` para un gap. */
function pairLabel(pair: AlignedPair): string {
  const fromA = pair.fromA === null ? '−' : symbolLabel(pair.fromA)
  const fromB = pair.fromB === null ? '−' : symbolLabel(pair.fromB)
  return `${fromA} / ${fromB}`
}

/** Un entero negativo entre paréntesis para restarlo en una fórmula: `-4` → `(-4)`. */
function signed(value: number): string {
  return value < 0 ? `(${value})` : String(value)
}

/**
 * Traza de Needleman-Wunsch paso a paso (REQ-16, REQ-17, AC-15): secuencias y puntajes, matriz F, movimientos,
 * alineamiento, puntaje, puntajes mínimo y máximo y similitud, cada paso con su fórmula en KaTeX (constitución
 * P6, P7). Las matrices pasan por `PagedMatrix`.
 *
 * Complejidad por render: O(min(m, 25)·min(n, 25) + min(m + n, 50)).
 */
function NeedlemanWunschTraceView({ trace, similarity }: NeedlemanWunschTraceViewProps) {
  const { match, mismatch, gap } = trace.scoring
  const m = trace.a.length
  const n = trace.b.length
  const rowLabels = dpLabels(trace.a)
  const colLabels = dpLabels(trace.b)
  const similarityFormula =
    trace.maxScore === trace.minScore
      ? String.raw`\text{sim} = ${decimal(similarity)}\quad (\text{alguna secuencia vacía})`
      : String.raw`\text{sim} = \frac{\text{score} - \text{minScore}}{\text{maxScore} - \text{minScore}} = \frac{${trace.score} - ${signed(trace.minScore)}}{${trace.maxScore} - ${signed(trace.minScore)}} = ${decimal(similarity)}`

  return (
    <StepList>
      <TraceStep
        number={1}
        title="Secuencias y puntajes"
        formula={String.raw`s(x, y) = \begin{cases} ${match} & x = y \\ ${mismatch} & x \ne y \end{cases},\qquad g = ${gap},\qquad m = ${m},\ n = ${n}`}
      >
        <PagedArray items={trace.a} renderItem={symbolLabel} label="Secuencia a" />
        <PagedArray items={trace.b} renderItem={symbolLabel} label="Secuencia b" />
      </TraceStep>
      <TraceStep
        number={2}
        title="Matriz de puntajes F"
        formula={String.raw`F_{i,0} = i\,g,\quad F_{0,j} = j\,g,\quad F_{i,j} = \max\big(F_{i-1,j-1} + s(a_i, b_j),\ F_{i-1,j} + g,\ F_{i,j-1} + g\big)`}
      >
        <PagedMatrix matrix={trace.scores} rowLabels={rowLabels} colLabels={colLabels} label="Matriz F" />
      </TraceStep>
      <TraceStep
        number={3}
        title="Movimientos"
        formula={String.raw`\nwarrow\ \text{diagonal},\qquad \uparrow\ \text{arriba (gap en } b),\qquad \leftarrow\ \text{izquierda (gap en } a)`}
      >
        <PagedMatrix
          matrix={trace.moves}
          rowLabels={rowLabels}
          colLabels={colLabels}
          label="Movimientos"
          renderCell={moveLabel}
        />
      </TraceStep>
      <TraceStep
        number={4}
        title="Alineamiento"
        formula={String.raw`\text{recorrido de los movimientos desde } (m, n) \text{ hasta } (0, 0)`}
      >
        <PagedArray items={trace.alignment} renderItem={pairLabel} label="Alineamiento a / b" />
      </TraceStep>
      <TraceStep number={5} title="Puntaje" formula={String.raw`\text{score} = F_{m,n} = ${trace.score}`}>
        <Typography variant="body2">Puntaje del alineamiento global óptimo.</Typography>
      </TraceStep>
      <TraceStep
        number={6}
        title="Puntajes mínimo y máximo"
        formula={String.raw`\text{minScore} = -\max(m, n) = ${trace.minScore},\qquad \text{maxScore} = 2\min(m, n) - \max(m, n) = ${trace.maxScore}`}
      >
        <Typography variant="body2" color="text.secondary">
          Cotas del puntaje con (1, −1, −1), usadas para llevarlo a [0,1].
        </Typography>
      </TraceStep>
      <TraceStep number={7} title="Similitud" formula={similarityFormula}>
        <Typography variant="body2">{`Similitud en [0,1]: ${decimal(similarity)}`}</Typography>
      </TraceStep>
    </StepList>
  )
}

export default NeedlemanWunschTraceView
