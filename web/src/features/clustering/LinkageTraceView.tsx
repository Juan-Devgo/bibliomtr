import Box from '@mui/material/Box'
import Typography from '@mui/material/Typography'
import PagedMatrix from '@/components/PagedMatrix'
import TraceStep from '@/components/TraceStep'
import StepList from '@/features/similarity-trace/StepList'
import type { LinkageTrace } from '@/types/clustering'
import { LINKAGE_FORMULAS, PAIR_FORMULA } from './clusteringFormulas'
import { formatDistance, groupLabel } from './traceFormat'

interface LinkageTraceViewProps {
  algorithm: keyof typeof LINKAGE_FORMULAS
  trace: LinkageTrace
}

/**
 * Traza de Single, Complete o Average Linkage paso a paso (spec 003, REQ-16, REQ-17, AC-15): por cada fusión,
 * la matriz entre los grupos activos, el par elegido con su distancia y la fila recalculada del grupo nuevo,
 * con la fórmula de Lance-Williams del algoritmo en KaTeX.
 *
 * Complejidad por render: O(Σ min(Gₖ, 25)²) en tiempo y espacio, con Gₖ = grupos activos en el paso k.
 */
function LinkageTraceView({ algorithm, trace }: LinkageTraceViewProps) {
  const formula = `${PAIR_FORMULA} \\qquad ${LINKAGE_FORMULAS[algorithm]}`

  return (
    <StepList>
      {trace.steps.map((step, k) => {
        const labels = step.clusters.map((id, index) => groupLabel(id, step.members[index]))
        const membersOf = (id: number) => step.members[step.clusters.indexOf(id)] ?? []
        const merged = [...membersOf(step.left), ...membersOf(step.right)]
        return (
          <TraceStep
            key={step.newCluster}
            number={k + 1}
            title={`Fusión de G${step.left} y G${step.right} en G${step.newCluster}`}
            formula={formula}
          >
            <PagedMatrix
              matrix={step.distances}
              rowLabels={labels}
              colLabels={labels}
              label={`Distancias entre grupos, paso ${k + 1}`}
              renderCell={formatDistance}
            />
            <Typography variant="body2">
              {`Par elegido: G${step.left} y G${step.right} · distancia ${formatDistance(step.distance)} · nuevo grupo ${groupLabel(step.newCluster, merged)}`}
            </Typography>
            {step.remaining.length === 0 ? (
              <Typography variant="body2" color="text.secondary">
                No quedan grupos por recalcular: es la última fusión.
              </Typography>
            ) : (
              <Box component="ul" aria-label={`Fila recalculada, paso ${k + 1}`} sx={{ my: 0.5, pl: 3 }}>
                {step.remaining.map((h, index) => (
                  <li key={h}>
                    <Typography variant="body2" component="span" sx={{ fontFamily: 'monospace' }}>
                      {`d(G${step.newCluster}, G${h}) = ${formatDistance(step.updatedRow[index])}`}
                    </Typography>
                  </li>
                ))}
              </Box>
            )}
          </TraceStep>
        )
      })}
    </StepList>
  )
}

export default LinkageTraceView
