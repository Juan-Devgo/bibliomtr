/**
 * Cliente HTTP mínimo. Las rutas son relativas: Vite (dev) y nginx (Docker) redirigen /api al backend.
 *
 * Ante una respuesta de error, el backend devuelve un ProblemDetail (RFC 9457) cuyo `detail` trae el
 * mensaje en español (REQ-4, AC-3); el cliente lo lanza como `ApiError` para que la interfaz lo muestre
 * tal cual.
 */

/** Error de la API con el código HTTP y el mensaje en español del ProblemDetail. */
export class ApiError extends Error {
  readonly status: number

  constructor(status: number, message: string) {
    super(message)
    this.name = 'ApiError'
    this.status = status
  }
}

/** Forma mínima de un ProblemDetail de Spring: solo se usa `detail`. */
interface ProblemDetail {
  detail?: string
}

/**
 * Convierte una respuesta no exitosa en `ApiError`. Usa el `detail` del ProblemDetail si existe y, si el
 * cuerpo no es JSON o no lo trae, un mensaje genérico con el código y la ruta.
 */
async function toApiError(response: Response, path: string): Promise<ApiError> {
  let detail: string | undefined
  try {
    const body = (await response.json()) as ProblemDetail
    detail = typeof body.detail === 'string' && body.detail.length > 0 ? body.detail : undefined
  } catch {
    detail = undefined
  }
  return new ApiError(response.status, detail ?? `Error ${response.status} al consultar ${path}`)
}

/** GET JSON; lanza `ApiError` si la respuesta no es 2xx. */
export async function apiGet<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(path, { headers: { Accept: 'application/json' }, ...init })
  if (!response.ok) {
    throw await toApiError(response, path)
  }
  return (await response.json()) as T
}

/** POST sin cuerpo o con cuerpo JSON; lanza `ApiError` si la respuesta no es 2xx. */
export async function apiPost<T>(path: string, body?: unknown, init?: RequestInit): Promise<T> {
  const headers: Record<string, string> = { Accept: 'application/json' }
  if (body !== undefined) {
    headers['Content-Type'] = 'application/json'
  }
  const response = await fetch(path, {
    method: 'POST',
    headers,
    body: body === undefined ? undefined : JSON.stringify(body),
    ...init,
  })
  if (!response.ok) {
    throw await toApiError(response, path)
  }
  return (await response.json()) as T
}
