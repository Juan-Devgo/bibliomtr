import Box from '@mui/material/Box'
import Typography from '@mui/material/Typography'
import TraceStep from '@/components/TraceStep'
import StepList from '@/features/similarity-trace/StepList'
import type { DianaIteration, DianaTrace } from '@/types/clustering'
import { DIANA_FORMULA } from './clusteringFormulas'
import { formatDistance, memberSet } from './traceFormat'

/** Estilo compartido de las tablas de la traza. */
const TABLE_SX = {
  borderCollapse: 'collapse',
  fontFamily: 'monospace',
  fontSize: '0.75rem',
  my: 0.5,
  '& th, & td': { border: 1, borderColor: 'divider', px: 0.75, py: 0.25, textAlign: 'center' },
  '& th': { color: 'text.secondary', fontWeight: 600 },
} as const

/** Tabla de una iteración: media a R, media a S y D(i) por artículo del resto R. */
function IterationTable({ iteration, label }: { iteration: DianaIteration; label: string }) {
  return (
    <Box component="section" aria-label={label} sx={{ my: 1 }}>
      <Typography variant="body2">
        {`${label}: S = ${memberSet(iteration.splinter)}, R = ${memberSet(iteration.remainder)}`}
      </Typography>
      <Box sx={{ overflowX: 'auto' }}>
        <Box component="table" aria-label={`${label}, tabla`} sx={TABLE_SX}>
          <thead>
            <tr>
              <th scope="col">i</th>
              <th scope="col">media a R \ {'{'}i{'}'}</th>
              <th scope="col">media a S</th>
              <th scope="col">D(i)</th>
            </tr>
          </thead>
          <tbody>
            {iteration.remainder.map((i, index) => (
              <tr key={i}>
                <th scope="row">{i}</th>
                <td>{formatDistance(iteration.averageToRemainder[index])}</td>
                <td>{formatDistance(iteration.averageToSplinter[index])}</td>
                <td>{formatDistance(iteration.differences[index])}</td>
              </tr>
            ))}
          </tbody>
        </Box>
      </Box>
      <Typography variant="body2" color="text.secondary">
        {iteration.moved >= 0 ? `Se mueve ${iteration.moved} a S.` : 'Fin: ningún D(i) es positivo.'}
      </Typography>
    </Box>
  )
}

interface DianaTraceViewProps {
  trace: DianaTrace
}

/**
 * Traza de DIANA paso a paso (spec 003, REQ-16, REQ-17, AC-15): por cada división, los grupos activos con su
 * diámetro, las iteraciones que arman el grupo disidente S (medias y diferencias D(i), artículo movido o fin) y
 * las dos partes, con las fórmulas del diámetro y de D(i) en KaTeX. Las fusiones del dendrograma son las
 * divisiones en orden inverso.
 *
 * Complejidad por render: O(Σ (Gₖ + Iₖ · |C|)) en tiempo y espacio, con Gₖ grupos e Iₖ iteraciones del paso k.
 */
function DianaTraceView({ trace }: DianaTraceViewProps) {
  return (
    <StepList>
      {trace.steps.map((step, k) => {
        const split = step.clusters[step.splitIndex] ?? []
        return (
          <TraceStep
            key={k}
            number={k + 1}
            title={`División de ${memberSet(split)} con diámetro ${formatDistance(step.diameter)}`}
            formula={DIANA_FORMULA}
          >
            <Box sx={{ overflowX: 'auto' }}>
              <Box component="table" aria-label={`Grupos y diámetros, paso ${k + 1}`} sx={TABLE_SX}>
                <thead>
                  <tr>
                    <th scope="col">grupo</th>
                    <th scope="col">diam</th>
                  </tr>
                </thead>
                <tbody>
                  {step.clusters.map((members, index) => (
                    <tr key={members.join('-')}>
                      <th scope="row">{`${memberSet(members)}${index === step.splitIndex ? ' (se divide)' : ''}`}</th>
                      <td>{formatDistance(step.diameters[index])}</td>
                    </tr>
                  ))}
                </tbody>
              </Box>
            </Box>
            {step.iterations.map((iteration, t) => (
              <IterationTable key={t} iteration={iteration} label={`Paso ${k + 1}, iteración ${t + 1}`} />
            ))}
            <Typography variant="body2">
              {`Partes: S = ${memberSet(step.splinter)} y C \\ S = ${memberSet(step.remainder)}`}
            </Typography>
          </TraceStep>
        )
      })}
    </StepList>
  )
}

export default DianaTraceView
