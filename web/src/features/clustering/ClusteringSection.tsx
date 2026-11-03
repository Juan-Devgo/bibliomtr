import Alert from '@mui/material/Alert'
import Box from '@mui/material/Box'
import CircularProgress from '@mui/material/CircularProgress'
import Stack from '@mui/material/Stack'
import Typography from '@mui/material/Typography'
import { useMemo, useState } from 'react'
import { useClustering } from '@/api/useClustering'
import { formatMs } from '@/features/similarity-matrix/resultFormat'
import { DEFAULT_SOURCE } from '@/types/clustering'
import type { ArticleSummary, SimilarityAlgorithmSlug } from '@/types/similarity'
import DendrogramCard from './DendrogramCard'
import SourceSelector from './SourceSelector'

interface ClusteringSectionProps {
  /** Artículos de la última comparación del Requerimiento 1: las hojas de los dendrogramas, en ese orden. */
  articles: readonly ArticleSummary[]
}

/**
 * Sección del Requerimiento 3 en la página de similitud textual (spec 003, REQ-13, REQ-14, REQ-18, REQ-19,
 * REQ-22).
 *
 * Agrupa los mismos artículos de la comparación con la fuente elegida (SPECTER2 coseno por defecto) y dibuja los
 * 4 dendrogramas a la vez: una columna en pantallas angostas y 2×2 desde `md`. Muestra "Preparación: <ms>"
 * (preprocesamiento y matriz de similitud) aparte del tiempo de cada algoritmo. Si la fuente está pendiente
 * muestra su ubicación y ningún dendrograma. Los errores de la API se muestran con su mensaje en español.
 */
function ClusteringSection({ articles }: ClusteringSectionProps) {
  const [source, setSource] = useState<SimilarityAlgorithmSlug>(DEFAULT_SOURCE)
  const articleIds = useMemo(() => articles.map((article) => article.id), [articles])
  const { data, isFetching, isError, error } = useClustering(articleIds, source)

  return (
    <Stack component="section" aria-label="Clustering jerárquico" spacing={2}>
      <Typography variant="h5" component="h2">
        Clustering jerárquico
      </Typography>
      <Typography color="text.secondary">
        Agrupa los artículos de la comparación con Single, Complete y Average Linkage y con DIANA, sobre la distancia
        d = 1 − s de la fuente elegida.
      </Typography>
      <SourceSelector value={source} onChange={setSource} />

      {isFetching && (
        <Stack direction="row" spacing={1} sx={{ alignItems: 'center' }}>
          <CircularProgress size={20} />
          <Typography>Calculando dendrogramas…</Typography>
        </Stack>
      )}

      {isError && <Alert severity="error">{error.message}</Alert>}

      {data && !isError && (
        <>
          {data.source.status === 'PENDIENTE' ? (
            <Alert severity="warning" sx={{ wordBreak: 'break-all' }}>
              {`La fuente ${data.source.label} está pendiente de implementar: ${data.source.pendingLocation ?? ''}`}
            </Alert>
          ) : (
            <>
              <Typography variant="body2" color="text.secondary">
                {`Preparación: ${formatMs(data.preparationMs ?? 0)}`}
              </Typography>
              <Box
                sx={{
                  display: 'grid',
                  gridTemplateColumns: { xs: '1fr', md: 'repeat(2, 1fr)' },
                  gap: 2,
                }}
              >
                {data.results.map((result) => (
                  <DendrogramCard key={result.algorithm} result={result} articles={data.articles} />
                ))}
              </Box>
            </>
          )}
        </>
      )}
    </Stack>
  )
}

export default ClusteringSection
