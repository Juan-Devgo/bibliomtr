import Paper from '@mui/material/Paper'
import Typography from '@mui/material/Typography'
import type { ReactNode } from 'react'
import { BlockMath } from 'react-katex'

interface TraceStepProps {
  /** Número del paso, desde 1, en el orden de ejecución del algoritmo. */
  number: number
  /** Nombre del paso (p. ej. "Conteo"). */
  title: string
  /** Fórmula del paso en LaTeX; KaTeX solo la dibuja (constitución P2, P6). */
  formula: string
  children: ReactNode
}

/**
 * Paso numerado de una traza: título, fórmula en KaTeX y resultados intermedios (constitución P6, P7).
 * Se dibuja como un elemento de lista para que los pasos de una traza formen una lista ordenada.
 *
 * Complejidad por render: O(1) más la de `children`.
 */
function TraceStep({ number, title, formula, children }: TraceStepProps) {
  return (
    <Paper component="li" variant="outlined" aria-label={title} sx={{ p: 2, listStyle: 'none' }}>
      <Typography variant="subtitle1" component="h3" sx={{ fontWeight: 600 }}>
        {`Paso ${number}. ${title}`}
      </Typography>
      <BlockMath math={formula} />
      {children}
    </Paper>
  )
}

export default TraceStep
