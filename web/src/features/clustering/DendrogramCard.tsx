import Alert from '@mui/material/Alert'
import Button from '@mui/material/Button'
import Paper from '@mui/material/Paper'
import Stack from '@mui/material/Stack'
import Typography from '@mui/material/Typography'
import Dendrogram from '@/components/Dendrogram'
import { formatMs } from '@/features/similarity-matrix/resultFormat'
import type { DendrogramResult } from '@/types/clustering'
import type { ArticleSummary } from '@/types/similarity'
import { leafLabel } from './dendrogramTree'

interface DendrogramCardProps {
  /** Uno de los 4 resultados del clustering. */
  result: DendrogramResult
  /** Artículos agrupados: la hoja i es el artículo i. */
  articles: readonly ArticleSummary[]
  /** Se llama al pedir la traza de este algoritmo; sin ella no se muestra el botón. */
  onShowTrace?: () => void
}

/**
 * Tarjeta de un algoritmo de clustering (spec 003, REQ-14, REQ-18, REQ-22).
 *
 * Con estado CALCULADO muestra "Ejecución: <elapsedMs> ms", el tiempo del algoritmo sin la preparación, el
 * dendrograma y el botón "Ver traza". Con PENDIENTE muestra "Pendiente de implementar: <archivo>#<método>", el
 * stub que debe completar el estudiante, sin dendrograma.
 *
 * Complejidad por render: O(N) en tiempo y espacio con CALCULADO y O(1) con PENDIENTE.
 */
function DendrogramCard({ result, articles, onShowTrace }: DendrogramCardProps) {
  const calculated = result.status === 'CALCULADO' && result.merges !== null

  return (
    <Paper component="section" variant="outlined" aria-label={result.label} sx={{ p: 2, minWidth: 0 }}>
      <Stack direction="row" spacing={1} sx={{ alignItems: 'center', justifyContent: 'space-between' }}>
        <Typography variant="h6" component="h3">
          {result.label}
        </Typography>
        {calculated && onShowTrace && (
          <Button size="small" variant="outlined" onClick={onShowTrace} aria-label={`Ver traza: ${result.label}`}>
            Ver traza
          </Button>
        )}
      </Stack>
      {calculated ? (
        <>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
            {`Ejecución: ${formatMs(result.elapsedMs ?? 0)}`}
          </Typography>
          <Dendrogram
            merges={result.merges ?? []}
            labels={articles.map(leafLabel)}
            titles={articles.map((article) => article.title)}
            ariaLabel={`Dendrograma: ${result.label}`}
          />
        </>
      ) : (
        <Alert severity="warning" sx={{ mt: 1, wordBreak: 'break-all' }}>
          {`Pendiente de implementar: ${result.pendingLocation ?? ''}`}
        </Alert>
      )}
    </Paper>
  )
}

export default DendrogramCard
