import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import Stack from '@mui/material/Stack'
import Typography from '@mui/material/Typography'
import { useState, type ReactNode } from 'react'

/** Tamaño de página de los arreglos de la traza (REQ-16). */
export const PAGE_SIZE = 50

interface PagedArrayProps<T> {
  /** Elementos del arreglo, en el orden de la traza. */
  items: readonly T[]
  /** Dibuja un elemento; `index` es su posición absoluta en el arreglo. */
  renderItem: (item: T, index: number) => ReactNode
  /** Nombre del arreglo (p. ej. "Conteo C"), también usado como nombre accesible. */
  label: string
  /**
   * `inline` (por defecto) dibuja los elementos como celdas en línea; `block` los apila, para elementos
   * con contenido propio (p. ej. una cubeta con sus arreglos antes y después).
   */
  layout?: 'inline' | 'block'
}

/**
 * Muestra un arreglo de la traza con el índice de cada elemento (REQ-16, AC-12).
 *
 * Con 50 elementos o menos los muestra todos. Con más de 50 pagina de a 50: indica el rango de índices
 * de la página ("0–49", "50–87") y la longitud total ("longitud 88"), con botones para cambiar de página.
 *
 * Complejidad por render: O(min(n, 50)) en tiempo y espacio, con n = longitud del arreglo.
 */
function PagedArray<T>({ items, renderItem, label, layout = 'inline' }: PagedArrayProps<T>) {
  const [page, setPage] = useState(0)
  const length = items.length
  const paged = length > PAGE_SIZE
  const pageCount = Math.max(1, Math.ceil(length / PAGE_SIZE))
  // Si el arreglo cambia y se encoge, la página actual se ajusta a la última disponible.
  const current = Math.min(page, pageCount - 1)
  const start = paged ? current * PAGE_SIZE : 0
  const end = paged ? Math.min(start + PAGE_SIZE, length) : length
  const visible = items.slice(start, end)

  return (
    <Box component="section" aria-label={label} sx={{ my: 1 }}>
      <Typography variant="subtitle2" component="h4">
        {label}
      </Typography>
      {paged && (
        <Stack direction="row" spacing={1} sx={{ alignItems: 'center', my: 0.5 }}>
          <Button
            size="small"
            variant="outlined"
            onClick={() => setPage(current - 1)}
            disabled={current === 0}
            aria-label={`${label}: página anterior`}
          >
            Anterior
          </Button>
          <Typography variant="body2">{`${start}–${end - 1}`}</Typography>
          <Typography variant="body2" color="text.secondary">
            {`longitud ${length}`}
          </Typography>
          <Button
            size="small"
            variant="outlined"
            onClick={() => setPage(current + 1)}
            disabled={current === pageCount - 1}
            aria-label={`${label}: página siguiente`}
          >
            Siguiente
          </Button>
        </Stack>
      )}
      {length === 0 ? (
        <Typography variant="body2" color="text.secondary">
          [ ] (vacío)
        </Typography>
      ) : (
        <Box
          component="ol"
          start={start}
          sx={{
            display: 'flex',
            flexDirection: layout === 'block' ? 'column' : 'row',
            flexWrap: layout === 'block' ? 'nowrap' : 'wrap',
            gap: 0.5,
            listStyle: 'none',
            p: 0,
            m: 0,
          }}
        >
          {visible.map((item, offset) => {
            const index = start + offset
            return (
              <Box
                component="li"
                key={index}
                sx={{
                  border: 1,
                  borderColor: 'divider',
                  borderRadius: 1,
                  px: layout === 'block' ? 1 : 0.75,
                  py: layout === 'block' ? 0.5 : 0.25,
                  fontFamily: layout === 'block' ? undefined : 'monospace',
                  fontSize: layout === 'block' ? undefined : '0.8rem',
                }}
              >
                <Box component="span" sx={{ color: 'text.secondary', mr: 0.5 }}>
                  {index}:
                </Box>
                <span>{renderItem(item, index)}</span>
              </Box>
            )
          })}
        </Box>
      )}
    </Box>
  )
}

export default PagedArray
