import Box from '@mui/material/Box'
import CircularProgress from '@mui/material/CircularProgress'
import Tab from '@mui/material/Tab'
import Tabs from '@mui/material/Tabs'
import { lazy, Suspense, useState, type ComponentType } from 'react'
import HomePage from '@/pages/HomePage'

// Las páginas de los requerimientos se cargan al abrir su pestaña: así KaTeX, D3 y Recharts no entran en el
// bundle inicial.
const SimilarityPage = lazy(() => import('@/pages/SimilarityPage'))
const TermRankingPage = lazy(() => import('@/pages/TermRankingPage'))
const PatternSearchPage = lazy(() => import('@/pages/PatternSearchPage'))

/** Pestañas de la aplicación; se agrega una por requerimiento. */
const TABS = [
  { id: 'home', label: 'Inicio' },
  { id: 'similarity', label: 'Similitud textual' },
  { id: 'term-ranking', label: 'Ranking de términos' },
  { id: 'pattern-search', label: 'Búsqueda Patrones' },
] as const

type TabId = (typeof TABS)[number]['id']

/** Página de cada pestaña. */
const PAGES: Record<TabId, ComponentType> = {
  home: HomePage,
  similarity: SimilarityPage,
  'term-ranking': TermRankingPage,
  'pattern-search': PatternSearchPage,
}

/**
 * Raíz de la interfaz: pestañas MUI entre la página inicial, la de similitud textual (spec 001, REQ-17), la
 * del ranking de términos (spec 002, REQ-5) y la de búsqueda de patrones (spec 004, REQ-19).
 */
function App() {
  const [tab, setTab] = useState<TabId>('home')
  const Page = PAGES[tab]

  return (
    <>
      <Box sx={{ borderBottom: 1, borderColor: 'divider', px: 2 }}>
        <Tabs value={tab} onChange={(_, value: TabId) => setTab(value)} aria-label="Secciones de bibliomtr">
          {TABS.map(({ id, label }) => (
            <Tab key={id} value={id} label={label} id={`tab-${id}`} aria-controls={`panel-${id}`} />
          ))}
        </Tabs>
      </Box>
      <div role="tabpanel" id={`panel-${tab}`} aria-labelledby={`tab-${tab}`}>
        <Suspense
          fallback={
            <Box sx={{ display: 'flex', justifyContent: 'center', p: 4 }}>
              <CircularProgress aria-label="Cargando página" />
            </Box>
          }
        >
          <Page />
        </Suspense>
      </div>
    </>
  )
}

export default App
