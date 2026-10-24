import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import Stack from '@mui/material/Stack'
import Typography from '@mui/material/Typography'
import { useState, type ReactNode } from 'react'

/** Lado de cada bloque de la matriz: 25 filas por 25 columnas (REQ-17). */
export const BLOCK_SIZE = 25

interface PagedMatrixProps<T> {
  /** Matriz de la traza, fila por fila (p. ej. D de Levenshtein, de (m + 1)×(n + 1)). */
  matrix: readonly (readonly T[])[]
  /** Encabezado de cada fila, en orden (p. ej. `ε, a₁, …, aₘ`). */
  rowLabels: readonly string[]
  /** Encabezado de cada columna, en orden. */
  colLabels: readonly string[]
  /** Nombre de la matriz (p. ej. "Matriz D"), también usado como nombre accesible. */
  label: string
  /** Dibuja una celda; `row` y `column` son índices absolutos. Por defecto, `String(value)`. */
  renderCell?: (value: T, row: number, column: number) => ReactNode
}

/** Cantidad de bloques de una dimensión de longitud `length` (al menos 1). */
function blockCount(length: number): number {
  return Math.max(1, Math.ceil(length / BLOCK_SIZE))
}

interface BlockNavigationProps {
  /** Nombre de la matriz, para los nombres accesibles de los botones. */
  label: string
  /** "filas" o "columnas". */
  dimension: string
  current: number
  count: number
  onChange: (block: number) => void
}

/** Botones anterior/siguiente de una dimensión; solo se dibujan si hay más de un bloque. */
function BlockNavigation({ label, dimension, current, count, onChange }: BlockNavigationProps) {
  if (count <= 1) {
    return null
  }
  return (
    <Stack direction="row" spacing={0.5}>
      <Button
        size="small"
        variant="outlined"
        onClick={() => onChange(current - 1)}
        disabled={current === 0}
        aria-label={`${label}: ${dimension} anteriores`}
      >
        {`${dimension} ←`}
      </Button>
      <Button
        size="small"
        variant="outlined"
        onClick={() => onChange(current + 1)}
        disabled={current === count - 1}
        aria-label={`${label}: ${dimension} siguientes`}
      >
        {`${dimension} →`}
      </Button>
    </Stack>
  )
}

/**
 * Muestra una matriz de la traza en bloques de 25×25 (REQ-17, AC-15), con encabezados de fila y columna y
 * botones para moverse por filas y por columnas. Indica siempre el bloque visible y el tamaño total con el
 * texto "filas i–j, columnas k–l de R×C", con índices desde 0 como los de la traza.
 *
 * Complejidad por render: O(min(R, 25)·min(C, 25)) en tiempo y espacio, con R×C = tamaño de la matriz.
 */
function PagedMatrix<T>({ matrix, rowLabels, colLabels, label, renderCell }: PagedMatrixProps<T>) {
  const [rowBlock, setRowBlock] = useState(0)
  const [colBlock, setColBlock] = useState(0)
  const rows = matrix.length
  const columns = rows === 0 ? 0 : matrix[0].length
  const rowBlocks = blockCount(rows)
  const colBlocks = blockCount(columns)
  // Si la matriz cambia y se encoge, el bloque actual se ajusta al último disponible.
  const currentRow = Math.min(rowBlock, rowBlocks - 1)
  const currentCol = Math.min(colBlock, colBlocks - 1)
  const rowStart = currentRow * BLOCK_SIZE
  const rowEnd = Math.min(rowStart + BLOCK_SIZE, rows)
  const colStart = currentCol * BLOCK_SIZE
  const colEnd = Math.min(colStart + BLOCK_SIZE, columns)
  const draw = renderCell ?? ((value: T) => String(value))

  // Índices absolutos visibles: filas rowStart..rowEnd − 1 y columnas colStart..colEnd − 1.
  const visibleRows = Array.from({ length: rowEnd - rowStart }, (_, offset) => rowStart + offset)
  const visibleColumns = Array.from({ length: colEnd - colStart }, (_, offset) => colStart + offset)

  return (
    <Box component="section" aria-label={label} sx={{ my: 1 }}>
      <Typography variant="subtitle2" component="h4">
        {label}
      </Typography>
      {rows === 0 || columns === 0 ? (
        <Typography variant="body2" color="text.secondary">
          {`[ ] (vacía, ${rows}×${columns})`}
        </Typography>
      ) : (
        <>
          <Stack direction="row" spacing={1} sx={{ alignItems: 'center', flexWrap: 'wrap', my: 0.5 }}>
            <Typography variant="body2">
              {`filas ${rowStart}–${rowEnd - 1}, columnas ${colStart}–${colEnd - 1} de ${rows}×${columns}`}
            </Typography>
            <BlockNavigation
              label={label}
              dimension="filas"
              current={currentRow}
              count={rowBlocks}
              onChange={setRowBlock}
            />
            <BlockNavigation
              label={label}
              dimension="columnas"
              current={currentCol}
              count={colBlocks}
              onChange={setColBlock}
            />
          </Stack>
          <Box sx={{ overflowX: 'auto' }}>
            <Box
              component="table"
              aria-label={`${label}, bloque`}
              sx={{
                borderCollapse: 'collapse',
                fontFamily: 'monospace',
                fontSize: '0.75rem',
                '& th, & td': { border: 1, borderColor: 'divider', px: 0.75, py: 0.25, textAlign: 'center' },
                '& th': { color: 'text.secondary', fontWeight: 600 },
              }}
            >
              <thead>
                <tr>
                  <th scope="col" />
                  {visibleColumns.map((j) => (
                    <th scope="col" key={j} title={`columna ${j}`}>
                      {colLabels[j]}
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {visibleRows.map((i) => (
                  <tr key={i}>
                    <th scope="row" title={`fila ${i}`}>
                      {rowLabels[i]}
                    </th>
                    {visibleColumns.map((j) => (
                      <td key={j}>{draw(matrix[i][j], i, j)}</td>
                    ))}
                  </tr>
                ))}
              </tbody>
            </Box>
          </Box>
        </>
      )}
    </Box>
  )
}

export default PagedMatrix
