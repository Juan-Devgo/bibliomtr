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

/** Pestañas de la aplicación; se agrega una por requerimiento. */
const TABS = [
  { id: 'home', label: 'Inicio' },
  { id: 'similarity', label: 'Similitud textual' },
  { id: 'term-ranking', label: 'Ranking de términos' },
] as const

type TabId = (typeof TABS)[number]['id']

/** Página de cada pestaña. */
const PAGES: Record<TabId, ComponentType> = {
  home: HomePage,
  similarity: SimilarityPage,
  'term-ranking': TermRankingPage,
}

/**
 * Raíz de la interfaz: pestañas MUI entre la página inicial, la de similitud textual (spec 001, REQ-17) y la
 * del ranking de términos (spec 002, REQ-5).
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
