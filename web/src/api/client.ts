/** Cliente HTTP mínimo. Las rutas son relativas: Vite (dev) y nginx (Docker) redirigen /api al backend. */
export async function apiGet<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(path, { headers: { Accept: 'application/json' }, ...init })
  if (!response.ok) {
    throw new Error(`${response.status} ${response.statusText}: ${path}`)
  }
  return (await response.json()) as T
}
