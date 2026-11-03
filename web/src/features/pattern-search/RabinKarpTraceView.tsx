import Box from '@mui/material/Box'
import Stack from '@mui/material/Stack'
import Typography from '@mui/material/Typography'
import { useState } from 'react'
import TraceStep from '@/components/TraceStep'
import type { RabinKarpTraceResponse } from '@/types/patternSearch'
import AlignedWindow from './AlignedWindow'
import { RK_HASH_FORMULA, RK_ROLLING_FORMULA } from './patternFormulas'
import StepNavigator from './StepNavigator'
import TraceChart from './TraceChart'
import { toFrames } from './traceSteps'

/**
 * Traza de Rabin-Karp sobre un abstract (spec 004, REQ-25, REQ-26, REQ-27, AC-18).
 *
 * Paso 1: d, q, h = d^(m−1) mod q, el hash del patrón y el hash inicial t(0). Paso 2: las ventanas paso a paso,
 * con el patrón alineado en s, el hash t(s), si coincide con h(P) y el resultado de la verificación; el gráfico
 * del hash por ventana frente a h(P) con las colisiones marcadas, y la lista de colisiones. Solo dibuja lo que trae
 * la traza.
 *
 * Complejidad por render: O(m + w) en tiempo y espacio, con m = longitud del patrón y w = ventanas.
 */
function RabinKarpTraceView({ response }: { response: RabinKarpTraceResponse }) {
  const [index, setIndex] = useState(0)
  const trace = response.trace
  if (!response.preprocessing || !trace) {
    return null
  }
  const frames = toFrames(response)
  const current = Math.min(index, frames.length - 1)
  const m = response.pattern.length

  return (
    <Stack component="ol" spacing={2} sx={{ p: 0, m: 0 }}>
      <TraceStep number={1} title="Hash del patrón" formula={RK_HASH_FORMULA}>
        <Box component="ul" aria-label="Parámetros de Rabin-Karp" sx={{ my: 0.5, pl: 3 }}>
          <li>
            <Typography variant="body2" component="span">{`d = ${trace.d}`}</Typography>
          </li>
          <li>
            <Typography variant="body2" component="span">{`q = ${trace.q}`}</Typography>
          </li>
          <li>
            <Typography variant="body2" component="span">{`h = d^(${m - 1}) mod q = ${trace.h}`}</Typography>
          </li>
          <li>
            <Typography variant="body2" component="span">{`h(P) = ${trace.patternHash}`}</Typography>
          </li>
          <li>
            <Typography variant="body2" component="span">
              {trace.initialWindowHash === null
                ? 't(0): sin ventanas (texto más corto que el patrón)'
                : `t(0) = ${trace.initialWindowHash}`}
            </Typography>
          </li>
        </Box>
      </TraceStep>
      <TraceStep number={2} title="Ventanas con rolling hash" formula={RK_ROLLING_FORMULA}>
        {frames.length === 0 ? (
          <Typography variant="body2" color="text.secondary">
            Sin ventanas: el texto está vacío o es más corto que el patrón.
          </Typography>
        ) : (
          <Stack spacing={1}>
            <StepNavigator
              index={current}
              count={frames.length}
              onChange={setIndex}
              label="Navegación de las ventanas"
            />
            <AlignedWindow
              text={response.abstractText}
              pattern={response.pattern}
              frame={frames[current]}
              last={current === frames.length - 1}
            />
          </Stack>
        )}
        <Typography variant="body2" sx={{ my: 1 }}>
          {`Ocurrencias: ${trace.occurrences.length === 0 ? 'ninguna' : trace.occurrences.join(', ')}`}
        </Typography>
        <TraceChart response={response} />
        <Typography variant="subtitle2" component="h4">
          Colisiones
        </Typography>
        {trace.collisions.length === 0 ? (
          <Typography variant="body2" color="text.secondary">
            Sin colisiones: toda ventana con el hash del patrón fue una ocurrencia.
          </Typography>
        ) : (
          <Box component="ul" aria-label="Colisiones" sx={{ my: 0.5, pl: 3 }}>
            {trace.collisions.map((shift) => (
              <li key={shift}>
                <Typography variant="body2" component="span">
                  {`s = ${shift}: t(${shift}) = h(P) = ${trace.patternHash} con texto distinto`}
                </Typography>
              </li>
            ))}
          </Box>
        )}
      </TraceStep>
    </Stack>
  )
}

export default RabinKarpTraceView
