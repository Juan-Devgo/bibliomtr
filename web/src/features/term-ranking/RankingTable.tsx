import Table from '@mui/material/Table'
import TableBody from '@mui/material/TableBody'
import TableCell from '@mui/material/TableCell'
import TableContainer from '@mui/material/TableContainer'
import TableHead from '@mui/material/TableHead'
import TableRow from '@mui/material/TableRow'
import type { TermFrequency } from '@/types/ranking'

interface RankingTableProps {
  /** Top N en el orden del ranking: frecuencia descendente, empates en orden alfabético ascendente. */
  top: readonly TermFrequency[]
  /** Nombre accesible de la tabla (p. ej. "Ranking de Counting Sort"). */
  label: string
}

/**
 * Tabla del top N de un algoritmo: posición (desde 1), término y frecuencia (REQ-5, AC-5).
 * Muestra las filas en el orden recibido del backend, sin reordenar.
 *
 * Complejidad por render: O(N) en tiempo y espacio.
 */
function RankingTable({ top, label }: RankingTableProps) {
  return (
    <TableContainer>
      <Table size="small" aria-label={label}>
        <TableHead>
          <TableRow>
            <TableCell>Posición</TableCell>
            <TableCell>Término</TableCell>
            <TableCell align="right">Frecuencia</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {top.map((entry, index) => (
            <TableRow key={entry.term}>
              <TableCell>{index + 1}</TableCell>
              <TableCell>{entry.term}</TableCell>
              <TableCell align="right">{entry.frequency}</TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </TableContainer>
  )
}

export default RankingTable
