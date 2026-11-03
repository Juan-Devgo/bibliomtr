import Box from '@mui/material/Box'
import Stack from '@mui/material/Stack'
import Typography from '@mui/material/Typography'
import { useState } from 'react'
import TraceStep from '@/components/TraceStep'
import type { BoyerMooreAlignment, BoyerMoorePreprocessing, BoyerMooreTraceResponse } from '@/types/patternSearch'
import AlignedWindow from './AlignedWindow'
import IndexedTable from './IndexedTable'
import { BM_BAD_CHARACTER_FORMULA, BM_GOOD_SUFFIX_FORMULA, BM_SHIFT_FORMULA } from './patternFormulas'
import StepNavigator from './StepNavigator'
import TraceChart from './TraceChart'
import { toFrames, visibleChar } from './traceSteps'

/**
 * Claves de la tabla del carácter malo en orden de primera aparición en el patrón, como las guarda el backend
 * (`LinkedHashMap`). Se recorre el patrón porque un objeto JSON reordena las claves numéricas ("1", "2", …).
 *
 * Complejidad: O(m) en tiempo y O(σ) en espacio, con σ = caracteres distintos del patrón.
 */
function badCharacterKeys(preprocessing: BoyerMoorePreprocessing): string[] {
  const keys: string[] = []
  const seen = new Set<string>()
  for (let k = 0; k < preprocessing.pattern.length; k++) {
    const character = preprocessing.pattern[k]
    if (!seen.has(character) && preprocessing.lastOccurrence[character] !== undefined) {
      seen.add(character)
      keys.push(character)
    }
  }
  return keys
}

/** Detalle de la alineación actual: comparaciones de derecha a izquierda y salto de cada regla (REQ-24). */
function AlignmentDetail({ alignment }: { alignment: BoyerMooreAlignment }) {
  return (
    <Box component="section" aria-label="Detalle de la alineación">
      <Box component="ol" aria-label="Comparaciones de derecha a izquierda" sx={{ my: 0.5, pl: 3 }}>
        {alignment.comparisons.map((comparison) => (
          <li key={comparison.j}>
            <Typography variant="body2" component="span">
              {`j = ${comparison.j}: T[${comparison.textIndex}] = '${visibleChar(comparison.textChar)}' y ` +
                `P[${comparison.j}] = '${visibleChar(comparison.patternChar)}' ` +
                `${comparison.matched ? 'coinciden' : 'no coinciden'}`}
            </Typography>
          </li>
        ))}
      </Box>
      <Stack direction="row" spacing={3} sx={{ flexWrap: 'wrap' }}>
        <Typography variant="body2">
          {alignment.occurrence ? 'Ocurrencia completa' : `Desajuste en j = ${alignment.mismatchIndex ?? '—'}`}
        </Typography>
        <Typography variant="body2">{`Salto del carácter malo: ${alignment.badCharacterShift ?? '—'}`}</Typography>
        <Typography variant="body2">{`Salto del sufijo bueno: ${alignment.goodSuffixShift ?? '—'}`}</Typography>
        <Typography variant="body2" sx={{ fontWeight: 600 }}>
          {`Salto aplicado: ${alignment.appliedShift}`}
        </Typography>
      </Stack>
    </Box>
  )
}

/**
 * Traza de Boyer-Moore sobre un abstract (spec 004, REQ-24, REQ-26, REQ-27, AC-17).
 *
 * Paso 1: la tabla del carácter malo (solo los caracteres del patrón). Paso 2: el arreglo de bordes f, la tabla del
 * sufijo bueno indexada por la posición j del desajuste y el salto tras una ocurrencia. Paso 3: las alineaciones
 * paso a paso, con la comparación de derecha a izquierda, el desajuste u ocurrencia, el salto de cada regla, el
 * salto aplicado y el gráfico del salto por alineación. Solo dibuja lo que trae la traza.
 *
 * Complejidad por render: O(m + c) en tiempo y espacio, con m = longitud del patrón y c = comparaciones.
 */
function BoyerMooreTraceView({ response }: { response: BoyerMooreTraceResponse }) {
  const [index, setIndex] = useState(0)
  const preprocessing = response.preprocessing
  const trace = response.trace
  if (!preprocessing || !trace) {
    return null
  }
  const frames = toFrames(response)
  const current = Math.min(index, frames.length - 1)
  const keys = badCharacterKeys(preprocessing)
  const m = preprocessing.pattern.length
  const positions = preprocessing.goodSuffixShift.map((_, j) => j)

  return (
    <Stack component="ol" spacing={2} sx={{ p: 0, m: 0 }}>
      <TraceStep number={1} title="Regla del carácter malo" formula={BM_BAD_CHARACTER_FORMULA}>
        <IndexedTable
          label="Tabla del carácter malo"
          indexHeader="c"
          columns={keys.map((key) => visibleChar(key))}
          rows={[{ header: 'last(c)', cells: keys.map((key) => preprocessing.lastOccurrence[key]) }]}
        />
        <Typography variant="body2" color="text.secondary">
          Un carácter que no está en el patrón vale −1.
        </Typography>
      </TraceStep>
      <TraceStep number={2} title="Regla del sufijo bueno (fuerte)" formula={BM_GOOD_SUFFIX_FORMULA}>
        <IndexedTable
          label="Arreglo de bordes f"
          indexHeader="i"
          columns={preprocessing.borderPositions.map((_, i) => i)}
          rows={[{ header: 'f[i]', cells: preprocessing.borderPositions }]}
        />
        <IndexedTable
          label="Tabla del sufijo bueno"
          indexHeader="j"
          columns={positions}
          rows={[
            { header: 'P[j]', cells: positions.map((j) => visibleChar(preprocessing.pattern[j])) },
            { header: 'salto_sb(j)', cells: preprocessing.goodSuffixShift },
          ]}
        />
        <Typography variant="body2">
          {`borde(P) = ${preprocessing.patternBorder} · salto tras ocurrencia m − borde(P) = ` +
            `${m} − ${preprocessing.patternBorder} = ${preprocessing.matchShift}`}
        </Typography>
      </TraceStep>
      <TraceStep number={3} title="Alineaciones" formula={BM_SHIFT_FORMULA}>
        {frames.length === 0 ? (
          <Typography variant="body2" color="text.secondary">
            Sin alineaciones: el texto está vacío o es más corto que el patrón.
          </Typography>
        ) : (
          <Stack spacing={1}>
            <StepNavigator
              index={current}
              count={frames.length}
              onChange={setIndex}
              label="Navegación de las alineaciones"
            />
            <AlignedWindow
              text={response.abstractText}
              pattern={response.pattern}
              frame={frames[current]}
              last={current === frames.length - 1}
            />
            <AlignmentDetail alignment={trace.alignments[current]} />
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

export default BoyerMooreTraceView
