import Alert from '@mui/material/Alert'
import CircularProgress from '@mui/material/CircularProgress'
import Stack from '@mui/material/Stack'
import Typography from '@mui/material/Typography'
import { usePatternTrace } from '@/api/usePatternTrace'
import type { PatternTraceResponse, StringMatchingSlug } from '@/types/patternSearch'
import BoyerMooreTraceView from './BoyerMooreTraceView'
import KmpTraceView from './KmpTraceView'
import RabinKarpTraceView from './RabinKarpTraceView'

interface PatternTraceViewProps {
  /** Algoritmo elegido en el selector. */
  algorithm: StringMatchingSlug
  /** Artículo elegido en las tablas de ocurrencias. */
  articleId: number
  /** Patrón buscado, sin recortar. */
  pattern: string
  /** q de Rabin-Karp como texto; si falta, el backend usa 101. */
  q?: string
}

/** Vista de pasos del algoritmo de una respuesta CALCULADO. */
function AlgorithmTrace({ response }: { response: PatternTraceResponse }) {
  switch (response.algorithm) {
    case 'kmp':
      return <KmpTraceView response={response} />
    case 'boyer-moore':
      return <BoyerMooreTraceView response={response} />
    case 'rabin-karp':
      return <RabinKarpTraceView response={response} />
    default:
      return null
  }
}

/**
 * Visor de la traza de la búsqueda de patrones (spec 004, REQ-22, REQ-26, REQ-28, AC-26).
 *
 * Pide `GET /api/pattern-search/trace` con el algoritmo y el artículo elegidos, el patrón y q, y muestra las tablas
 * del preprocesamiento, la búsqueda paso a paso con el patrón alineado sobre el abstract, las fórmulas en KaTeX y
 * el gráfico del algoritmo. Con un algoritmo PENDIENTE muestra "Pendiente de implementar: <archivo>#<método>"; los
 * errores de la API se muestran con su mensaje en español. Cada traza nueva empieza en el primer paso. No busca ni
 * compara el patrón con el texto (REQ-21).
 *
 * Complejidad por render: la de la vista del algoritmo, O(m + c) con c = tamaño de la traza.
 */
function PatternTraceView({ algorithm, articleId, pattern, q }: PatternTraceViewProps) {
  const { data, isFetching, isError, error } = usePatternTrace(algorithm, articleId, pattern, q)

  return (
    <Stack component="section" aria-label="Traza paso a paso" spacing={2}>
      <Typography variant="h6" component="h2">
        Traza paso a paso
      </Typography>
      {isFetching && (
        <Stack direction="row" spacing={1} sx={{ alignItems: 'center' }}>
          <CircularProgress size={20} />
          <Typography>Cargando traza…</Typography>
        </Stack>
      )}
      {isError && <Alert severity="error">{error.message}</Alert>}
      {data && !isFetching && !isError && (
        <Stack spacing={1}>
          <Typography variant="subtitle1" component="p" sx={{ fontWeight: 600 }}>
            {`${data.label} · #${data.articleId} ${data.title} · m = ${data.pattern.length} · n = ` +
              `${data.abstractText.length}${data.algorithm === 'rabin-karp' ? ` · q = ${data.q}` : ''}`}
          </Typography>
          {data.status === 'PENDIENTE' ? (
            <Alert severity="warning" sx={{ wordBreak: 'break-all' }}>
              {`Pendiente de implementar: ${data.pendingLocation ?? ''}`}
            </Alert>
          ) : (
            // La clave reinicia el paso actual cuando cambia la traza.
            <AlgorithmTrace
              key={`${data.algorithm}-${data.articleId}-${data.q}-${data.pattern}`}
              response={data}
            />
          )}
        </Stack>
      )}
    </Stack>
  )
}

export default PatternTraceView
