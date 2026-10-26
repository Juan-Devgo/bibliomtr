import type { ReactNode } from 'react'

/**
 * Lista ordenada de los pasos de una traza (`TraceStep`), en el orden de ejecución del algoritmo.
 *
 * Complejidad por render: O(1) más la de `children`.
 */
function StepList({ children }: { children: ReactNode }) {
  return <ol style={{ display: 'flex', flexDirection: 'column', gap: 12, padding: 0, margin: 0 }}>{children}</ol>
}

export default StepList
