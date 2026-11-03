import Alert from '@mui/material/Alert'
import Box from '@mui/material/Box'
import CircularProgress from '@mui/material/CircularProgress'
import Stack from '@mui/material/Stack'
import TextField from '@mui/material/TextField'
import Typography from '@mui/material/Typography'
import { BlockMath } from 'react-katex'
import { useClusteringTrace } from '@/api/useClusteringTrace'
import PagedMatrix from '@/components/PagedMatrix'
import { articleLabel, formatMs } from '@/features/similarity-matrix/resultFormat'
import {
  CLUSTERING_ALGORITHM_LABELS,
  CLUSTERING_ALGORITHMS,
  type ClusteringAlgorithmSlug,
  type ClusteringTraceResponse,
} from '@/types/clustering'
import type { ArticleSummary, SimilarityAlgorithmSlug } from '@/types/similarity'
import { INPUT_DISTANCE_FORMULA } from './clusteringFormulas'
import DianaTraceView from './DianaTraceView'
import LinkageTraceView from './LinkageTraceView'
import { formatDistance } from './traceFormat'

interface ClusteringTraceViewProps {
  /** Artículos agrupados: la hoja i es el artículo i. */
  articles: readonly ArticleSummary[]
  /** Fuente de similitud de la sección. */
  source: SimilarityAlgorithmSlug
  /** Algoritmo cuya traza se muestra; `null` mientras no se elige. */
  algorithm: ClusteringAlgorithmSlug | null
  onAlgorithmChange: (algorithm: ClusteringAlgorithmSlug | null) => void
}

/** Elige la vista de pasos según el algoritmo de una respuesta CALCULADO. */
function TraceSteps({ response }: { response: ClusteringTraceResponse }) {
  switch (response.algorithm) {
    case 'single-linkage':
    case 'complete-linkage':
    case 'average-linkage':
      return response.trace && <LinkageTraceView algorithm={response.algorithm} trace={response.trace} />
    case 'diana':
      return response.trace && <DianaTraceView trace={response.trace} />
  }
}

/** Contenido de una respuesta: aviso de pendiente (fuente o algoritmo), o entrada y pasos. */
function TraceContent({ response }: { response: ClusteringTraceResponse }) {
  if (response.source.status === 'PENDIENTE') {
    return (
      <Alert severity="warning" sx={{ wordBreak: 'break-all' }}>
        {`La fuente ${response.source.label} está pendiente de implementar: ${response.source.pendingLocation ?? ''}`}
      </Alert>
    )
  }
  if (response.status === 'PENDIENTE') {
    return (
      <Alert severity="warning" sx={{ wordBreak: 'break-all' }}>
        {`Pendiente de implementar: ${response.pendingLocation ?? ''}`}
      </Alert>
    )
  }
  const labels = response.articles.map((article) => articleLabel(article.id))
  return (
    <>
      <Typography variant="body2" color="text.secondary">
        {`Ejecución: ${formatMs(response.elapsedMs ?? 0)} · Preparación: ${formatMs(response.preparationMs ?? 0)}`}
      </Typography>
      <Box component="section" aria-label="Hojas del dendrograma">
        <Typography variant="subtitle2" component="h4">
          Hojas
        </Typography>
        <Box component="ul" sx={{ my: 0.5, pl: 3 }}>
          {response.articles.map((article, i) => (
            <li key={article.id}>
              <Typography variant="body2" component="span">{`${i} = ${articleLabel(article.id)} ${article.title}`}</Typography>
            </li>
          ))}
        </Box>
      </Box>
      <BlockMath math={INPUT_DISTANCE_FORMULA} />
      <PagedMatrix
        matrix={response.distances ?? []}
        rowLabels={labels}
        colLabels={labels}
        label="Matriz de distancias D"
        renderCell={formatDistance}
      />
      <TraceSteps response={response} />
    </>
  )
}

/**
 * Visor de la traza del clustering (spec 003, REQ-16, REQ-17, REQ-18, AC-15).
 *
 * El usuario elige el algoritmo, o pulsa "Ver traza" en su tarjeta. Con el algoritmo elegido pide
 * `GET /api/clustering/trace` con los artículos y la fuente de la sección y muestra la matriz de distancias de
 * entrada y los pasos del algoritmo en orden, cada uno con su fórmula en KaTeX. Si el algoritmo o la fuente
 * están pendientes muestra su ubicación. Los errores de la API se muestran con su mensaje en español.
 */
function ClusteringTraceView({ articles, source, algorithm, onAlgorithmChange }: ClusteringTraceViewProps) {
  const articleIds = articles.map((article) => article.id)
  const { data, isFetching, isError, error } = useClusteringTrace(algorithm, source, articleIds)

  return (
    <Stack component="section" aria-label="Traza del clustering" spacing={2}>
      <Typography variant="h6" component="h3">
        Traza del clustering paso a paso
      </Typography>
      <TextField
        select
        label="Algoritmo de clustering"
        value={algorithm ?? ''}
        onChange={(event) =>
          onAlgorithmChange(event.target.value === '' ? null : (event.target.value as ClusteringAlgorithmSlug))
        }
        size="small"
        sx={{ maxWidth: 320 }}
        slotProps={{ select: { native: true }, inputLabel: { shrink: true } }}
      >
        <option value="">Elige un algoritmo…</option>
        {CLUSTERING_ALGORITHMS.map((slug) => (
          <option key={slug} value={slug}>
            {CLUSTERING_ALGORITHM_LABELS[slug]}
          </option>
        ))}
      </TextField>

      {algorithm === null && (
        <Typography color="text.secondary">Elige un algoritmo o pulsa "Ver traza" en su dendrograma.</Typography>
      )}

      {isFetching && (
        <Stack direction="row" spacing={1} sx={{ alignItems: 'center' }}>
          <CircularProgress size={20} />
          <Typography>Cargando traza…</Typography>
        </Stack>
      )}

      {isError && <Alert severity="error">{error.message}</Alert>}

      {algorithm !== null && data && !isError && (
        <Stack spacing={1}>
          <Typography variant="subtitle1" component="p" sx={{ fontWeight: 600 }}>
            {`${data.label} · ${data.source.label}`}
          </Typography>
          <TraceContent response={data} />
        </Stack>
      )}
    </Stack>
  )
}

export default ClusteringTraceView
