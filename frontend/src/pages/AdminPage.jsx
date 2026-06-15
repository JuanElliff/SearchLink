import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { apiFetch } from '../api/client'
import { useAuth } from '../auth/AuthContext'

// RolUsuario (verificado en backend/model/RolUsuario.java)
const ROL_BADGE = {
  ADMIN:    'bg-rose-100 text-rose-800',
  OPERADOR: 'bg-sky-100 text-sky-800',
  ESTANDAR: 'bg-slate-100 text-slate-600',
}

export default function AdminPage() {
  const { usuario } = useAuth()
  const navigate = useNavigate()
  const [usuarios, setUsuarios]       = useState([])
  const [cargando, setCargando]       = useState(true)
  const [error, setError]             = useState(null)
  const [toggling, setToggling]       = useState({})      // { [id]: boolean }
  const [erroresToggle, setErroresToggle] = useState({})  // { [id]: string }

  useEffect(() => {
    apiFetch('/api/usuarios')
      .then(setUsuarios)
      .catch((e) => setError(e.message))
      .finally(() => setCargando(false))
  }, [])

  const toggleActivo = async (u) => {
    setToggling((t) => ({ ...t, [u.id]: true }))
    setErroresToggle((e) => ({ ...e, [u.id]: null }))
    try {
      const actualizado = await apiFetch(`/api/usuarios/${u.id}/activo`, {
        method: 'PATCH',
        body: { activo: !u.activo },
      })
      setUsuarios((prev) => prev.map((x) => (x.id === u.id ? actualizado : x)))
    } catch (e) {
      setErroresToggle((prev) => ({ ...prev, [u.id]: e.message }))
    } finally {
      setToggling((t) => ({ ...t, [u.id]: false }))
    }
  }

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-xl font-bold text-slate-900">Gestión de usuarios</h1>
        <button
          onClick={() => navigate('/admin/crear')}
          className="rounded bg-sky-600 px-4 py-2 text-sm font-medium text-white hover:bg-sky-700"
        >
          + Crear usuario
        </button>
      </div>

      {cargando && <p className="text-slate-500">Cargando usuarios…</p>}
      {error    && <p className="text-red-600">{error}</p>}

      {!cargando && !error && usuarios.length === 0 && (
        <p className="text-slate-500">No hay usuarios registrados.</p>
      )}

      {!cargando && !error && usuarios.length > 0 && (
        <div className="divide-y divide-slate-200 rounded border border-slate-200 bg-white">
          {usuarios.map((u) => {
            const esPropioAdmin = u.id === usuario?.id
            // Guard anti-lockout: deshabilitar solo la acción de DESACTIVAR sobre sí mismo.
            // Reactivar propio usuario (si quedara inactivo) sí se permite.
            const desactivarBloqueado = esPropioAdmin && u.activo
            return (
              <div key={u.id} className="flex flex-wrap items-start gap-3 px-4 py-3">
                <div className="min-w-0 flex-1">
                  <p className="font-medium text-slate-900">
                    {u.nombre}
                    {esPropioAdmin && (
                      <span className="ml-2 text-xs text-slate-400">(vos)</span>
                    )}
                  </p>
                  <p className="text-sm text-slate-500">{u.email}</p>
                </div>

                <span className={`shrink-0 rounded-full px-2 py-0.5 text-xs font-medium ${ROL_BADGE[u.rol] ?? 'bg-slate-100 text-slate-600'}`}>
                  {u.rol}
                </span>

                <span className={`shrink-0 rounded-full px-2 py-0.5 text-xs font-medium ${u.activo ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-700'}`}>
                  {u.activo ? 'Activo' : 'Inactivo'}
                </span>

                <div className="flex flex-col items-end gap-1">
                  <button
                    disabled={toggling[u.id] || desactivarBloqueado}
                    onClick={() => toggleActivo(u)}
                    title={desactivarBloqueado ? 'No podés desactivarte a vos mismo' : undefined}
                    className="shrink-0 rounded border border-slate-300 px-3 py-1 text-sm text-slate-700 hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-40"
                  >
                    {toggling[u.id] ? '…' : u.activo ? 'Desactivar' : 'Activar'}
                  </button>
                  {erroresToggle[u.id] && (
                    <p className="text-xs text-red-600">{erroresToggle[u.id]}</p>
                  )}
                </div>
              </div>
            )
          })}
        </div>
      )}
    </div>
  )
}
