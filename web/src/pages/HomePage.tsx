import { useHealth } from '@/api/useHealth'

/** Página inicial: muestra el estado del backend mientras se agregan las vistas de cada requerimiento. */
function HomePage() {
  const { data, isLoading, isError } = useHealth()

  return (
    <main className="mx-auto max-w-3xl p-6">
      <h1 className="text-2xl font-semibold">bibliomtr</h1>
      <p className="text-muted-foreground mt-2">
        Similitud textual en bibliometría: algoritmos clásicos frente a embeddings.
      </p>
      <p className="mt-6 text-sm">
        API: {isLoading ? 'consultando…' : isError ? 'sin conexión' : data?.status}
      </p>
    </main>
  )
}

export default HomePage
