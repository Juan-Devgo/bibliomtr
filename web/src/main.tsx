import GlobalStyles from '@mui/material/GlobalStyles'
import { StyledEngineProvider } from '@mui/material/styles'
import { QueryClientProvider } from '@tanstack/react-query'
import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import 'katex/dist/katex.min.css'
import './index.css'
import { queryClient } from '@/api/queryClient'
import App from './App.tsx'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <QueryClientProvider client={queryClient}>
      {/* Estilos de MUI en una capa CSS propia para convivir con Tailwind (shadcn/ui). */}
      <StyledEngineProvider enableCssLayer>
        {/* Fija el orden de capas: "mui" sobre "base" (preflight de Tailwind), que si no reinicia sus estilos. */}
        <GlobalStyles styles="@layer theme, base, mui, components, utilities;" />
        <App />
      </StyledEngineProvider>
    </QueryClientProvider>
  </StrictMode>,
)
