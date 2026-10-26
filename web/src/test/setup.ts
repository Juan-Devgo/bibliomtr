/**
 * Configuración compartida de Vitest (spec 002): registra los matchers de
 * `@testing-library/jest-dom` (p. ej. `toBeInTheDocument`) y limpia el DOM tras cada test.
 * También exporta `renderWithQuery`, que monta un componente dentro de un `QueryClientProvider`
 * nuevo por test, sin reintentos, para que un error de la API se vea de inmediato.
 */
import '@testing-library/jest-dom/vitest'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { cleanup, render, type RenderOptions } from '@testing-library/react'
import { createElement, type ReactElement } from 'react'
import { afterEach } from 'vitest'

// Sin `globals: true`, Testing Library no desmonta solo: se limpia explícitamente.
afterEach(() => {
  cleanup()
})

/**
 * Renderiza `ui` con un `QueryClient` aislado y sin reintentos.
 * Devuelve el resultado de `render` más el `queryClient` usado, para inspeccionar o invalidar consultas.
 */
export function renderWithQuery(ui: ReactElement, options?: Omit<RenderOptions, 'wrapper'>) {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false, refetchOnWindowFocus: false },
      mutations: { retry: false },
    },
  })
  const result = render(createElement(QueryClientProvider, { client: queryClient }, ui), options)
  return { ...result, queryClient }
}
