import Box from '@mui/material/Box'

interface HeatmapProps {
  /** Encabezado de cada fila y columna (p. ej. `#12`), en el orden de la matriz. */
  labels: readonly string[]
  /** Similitudes N×N en [0,1]. */
  matrix: readonly (readonly number[])[]
  /** Se llama con los índices (fila, columna) de la celda elegida; sin ella, las celdas no son clicables. */
  onSelectCell?: (row: number, column: number) => void
  /** Nombre accesible de la tabla. */
  ariaLabel?: string
}

/** Luminosidad HSL de una similitud v ∈ [0,1]: 95 % para v = 0 y 40 % para v = 1. */
function lightness(value: number): number {
  return 95 - 55 * value
}

/** Texto de una similitud con 3 decimales. */
function formatValue(value: number): string {
  return value.toFixed(3)
}

/**
 * Mapa de calor de una matriz de similitud N×N (REQ-17, AC-13), dibujado como una tabla HTML propia: D3 queda
 * reservado al dendrograma y Recharts no tiene mapa de calor (plan, decisiones).
 *
 * Cada celda muestra el valor con 3 decimales sobre el fondo `hsl(210, 80%, (95 − 55·v)%)`, más oscuro cuanto
 * más similar, y tiene el nombre accesible "fila i, columna j: v", con i y j los encabezados. Con
 * `onSelectCell`, cada celda es un botón que informa su par (fila, columna), p. ej. para pedir su traza.
 *
 * Complejidad por render: O(N²) en tiempo y espacio, con N = número de filas.
 */
function Heatmap({ labels, matrix, onSelectCell, ariaLabel }: HeatmapProps) {
  return (
    <Box sx={{ overflowX: 'auto' }}>
      <Box
        component="table"
        aria-label={ariaLabel}
        sx={{ borderCollapse: 'collapse', fontFamily: 'monospace', fontSize: '0.75rem' }}
      >
        <thead>
          <tr>
            <Box component="th" scope="col" sx={{ p: 0.5 }} />
            {labels.map((label, column) => (
              <Box component="th" scope="col" key={column} sx={{ p: 0.5, fontWeight: 600 }}>
                {label}
              </Box>
            ))}
          </tr>
        </thead>
        <tbody>
          {matrix.map((row, i) => (
            <tr key={i}>
              <Box component="th" scope="row" sx={{ p: 0.5, fontWeight: 600, textAlign: 'right' }}>
                {labels[i]}
              </Box>
              {row.map((value, j) => {
                const text = formatValue(value)
                const name = `fila ${labels[i]}, columna ${labels[j]}: ${text}`
                const l = lightness(value)
                // Con fondo oscuro (luminosidad < 60 %) el texto pasa a blanco para mantener el contraste.
                const color = l < 60 ? '#fff' : '#000'
                return (
                  <Box
                    component="td"
                    key={j}
                    aria-label={name}
                    sx={{
                      p: 0,
                      minWidth: 48,
                      textAlign: 'center',
                      border: 1,
                      borderColor: 'background.paper',
                      bgcolor: `hsl(210, 80%, ${l}%)`,
                      color,
                    }}
                  >
                    {onSelectCell ? (
                      <Box
                        component="button"
                        type="button"
                        aria-label={name}
                        onClick={() => onSelectCell(i, j)}
                        sx={{
                          all: 'unset',
                          boxSizing: 'border-box',
                          display: 'block',
                          width: '100%',
                          px: 0.75,
                          py: 0.5,
                          cursor: 'pointer',
                          '&:hover, &:focus-visible': { outline: '2px solid', outlineOffset: -2 },
                        }}
                      >
                        {text}
                      </Box>
                    ) : (
                      <Box component="span" sx={{ display: 'block', px: 0.75, py: 0.5 }}>
                        {text}
                      </Box>
                    )}
                  </Box>
                )
              })}
            </tr>
          ))}
        </tbody>
      </Box>
    </Box>
  )
}

export default Heatmap
