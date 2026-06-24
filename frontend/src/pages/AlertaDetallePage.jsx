import { useEffect, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { MapContainer, Marker, Circle } from 'react-leaflet'
import { apiFetch } from '../api/client'
import BaseTiles from '../components/map/BaseTiles'
import { iconAlerta } from '../lib/leafletIcons'

function formatFechaCorta(iso) {
  if (!iso) return '—'
  return new Date(iso).toLocaleString('es-AR', { dateStyle: 'short', timeStyle: 'short' })
}

const ESTADO_LABEL = {
  ACTIVA: { texto: 'Activa', cls: 'bg-green-100 text-green-800' },
  RESUELTA: { texto: 'Resuelta', cls: 'bg-slate-100 text-slate-600' },
  CANCELADA: { texto: 'Cancelada', cls: 'bg-red-100 text-red-700' },
}

function formatFecha(iso) {
  if (!iso) return '—'
  return new Date(iso).toLocaleString('es-AR', {
    dateStyle: 'medium',
    timeStyle: 'short',
  })
}

export default function AlertaDetallePage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const [alerta, setAlerta] = useState(null)
  const [avistamientos, setAvistamientos] = useState([])
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState(null)

  useEffect(() => {
    Promise.all([
      apiFetch(`/api/alertas/${id}`),
      apiFetch(`/api/avistamientos?alertaId=${id}`),
    ])
      .then(([a, av]) => { setAlerta(a); setAvistamientos(av) })
      .catch((e) => setError(e.status === 404 ? 'Alerta no encontrada.' : e.message))
      .finally(() => setCargando(false))
  }, [id])

  if (cargando) return <p className="text-slate-500">Cargando…</p>
  if (error) return <p className="text-red-600">{error}</p>

  // GeoJSON: coordinates = [lng, lat] → Leaflet necesita [lat, lng]
  const lat = alerta.ubicacion.coordinates[1]
  const lng = alerta.ubicacion.coordinates[0]
  const radioMetros = (alerta.radioKm ?? 10) * 1000
  const estadoInfo = ESTADO_LABEL[alerta.estado] ?? { texto: alerta.estado, cls: 'bg-slate-100 text-slate-600' }

  return (
    <div className="mx-auto max-w-2xl space-y-6">
      {/* Cabecera */}
      <div className="flex items-start justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-900">{alerta.nombreMenor}</h1>
          {alerta.edad != null && (
            <p className="text-slate-500">{alerta.edad} años</p>
          )}
        </div>
        <span className={`shrink-0 rounded-full px-3 py-1 text-sm font-medium ${estadoInfo.cls}`}>
          {estadoInfo.texto}
        </span>
      </div>

      {/* Foto */}
      {alerta.fotoUrl && (
        <img
          src={alerta.fotoUrl}
          alt={`Foto de ${alerta.nombreMenor}`}
          className="w-full max-h-64 rounded object-cover"
        />
      )}

      {/* Descripción */}
      {alerta.descripcion && (
        <p className="text-slate-700">{alerta.descripcion}</p>
      )}

      {/* Mapa con círculo de radio */}
      <div className="h-64 w-full overflow-hidden rounded border border-slate-300">
        <MapContainer center={[lat, lng]} zoom={13} className="h-full w-full">
          <BaseTiles />
          <Marker position={[lat, lng]} icon={iconAlerta} />
          <Circle
            center={[lat, lng]}
            radius={radioMetros}
            pathOptions={{ color: '#0ea5e9', fillColor: '#0ea5e9', fillOpacity: 0.1 }}
          />
        </MapContainer>
      </div>

      {/* Metadatos */}
      <dl className="grid grid-cols-2 gap-x-4 gap-y-2 text-sm">
        <div>
          <dt className="font-medium text-slate-500">Radio de búsqueda</dt>
          <dd className="text-slate-900">{alerta.radioKm} km</dd>
        </div>
        <div>
          <dt className="font-medium text-slate-500">Creada</dt>
          <dd className="text-slate-900">{formatFecha(alerta.creadaEn)}</dd>
        </div>
        <div>
          <dt className="font-medium text-slate-500">Expira</dt>
          <dd className="text-slate-900">{formatFecha(alerta.expiraEn)}</dd>
        </div>
      </dl>

      {/* Avistamientos confirmados */}
      <div>
        <h2 className="mb-3 font-semibold text-slate-800">Avistamientos confirmados</h2>
        {avistamientos.length === 0 ? (
          <p className="text-sm text-slate-500">Sin avistamientos confirmados aún.</p>
        ) : (
          <div className="space-y-3">
            {avistamientos.map((av) => (
              <div key={av.id} className="rounded border border-slate-200 bg-white p-4 space-y-2">
                {av.descripcion && (
                  <p className="text-slate-800 text-sm">{av.descripcion}</p>
                )}
                {av.fotoUrl && (
                  <img
                    src={av.fotoUrl}
                    alt="Foto del avistamiento"
                    className="max-h-40 rounded object-cover"
                  />
                )}
                <p className="text-xs text-slate-400">
                  Reportado {formatFechaCorta(av.creadoEn)}
                </p>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Acciones */}
      <div className="flex gap-3">
        {/* Solo cuando la alerta está ACTIVA (EstadoAlerta.ACTIVA del backend). */}
        {alerta.estado === 'ACTIVA' && (
          <button
            onClick={() => navigate(`/alerta/${id}/avistamiento`)}
            className="rounded bg-sky-600 px-5 py-2 font-medium text-white hover:bg-sky-700"
          >
            Reportar avistamiento
          </button>
        )}
        <button
          onClick={() => navigate('/')}
          className="rounded border border-slate-300 px-5 py-2 text-slate-700 hover:bg-slate-50"
        >
          Volver al mapa
        </button>
      </div>
    </div>
  )
}
