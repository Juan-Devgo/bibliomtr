import Box from '@mui/material/Box'
import Stack from '@mui/material/Stack'
import Typography from '@mui/material/Typography'
import { useState } from 'react'
import TraceStep from '@/components/TraceStep'
import type { KmpTraceResponse } from '@/types/patternSearch'
import AlignedWindow from './AlignedWindow'
import IndexedTable from './IndexedTable'
import { KMP_PREFIX_FORMULA, KMP_SEARCH_FORMULA } from './patternFormulas'
import StepNavigator from './StepNavigator'
import TraceChart from './TraceChart'
import { toFrames, visibleChar } from './traceSteps'

/**
 * Traza de KMP sobre un abstract (spec 004, REQ-23, REQ-26, REQ-27, AC-16).
 *
 * Paso 1: la tabla π con cada paso de su construcción (los k probados y π[i]). Paso 2: la búsqueda paso a paso,
 * una comparación por paso, con el patrón alineado en s = i − j de la traza, la comparación actual resaltada, el
 * retroceso aplicado y el gráfico de s por comparación. Solo dibuja lo que trae la traza.
 *
 * Complejidad por render: O(m + c) en tiempo y espacio, con m = longitud del patrón y c = comparaciones.
 */
function KmpTraceView({ response }: { response: KmpTraceResponse }) {
  const [index, setIndex] = useState(0)
  const preprocessing = response.preprocessing
  const trace = response.trace
  if (!preprocessing || !trace) {
    return null
  }
  const frames = toFrames(response)
  const current = Math.min(index, frames.length - 1)
  const columns = preprocessing.pi.map((_, i) => i)

  return (
    <Stack component="ol" spacing={2} sx={{ p: 0, m: 0 }}>
      <TraceStep number={1} title="Función de prefijo π" formula={KMP_PREFIX_FORMULA}>
        <IndexedTable
          label="Tabla π"
          indexHeader="i"
          columns={columns}
          rows={[
            { header: 'P[i]', cells: columns.map((i) => visibleChar(preprocessing.pattern[i])) },
            { header: 'π[i]', cells: preprocessing.pi },
          ]}
        />
        {preprocessing.steps.length === 0 ? (
          <Typography variant="body2" color="text.secondary">
            Sin pasos de construcción: con m = 1, π = [0].
          </Typography>
        ) : (
          <Box component="ol" aria-label="Construcción de π" sx={{ my: 0.5, pl: 3 }}>
            {preprocessing.steps.map((step) => (
              <li key={step.i}>
                <Typography variant="body2" component="span">
                  {`i = ${step.i}, P[${step.i}] = '${visibleChar(step.character)}': k probados ` +
                    `${step.candidates.join(' → ')} → π[${step.i}] = ${step.value}`}
                </Typography>
              </li>
            ))}
          </Box>
        )}
      </TraceStep>
      <TraceStep number={2} title="Búsqueda con π" formula={KMP_SEARCH_FORMULA}>
        {frames.length === 0 ? (
          <Typography variant="body2" color="text.secondary">
            Sin comparaciones: el texto está vacío o es más corto que el patrón.
          </Typography>
        ) : (
          <Stack spacing={1}>
            <StepNavigator
              index={current}
              count={frames.length}
              onChange={setIndex}
              label="Navegación de las comparaciones"
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
      </TraceStep>
    </Stack>
  )
}

export default KmpTraceView
