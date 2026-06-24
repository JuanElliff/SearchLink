import { useEffect, useState, useMemo } from 'react'
import { useNavigate } from 'react-router-dom'
import { apiFetch } from '../api/client'
import { useAuth } from '../auth/AuthContext'

const ROL_BADGE = {
  ADMIN:    'bg-rose-100 text-rose-800',
  OPERADOR: 'bg-sky-100 text-sky-800',
  ESTANDAR: 'bg-slate-100 text-slate-600',
}

const ROLES = ['ADMIN', 'OPERADOR', 'ESTANDAR']
const FILTROS_ROL = ['TODOS', ...ROLES]

export default function AdminPage() {
  const { usuario } = useAuth()
  const navigate = useNavigate()

  const [usuarios, setUsuarios]           = useState([])
  const [cargando, setCargando]           = useState(true)
  const [error, setError]                 = useState(null)

  // Filtros
  const [busqueda, setBusqueda]           = useState('')
  const [filtroRol, setFiltroRol]         = useState('TODOS')

  // Operaciones en progreso por usuario
  const [toggling, setToggling]           = useState({})
  const [changingRol, setChangingRol]     = useState({})
  const [erroresOp, setErroresOp]         = useState({})

  // Modal de cambio de rol
  const [modalRol, setModalRol]           = useState(null) // { id, nombre, rolActual }
  const [nuevoRol, setNuevoRol]           = useState('')

  useEffect(() => {
    apiFetch('/api/usuarios')
      .then(setUsuarios)
      .catch((e) => setError(e.message))
      .finally(() => setCargando(false))
  }, [])

  const usuariosFiltrados = useMemo(() => {
    return usuarios.filter((u) => {
      const matchRol = filtroRol === 'TODOS' || u.rol === filtroRol
      const q = busqueda.trim().toLowerCase()
      const matchBusqueda = !q || u.email.toLowerCase().includes(q) || u.nombre.toLowerCase().includes(q)
      return matchRol && matchBusqueda
    })
  }, [usuarios, filtroRol, busqueda])

  const toggleActivo = async (u) => {
    setToggling((t) => ({ ...t, [u.id]: true }))
    setErroresOp((e) => ({ ...e, [u.id]: null }))
    try {
      const actualizado = await apiFetch(`/api/usuarios/${u.id}/activo`, {
        method: 'PATCH',
        body: { activo: !u.activo },
      })
      setUsuarios((prev) => prev.map((x) => (x.id === u.id ? actualizado : x)))
    } catch (e) {
      setErroresOp((prev) => ({ ...prev, [u.id]: e.message }))
    } finally {
      setToggling((t) => ({ ...t, [u.id]: false }))
    }
  }

  const abrirModalRol = (u) => {
    setModalRol({ id: u.id, nombre: u.nombre, rolActual: u.rol })
    setNuevoRol(u.rol)
  }

  const confirmarCambioRol = async () => {
    if (!modalRol || nuevoRol === modalRol.rolActual) { setModalRol(null); return }
    setChangingRol((c) => ({ ...c, [modalRol.id]: true }))
    setErroresOp((e) => ({ ...e, [modalRol.id]: null }))
    try {
      const actualizado = await apiFetch(`/api/usuarios/${modalRol.id}/rol`, {
        method: 'PATCH',
        body: { rol: nuevoRol },
      })
      setUsuarios((prev) => prev.map((x) => (x.id === modalRol.id ? actualizado : x)))
      setModalRol(null)
    } catch (e) {
      setErroresOp((prev) => ({ ...prev, [modalRol.id]: e.message }))
    } finally {
      setChangingRol((c) => ({ ...c, [modalRol.id]: false }))
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

      {/* Filtros */}
      <div className="flex flex-wrap items-center gap-3">
        <input
          type="search"
          value={busqueda}
          onChange={(e) => setBusqueda(e.target.value)}
          placeholder="Buscar por nombre o email…"
          className="min-w-0 flex-1 rounded border border-slate-300 px-3 py-1.5 text-sm focus:border-sky-500 focus:outline-none"
        />
        <div className="flex items-center gap-1">
          {FILTROS_ROL.map((r) => (
            <button
              key={r}
              onClick={() => setFiltroRol(r)}
              className={`rounded-full px-3 py-0.5 text-xs font-medium border transition-colors ${
                filtroRol === r
                  ? 'bg-sky-600 text-white border-sky-600'
                  : 'bg-white text-slate-600 border-slate-300 hover:bg-slate-50'
              }`}
            >
              {r === 'TODOS' ? 'Todos' : r.charAt(0) + r.slice(1).toLowerCase()}
            </button>
          ))}
        </div>
      </div>

      {cargando && <p className="text-slate-500">Cargando usuarios…</p>}
      {error    && <p className="text-red-600">{error}</p>}

      {!cargando && !error && usuariosFiltrados.length === 0 && (
        <p className="text-slate-500">No hay usuarios que coincidan con el filtro.</p>
      )}

      {!cargando && !error && usuariosFiltrados.length > 0 && (
        <div className="divide-y divide-slate-200 rounded border border-slate-200 bg-white">
          {usuariosFiltrados.map((u) => {
            const esPropioAdmin = u.id === usuario?.id
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
                  <div className="flex gap-2">
                    {/* Cambiar rol: deshabilitado para sí mismo */}
                    <button
                      disabled={esPropioAdmin || changingRol[u.id]}
                      onClick={() => abrirModalRol(u)}
                      title={esPropioAdmin ? 'No podés cambiar tu propio rol' : 'Cambiar rol'}
                      className="shrink-0 rounded border border-slate-300 px-3 py-1 text-sm text-slate-700 hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-40"
                    >
                      Cambiar rol
                    </button>
                    <button
                      disabled={toggling[u.id] || desactivarBloqueado}
                      onClick={() => toggleActivo(u)}
                      title={desactivarBloqueado ? 'No podés desactivarte a vos mismo' : undefined}
                      className="shrink-0 rounded border border-slate-300 px-3 py-1 text-sm text-slate-700 hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-40"
                    >
                      {toggling[u.id] ? '…' : u.activo ? 'Desactivar' : 'Activar'}
                    </button>
                  </div>
                  {erroresOp[u.id] && (
                    <p className="text-xs text-red-600">{erroresOp[u.id]}</p>
                  )}
                </div>
              </div>
            )
          })}
        </div>
      )}

      {/* Modal cambio de rol */}
      {modalRol && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40">
          <div className="w-full max-w-sm rounded-lg bg-white p-6 shadow-xl space-y-4">
            <h2 className="text-lg font-bold text-slate-900">Cambiar rol</h2>
            <p className="text-sm text-slate-600">
              Usuario: <span className="font-medium">{modalRol.nombre}</span>
            </p>
            <div className="space-y-2">
              {ROLES.map((r) => (
                <label key={r} className="flex items-center gap-3 cursor-pointer">
                  <input
                    type="radio"
                    name="nuevoRol"
                    value={r}
                    checked={nuevoRol === r}
                    onChange={() => setNuevoRol(r)}
                    className="accent-sky-600"
                  />
                  <span className={`rounded-full px-2 py-0.5 text-xs font-medium ${ROL_BADGE[r]}`}>
                    {r}
                  </span>
                  {r === modalRol.rolActual && (
                    <span className="text-xs text-slate-400">(actual)</span>
                  )}
                </label>
              ))}
            </div>
            {erroresOp[modalRol.id] && (
              <p className="text-sm text-red-600">{erroresOp[modalRol.id]}</p>
            )}
            <div className="flex justify-end gap-3 pt-2">
              <button
                onClick={() => setModalRol(null)}
                className="rounded border border-slate-300 px-4 py-1.5 text-sm text-slate-700 hover:bg-slate-50"
              >
                Cancelar
              </button>
              <button
                disabled={changingRol[modalRol.id] || nuevoRol === modalRol.rolActual}
                onClick={confirmarCambioRol}
                className="rounded bg-sky-600 px-4 py-1.5 text-sm font-medium text-white hover:bg-sky-700 disabled:opacity-60"
              >
                {changingRol[modalRol.id] ? 'Guardando…' : 'Confirmar'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
