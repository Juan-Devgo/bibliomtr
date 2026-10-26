import Alert from '@mui/material/Alert'
import CircularProgress from '@mui/material/CircularProgress'
import Stack from '@mui/material/Stack'
import TextField from '@mui/material/TextField'
import Typography from '@mui/material/Typography'
import { usePairTrace } from '@/api/usePairTrace'
import { formatMs } from '@/features/similarity-matrix/resultFormat'
import {
  SIMILARITY_ALGORITHM_LABELS,
  SIMILARITY_ALGORITHMS,
  type ArticleSummary,
  type PairTraceResponse,
  type SimilarityAlgorithmSlug,
} from '@/types/similarity'
import CosineTraceView from './CosineTraceView'
import EuclideanTraceView from './EuclideanTraceView'
import JaccardTraceView from './JaccardTraceView'
import LevenshteinTraceView from './LevenshteinTraceView'
import NeedlemanWunschTraceView from './NeedlemanWunschTraceView'
import TfIdfCosineTraceView from './TfIdfCosineTraceView'
import { decimal } from './traceFormat'

/** Algoritmo y par de artículos cuya traza se muestra; `null` mientras no se elige. */
export interface TraceSelection {
  algorithm: SimilarityAlgorithmSlug | null
  a: number | null
  b: number | null
}

interface SimilarityTraceViewProps {
  /** Artículos de la última comparación, entre los que se elige el par. */
  articles: readonly ArticleSummary[]
  /** Selección actual; también la cambia un clic en una celda del mapa de calor. */
  value: TraceSelection
  onChange: (selection: TraceSelection) => void
}

/** Elige la vista de pasos según el algoritmo de una respuesta CALCULADO. */
function TraceSteps({ response }: { response: PairTraceResponse }) {
  const { value, similarity } = response
  if (value === null || similarity === null) {
    return null
  }
  switch (response.algorithm) {
    case 'levenshtein':
      return response.trace && <LevenshteinTraceView trace={response.trace} similarity={similarity} />
    case 'needleman-wunsch':
      return response.trace && <NeedlemanWunschTraceView trace={response.trace} similarity={similarity} />
    case 'tfidf-cosine':
      return response.trace && <TfIdfCosineTraceView trace={response.trace} similarity={similarity} />
    case 'jaccard':
      return response.trace && <JaccardTraceView trace={response.trace} similarity={similarity} />
    case 'specter2-cosine':
    case 'nomic-cosine':
      return response.trace && <CosineTraceView trace={response.trace} value={value} similarity={similarity} />
    case 'specter2-euclidean':
    case 'nomic-euclidean':
      return response.trace && <EuclideanTraceView trace={response.trace} value={value} similarity={similarity} />
  }
}

/** Algoritmos que dependen de `bibliomtr.similarity.edit-granularity` (REQ-3). */
const EDIT_DISTANCES: readonly SimilarityAlgorithmSlug[] = ['levenshtein', 'needleman-wunsch']

/** Resumen de una respuesta CALCULADO: valor crudo, similitud, tiempo y, si aplica, la granularidad. */
function summary(response: PairTraceResponse): string {
  const parts = [
    `Valor: ${decimal(response.value ?? 0)}`,
    `Similitud: ${decimal(response.similarity ?? 0)}`,
    `Ejecución: ${formatMs(response.elapsedMs ?? 0)}`,
  ]
  if (EDIT_DISTANCES.includes(response.algorithm)) {
    parts.push(`Granularidad: ${response.editGranularity === 'WORDS' ? 'palabras' : 'caracteres'}`)
  }
  return parts.join(' · ')
}

/** Convierte el valor de un `<select>` de artículo en id, o `null` si está vacío. */
function toId(text: string): number | null {
  return text === '' ? null : Number(text)
}

/**
 * Visor de la traza de un algoritmo para un par de artículos (REQ-16, REQ-17, REQ-21, AC-15, AC-18).
 *
 * El usuario elige el algoritmo y los artículos a y b (pueden ser el mismo) entre los de la última
 * comparación, o hace clic en una celda de un mapa de calor. Con los tres valores definidos pide
 * `GET /api/similarity/trace` y muestra el resultado y los pasos del algoritmo en orden, cada uno con su
 * fórmula en KaTeX. Si el algoritmo está PENDIENTE muestra "Pendiente de implementar: <archivo>#<método>".
 * Los errores de la API se muestran con su mensaje en español, sin reescribirlo.
 */
function SimilarityTraceView({ articles, value, onChange }: SimilarityTraceViewProps) {
  const { algorithm, a, b } = value
  const { data, isFetching, isError, error } = usePairTrace(algorithm, a, b)
  const complete = algorithm !== null && a !== null && b !== null

  return (
    <Stack component="section" aria-label="Traza de un par" spacing={2}>
      <Typography variant="h5" component="h2">
        Traza paso a paso
      </Typography>
      <Stack direction={{ xs: 'column', md: 'row' }} spacing={2}>
        <TextField
          select
          label="Algoritmo"
          value={algorithm ?? ''}
          onChange={(event) =>
            onChange({ ...value, algorithm: event.target.value === '' ? null : (event.target.value as SimilarityAlgorithmSlug) })
          }
          size="small"
          sx={{ minWidth: 240 }}
          slotProps={{ select: { native: true }, inputLabel: { shrink: true } }}
        >
          <option value="">Elige un algoritmo…</option>
          {SIMILARITY_ALGORITHMS.map((slug) => (
            <option key={slug} value={slug}>
              {SIMILARITY_ALGORITHM_LABELS[slug]}
            </option>
          ))}
        </TextField>
        {(['a', 'b'] as const).map((side) => (
          <TextField
            key={side}
            select
            label={`Artículo ${side}`}
            value={value[side] === null ? '' : String(value[side])}
            onChange={(event) => onChange({ ...value, [side]: toId(event.target.value) })}
            size="small"
            sx={{ minWidth: 240 }}
            slotProps={{ select: { native: true }, inputLabel: { shrink: true } }}
          >
            <option value="">Elige un artículo…</option>
            {articles.map((article) => (
              <option key={article.id} value={article.id}>
                {`#${article.id} ${article.title}`}
              </option>
            ))}
          </TextField>
        ))}
      </Stack>

      {!complete && (
        <Typography color="text.secondary">
          Elige un algoritmo y dos artículos, o haz clic en una celda de un mapa de calor.
        </Typography>
      )}

      {isFetching && (
        <Stack direction="row" spacing={1} sx={{ alignItems: 'center' }}>
          <CircularProgress size={20} />
          <Typography>Cargando traza…</Typography>
        </Stack>
      )}

      {isError && <Alert severity="error">{error.message}</Alert>}

      {complete && data && !isError && (
        <Stack spacing={1}>
          <Typography variant="h6" component="p">
            {`${data.label} · #${data.articleA.id} frente a #${data.articleB.id}`}
          </Typography>
          {data.status === 'PENDIENTE' ? (
            <Alert severity="warning" sx={{ wordBreak: 'break-all' }}>
              {`Pendiente de implementar: ${data.pendingLocation ?? ''}`}
            </Alert>
          ) : (
            <>
              <Typography variant="body2" color="text.secondary">
                {summary(data)}
              </Typography>
              <TraceSteps response={data} />
            </>
          )}
        </Stack>
      )}
    </Stack>
  )
}

export default SimilarityTraceView
