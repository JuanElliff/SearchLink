import { useEffect, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { MapContainer, Marker, Circle, Popup } from 'react-leaflet'
import { apiFetch } from '../api/client'
import BaseTiles from '../components/map/BaseTiles'
import '../lib/leafletIcons'

// EstadoVerificacion (verificado en backend/model/EstadoVerificacion.java)
const ESTADO_BADGE = {
  PENDIENTE:  'bg-amber-100 text-amber-800',
  VERIFICADO: 'bg-green-100 text-green-800',
  DESCARTADO: 'bg-slate-100 text-slate-600',
}

function formatFecha(iso) {
  if (!iso) return '—'
  return new Date(iso).toLocaleString('es-AR', { dateStyle: 'short', timeStyle: 'short' })
}

export default function ModerarAvistamientosPage() {
  const { id } = useParams()
  const navigate = useNavigate()

  const [alerta, setAlerta]               = useState(null)
  const [avistamientos, setAvistamientos] = useState([])
  const [cargando, setCargando]           = useState(true)
  const [error, setError]                 = useState(null)

  // Estado por avistamiento: comentario del operador y flag de en-progreso/error.
  const [comentarios, setComentarios]     = useState({})
  const [moderando, setModerando]         = useState({})
  const [erroresMod, setErroresMod]       = useState({})

  useEffect(() => {
    // Fetch paralelo: contexto de la alerta + lista de avistamientos.
    Promise.all([
      apiFetch(`/api/alertas/${id}`),
      apiFetch(`/api/avistamientos?alertaId=${id}`),
    ])
      .then(([a, av]) => { setAlerta(a); setAvistamientos(av) })
      .catch((e) => setError(e.message))
      .finally(() => setCargando(false))
  }, [id])

  const moderar = async (avId, nuevoEstado) => {
    setModerando((m) => ({ ...m, [avId]: true }))
    setErroresMod((e) => ({ ...e, [avId]: null }))
    try {
      // La UI solo ofrece VERIFICADO o DESCARTADO; PENDIENTE es rechazado por el backend (400).
      const actualizado = await apiFetch(`/api/avistamientos/${avId}/estado`, {
        method: 'PATCH',
        body: {
          nuevoEstado,
          comentariosAdmin: comentarios[avId]?.trim() || null,
        },
      })
      // Actualización local: reemplaza el item sin re-fetch.
      setAvistamientos((prev) => prev.map((a) => (a.id === avId ? actualizado : a)))
    } catch (e) {
      setErroresMod((prev) => ({ ...prev, [avId]: e.message }))
    } finally {
      setModerando((m) => ({ ...m, [avId]: false }))
    }
  }

  if (cargando) return <p className="text-slate-500">Cargando…</p>
  if (error)    return <p className="text-red-600">{error}</p>

  // GeoJSON: coordinates = [lng, lat] → Leaflet necesita [lat, lng].
  const alertaLat    = alerta.ubicacion.coordinates[1]
  const alertaLng    = alerta.ubicacion.coordinates[0]
  const radioMetros  = (alerta.radioKm ?? 10) * 1000

  return (
    <div className="space-y-6">
      {/* Cabecera */}
      <div className="flex items-center gap-3">
        <button onClick={() => navigate('/operador')} className="text-slate-400 hover:text-slate-700">
          ← Volver
        </button>
        <div>
          <h1 className="text-xl font-bold text-slate-900">
            Avistamientos — {alerta.nombreMenor}
          </h1>
          <p className="text-sm text-slate-500">
            Estado de la alerta:{' '}
            <span className="font-medium text-slate-700">{alerta.estado}</span>
          </p>
        </div>
      </div>

      {/* Mapa: alerta + avistamientos */}
      <div className="h-72 w-full overflow-hidden rounded border border-slate-300">
        <MapContainer center={[alertaLat, alertaLng]} zoom={13} className="h-full w-full">
          <BaseTiles />
          {/* Alerta: marker + círculo de radio */}
          <Marker position={[alertaLat, alertaLng]}>
            <Popup><strong>Alerta:</strong> {alerta.nombreMenor}</Popup>
          </Marker>
          <Circle
            center={[alertaLat, alertaLng]}
            radius={radioMetros}
            pathOptions={{ color: '#0ea5e9', fillColor: '#0ea5e9', fillOpacity: 0.08 }}
          />
          {/* Un marker por avistamiento (null-guard: el backend permite ubicacion null) */}
          {avistamientos.map((av) => {
            if (!av.ubicacion?.coordinates) return null
            const lat = av.ubicacion.coordinates[1]
            const lng = av.ubicacion.coordinates[0]
            return (
              <Marker key={av.id} position={[lat, lng]}>
                <Popup>
                  <strong>Avistamiento</strong>
                  <br />{av.descripcion}
                  <br />
                  <span className="text-xs text-gray-500">{av.estado}</span>
                </Popup>
              </Marker>
            )
          })}
        </MapContainer>
      </div>

      {/* Lista de avistamientos */}
      <div>
        <h2 className="mb-3 font-semibold text-slate-800">
          {avistamientos.length === 0
            ? 'Sin avistamientos reportados'
            : `${avistamientos.length} avistamiento${avistamientos.length > 1 ? 's' : ''}`}
        </h2>

        <div className="space-y-4">
          {avistamientos.map((av) => (
            <div key={av.id} className="rounded border border-slate-200 bg-white p-4 space-y-3">
              {/* Datos del avistamiento */}
              <div className="flex items-start justify-between gap-3">
                <p className="text-slate-800">{av.descripcion}</p>
                <span className={`shrink-0 rounded-full px-2 py-0.5 text-xs font-medium ${ESTADO_BADGE[av.estado] ?? 'bg-slate-100 text-slate-600'}`}>
                  {av.estado}
                </span>
              </div>

              <p className="text-xs text-slate-400">
                Reportado {formatFecha(av.creadoEn)}
              </p>

              {av.fotoUrl && (
                <img
                  src={av.fotoUrl}
                  alt="Foto del avistamiento"
                  className="max-h-40 rounded object-cover"
                />
              )}

              {av.comentariosAdmin && (
                <p className="rounded bg-slate-50 px-3 py-2 text-sm text-slate-600">
                  <span className="font-medium">Nota del operador:</span> {av.comentariosAdmin}
                </p>
              )}

              {/* Acciones: solo para PENDIENTE */}
              {av.estado === 'PENDIENTE' && (
                <div className="space-y-2 border-t border-slate-100 pt-3">
                  <input
                    type="text"
                    value={comentarios[av.id] ?? ''}
                    onChange={(e) =>
                      setComentarios((c) => ({ ...c, [av.id]: e.target.value }))
                    }
                    placeholder="Comentario opcional para el operador…"
                    className="w-full rounded border border-slate-300 px-3 py-1.5 text-sm focus:border-sky-500 focus:outline-none"
                  />
                  {erroresMod[av.id] && (
                    <p className="text-xs text-red-600">{erroresMod[av.id]}</p>
                  )}
                  <div className="flex gap-2">
                    <button
                      disabled={moderando[av.id]}
                      onClick={() => moderar(av.id, 'VERIFICADO')}
                      className="rounded bg-green-600 px-4 py-1.5 text-sm font-medium text-white hover:bg-green-700 disabled:opacity-60"
                    >
                      {moderando[av.id] ? '…' : 'Verificar'}
                    </button>
                    <button
                      disabled={moderando[av.id]}
                      onClick={() => moderar(av.id, 'DESCARTADO')}
                      className="rounded border border-slate-300 px-4 py-1.5 text-sm text-slate-700 hover:bg-slate-50 disabled:opacity-60"
                    >
                      {moderando[av.id] ? '…' : 'Descartar'}
                    </button>
                  </div>
                </div>
              )}
            </div>
          ))}
        </div>
      </div>
    </div>
  )
}
