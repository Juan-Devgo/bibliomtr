import Button from '@mui/material/Button'
import Stack from '@mui/material/Stack'
import Typography from '@mui/material/Typography'

interface StepNavigatorProps {
  /** Índice base 0 del paso actual. */
  index: number
  /** Número total de pasos (N ≥ 1). */
  count: number
  /** Se llama con el índice del paso al que se navega. */
  onChange: (index: number) => void
  /** Nombre accesible del grupo de navegación. */
  label: string
}

/**
 * Navegación paso a paso de una traza (spec 004, REQ-26): "Primero", "Anterior", "Siguiente", "Último" y
 * "Paso k de N". Los botones que no llevan a otro paso quedan deshabilitados.
 *
 * Complejidad por render: O(1).
 */
function StepNavigator({ index, count, onChange, label }: StepNavigatorProps) {
  const last = count - 1
  return (
    <Stack
      role="group"
      aria-label={label}
      direction="row"
      spacing={1}
      sx={{ alignItems: 'center', flexWrap: 'wrap' }}
    >
      <Button size="small" variant="outlined" onClick={() => onChange(0)} disabled={index <= 0}>
        Primero
      </Button>
      <Button size="small" variant="outlined" onClick={() => onChange(index - 1)} disabled={index <= 0}>
        Anterior
      </Button>
      <Typography variant="body2" aria-live="polite" sx={{ minWidth: 110, textAlign: 'center' }}>
        {`Paso ${index + 1} de ${count}`}
      </Typography>
      <Button size="small" variant="outlined" onClick={() => onChange(index + 1)} disabled={index >= last}>
        Siguiente
      </Button>
      <Button size="small" variant="outlined" onClick={() => onChange(last)} disabled={index >= last}>
        Último
      </Button>
    </Stack>
  )
}

export default StepNavigator
