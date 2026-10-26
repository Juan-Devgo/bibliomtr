import Alert from '@mui/material/Alert'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import CircularProgress from '@mui/material/CircularProgress'
import Paper from '@mui/material/Paper'
import Stack from '@mui/material/Stack'
import TextField from '@mui/material/TextField'
import Typography from '@mui/material/Typography'
import { useState, type FormEvent } from 'react'
import { useTermRanking } from '@/api/useTermRanking'
import {
  DEFAULT_BUCKET_SIZE,
  DEFAULT_N,
  SORT_ALGORITHM_LABELS,
  type AlgorithmRanking,
} from '@/types/ranking'
import RankingBarChart from './RankingBarChart'
import RankingTable from './RankingTable'

/** Formatea un tiempo en milisegundos con tres decimales. */
function formatMs(ms: number): string {
  return `${ms.toFixed(3)} ms`
}

/**
 * Convierte el texto de un campo en el número que se envía a la API. No valida: un valor no entero o
 * menor que 1 llega al backend, que responde 400 con el mensaje en español que se muestra tal cual (REQ-4).
 */
function toParam(text: string): number {
  return text.trim() === '' ? Number.NaN : Number(text)
}

/** Resultado de un algoritmo: tiempo sin traza, tabla y barras del top N (REQ-5, REQ-11). */
function AlgorithmRankingCard({ result }: { result: AlgorithmRanking }) {
  const name = SORT_ALGORITHM_LABELS[result.algorithm] ?? result.algorithm
  return (
    <Paper component="section" variant="outlined" aria-label={name} sx={{ p: 2 }}>
      <Typography variant="h6" component="h3">
        {name}
      </Typography>
      <Typography variant="body2" color="text.secondary">
        Tiempo del algoritmo (sin traza): {formatMs(result.elapsedMs)}
      </Typography>
      <Stack direction={{ xs: 'column', md: 'row' }} spacing={2} sx={{ mt: 1, alignItems: 'flex-start' }}>
        <Box sx={{ minWidth: 280 }}>
          <RankingTable top={result.top} label={`Tabla del ranking de ${name}`} />
        </Box>
        <RankingBarChart top={result.top} label={`Gráfico de barras del ranking de ${name}`} />
      </Stack>
    </Paper>
  )
}

/**
 * Vista del ranking de términos (REQ-5, REQ-11, AC-4, AC-5, AC-7).
 *
 * Permite elegir N y `bucketSize`, pide el ranking a la API y muestra, por algoritmo, el top N como tabla
 * y como gráfico de barras junto a su tiempo; además el tiempo de preprocesamiento y si los tres rankings
 * completos coinciden. Con `termCount = 0` muestra "Sin términos en el corpus". Los errores de la API se
 * muestran con su mensaje en español, sin reescribirlo.
 */
function RankingView() {
  const [nText, setNText] = useState(String(DEFAULT_N))
  const [bucketText, setBucketText] = useState(String(DEFAULT_BUCKET_SIZE))
  const [params, setParams] = useState({ n: DEFAULT_N, bucketSize: DEFAULT_BUCKET_SIZE })
  const { data, isPending, isError, error } = useTermRanking(params.n, params.bucketSize)

  const submit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setParams({ n: toParam(nText), bucketSize: toParam(bucketText) })
  }

  return (
    <Stack component="section" aria-label="Ranking de términos" spacing={2}>
      <Typography variant="h5" component="h2">
        Ranking de términos
      </Typography>
      <Box component="form" onSubmit={submit} sx={{ display: 'flex', gap: 2, alignItems: 'center' }}>
        <TextField
          label="N (términos del top)"
          value={nText}
          onChange={(event) => setNText(event.target.value)}
          size="small"
          slotProps={{ htmlInput: { inputMode: 'numeric' } }}
        />
        <TextField
          label="bucketSize"
          value={bucketText}
          onChange={(event) => setBucketText(event.target.value)}
          size="small"
          slotProps={{ htmlInput: { inputMode: 'numeric' } }}
        />
        <Button type="submit" variant="contained">
          Calcular ranking
        </Button>
      </Box>

      {isPending && (
        <Stack direction="row" spacing={1} sx={{ alignItems: 'center' }}>
          <CircularProgress size={20} />
          <Typography>Calculando ranking…</Typography>
        </Stack>
      )}

      {isError && <Alert severity="error">{error.message}</Alert>}

      {data && data.termCount === 0 && <Alert severity="info">Sin términos en el corpus</Alert>}

      {data && data.termCount > 0 && (
        <>
          <Typography variant="body2">
            Términos distintos (n): {data.termCount} · Top pedido: {data.n} · bucketSize: {data.bucketSize}
          </Typography>
          <Typography variant="body2">Tiempo de preprocesamiento: {formatMs(data.preprocessingMs)}</Typography>
          {data.allMatch ? (
            <Alert severity="success">Los tres rankings completos coinciden.</Alert>
          ) : (
            <Alert severity="warning">Los rankings completos de los algoritmos no coinciden.</Alert>
          )}
          {data.algorithms.map((result) => (
            <AlgorithmRankingCard key={result.algorithm} result={result} />
          ))}
        </>
      )}
    </Stack>
  )
}

export default RankingView
