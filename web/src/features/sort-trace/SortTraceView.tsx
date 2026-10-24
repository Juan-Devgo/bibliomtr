import Alert from '@mui/material/Alert'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import CircularProgress from '@mui/material/CircularProgress'
import Stack from '@mui/material/Stack'
import TextField from '@mui/material/TextField'
import ToggleButton from '@mui/material/ToggleButton'
import ToggleButtonGroup from '@mui/material/ToggleButtonGroup'
import Typography from '@mui/material/Typography'
import { useState, type FormEvent } from 'react'
import { useSortTrace } from '@/api/useSortTrace'
import {
  DEFAULT_BUCKET_SIZE,
  SORT_ALGORITHM_LABELS,
  SORT_ALGORITHMS,
  type SortAlgorithmSlug,
  type SortTraceResponse,
} from '@/types/ranking'
import BucketTraceView from './BucketTraceView'
import CountingTraceView from './CountingTraceView'
import RadixTraceView from './RadixTraceView'

/** Elige la vista de pasos según el algoritmo de la respuesta. */
function TraceSteps({ response }: { response: SortTraceResponse }) {
  switch (response.algorithm) {
    case 'counting':
      return <CountingTraceView trace={response.trace} ranking={response.ranking} />
    case 'radix':
      return <RadixTraceView trace={response.trace} ranking={response.ranking} />
    case 'bucket':
      return <BucketTraceView trace={response.trace} ranking={response.ranking} />
  }
}

/**
 * Traza paso a paso de un algoritmo sobre la lista de frecuencias del corpus completo (REQ-9, REQ-16,
 * AC-12, AC-25).
 *
 * El usuario elige el algoritmo y, opcionalmente, `bucketSize` (default 5, solo afecta a Bucket Sort); la
 * traza se pide al elegir un algoritmo. Los pasos se muestran en el orden de ejecución, cada uno con su
 * fórmula, y todo arreglo de más de 50 elementos se pagina de a 50. Los errores de la API (p. ej. 400 por
 * `bucketSize` inválido) se muestran con su mensaje en español, sin reescribirlo.
 */
function SortTraceView() {
  const [algorithm, setAlgorithm] = useState<SortAlgorithmSlug | null>(null)
  const [bucketText, setBucketText] = useState(String(DEFAULT_BUCKET_SIZE))
  const [bucketSize, setBucketSize] = useState(DEFAULT_BUCKET_SIZE)
  const { data, isFetching, isError, error } = useSortTrace(algorithm, bucketSize)

  const submit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    // Sin validar en el cliente: un valor no entero o menor que 1 lo rechaza el backend con 400 (REQ-4).
    setBucketSize(bucketText.trim() === '' ? Number.NaN : Number(bucketText))
  }

  return (
    <Stack component="section" aria-label="Traza de los algoritmos" spacing={2}>
      <Typography variant="h5" component="h2">
        Traza paso a paso
      </Typography>
      <ToggleButtonGroup
        exclusive
        value={algorithm}
        onChange={(_, value: SortAlgorithmSlug | null) => {
          if (value !== null) {
            setAlgorithm(value)
          }
        }}
        aria-label="Algoritmo de la traza"
        size="small"
      >
        {SORT_ALGORITHMS.map((slug) => (
          <ToggleButton key={slug} value={slug}>
            {SORT_ALGORITHM_LABELS[slug]}
          </ToggleButton>
        ))}
      </ToggleButtonGroup>
      <Box component="form" onSubmit={submit} sx={{ display: 'flex', gap: 2, alignItems: 'center' }}>
        <TextField
          label="bucketSize de la traza"
          value={bucketText}
          onChange={(event) => setBucketText(event.target.value)}
          size="small"
          helperText="Solo afecta a Bucket Sort"
          slotProps={{ htmlInput: { inputMode: 'numeric' } }}
        />
        <Button type="submit" variant="outlined">
          Aplicar
        </Button>
      </Box>

      {algorithm === null && (
        <Typography color="text.secondary">Elige un algoritmo para ver su traza sobre el corpus completo.</Typography>
      )}

      {isFetching && (
        <Stack direction="row" spacing={1} sx={{ alignItems: 'center' }}>
          <CircularProgress size={20} />
          <Typography>Cargando traza…</Typography>
        </Stack>
      )}

      {isError && <Alert severity="error">{error.message}</Alert>}

      {data && !isError && (
        <Stack spacing={1}>
          <Typography variant="h6" component="p">
            {SORT_ALGORITHM_LABELS[data.algorithm]} · n = {data.input.length} · bucketSize = {data.bucketSize}
          </Typography>
          <TraceSteps response={data} />
        </Stack>
      )}
    </Stack>
  )
}

export default SortTraceView
