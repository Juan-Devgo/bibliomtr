import Box from '@mui/material/Box'
import type { ReactNode } from 'react'

/** Fila de una tabla indexada: encabezado (p. ej. "π[i]") y una celda por columna. */
export interface IndexedRow {
  header: string
  cells: ReactNode[]
}

interface IndexedTableProps {
  /** Nombre accesible de la tabla (p. ej. "Tabla π"). */
  label: string
  /** Encabezado de la fila de índices (p. ej. "i", "j", "c"). */
  indexHeader: string
  /** Índices o claves de cada columna, en el orden de la traza. */
  columns: readonly ReactNode[]
  rows: readonly IndexedRow[]
}

/**
 * Tabla horizontal de una tabla del preprocesamiento (spec 004, REQ-23, REQ-24, REQ-26): una fila de índices y
 * una fila por arreglo, con los valores tal como los trae la traza (π, carácter malo, sufijo bueno, bordes).
 *
 * Complejidad por render: O(r · c) en tiempo y espacio, con r = filas y c = columnas.
 */
function IndexedTable({ label, indexHeader, columns, rows }: IndexedTableProps) {
  const cell = { border: 1, borderColor: 'divider', px: 1, py: 0.25, textAlign: 'center' } as const
  return (
    <Box sx={{ overflowX: 'auto', my: 1 }}>
      <Box
        component="table"
        aria-label={label}
        sx={{ borderCollapse: 'collapse', fontFamily: 'monospace', fontSize: '0.85rem' }}
      >
        <thead>
          <tr>
            <Box component="th" scope="row" sx={{ ...cell, color: 'text.secondary' }}>
              {indexHeader}
            </Box>
            {columns.map((column, index) => (
              <Box component="th" scope="col" key={index} sx={{ ...cell, color: 'text.secondary' }}>
                {column}
              </Box>
            ))}
          </tr>
        </thead>
        <tbody>
          {rows.map((row) => (
            <tr key={row.header}>
              <Box component="th" scope="row" sx={cell}>
                {row.header}
              </Box>
              {row.cells.map((value, index) => (
                <Box component="td" key={index} sx={cell}>
                  {value}
                </Box>
              ))}
            </tr>
          ))}
        </tbody>
      </Box>
    </Box>
  )
}

export default IndexedTable
