import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { apiFetch } from '../api/client'

const ESTADO_BADGE = {
  ACTIVA:    'bg-green-100 text-green-800',
  RESUELTA:  'bg-slate-100 text-slate-600',
  CANCELADA: 'bg-red-100 text-red-700',
}

function formatFecha(iso) {
  if (!iso) return '—'
  return new Date(iso).toLocaleString('es-AR', { dateStyle: 'short', timeStyle: 'short' })
}

export default function OperadorPage() {
  const [alertas, setAlertas] = useState([])
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState(null)
  const navigate = useNavigate()

  useEffect(() => {
    apiFetch('/api/alertas')
      .then(setAlertas)
      .catch((e) => setError(e.message))
      .finally(() => setCargando(false))
  }, [])

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-xl font-bold text-slate-900">Alertas activas</h1>
        <button
          onClick={() => navigate('/operador/nueva')}
          className="rounded bg-sky-600 px-4 py-2 text-sm font-medium text-white hover:bg-sky-700"
        >
          + Crear alerta
        </button>
      </div>

      {cargando && <p className="text-slate-500">Cargando alertas…</p>}
      {error   && <p className="text-red-600">{error}</p>}

      {!cargando && !error && alertas.length === 0 && (
        <p className="text-slate-500">No hay alertas activas.</p>
      )}

      {!cargando && !error && alertas.length > 0 && (
        <div className="divide-y divide-slate-200 rounded border border-slate-200 bg-white">
          {alertas.map((a) => (
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
            </div>
          ))}
        </div>
      )}

      <p className="text-xs text-slate-400">
        Solo se muestran las alertas activas. Para ver una alerta resuelta o cancelada, accedé por
        su id directamente.
      </p>
    </div>
  )
}
