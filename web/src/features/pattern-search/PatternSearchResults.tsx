import Alert from '@mui/material/Alert'
import CircularProgress from '@mui/material/CircularProgress'
import Paper from '@mui/material/Paper'
import Stack from '@mui/material/Stack'
import Typography from '@mui/material/Typography'
import { formatMs } from '@/features/similarity-matrix/resultFormat'
import type { AlgorithmOccurrences, PatternSearchResponse } from '@/types/patternSearch'
import AlgorithmOccurrencesTable from './AlgorithmOccurrencesTable'

interface PatternSearchResultsProps {
  /** Última respuesta de la búsqueda; `undefined` antes de la primera o tras un error. */
  response: PatternSearchResponse | undefined
  /** Error de la API, con el mensaje en español del backend. */
  error: Error | null
  /** Hay una búsqueda en curso. */
  loading: boolean
  /** Artículo cuyo abstract se muestra. */
  selectedArticleId: number | null
  /** Se llama al elegir un artículo en cualquiera de las tablas. */
  onSelectArticle: (articleId: number) => void
}

/** Resultado de un algoritmo: tiempos por fase, total y tabla, o la ubicación del stub pendiente (REQ-28). */
function AlgorithmCard({
  result,
  selectedArticleId,
  onSelectArticle,
}: {
  result: AlgorithmOccurrences
  selectedArticleId: number | null
  onSelectArticle: (articleId: number) => void
}) {
  return (
    <Paper component="section" variant="outlined" aria-label={result.label} sx={{ p: 2 }}>
      <Typography variant="h6" component="h3">
        {result.label}
      </Typography>
      {result.status === 'CALCULADO' ? (
        <>
          <Stack direction="row" spacing={3} sx={{ flexWrap: 'wrap', mb: 1 }}>
            <Typography variant="body2" color="text.secondary">
              {`Preprocesamiento: ${formatMs(result.preprocessingMs ?? 0)}`}
            </Typography>
            <Typography variant="body2" color="text.secondary">
              {`Búsqueda: ${formatMs(result.searchMs ?? 0)}`}
            </Typography>
            <Typography variant="body2">{`Total: ${result.totalOccurrences ?? 0}`}</Typography>
          </Stack>
          <AlgorithmOccurrencesTable
            result={result}
            selectedArticleId={selectedArticleId}
            onSelectArticle={onSelectArticle}
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

/**
 * Resultados de una búsqueda en la pestaña "Búsqueda Patrones" (spec 004, REQ-16, REQ-18, REQ-20, REQ-28,
 * REQ-29).
 *
 * Por algoritmo, en el orden de la API, muestra "Preprocesamiento: <ms>" (el único preprocesamiento del patrón) y
 * "Búsqueda: <ms>" (la suma de las búsquedas en cada abstract) por separado, el total de ocurrencias y la tabla
 * por artículo; un algoritmo pendiente muestra "Pendiente de implementar: <archivo>#<método>" sin afectar a los
 * demás. "Coinciden todas las posiciones: Sí/No" solo aparece si `allMatch` no es nulo (2 o más implementados).
 * Con el corpus sin abstracts muestra "Sin abstracts en el corpus". Los errores de la API se muestran con su
 * mensaje en español, sin reescribirlo.
 *
 * Complejidad por render: O(A · (a + k)), con A = 3 algoritmos, a = artículos y k = posiciones por algoritmo.
 */
function PatternSearchResults({
  response,
  error,
  loading,
  selectedArticleId,
  onSelectArticle,
}: PatternSearchResultsProps) {
  if (loading) {
    return (
      <Stack direction="row" spacing={1} sx={{ alignItems: 'center' }}>
        <CircularProgress size={20} />
        <Typography>Buscando ocurrencias…</Typography>
      </Stack>
    )
  }
  if (error) {
    return <Alert severity="error">{error.message}</Alert>
  }
  if (!response) {
    return null
  }
  if (response.articleCount === 0) {
    return <Alert severity="info">Sin abstracts en el corpus</Alert>
  }

  return (
    <Stack spacing={2}>
      <Typography variant="body2">
        {`Patrón de ${response.pattern.length} caracteres · Abstracts buscados: ${response.articleCount} · ` +
          `q = ${response.q} · d = ${response.d}`}
      </Typography>
      {response.allMatch !== null && (
        <Alert severity={response.allMatch ? 'success' : 'warning'}>
          {`Coinciden todas las posiciones: ${response.allMatch ? 'Sí' : 'No'}`}
        </Alert>
      )}
      {response.algorithms.map((result) => (
        <AlgorithmCard
          key={result.algorithm}
          result={result}
          selectedArticleId={selectedArticleId}
          onSelectArticle={onSelectArticle}
        />
      ))}
    </Stack>
  )
}

export default PatternSearchResults
