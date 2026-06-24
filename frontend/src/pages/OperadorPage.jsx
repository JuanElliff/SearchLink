import { useEffect, useState, useMemo } from 'react'
import { useNavigate, useLocation } from 'react-router-dom'
import { apiFetch } from '../api/client'

const ESTADO_BADGE = {
  ACTIVA:    'bg-green-100 text-green-800',
  RESUELTA:  'bg-slate-100 text-slate-600',
  CANCELADA: 'bg-red-100 text-red-700',
}

const FILTROS_ESTADO = ['TODOS', 'ACTIVA', 'RESUELTA', 'CANCELADA']

function formatFecha(iso) {
  if (!iso) return '—'
  return new Date(iso).toLocaleString('es-AR', { dateStyle: 'short', timeStyle: 'short' })
}

export default function OperadorPage() {
  const [alertas, setAlertas] = useState([])
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState(null)
  const [filtroEstado, setFiltroEstado] = useState('TODOS')
  const [ordenReciente, setOrdenReciente] = useState(true)
  const navigate = useNavigate()
  const location = useLocation()

  useEffect(() => {
    setCargando(true)
    setError(null)
    apiFetch('/api/alertas?todas=true')
      .then(setAlertas)
      .catch((e) => setError(e.message))
      .finally(() => setCargando(false))
  // Se recarga cada vez que se vuelve desde AlertaEditarPage (state.ts cambia).
  }, [location.state?.ts])

  const alertasFiltradas = useMemo(() => {
    let lista = filtroEstado === 'TODOS'
      ? alertas
      : alertas.filter((a) => a.estado === filtroEstado)
    lista = [...lista].sort((a, b) => {
      const ta = new Date(a.creadaEn).getTime()
      const tb = new Date(b.creadaEn).getTime()
      return ordenReciente ? tb - ta : ta - tb
    })
    return lista
  }, [alertas, filtroEstado, ordenReciente])

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-xl font-bold text-slate-900">Alertas</h1>
        <button
          onClick={() => navigate('/operador/nueva')}
          className="rounded bg-sky-600 px-4 py-2 text-sm font-medium text-white hover:bg-sky-700"
        >
          + Crear alerta
        </button>
      </div>

      {/* Filtros */}
      <div className="flex flex-wrap items-center gap-3">
        <div className="flex items-center gap-2">
          <label className="text-sm text-slate-600">Estado:</label>
          <div className="flex gap-1">
            {FILTROS_ESTADO.map((e) => (
              <button
                key={e}
                onClick={() => setFiltroEstado(e)}
                className={`rounded-full px-3 py-0.5 text-xs font-medium border transition-colors ${
                  filtroEstado === e
                    ? 'bg-sky-600 text-white border-sky-600'
                    : 'bg-white text-slate-600 border-slate-300 hover:bg-slate-50'
                }`}
              >
                {e === 'TODOS' ? 'Todos' : e.charAt(0) + e.slice(1).toLowerCase()}
              </button>
            ))}
          </div>
        </div>
        <div className="flex items-center gap-2">
          <label className="text-sm text-slate-600">Orden:</label>
          <button
            onClick={() => setOrdenReciente((v) => !v)}
            className="rounded border border-slate-300 px-3 py-0.5 text-xs text-slate-600 hover:bg-slate-50"
          >
            {ordenReciente ? 'Más recientes primero' : 'Más antiguos primero'}
          </button>
        </div>
      </div>

      {cargando && <p className="text-slate-500">Cargando alertas…</p>}
      {error   && <p className="text-red-600">{error}</p>}

      {!cargando && !error && alertasFiltradas.length === 0 && (
        <p className="text-slate-500">No hay alertas{filtroEstado !== 'TODOS' ? ` con estado ${filtroEstado}` : ''}.</p>
      )}

      {!cargando && !error && alertasFiltradas.length > 0 && (
        <div className="divide-y divide-slate-200 rounded border border-slate-200 bg-white">
          {alertasFiltradas.map((a) => (
            <div key={a.id} className="flex items-center justify-between gap-4 px-4 py-3">
              <div className="min-w-0 flex-1">
                <p className="truncate font-medium text-slate-900">
                  {a.nombreMenor}
                  {a.edad != null && (
                    <span className="ml-1 font-normal text-slate-500">{a.edad} años</span>
                  )}
                </p>
                <p className="mt-0.5 text-xs text-slate-400">
                  {a.radioKm} km · creada {formatFecha(a.creadaEn)}
                </p>
              </div>
              <span className={`shrink-0 rounded-full px-2 py-0.5 text-xs font-medium ${ESTADO_BADGE[a.estado] ?? 'bg-slate-100 text-slate-600'}`}>
                {a.estado}
              </span>
              <button
                onClick={() => navigate(`/operador/alerta/${a.id}/editar`)}
                className="shrink-0 rounded border border-slate-300 px-3 py-1 text-sm text-slate-700 hover:bg-slate-50"
              >
                Editar
              </button>
              <button
                onClick={() => navigate(`/operador/alerta/${a.id}/avistamientos`)}
                className="shrink-0 rounded border border-slate-300 px-3 py-1 text-sm text-slate-700 hover:bg-slate-50"
              >
                Moderar
              </button>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
