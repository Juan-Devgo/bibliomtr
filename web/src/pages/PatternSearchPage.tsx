import Stack from '@mui/material/Stack'
import Typography from '@mui/material/Typography'
import PatternSearchView from '@/features/pattern-search/PatternSearchView'

/**
 * Página del Requerimiento 4: coincidencia exacta de cadenas con KMP, Boyer-Moore y Rabin-Karp sobre los
 * abstracts del corpus (spec 004, REQ-19, REQ-20).
 */
function PatternSearchPage() {
  return (
    <Stack component="main" spacing={3} sx={{ mx: 'auto', maxWidth: 1100, p: 3 }}>
      <Typography variant="h4" component="h1">
        Requerimiento 4: Coincidencia exacta de cadenas
      </Typography>
      <Typography color="text.secondary">
        Busca un patrón en los abstracts del corpus, tal como quedaron almacenados, con Knuth-Morris-Pratt,
        Boyer-Moore y Rabin-Karp. Se reportan todas las ocurrencias exactas, incluidas las solapadas, como posiciones
        base 0.
      </Typography>
      <PatternSearchView />
    </Stack>
  )
}

export default PatternSearchPage
