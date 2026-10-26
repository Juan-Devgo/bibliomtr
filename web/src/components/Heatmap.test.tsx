import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import Heatmap from './Heatmap'

/** Matriz 3×3 simétrica con diagonal 1. */
const MATRIX = [
  [1, 0.75, 0.2],
  [0.75, 1, 0.5],
  [0.2, 0.5, 1],
]
const LABELS = ['#1', '#2', '#3']

describe('Heatmap (REQ-17, AC-13)', () => {
  it('dibuja las 9 celdas de una matriz 3×3 con sus valores a 3 decimales y sus encabezados', () => {
    render(<Heatmap labels={LABELS} matrix={MATRIX} ariaLabel="Jaccard" />)

    const table = screen.getByRole('table', { name: 'Jaccard' })
    expect(table).toBeInTheDocument()
    expect(screen.getAllByRole('cell')).toHaveLength(9)
    expect(screen.getAllByRole('columnheader').map((th) => th.textContent)).toEqual(['', '#1', '#2', '#3'])
    expect(screen.getAllByRole('rowheader').map((th) => th.textContent)).toEqual(['#1', '#2', '#3'])

    expect(screen.getByRole('cell', { name: 'fila #1, columna #2: 0.750' })).toHaveTextContent('0.750')
    expect(screen.getByRole('cell', { name: 'fila #3, columna #1: 0.200' })).toHaveTextContent('0.200')
    expect(screen.getByRole('cell', { name: 'fila #2, columna #3: 0.500' })).toHaveTextContent('0.500')
    expect(screen.getAllByText('1.000')).toHaveLength(3)

    // Color por valor: hsl(210, 80%, (95 − 55·v)%).
    expect(screen.getByRole('cell', { name: 'fila #1, columna #1: 1.000' })).toHaveStyle({
      backgroundColor: 'hsl(210, 80%, 40%)',
    })
    expect(screen.getByRole('cell', { name: 'fila #2, columna #3: 0.500' })).toHaveStyle({
      backgroundColor: 'hsl(210, 80%, 67.5%)',
    })
  })

  it('informa la fila y la columna de la celda en la que se hace clic', async () => {
    const user = userEvent.setup()
    const onSelectCell = vi.fn()
    render(<Heatmap labels={LABELS} matrix={MATRIX} onSelectCell={onSelectCell} />)

    await user.click(screen.getByRole('button', { name: 'fila #2, columna #3: 0.500' }))

    expect(onSelectCell).toHaveBeenCalledTimes(1)
    expect(onSelectCell).toHaveBeenCalledWith(1, 2)
  })

  it('sin onSelectCell, las celdas no son botones', () => {
    render(<Heatmap labels={LABELS} matrix={MATRIX} />)

    expect(screen.queryAllByRole('button')).toHaveLength(0)
  })
})
