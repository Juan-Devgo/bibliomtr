import Paper from '@mui/material/Paper'
import Stack from '@mui/material/Stack'
import Typography from '@mui/material/Typography'
import { BlockMath } from 'react-katex'
import PagedArray from '@/components/PagedArray'
import TraceStep from '@/components/TraceStep'
import type { RadixPass, RadixSortTrace, TermFrequency } from '@/types/ranking'
import { numberLabel, termLabel } from './traceFormat'

interface RadixTraceViewProps {
  trace: RadixSortTrace
  /** Salida leída de derecha a izquierda (REQ-2). */
  ranking: readonly TermFrequency[]
}

/** Tarjeta de una pasada: exp, dígitos, conteo, acumulado y arreglo resultante (REQ-14). */
function PassCard({ pass, index }: { pass: RadixPass; index: number }) {
  const name = `Pasada ${index + 1} (exp = ${pass.exp})`
  return (
    <Paper component="section" variant="outlined" aria-label={name} sx={{ p: 1.5 }}>
      <Typography variant="subtitle2" component="h4">
        {name}
      </Typography>
      <BlockMath math={String.raw`d_i = \left\lfloor a_i / ${pass.exp} \right\rfloor \bmod 10`} />
      <PagedArray items={pass.digits} renderItem={numberLabel} label={`Dígitos (exp = ${pass.exp})`} />
      <PagedArray items={pass.count} renderItem={numberLabel} label={`Conteo C por dígito (exp = ${pass.exp})`} />
      <PagedArray
        items={pass.cumulative}
        renderItem={numberLabel}
        label={`Acumulado C (exp = ${pass.exp})`}
      />
      <PagedArray items={pass.result} renderItem={termLabel} label={`Resultado (exp = ${pass.exp})`} />
    </Paper>
  )
}

/**
 * Traza de Radix Sort LSD en base 10 paso a paso (REQ-14, REQ-16): entrada, máximo, una tarjeta por pasada
 * de Counting Sort estable por dígito y ranking. Complejidad del algoritmo trazado: O(d · (n + b)) en
 * tiempo y O(n + b) en espacio, con d = dígitos del máximo y b = 10.
 */
function RadixTraceView({ trace, ranking }: RadixTraceViewProps) {
  const digitCount = trace.passes.length
  return (
    <ol style={{ display: 'flex', flexDirection: 'column', gap: 12, padding: 0, margin: 0 }}>
      <TraceStep number={1} title="Entrada" formula={String.raw`A = [a_0, \dots, a_{n-1}],\quad n = ${trace.input.length}`}>
        <PagedArray items={trace.input} renderItem={termLabel} label="Entrada A" />
      </TraceStep>
      <TraceStep
        number={2}
        title="Máximo"
        formula={String.raw`\max_i a_i = ${trace.max},\quad d = ${digitCount},\quad b = 10`}
      >
        <Typography variant="body2">
          Se hace una pasada por dígito, con exp = 1, 10, 100, … mientras ⌊máx / exp⌋ &gt; 0.
        </Typography>
      </TraceStep>
      <TraceStep
        number={3}
        title="Pasadas por dígito"
        formula={String.raw`C[d] \mathrel{+}= C[d-1],\quad B\big[C[d_i] - 1\big] = a_i,\quad i = n-1, \dots, 0`}
      >
        {digitCount === 0 ? (
          <Typography variant="body2" color="text.secondary">
            Sin pasadas: la entrada está vacía.
          </Typography>
        ) : (
          <Stack spacing={1}>
            {trace.passes.map((pass, index) => (
              <PassCard key={pass.exp} pass={pass} index={index} />
            ))}
          </Stack>
        )}
      </TraceStep>
      <TraceStep number={4} title="Ranking" formula={String.raw`R_j = b_{n-1-j},\quad 0 \le j \le n-1`}>
        <PagedArray items={ranking} renderItem={termLabel} label="Ranking R" />
      </TraceStep>
    </ol>
  )
}

export default RadixTraceView
