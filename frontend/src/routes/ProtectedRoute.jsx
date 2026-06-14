import { Navigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { homePathForRol } from '../auth/roles'

/**
 * Guard de ruta. Sin sesión → manda a /login. Con sesión pero rol no permitido → redirige
 * al home del rol del usuario (evita loops y "pantallas ajenas"). `roles` opcional: si no se
 * pasa, basta con estar autenticado.
 */
export default function ProtectedRoute({ roles, children }) {
  const { isAuthenticated, rol } = useAuth()

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />
  }
  if (roles && !roles.includes(rol)) {
    return <Navigate to={homePathForRol(rol)} replace />
  }
  return children
}
