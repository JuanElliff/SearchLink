import { createContext, useContext, useState, useCallback } from 'react'
import { apiFetch, setToken, clearToken, getToken } from '../api/client'

const USER_KEY = 'searchlink.user'
const AuthContext = createContext(null)

function loadUser() {
  try {
    const raw = localStorage.getItem(USER_KEY)
    return raw ? JSON.parse(raw) : null
  } catch {
    return null
  }
}

export function AuthProvider({ children }) {
  const [usuario, setUsuario] = useState(loadUser)

  // POST /api/sesiones → { token, tokenTipo, usuario: { id, nombre, email, rol, activo } }.
  // El rol ya viene en la respuesta: no hace falta una segunda llamada.
  const login = useCallback(async (email, password) => {
    const data = await apiFetch('/api/sesiones', {
      method: 'POST',
      auth: false,
      body: { email, password },
    })
    setToken(data.token)
    localStorage.setItem(USER_KEY, JSON.stringify(data.usuario))
    setUsuario(data.usuario)
    return data.usuario
  }, [])

  const logout = useCallback(() => {
    clearToken()
    localStorage.removeItem(USER_KEY)
    setUsuario(null)
  }, [])

  const value = {
    usuario,
    rol: usuario?.rol ?? null,
    isAuthenticated: Boolean(getToken()) && Boolean(usuario),
    login,
    logout,
  }
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

// eslint-disable-next-line react-refresh/only-export-components
export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth debe usarse dentro de <AuthProvider>')
  return ctx
}
