import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { renderWithQuery } from '@/test/setup'
import App from './App'

afterEach(() => {
  vi.unstubAllGlobals()
})

describe('App (spec 004, REQ-19)', () => {
  it('tiene la pestaña "Búsqueda Patrones" y al abrirla carga su página con el formulario', async () => {
    // La página inicial consulta el estado del backend.
    vi.stubGlobal('fetch', vi.fn(async () => Response.json({ status: 'UP' })))
    const user = userEvent.setup()
    renderWithQuery(<App />)

    const tab = screen.getByRole('tab', { name: 'Búsqueda Patrones' })
    expect(tab).toHaveAttribute('aria-selected', 'false')

    await user.click(tab)

    expect(tab).toHaveAttribute('aria-selected', 'true')
    // La página se carga con `lazy`: con toda la suite en paralelo, el import dinámico puede tardar más que el
    // segundo por defecto de `findBy*`.
    expect(
      await screen.findByRole(
        'heading',
        { name: 'Requerimiento 4: Coincidencia exacta de cadenas' },
        { timeout: 10_000 },
      ),
    ).toBeInTheDocument()
    expect(screen.getByLabelText('Patrón')).toBeInTheDocument()
    expect(screen.getByLabelText('q de Rabin-Karp (opcional)')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Buscar' })).toBeInTheDocument()
  }, 15_000)
})
