import Alert from '@mui/material/Alert'
import Button from '@mui/material/Button'
import Checkbox from '@mui/material/Checkbox'
import Chip from '@mui/material/Chip'
import CircularProgress from '@mui/material/CircularProgress'
import List from '@mui/material/List'
import ListItem from '@mui/material/ListItem'
import ListItemButton from '@mui/material/ListItemButton'
import ListItemIcon from '@mui/material/ListItemIcon'
import ListItemText from '@mui/material/ListItemText'
import Paper from '@mui/material/Paper'
import Stack from '@mui/material/Stack'
import Typography from '@mui/material/Typography'
import { useState } from 'react'
import { useArticles } from '@/api/useArticles'
import { MAX_SELECTION, MIN_SELECTION } from '@/types/similarity'

interface ArticleSelectorProps {
  /** Se llama con los ids elegidos, en el orden de la lista (por nombre de archivo). */
  onCompare: (articleIds: number[]) => void
  /** `true` mientras la comparación está en curso; deshabilita el botón. */
  comparing?: boolean
  /** Error de la última comparación (p. ej. `ApiError` 400); su mensaje se muestra tal cual. */
  error?: Error | null
}

/**
 * Selector de artículos del Requerimiento 1 (REQ-13, REQ-15, REQ-17, AC-14).
 *
 * Lista los artículos seleccionables de `GET /api/articles` con una casilla y su título, autores y archivo. El
 * botón "Comparar" se habilita solo con 2 a 20 artículos elegidos; los ids se envían en el orden de la lista,
 * que es el orden de filas y columnas de las matrices. Un error de la API (p. ej. 400 con un id que dejó de
 * ser seleccionable) se muestra en un `Alert` con su mensaje en español, sin reescribirlo.
 *
 * Complejidad por render: O(n) en tiempo y espacio, con n = número de artículos seleccionables (≤ 20).
 */
function ArticleSelector({ onCompare, comparing = false, error = null }: ArticleSelectorProps) {
  const { data: articles, isLoading, isError, error: loadError } = useArticles()
  const [selected, setSelected] = useState<ReadonlySet<number>>(new Set())

  const toggle = (id: number) => {
    setSelected((current) => {
      const next = new Set(current)
      if (next.has(id)) {
        next.delete(id)
      } else {
        next.add(id)
      }
      return next
    })
  }

  // Ids elegidos en el orden de la lista; un id que ya no está en la lista no se envía.
  const selectedIds = (articles ?? []).filter((article) => selected.has(article.id)).map((article) => article.id)
  const count = selectedIds.length
  const canCompare = count >= MIN_SELECTION && count <= MAX_SELECTION && !comparing

  return (
    <Paper component="section" variant="outlined" aria-label="Selección de artículos" sx={{ p: 2 }}>
      <Typography variant="h5" component="h2">
        Artículos
      </Typography>
      <Typography variant="body2" color="text.secondary">
        Elige entre {MIN_SELECTION} y {MAX_SELECTION} artículos. Solo se listan los que tienen abstract detectado.
      </Typography>

      {isLoading && (
        <Stack direction="row" spacing={1} sx={{ alignItems: 'center', my: 1 }}>
          <CircularProgress size={20} />
          <Typography>Cargando artículos…</Typography>
        </Stack>
      )}
      {isError && <Alert severity="error">{loadError.message}</Alert>}
      {articles && articles.length === 0 && (
        <Alert severity="info" sx={{ my: 1 }}>
          No hay artículos seleccionables. Copia los PDF en corpus/ y reinicia la API.
        </Alert>
      )}

      {articles && articles.length > 0 && (
        <List dense aria-label="Artículos seleccionables">
          {articles.map((article) => {
            const labelId = `article-${article.id}-label`
            const authors = article.authors.length > 0 ? article.authors.join(', ') : 'Sin autores'
            return (
              <ListItem key={article.id} disablePadding>
                <ListItemButton onClick={() => toggle(article.id)}>
                  <ListItemIcon>
                    <Checkbox
                      edge="start"
                      checked={selected.has(article.id)}
                      tabIndex={-1}
                      disableRipple
                      slotProps={{ input: { 'aria-labelledby': labelId } }}
                    />
                  </ListItemIcon>
                  <ListItemText
                    slotProps={{ primary: { id: labelId } }}
                    primary={`#${article.id} ${article.title}`}
                    secondary={`${authors} · ${article.fileName}`}
                  />
                  {article.specter2Truncated && <Chip size="small" label="SPECTER2 truncado a 512 tokens" />}
                </ListItemButton>
              </ListItem>
            )
          })}
        </List>
      )}

      <Stack direction="row" spacing={2} sx={{ alignItems: 'center', mt: 1 }}>
        <Button variant="contained" disabled={!canCompare} onClick={() => onCompare(selectedIds)}>
          Comparar
        </Button>
        <Typography variant="body2" color="text.secondary">
          {`${count} seleccionados`}
        </Typography>
        {comparing && <CircularProgress size={20} aria-label="Comparando" />}
      </Stack>

      {error && (
        <Alert severity="error" sx={{ mt: 1 }}>
          {error.message}
        </Alert>
      )}
    </Paper>
  )
}

export default ArticleSelector
