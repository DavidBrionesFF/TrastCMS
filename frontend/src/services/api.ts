let csrfToken: string | null = null
let csrfHeader = 'X-XSRF-TOKEN'

export class ApiError extends Error {
  constructor(public status: number, message: string) {
    super(message)
    this.name = 'ApiError'
  }
}

function readCookie(name: string): string | null {
  const prefix = `${name}=`
  const cookie = document.cookie
    .split('; ')
    .find((entry) => entry.startsWith(prefix))

  if (!cookie) return null

  const rawValue = cookie.substring(prefix.length)
  try {
    return decodeURIComponent(rawValue)
  } catch {
    return rawValue
  }
}

async function loadCsrf(): Promise<void> {
  const response = await fetch('/api/auth/csrf', {
    method: 'GET',
    credentials: 'same-origin',
    cache: 'no-store',
    headers: {
      Accept: 'application/json'
    }
  })

  if (!response.ok) {
    throw new ApiError(response.status, 'No se pudo iniciar la sesión segura')
  }

  const data = await response.json()
  csrfHeader = data.headerName || 'X-XSRF-TOKEN'

  /*
   * CookieCsrfTokenRepository escribe el token sin enmascarar en XSRF-TOKEN.
   * El valor devuelto por el endpoint puede estar protegido con XOR para
   * mitigar BREACH; ese valor no debe enviarse como encabezado sin procesar.
   */
  csrfToken = readCookie('XSRF-TOKEN') || data.token

  if (!csrfToken) {
    throw new ApiError(403, 'No se recibió el token de seguridad')
  }
}

export async function api<T>(url: string, options: RequestInit = {}): Promise<T> {
  const method = (options.method || 'GET').toUpperCase()
  const headers = new Headers(options.headers)

  if (!headers.has('Accept')) {
    headers.set('Accept', 'application/json')
  }

  if (!(options.body instanceof FormData) && options.body && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json')
  }

  if (!['GET', 'HEAD', 'OPTIONS'].includes(method)) {
    if (!csrfToken) await loadCsrf()
    headers.set(csrfHeader, csrfToken as string)
  }

  const response = await fetch(url, {
    ...options,
    method,
    headers,
    credentials: 'same-origin',
    cache: 'no-store'
  })

  if (response.status === 204) return undefined as T

  if (!response.ok) {
    const body = await response.json().catch(() => ({}))
    throw new ApiError(
      response.status,
      body.detail || body.message || body.title || 'Error en la solicitud'
    )
  }

  return response.json() as Promise<T>
}

export async function login(email: string, password: string): Promise<void> {
  csrfToken = null
  await loadCsrf()

  const body = new URLSearchParams({
    username: email,
    password
  })

  await api('/api/auth/login', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8'
    },
    body
  })

  // Spring Security rota la sesión y el token CSRF después de autenticar.
  csrfToken = null
  await loadCsrf()
}

export async function logout(): Promise<void> {
  await api('/api/auth/logout', { method: 'POST' })
  csrfToken = null
}
