import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import PagedMatrix from './PagedMatrix'

/** Matriz R×C cuya celda (i, j) vale `i,j`, para comprobar qué índices se ven en cada bloque. */
function indexMatrix(rows: number, columns: number): string[][] {
  return Array.from({ length: rows }, (_, i) => Array.from({ length: columns }, (_, j) => `${i},${j}`))
}

/** Encabezados `r0, r1, …` o `c0, c1, …`. */
function labels(prefix: string, n: number): string[] {
  return Array.from({ length: n }, (_, i) => `${prefix}${i}`)
}

describe('PagedMatrix (REQ-17, AC-15)', () => {
  it('muestra una matriz de 30×30 en bloques de 25 con el texto de rango', async () => {
    const user = userEvent.setup()
    render(
      <PagedMatrix matrix={indexMatrix(30, 30)} rowLabels={labels('r', 30)} colLabels={labels('c', 30)} label="Matriz D" />,
    )
    const section = screen.getByRole('region', { name: 'Matriz D' })

    // Bloque (0, 0): filas 0–24 y columnas 0–24, 25×25 celdas.
    expect(within(section).getByText('filas 0–24, columnas 0–24 de 30×30')).toBeInTheDocument()
    expect(within(section).getAllByRole('cell')).toHaveLength(625)
    expect(within(section).getByText('0,0')).toBeInTheDocument()
    expect(within(section).getByText('24,24')).toBeInTheDocument()
    expect(within(section).queryByText('25,25')).not.toBeInTheDocument()
    expect(within(section).getByRole('rowheader', { name: 'r0' })).toBeInTheDocument()
    expect(within(section).getByRole('columnheader', { name: 'c24' })).toBeInTheDocument()
    expect(within(section).getByRole('button', { name: 'Matriz D: filas anteriores' })).toBeDisabled()

    // Bloque de columnas siguiente: columnas 25–29, 25×5 celdas.
    await user.click(within(section).getByRole('button', { name: 'Matriz D: columnas siguientes' }))
    expect(within(section).getByText('filas 0–24, columnas 25–29 de 30×30')).toBeInTheDocument()
    expect(within(section).getAllByRole('cell')).toHaveLength(125)
    expect(within(section).getByText('0,25')).toBeInTheDocument()
    expect(within(section).queryByText('0,24')).not.toBeInTheDocument()

    // Bloque de filas siguiente: filas 25–29 y columnas 25–29, 5×5 celdas.
    await user.click(within(section).getByRole('button', { name: 'Matriz D: filas siguientes' }))
    expect(within(section).getByText('filas 25–29, columnas 25–29 de 30×30')).toBeInTheDocument()
    expect(within(section).getAllByRole('cell')).toHaveLength(25)
    expect(within(section).getByText('29,29')).toBeInTheDocument()
    expect(within(section).getByRole('button', { name: 'Matriz D: filas siguientes' })).toBeDisabled()
    expect(within(section).getByRole('button', { name: 'Matriz D: columnas siguientes' })).toBeDisabled()
  })

  it('muestra completa una matriz de 5×5, sin botones de bloque', () => {
    render(<PagedMatrix matrix={indexMatrix(5, 5)} rowLabels={labels('r', 5)} colLabels={labels('c', 5)} label="Matriz F" />)

    expect(screen.getByText('filas 0–4, columnas 0–4 de 5×5')).toBeInTheDocument()
    expect(screen.getAllByRole('cell')).toHaveLength(25)
    expect(screen.queryAllByRole('button')).toHaveLength(0)
  })
})
