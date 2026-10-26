import Stack from '@mui/material/Stack'
import Typography from '@mui/material/Typography'
import PagedArray from '@/components/PagedArray'
import TraceStep from '@/components/TraceStep'
import type { BucketSortTrace, SortBucket, TermFrequency } from '@/types/ranking'
import { termLabel } from './traceFormat'

interface BucketTraceViewProps {
  trace: BucketSortTrace
  /** Salida leída de derecha a izquierda (REQ-2). */
  ranking: readonly TermFrequency[]
}

/** Contenido de una cubeta antes y después de su insertion sort (REQ-15). */
function BucketContent({ bucket }: { bucket: SortBucket }) {
  return (
    <Stack component="section" aria-label={`Cubeta ${bucket.index}`} spacing={0.5}>
      <Typography variant="subtitle2" component="h4">
        Cubeta {bucket.index}
      </Typography>
      <PagedArray items={bucket.before} renderItem={termLabel} label={`Cubeta ${bucket.index} antes`} />
      <PagedArray items={bucket.after} renderItem={termLabel} label={`Cubeta ${bucket.index} después`} />
    </Stack>
  )
}

/**
 * Traza de Bucket Sort paso a paso (REQ-15, REQ-16): entrada, mínimo y máximo, número de cubetas m,
 * cubetas antes y después del insertion sort, arreglo concatenado y ranking. Complejidad del algoritmo
 * trazado: O(n + m) promedio y O(n²) peor caso en tiempo, O(n + m) en espacio.
 */
function BucketTraceView({ trace, ranking }: BucketTraceViewProps) {
  return (
    <ol style={{ display: 'flex', flexDirection: 'column', gap: 12, padding: 0, margin: 0 }}>
      <TraceStep number={1} title="Entrada" formula={String.raw`A = [a_0, \dots, a_{n-1}],\quad n = ${trace.input.length}`}>
        <PagedArray items={trace.input} renderItem={termLabel} label="Entrada A" />
      </TraceStep>
      <TraceStep
        number={2}
        title="Mínimo y máximo"
        formula={String.raw`\min_i a_i = ${trace.min},\quad \max_i a_i = ${trace.max}`}
      >
        <Typography variant="body2">bucketSize = {trace.bucketSize}</Typography>
      </TraceStep>
      <TraceStep
        number={3}
        title="Número de cubetas"
        formula={String.raw`m = \left\lfloor \frac{\max - \min}{\text{bucketSize}} \right\rfloor + 1 = ${trace.bucketCount}`}
      >
        <Typography variant="body2">Cada elemento va a la cubeta ⌊(aᵢ − mín) / bucketSize⌋.</Typography>
      </TraceStep>
      <TraceStep
        number={4}
        title="Cubetas antes y después del insertion sort"
        formula={String.raw`j = \left\lfloor \frac{a_i - \min}{\text{bucketSize}} \right\rfloor;\quad \text{insertion sort: desplaza mientras } x_{p} > \text{clave}`}
      >
        <Typography variant="body2" color="text.secondary">
          El desplazamiento usa «&gt;» estricto, así los empates conservan su orden (estable).
        </Typography>
        <PagedArray
          items={trace.buckets}
          renderItem={(bucket) => <BucketContent bucket={bucket} />}
          label="Cubetas"
          layout="block"
        />
      </TraceStep>
      <TraceStep
        number={5}
        title="Concatenado"
        formula={String.raw`B = \text{cubeta}_0 \,\Vert\, \text{cubeta}_1 \,\Vert\, \dots \,\Vert\, \text{cubeta}_{m-1}`}
      >
        <PagedArray items={trace.concatenated} renderItem={termLabel} label="Concatenado B" />
      </TraceStep>
      <TraceStep number={6} title="Ranking" formula={String.raw`R_j = b_{n-1-j},\quad 0 \le j \le n-1`}>
        <PagedArray items={ranking} renderItem={termLabel} label="Ranking R" />
      </TraceStep>
    </ol>
  )
}

export default BucketTraceView
