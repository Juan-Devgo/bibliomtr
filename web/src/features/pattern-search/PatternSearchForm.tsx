import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import TextField from '@mui/material/TextField'
import { useState, type FormEvent } from 'react'
import type { PatternSearchParams } from '@/api/usePatternSearch'
import { DEFAULT_Q } from '@/types/patternSearch'

interface PatternSearchFormProps {
  /** Se llama al pulsar "Buscar" con el patrón tal como se escribió y q si el campo no está vacío. */
  onSubmit: (params: PatternSearchParams) => void
  /** Deshabilita el botón mientras corre una búsqueda. */
  busy: boolean
}

/**
 * Formulario de la pestaña "Búsqueda Patrones": patrón, q opcional de Rabin-Karp y "Buscar" (spec 004, REQ-19).
 *
 * El patrón se envía exactamente como se escribió, sin recortar espacios: " " es un patrón válido de 1 carácter
 * (REQ-15, AC-25). No valida: un patrón vacío, uno de más de 200 caracteres o un q inválido se envían igual y el
 * backend responde 400 con el mensaje en español que muestra la vista (REQ-17). Un q vacío se omite y el backend
 * usa 101.
 *
 * Complejidad por render: O(1).
 */
function PatternSearchForm({ onSubmit, busy }: PatternSearchFormProps) {
  const [pattern, setPattern] = useState('')
  const [q, setQ] = useState('')

  const submit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    onSubmit(q.trim() === '' ? { pattern } : { pattern, q })
  }

  return (
    <Box
      component="form"
      aria-label="Buscar patrón"
      onSubmit={submit}
      sx={{ display: 'flex', flexWrap: 'wrap', gap: 2, alignItems: 'flex-start' }}
    >
      <TextField
        label="Patrón"
        value={pattern}
        onChange={(event) => setPattern(event.target.value)}
        size="small"
        helperText="Se busca tal cual: distingue mayúsculas y tildes, sin recortar espacios"
        sx={{ minWidth: 320 }}
      />
      <TextField
        label="q de Rabin-Karp (opcional)"
        value={q}
        onChange={(event) => setQ(event.target.value)}
        size="small"
        helperText={`Primo entre 2 y 1000000007; vacío = ${DEFAULT_Q}`}
        slotProps={{ htmlInput: { inputMode: 'numeric' } }}
      />
      <Button type="submit" variant="contained" disabled={busy}>
        Buscar
      </Button>
    </Box>
  )
}

export default PatternSearchForm
