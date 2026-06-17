import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { MapContainer, Marker, Popup } from 'react-leaflet'
import { apiFetch } from '../api/client'
import BaseTiles from '../components/map/BaseTiles'
import '../lib/leafletIcons'

const DEFAULT_CENTER = [-34.6037, -58.3816]

export default function EstandarHomePage() {
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
      <h1 className="text-xl font-bold text-slate-900">Alertas activas</h1>

      {cargando && <p className="text-slate-500">Cargando alertas…</p>}
      {error && <p className="text-red-600">{error}</p>}

      {!cargando && !error && (
        <div className="h-[70vh] w-full overflow-hidden rounded border border-slate-300">
          <MapContainer center={DEFAULT_CENTER} zoom={12} className="h-full w-full">
            <BaseTiles />
            {alertas.map((alerta) => {
              // GeoJSON: coordinates = [lng, lat] → Leaflet necesita [lat, lng]
              const lat = alerta.ubicacion.coordinates[1]
              const lng = alerta.ubicacion.coordinates[0]
              return (
                <Marker key={alerta.id} position={[lat, lng]}>
                  <Popup>
                    <strong>{alerta.nombreMenor}</strong>
                    {alerta.edad != null && <span>, {alerta.edad} años</span>}
                    <br />
                    <button
                      className="mt-1 text-sky-600 underline"
                      onClick={() => navigate(`/alerta/${alerta.id}`)}
                    >
                      Ver detalle
                    </button>
                  </Popup>
                </Marker>
              )
            })}
          </MapContainer>
        </div>
      )}

      {!cargando && !error && alertas.length === 0 && (
        <p className="text-slate-500">No hay alertas activas en este momento.</p>
      )}
    </div>
  )
}
