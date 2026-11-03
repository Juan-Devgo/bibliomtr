import Box from '@mui/material/Box'
import Typography from '@mui/material/Typography'
import { visibleChar, type TraceFrame } from './traceSteps'

/** Caracteres de contexto a cada lado del patrón alineado. */
const CONTEXT = 10

interface AlignedWindowProps {
  /** Abstract tal como lo devolvió la API. */
  text: string
  /** Patrón tal como se buscó. */
  pattern: string
  /** Paso actual de la traza. */
  frame: TraceFrame
  /** Es el último paso: si el salto deja s > n − m, la búsqueda termina ahí. */
  last: boolean
}

/**
 * Estado de una columna en el paso: comparada con coincidencia o con desajuste (KMP, Boyer-Moore), o dentro de la
 * ventana de Rabin-Karp verificada como ocurrencia o como colisión; `null` si no aplica.
 */
type CellState = 'coincide' | 'desajuste' | 'ocurrencia' | 'colision' | null

/** Color de fondo de una celda según su estado. */
function background(state: CellState): string | undefined {
  switch (state) {
    case 'coincide':
    case 'ocurrencia':
      return 'success.light'
    case 'desajuste':
      return 'error.light'
    case 'colision':
      return 'warning.light'
    default:
      return undefined
  }
}

/**
 * Texto con el patrón alineado debajo en el desplazamiento s del paso (spec 004, REQ-26, AC-16, AC-17).
 *
 * Muestra T en [max(0, s − 10), min(n, s + m + 10)) con sus índices y, debajo, P[k] en la columna s + k (si el
 * patrón pasa del final del texto, las columnas siguen sin texto). Resalta los caracteres comparados en el paso
 * según la traza (verde si coincidieron, rojo si hubo desajuste; en Rabin-Karp la ventana verificada, verde si es
 * ocurrencia y amarilla si es colisión) y muestra la descripción del paso y el salto al paso siguiente (en el último
 * paso, si ese salto deja s > n − m, indica que la búsqueda termina). Lee cada carácter por índice (`text[i]`, `pattern[k]`): no compara el patrón con el texto.
 *
 * Complejidad por render: O(m + c) en tiempo y espacio, con m = longitud del patrón y c = comparaciones del paso.
 */
function AlignedWindow({ text, pattern, frame, last }: AlignedWindowProps) {
  const n = text.length
  const m = pattern.length
  const s = frame.shift
  const start = Math.max(0, s - CONTEXT)
  const end = Math.max(Math.min(n, s + m + CONTEXT), s + m)
  const columns: number[] = []
  for (let column = start; column < end; column++) {
    columns.push(column)
  }
  // Estado de cada índice del texto en este paso, tomado de la traza: en Rabin-Karp toda la ventana [s, s + m)
  // si se verificó (ocurrencia o colisión); en KMP y Boyer-Moore cada carácter comparado.
  const states = new Map<number, CellState>()
  if (frame.window?.hashMatches) {
    for (let column = s; column < s + m; column++) {
      states.set(column, frame.window.collision ? 'colision' : 'ocurrencia')
    }
  }
  for (const compared of frame.compared) {
    states.set(compared.textIndex, compared.matched ? 'coincide' : 'desajuste')
  }
  const cell = { border: 1, borderColor: 'divider', px: 0.75, py: 0.25, textAlign: 'center', minWidth: 22 } as const
  const next = frame.nextShift

  return (
    <Box component="section" aria-label="Alineación del paso">
      <Box sx={{ overflowX: 'auto' }}>
        <Box
          component="table"
          aria-label={`Texto y patrón alineados en s = ${s}`}
          sx={{ borderCollapse: 'collapse', fontFamily: 'monospace', fontSize: '0.85rem' }}
        >
          <tbody>
            <tr>
              <Box component="th" scope="row" sx={{ ...cell, color: 'text.secondary' }}>
                i
              </Box>
              {columns.map((column) => (
                <Box component="td" key={column} sx={{ ...cell, color: 'text.secondary', fontSize: '0.7rem' }}>
                  {column}
                </Box>
              ))}
            </tr>
            <tr>
              <Box component="th" scope="row" sx={cell}>
                T
              </Box>
              {columns.map((column) => {
                const state = states.get(column) ?? null
                return (
                  <Box
                    component="td"
                    key={column}
                    aria-label={`T[${column}]`}
                    data-state={state ?? undefined}
                    sx={{ ...cell, bgcolor: background(state) }}
                  >
                    {column < n ? visibleChar(text[column]) : ''}
                  </Box>
                )
              })}
            </tr>
            <tr>
              <Box component="th" scope="row" sx={cell}>
                P
              </Box>
              {columns.map((column) => {
                const k = column - s
                if (k < 0 || k >= m) {
                  return <Box component="td" key={column} sx={{ ...cell, borderColor: 'transparent' }} />
                }
                const state = states.get(column) ?? null
                return (
                  <Box
                    component="td"
                    key={column}
                    aria-label={`P[${k}] bajo T[${column}]`}
                    data-state={state ?? undefined}
                    sx={{ ...cell, fontWeight: 600, bgcolor: background(state) ?? 'action.hover' }}
                  >
                    {visibleChar(pattern[k])}
                  </Box>
                )
              })}
            </tr>
          </tbody>
        </Box>
      </Box>
      <Typography variant="body2" sx={{ mt: 1 }}>
        {frame.label}
      </Typography>
      <Typography variant="body2" color="text.secondary">
        {next === undefined
          ? 'Último paso de la traza.'
          : `Salto al paso siguiente: s = ${next} (+${next - s})${last && next > n - m ? '; fin: s > n − m' : ''}`}
      </Typography>
    </Box>
  )
}

export default AlignedWindow
