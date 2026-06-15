import { API_BASE_URL } from '../config'

const TOKEN_KEY = 'searchlink.token'

export function getToken() {
  return localStorage.getItem(TOKEN_KEY)
}
export function setToken(token) {
  localStorage.setItem(TOKEN_KEY, token)
}
export function clearToken() {
  localStorage.removeItem(TOKEN_KEY)
}

/**
 * Wrapper delgado sobre fetch. Apunta a API_BASE_URL y, si `auth` está activo, adjunta el JWT
 * del localStorage como `Authorization: Bearer <token>`.
 *
 * Body JSON por default. Si `body` es un FormData (upload de archivos), NO se setea
 * Content-Type ni se serializa: el browser pone `multipart/form-data` con su boundary y manda
 * el FormData tal cual. El JWT se adjunta igual en ambos casos.
 *
 * Ante respuesta no-2xx, lanza un Error con `.status` y `.body`, usando el `message` del
 * ErrorResponse uniforme del backend cuando está disponible.
 */
export async function apiFetch(path, { method = 'GET', body, auth = true, headers = {} } = {}) {
  const esFormData = body instanceof FormData
  const finalHeaders = esFormData
    ? { ...headers }
    : { 'Content-Type': 'application/json', ...headers }
  let conJwt = false
  if (auth) {
    const token = getToken()
    if (token) {
      finalHeaders['Authorization'] = `Bearer ${token}`
      conJwt = true
    }
  }

  const res = await fetch(`${API_BASE_URL}${path}`, {
    method,
    headers: finalHeaders,
    body: esFormData ? body : (body != null ? JSON.stringify(body) : undefined),
  })

  const text = await res.text()
  const data = text ? JSON.parse(text) : null

  if (!res.ok) {
    // Auto-logout SÓLO si el request llevaba JWT: un 401 ahí = sesión expirada/revocada.
    // NO aplica al 401 de login ni a los 4xx de registro (van sin Authorization, conJwt=false).
    if (res.status === 401 && conJwt) {
      clearToken()
      if (window.location.pathname !== '/login') {
        window.location.assign('/login')
      }
    }
    const message = data?.message || data?.error || `Error ${res.status}`
    const error = new Error(message)
    error.status = res.status
    error.body = data
    throw error
  }
  return data
}
