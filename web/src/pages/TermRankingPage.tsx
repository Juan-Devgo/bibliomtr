import Divider from '@mui/material/Divider'
import Stack from '@mui/material/Stack'
import Typography from '@mui/material/Typography'
import RankingView from '@/features/term-ranking/RankingView'
import SortTraceView from '@/features/sort-trace/SortTraceView'
import BenchmarkHistory from '@/features/sorting-benchmark/BenchmarkHistory'
import BenchmarkPanel from '@/features/sorting-benchmark/BenchmarkPanel'

/**
 * Página del Requerimiento 2: ranking de términos con Counting, Radix y Bucket Sort (REQ-5) y la traza
 * paso a paso de cada algoritmo sobre el corpus completo (REQ-16), y las mediciones JMH de los tres
 * algoritmos con su historial completo (REQ-17, REQ-28).
 */
function TermRankingPage() {
  return (
    <Stack component="main" spacing={3} sx={{ mx: 'auto', maxWidth: 1100, p: 3 }}>
      <Typography variant="h4" component="h1">
        Requerimiento 2: Ranking de términos
      </Typography>
      <Typography color="text.secondary">
        Frecuencia de cada término en los abstracts del corpus, ordenada con algoritmos no basados en
        comparaciones.
      </Typography>
      <RankingView />
      <Divider />
      <SortTraceView />
      <Divider />
      <BenchmarkPanel />
      <BenchmarkHistory />
    </Stack>
  )
}

export default TermRankingPage
