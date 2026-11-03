import Table from '@mui/material/Table'
import TableBody from '@mui/material/TableBody'
import TableCell from '@mui/material/TableCell'
import TableContainer from '@mui/material/TableContainer'
import TableHead from '@mui/material/TableHead'
import TableRow from '@mui/material/TableRow'
import type { KeyboardEvent } from 'react'
import type { AlgorithmOccurrences } from '@/types/patternSearch'

interface AlgorithmOccurrencesTableProps {
  /** Resultado CALCULADO de un algoritmo. */
  result: AlgorithmOccurrences
  /** Artículo cuyo abstract se muestra; su fila queda seleccionada. */
  selectedArticleId: number | null
  /** Se llama al elegir la fila de un artículo. */
  onSelectArticle: (articleId: number) => void
}

/**
 * Tabla de ocurrencias de un algoritmo: una fila por artículo con su título, el número de ocurrencias, las
 * posiciones y, en Rabin-Karp, las colisiones (spec 004, REQ-15, REQ-20). Muestra los valores de la API sin
 * recalcularlos ni reordenarlos. Un clic (o Enter) en una fila elige ese artículo para resaltar su abstract.
 *
 * Complejidad por render: O(a + k) en tiempo y espacio, con a = artículos y k = posiciones en total.
 */
function AlgorithmOccurrencesTable({ result, selectedArticleId, onSelectArticle }: AlgorithmOccurrencesTableProps) {
  const showCollisions = result.algorithm === 'rabin-karp'

  const selectOnKey = (event: KeyboardEvent<HTMLTableRowElement>, articleId: number) => {
    if (event.key === 'Enter' || event.key === ' ') {
      event.preventDefault()
      onSelectArticle(articleId)
    }
  }

  return (
    <TableContainer>
      <Table size="small" aria-label={`Ocurrencias de ${result.label}`}>
        <TableHead>
          <TableRow>
            <TableCell>Artículo</TableCell>
            <TableCell align="right">Ocurrencias</TableCell>
            <TableCell>Posiciones</TableCell>
            {showCollisions && <TableCell align="right">Colisiones</TableCell>}
          </TableRow>
        </TableHead>
        <TableBody>
          {result.articles.map((article) => {
            const selected = article.articleId === selectedArticleId
            return (
              <TableRow
                key={article.articleId}
                hover
                selected={selected}
                aria-selected={selected}
                tabIndex={0}
                onClick={() => onSelectArticle(article.articleId)}
                onKeyDown={(event) => selectOnKey(event, article.articleId)}
                sx={{ cursor: 'pointer' }}
              >
                <TableCell>{`#${article.articleId} ${article.title}`}</TableCell>
                <TableCell align="right">{article.count}</TableCell>
                <TableCell sx={{ wordBreak: 'break-word' }}>
                  {article.positions.length === 0 ? '—' : article.positions.join(', ')}
                </TableCell>
                {showCollisions && <TableCell align="right">{article.collisions ?? 0}</TableCell>}
              </TableRow>
            )
          })}
        </TableBody>
      </Table>
    </TableContainer>
  )
}

export default AlgorithmOccurrencesTable
