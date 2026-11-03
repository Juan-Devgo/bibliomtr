import Box from '@mui/material/Box'
import Paper from '@mui/material/Paper'
import Typography from '@mui/material/Typography'
import { highlightSegments } from './highlightSegments'

interface HighlightedAbstractProps {
  /** Título del artículo elegido. */
  title: string
  /** Abstract tal como lo devolvió la API. */
  text: string
  /** Posiciones del algoritmo elegido en este abstract, ascendentes. */
  positions: readonly number[]
  /** Longitud m del patrón buscado. */
  patternLength: number
  /** Nombre del algoritmo cuyas posiciones se resaltan. */
  algorithmLabel: string
}

/**
 * Abstract de un artículo con las ocurrencias resaltadas en las posiciones que devolvió la API (spec 004,
 * REQ-20, REQ-21, AC-15).
 *
 * Cada tramo de `highlightSegments` se dibuja con `text.slice(start, end)`: los resaltados en un `<mark>`, con
 * una marca (borde izquierdo, `aria-label` "Inicio de ocurrencia en <p>") en cada posición inicial; así las
 * ocurrencias solapadas se ven como un solo tramo continuo con una marca por inicio ("aaaa", "aa": tramo [0, 4)
 * con marcas en 0, 1 y 2). No busca ni compara el patrón: solo corta el texto en esos índices UTF-16.
 *
 * Complejidad por render: O(n + k) en tiempo y espacio, con n = longitud del abstract y k = posiciones.
 */
function HighlightedAbstract({ title, text, positions, patternLength, algorithmLabel }: HighlightedAbstractProps) {
  const segments = highlightSegments(text.length, positions, patternLength)

  return (
    <Paper component="section" variant="outlined" aria-label="Abstract con ocurrencias resaltadas" sx={{ p: 2 }}>
      <Typography variant="subtitle1" component="h3" sx={{ fontWeight: 600 }}>
        {title}
      </Typography>
      <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
        {`Posiciones de ${algorithmLabel}: ${positions.length === 0 ? 'ninguna' : positions.join(', ')}`}
      </Typography>
      <Typography
        component="p"
        variant="body2"
        aria-label="Texto del abstract"
        sx={{ whiteSpace: 'pre-wrap', wordBreak: 'break-word', lineHeight: 1.9 }}
      >
        {segments.map((segment) =>
          segment.highlighted ? (
            <Box
              component="mark"
              key={segment.start}
              sx={{ bgcolor: 'warning.light', color: 'inherit', borderRadius: 0.5 }}
            >
              {segment.starts.map((start, index) => (
                <Box
                  component="span"
                  key={start}
                  aria-label={`Inicio de ocurrencia en ${start}`}
                  data-start={start}
                  sx={{ borderLeft: 2, borderColor: 'error.main' }}
                >
                  {text.slice(start, segment.starts[index + 1] ?? segment.end)}
                </Box>
              ))}
            </Box>
          ) : (
            <span key={segment.start}>{text.slice(segment.start, segment.end)}</span>
          ),
        )}
      </Typography>
    </Paper>
  )
}

export default HighlightedAbstract
