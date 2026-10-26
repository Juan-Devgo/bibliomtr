import Paper from '@mui/material/Paper'
import Table from '@mui/material/Table'
import TableBody from '@mui/material/TableBody'
import TableCell from '@mui/material/TableCell'
import TableContainer from '@mui/material/TableContainer'
import TableHead from '@mui/material/TableHead'
import TableRow from '@mui/material/TableRow'
import Typography from '@mui/material/Typography'
import type { BenchmarkRun } from '@/types/benchmark'
import { SORT_ALGORITHM_LABELS } from '@/types/ranking'
import { formatAlgorithmParameters, formatDateTime, formatJmhParameters, formatScore } from './benchmarkFormat'

interface BenchmarkResultCardProps {
  /** Medición COMPLETADA cuyos resultados se muestran. */
  run: BenchmarkRun
}

/**
 * Resultados de una medición JMH COMPLETADA, uno por algoritmo (REQ-22, AC-13): tiempo promedio como
 * `ms ± scoreError`, número de términos n, parámetros propios (Counting k; Radix d y b; Bucket m) y
 * complejidad teórica, para contrastar la práctica con la teoría (constitución P11). Muestra también la
 * fecha y los parámetros JMH fijos con que se midió (REQ-18).
 *
 * Complejidad por render: O(a) en tiempo y espacio, con a = número de resultados (3).
 */
function BenchmarkResultCard({ run }: BenchmarkResultCardProps) {
  const n = run.termCount ?? '—'
  return (
    <Paper component="section" variant="outlined" aria-label="Última medición COMPLETADA" sx={{ p: 2 }}>
      <Typography variant="h6" component="h3">
        Última medición COMPLETADA
      </Typography>
      <Typography variant="body2" color="text.secondary">
        {formatDateTime(run.startedAt)} · n = {n} términos
      </Typography>
      <Typography variant="body2" color="text.secondary">
        Parámetros JMH: {formatJmhParameters(run.parameters)}
      </Typography>
      <TableContainer>
        <Table size="small" aria-label="Resultados JMH por algoritmo">
          <TableHead>
            <TableRow>
              <TableCell>Algoritmo</TableCell>
              <TableCell align="right">Tiempo promedio (ms ± error)</TableCell>
              <TableCell align="right">n</TableCell>
              <TableCell>Parámetros</TableCell>
              <TableCell>Complejidad teórica</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {run.results.map((result) => (
              <TableRow key={result.algorithm}>
                <TableCell>{SORT_ALGORITHM_LABELS[result.algorithm] ?? result.algorithm}</TableCell>
                <TableCell align="right">{formatScore(result)}</TableCell>
                <TableCell align="right">{n}</TableCell>
                <TableCell>{formatAlgorithmParameters(result)}</TableCell>
                <TableCell>{result.complexity}</TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </TableContainer>
    </Paper>
  )
}

export default BenchmarkResultCard
