import Box from '@mui/material/Box'
import Tab from '@mui/material/Tab'
import Tabs from '@mui/material/Tabs'
import { useState, type ReactElement } from 'react'
import HomePage from '@/pages/HomePage'
import SimilarityPage from '@/pages/SimilarityPage'
import TermRankingPage from '@/pages/TermRankingPage'

/** Pestañas de la aplicación; se agrega una por requerimiento. */
const TABS = [
  { id: 'home', label: 'Inicio' },
  { id: 'similarity', label: 'Similitud textual' },
  { id: 'term-ranking', label: 'Ranking de términos' },
] as const

type TabId = (typeof TABS)[number]['id']

/** Página de cada pestaña. */
const PAGES: Record<TabId, () => ReactElement> = {
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
        <Page />
      </div>
    </>
  )
}

export default App
