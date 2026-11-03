import TextField from '@mui/material/TextField'
import {
  STRING_MATCHING_ALGORITHMS,
  type AlgorithmOccurrences,
  type PatternSearchResponse,
  type StringMatchingSlug,
} from '@/types/patternSearch'

/** Resultados CALCULADO de la respuesta, en el orden KMP, Boyer-Moore, Rabin-Karp. */
function calculatedResults(response: PatternSearchResponse): AlgorithmOccurrences[] {
  const calculated: AlgorithmOccurrences[] = []
  for (const { slug } of STRING_MATCHING_ALGORITHMS) {
    const result = response.algorithms.find((candidate) => candidate.algorithm === slug)
    if (result && result.status === 'CALCULADO') {
      calculated.push(result)
    }
  }
  return calculated
}

/**
 * Algoritmo cuyas posiciones se resaltan por defecto: el primero CALCULADO en el orden KMP, Boyer-Moore,
 * Rabin-Karp; `null` si todos están pendientes (spec 004, REQ-20, AC-15).
 *
 * Complejidad: O(1) (3 algoritmos).
 */
// oxlint-disable-next-line react/only-export-components -- el plan (§6) ubica la regla junto a su selector
export function defaultAlgorithm(response: PatternSearchResponse): StringMatchingSlug | null {
  return calculatedResults(response)[0]?.algorithm ?? null
}

interface AlgorithmSelectorProps {
  response: PatternSearchResponse
  /** Algoritmo elegido; debe ser uno CALCULADO de la respuesta. */
  value: StringMatchingSlug
  onChange: (algorithm: StringMatchingSlug) => void
}

/**
 * Selector del algoritmo cuyas posiciones se resaltan en el abstract (spec 004, REQ-20, AC-15). Solo ofrece los
 * algoritmos CALCULADO: un pendiente no tiene posiciones que mostrar.
 *
 * Complejidad por render: O(1) (3 opciones).
 */
function AlgorithmSelector({ response, value, onChange }: AlgorithmSelectorProps) {
  return (
    <TextField
      select
      label="Algoritmo resaltado"
      value={value}
      onChange={(event) => onChange(event.target.value as StringMatchingSlug)}
      size="small"
      sx={{ minWidth: 280 }}
      slotProps={{ select: { native: true }, inputLabel: { shrink: true } }}
    >
      {calculatedResults(response).map((result) => (
        <option key={result.algorithm} value={result.algorithm}>
          {result.label}
        </option>
      ))}
    </TextField>
  )
}

export default AlgorithmSelector
