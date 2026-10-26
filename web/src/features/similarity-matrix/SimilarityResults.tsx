import Box from '@mui/material/Box'
import Stack from '@mui/material/Stack'
import Typography from '@mui/material/Typography'
import type { ComparisonResponse, SimilarityAlgorithmSlug } from '@/types/similarity'
import AlgorithmResultCard from './AlgorithmResultCard'
import { articleLabel, formatMs } from './resultFormat'

interface SimilarityResultsProps {
  /** Respuesta de `POST /api/similarity/comparisons`. */
  response: ComparisonResponse
  /** Se llama con el algoritmo y los índices (fila, columna) de la celda elegida en su mapa de calor. */
  onSelectCell?: (algorithm: SimilarityAlgorithmSlug, row: number, column: number) => void
}

/**
 * Resultados de una comparación (REQ-17, REQ-22, AC-13, AC-19): "Preprocesamiento: <ms> ms", medido aparte
 * de los algoritmos (constitución P11), la granularidad de las distancias de edición y las 8 tarjetas en el
 * orden recibido (4 clásicos y 2 modelos × 2 métricas). Filas y columnas de cada mapa de calor siguen el
 * orden de `articles`.
 *
 * Complejidad por render: O(R·N²) en tiempo y espacio, con R = 8 resultados y N = artículos comparados.
 */
function SimilarityResults({ response, onSelectCell }: SimilarityResultsProps) {
  const labels = response.articles.map((article) => articleLabel(article.id))

  return (
    <Stack component="section" aria-label="Resultados de la comparación" spacing={2}>
      <Typography variant="h5" component="h2">
        Matrices de similitud
      </Typography>
      <Typography variant="body2">{`Preprocesamiento: ${formatMs(response.preprocessingMs)}`}</Typography>
      <Typography variant="body2" color="text.secondary">
        {`Granularidad de Levenshtein y Needleman-Wunsch: ${response.editGranularity === 'WORDS' ? 'palabras' : 'caracteres'}`}
        {' (bibliomtr.similarity.edit-granularity)'}
      </Typography>
      <Box component="ul" sx={{ m: 0, p: 0, pl: 2 }}>
        {response.articles.map((article) => (
          <Typography component="li" variant="body2" key={article.id}>
            {`${articleLabel(article.id)}: ${article.title}`}
          </Typography>
        ))}
      </Box>
      <Box
        sx={{
          display: 'grid',
          gap: 2,
          gridTemplateColumns: { xs: '1fr', md: 'repeat(2, minmax(0, 1fr))' },
          alignItems: 'start',
        }}
      >
        {response.results.map((result) => (
          <AlgorithmResultCard
            key={result.algorithm}
            result={result}
            labels={labels}
            onSelectCell={onSelectCell ? (row, column) => onSelectCell(result.algorithm, row, column) : undefined}
          />
        ))}
      </Box>
    </Stack>
  )
}

export default SimilarityResults
