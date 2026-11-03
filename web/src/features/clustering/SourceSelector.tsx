import TextField from '@mui/material/TextField'
import { SIMILARITY_ALGORITHM_LABELS, SIMILARITY_ALGORITHMS, type SimilarityAlgorithmSlug } from '@/types/similarity'

interface SourceSelectorProps {
  value: SimilarityAlgorithmSlug
  onChange: (source: SimilarityAlgorithmSlug) => void
}

/**
 * Selector de la fuente de similitud del clustering: uno de los 8 resultados del Requerimiento 1 (spec 003,
 * REQ-13). La distancia que agrupan los algoritmos es d = 1 − s de esa fuente.
 *
 * Complejidad por render: O(1) (8 opciones).
 */
function SourceSelector({ value, onChange }: SourceSelectorProps) {
  return (
    <TextField
      select
      label="Fuente de similitud"
      value={value}
      onChange={(event) => onChange(event.target.value as SimilarityAlgorithmSlug)}
      size="small"
      sx={{ minWidth: 280 }}
      slotProps={{ select: { native: true }, inputLabel: { shrink: true } }}
    >
      {SIMILARITY_ALGORITHMS.map((slug) => (
        <option key={slug} value={slug}>
          {SIMILARITY_ALGORITHM_LABELS[slug]}
        </option>
      ))}
    </TextField>
  )
}

export default SourceSelector
