import { getSelectedSite, shouldSendSiteHeader } from '@/services/site-context'
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
    headers: new Headers({ Accept: 'application/json' })
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
  const unsafe = !['GET', 'HEAD', 'OPTIONS'].includes(method)

  async function execute(retryCsrf: boolean): Promise<Response> {
    const headers = new Headers(options.headers)

    if (!headers.has('Accept')) {
      headers.set('Accept', 'application/json')
    }

    const selectedSite = getSelectedSite()
    if (selectedSite && shouldSendSiteHeader(url) && !headers.has('X-TrastCMS-Site')) {
      headers.set('X-TrastCMS-Site', selectedSite)
    }

    if (!(options.body instanceof FormData) && options.body && !headers.has('Content-Type')) {
      headers.set('Content-Type', 'application/json')
    }

    if (unsafe) {
      const cookieToken = readCookie('XSRF-TOKEN')
      if (cookieToken && cookieToken !== csrfToken) csrfToken = cookieToken
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

    // Spring Security puede rotar el token al renovar la sesión. Reintentamos una
    // sola vez con un token fresco para que acciones como iniciar una evaluación
    // no fallen con un 403 opaco después de varias horas con la SPA abierta.
    if (unsafe && response.status === 403 && retryCsrf) {
      csrfToken = null
      await loadCsrf()
      return execute(false)
    }

    return response
  }

  const response = await execute(true)

  if (response.status === 204) return undefined as T

  if (!response.ok) {
    const contentType = response.headers.get('content-type') || ''
    let detail = ''
    if (contentType.includes('application/json') || contentType.includes('application/problem+json')) {
      const body = await response.json().catch(() => ({}))
      detail = body.detail || body.message || body.title || ''
    } else {
      const text = await response.text().catch(() => '')
      detail = text.replace(/<[^>]+>/g, ' ').replace(/\s+/g, ' ').trim().slice(0, 280)
    }
    throw new ApiError(response.status, detail || `La solicitud falló con estado ${response.status}`)
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
