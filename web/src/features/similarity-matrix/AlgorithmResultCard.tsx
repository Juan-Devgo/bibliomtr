import Alert from '@mui/material/Alert'
import Paper from '@mui/material/Paper'
import Typography from '@mui/material/Typography'
import Heatmap from '@/components/Heatmap'
import type { AlgorithmMatrix } from '@/types/similarity'
import { formatMs } from './resultFormat'

interface AlgorithmResultCardProps {
  /** Uno de los 8 resultados de la comparación. */
  result: AlgorithmMatrix
  /** Encabezados de filas y columnas (`#id` de cada artículo comparado). */
  labels: readonly string[]
  /** Se llama con la fila y la columna de la celda elegida del mapa de calor. */
  onSelectCell?: (row: number, column: number) => void
}

/**
 * Tarjeta de un resultado de la comparación (REQ-17, REQ-21, REQ-22, AC-18, AC-19).
 *
 * Con estado CALCULADO dibuja el mapa de calor N×N y "Ejecución: <elapsedMs> ms", el tiempo de recorrer todos
 * los pares sin el preprocesamiento. Con PENDIENTE muestra "Pendiente de implementar: <archivo>#<método>", el
 * stub que debe completar el estudiante, sin valores.
 *
 * Complejidad por render: O(N²) en tiempo y espacio con CALCULADO y O(1) con PENDIENTE.
 */
function AlgorithmResultCard({ result, labels, onSelectCell }: AlgorithmResultCardProps) {
  const calculated = result.status === 'CALCULADO' && result.matrix !== null

  return (
    <Paper component="section" variant="outlined" aria-label={result.label} sx={{ p: 2 }}>
      <Typography variant="h6" component="h3">
        {result.label}
      </Typography>
      {calculated ? (
        <>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
            {`Ejecución: ${formatMs(result.elapsedMs ?? 0)}`}
          </Typography>
          <Heatmap
            labels={labels}
            matrix={result.matrix ?? []}
            onSelectCell={onSelectCell}
            ariaLabel={`Mapa de calor: ${result.label}`}
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

export default AlgorithmResultCard
