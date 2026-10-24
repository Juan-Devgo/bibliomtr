import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import PagedArray from './PagedArray'

/** Arreglo [v0, v1, …, v(n−1)] para comprobar qué índices se ven en cada página. */
function values(n: number): string[] {
  return Array.from({ length: n }, (_, i) => `v${i}`)
}

describe('PagedArray (REQ-16, AC-12)', () => {
  it('pagina un arreglo de 88 elementos en 0–49 y 50–87 con "longitud 88"', async () => {
    const user = userEvent.setup()
    render(<PagedArray items={values(88)} renderItem={(v) => v} label="Entrada" />)

    // Página 1: índices 0–49.
    expect(screen.getByText('0–49')).toBeInTheDocument()
    expect(screen.getByText('longitud 88')).toBeInTheDocument()
    expect(screen.getByText('v0')).toBeInTheDocument()
    expect(screen.getByText('v49')).toBeInTheDocument()
    expect(screen.queryByText('v50')).not.toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Entrada: página anterior' })).toBeDisabled()

    // Página 2: índices 50–87.
    await user.click(screen.getByRole('button', { name: 'Entrada: página siguiente' }))
    expect(screen.getByText('50–87')).toBeInTheDocument()
    expect(screen.getByText('longitud 88')).toBeInTheDocument()
    expect(screen.getByText('v50')).toBeInTheDocument()
    expect(screen.getByText('v87')).toBeInTheDocument()
    expect(screen.queryByText('v49')).not.toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Entrada: página siguiente' })).toBeDisabled()
  })

  it('muestra completo un arreglo de 50 elementos, sin paginar', () => {
    render(<PagedArray items={values(50)} renderItem={(v) => v} label="Conteo" />)

    expect(screen.getByText('v0')).toBeInTheDocument()
    expect(screen.getByText('v49')).toBeInTheDocument()
    expect(screen.queryByText('0–49')).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /página siguiente/ })).not.toBeInTheDocument()
  })

  it('muestra un arreglo vacío sin error', () => {
    render(<PagedArray items={[]} renderItem={(v: string) => v} label="Salida" />)

    expect(screen.getByText('[ ] (vacío)')).toBeInTheDocument()
  })
})
