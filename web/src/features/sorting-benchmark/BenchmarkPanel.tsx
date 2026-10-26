import Alert from '@mui/material/Alert'
import Button from '@mui/material/Button'
import CircularProgress from '@mui/material/CircularProgress'
import Stack from '@mui/material/Stack'
import Typography from '@mui/material/Typography'
import { useBenchmarks } from '@/api/useBenchmarks'
import { useStartBenchmark } from '@/api/useStartBenchmark'
import type { BenchmarkRun } from '@/types/benchmark'
import BenchmarkResultCard from './BenchmarkResultCard'

/**
 * Primera medición del historial con el estado dado. El backend entrega el historial de la más reciente a
 * la más antigua (REQ-28), así que la primera coincidencia es la más reciente; no se reordena.
 *
 * Complejidad: O(r) en tiempo y O(1) en espacio, con r = número de mediciones.
 */
function latestWithStatus(runs: readonly BenchmarkRun[], status: BenchmarkRun['status']): BenchmarkRun | undefined {
  return runs.find((run) => run.status === status)
}

/**
 * Panel de mediciones JMH de Counting, Radix y Bucket Sort (REQ-17, REQ-19, REQ-20, REQ-21, REQ-23, AC-13,
 * AC-15, AC-16).
 *
 * - Botón "Lanzar medición JMH", deshabilitado mientras alguna medición está EN_CURSO (REQ-20).
 * - Estado de la medición más reciente; mientras está EN_CURSO, `useBenchmarks` refresca cada 2 s.
 * - Si la más reciente quedó FALLIDA, su mensaje en español va en un `Alert` (REQ-21); el 409 "Ya hay una
 *   medición en curso" también se muestra en un `Alert`, tal cual lo envía el backend (REQ-19).
 * - Muestra los resultados de la última COMPLETADA aunque haya FALLIDAS más recientes (AC-15); sin ninguna
 *   COMPLETADA muestra "Sin mediciones JMH" junto al botón (REQ-23, AC-16).
 */
function BenchmarkPanel() {
  const { data: runs, isPending, isError, error } = useBenchmarks()
  const start = useStartBenchmark()

  const latest = runs?.[0]
  const inProgress = runs?.some((run) => run.status === 'EN_CURSO') ?? false
  const latestCompleted = runs ? latestWithStatus(runs, 'COMPLETADA') : undefined

  return (
    <Stack component="section" aria-label="Mediciones JMH" spacing={2}>
      <Typography variant="h5" component="h2">
        Mediciones JMH
      </Typography>
      <Typography color="text.secondary">
        Tiempo promedio de cada algoritmo sobre la lista de frecuencias del corpus completo, medido con JMH y
        comparado con su complejidad teórica.
      </Typography>

      <Stack direction="row" spacing={2} sx={{ alignItems: 'center' }}>
        <Button
          variant="contained"
          onClick={() => start.mutate()}
          disabled={isPending || inProgress || start.isPending}
        >
          Lanzar medición JMH
        </Button>
        {inProgress && <CircularProgress size={20} aria-label="Medición en curso" />}
      </Stack>

      {latest && (
        <Typography>
          Estado de la medición más reciente: <strong>{latest.status}</strong>
        </Typography>
      )}

      {start.isError && <Alert severity="error">{start.error.message}</Alert>}

      {isError && <Alert severity="error">{error.message}</Alert>}

      {latest?.status === 'FALLIDA' && latest.message && <Alert severity="error">{latest.message}</Alert>}

      {runs && !latestCompleted && <Alert severity="info">Sin mediciones JMH</Alert>}

      {latestCompleted && <BenchmarkResultCard run={latestCompleted} />}
    </Stack>
  )
}

export default BenchmarkPanel
