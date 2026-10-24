import Alert from '@mui/material/Alert'
import List from '@mui/material/List'
import ListItem from '@mui/material/ListItem'
import ListItemText from '@mui/material/ListItemText'
import Stack from '@mui/material/Stack'
import Typography from '@mui/material/Typography'
import { useBenchmarks } from '@/api/useBenchmarks'
import type { BenchmarkRun } from '@/types/benchmark'
import { SORT_ALGORITHM_LABELS } from '@/types/ranking'
import { formatAlgorithmParameters, formatDateTime, formatScore } from './benchmarkFormat'

/**
 * Detalle de una medición del historial: mensaje si quedó FALLIDA y, si está COMPLETADA, una línea por
 * algoritmo con ms ± error, parámetros propios y complejidad.
 *
 * Complejidad por render: O(a), con a = número de resultados.
 */
function RunDetails({ run }: { run: BenchmarkRun }) {
  return (
    <Stack component="span" sx={{ display: 'flex' }}>
      {run.message && (
        <Typography component="span" variant="body2" color="error">
          {run.message}
        </Typography>
      )}
      {run.results.map((result) => (
        <Typography component="span" variant="body2" key={result.algorithm}>
          {`${SORT_ALGORITHM_LABELS[result.algorithm] ?? result.algorithm}: ${formatScore(result)} · ` +
            `n = ${run.termCount ?? '—'} · ${formatAlgorithmParameters(result)} · ${result.complexity}`}
        </Typography>
      ))}
    </Stack>
  )
}

/**
 * Historial completo de mediciones JMH (REQ-28, AC-20, AC-27).
 *
 * Lista MUI con todas las mediciones en el orden recibido del backend, de la más reciente a la más antigua,
 * sin paginar ni reordenar; cada elemento muestra su fecha, su estado y sus resultados (o su mensaje si
 * quedó FALLIDA).
 *
 * Complejidad por render: O(r · a) en tiempo y espacio, con r = mediciones y a = resultados por medición.
 */
function BenchmarkHistory() {
  const { data: runs, isError, error } = useBenchmarks()

  return (
    <Stack component="section" aria-label="Historial de mediciones JMH" spacing={1}>
      <Typography variant="h6" component="h3">
        Historial de mediciones
      </Typography>

      {isError && <Alert severity="error">{error.message}</Alert>}

      {runs && runs.length === 0 && <Typography color="text.secondary">Todavía no hay mediciones.</Typography>}

      {runs && runs.length > 0 && (
        <List dense aria-label="Mediciones JMH, de la más reciente a la más antigua">
          {runs.map((run) => (
            <ListItem key={run.id} divider aria-label={`Medición ${run.id}`}>
              <ListItemText
                primary={`${formatDateTime(run.startedAt)} · ${run.status}`}
                secondary={<RunDetails run={run} />}
              />
            </ListItem>
          ))}
        </List>
      )}
    </Stack>
  )
}

export default BenchmarkHistory
