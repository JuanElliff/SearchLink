import { useEffect, useState } from 'react'
import { MapContainer, Marker, useMapEvents, useMap } from 'react-leaflet'
import BaseTiles from './map/BaseTiles'
import { iconUbicacion } from '../lib/leafletIcons'

const DEFAULT_CENTER = [-34.6037, -58.3816] // Buenos Aires
const NOMINATIM_URL = 'https://nominatim.openstreetmap.org/search'

function ClickHandler({ onPick }) {
  useMapEvents({
    click(e) {
      onPick(e.latlng.lat, e.latlng.lng)
    },
  })
  return null
}

function Recenter({ position, zoom }) {
  const map = useMap()
  useEffect(() => {
    if (position) map.setView(position, zoom ?? map.getZoom())
  }, [position, zoom, map])
  return null
}

export default function LocationPicker({ value, onChange }) {
  const position = value ? [value.latitud, value.longitud] : null

  const [direccion, setDireccion] = useState('')
  const [buscando, setBuscando] = useState(false)
  const [errorGeo, setErrorGeo] = useState(null)
  const [geoZoom, setGeoZoom] = useState(undefined)

  const usarMiUbicacion = () => {
    if (!navigator.geolocation) return
    navigator.geolocation.getCurrentPosition(
      (pos) => onChange({ latitud: pos.coords.latitude, longitud: pos.coords.longitude }),
      () => {},
    )
  }

  const buscarDireccion = async () => {
    const query = direccion.trim()
    if (!query) return
    setErrorGeo(null)
    setBuscando(true)
    try {
      const url = `${NOMINATIM_URL}?format=json&q=${encodeURIComponent(query)}&countrycodes=ar&limit=1`
      const res = await fetch(url)
      if (!res.ok) throw new Error(`Error de red (${res.status})`)
      const data = await res.json()
      if (!data.length) {
        setErrorGeo('No se encontró la dirección.')
        return
      }
      setGeoZoom(15)
      onChange({ latitud: Number(data[0].lat), longitud: Number(data[0].lon) })
    } catch (err) {
      setErrorGeo(err.message || 'No se pudo contactar el servicio de geocodificación.')
    } finally {
      setBuscando(false)
    }
  }

  const onKeyDown = (e) => {
    if (e.key === 'Enter') {
      e.preventDefault()
      buscarDireccion()
    }
  }

  return (
    <div className="space-y-2">
      <div className="flex gap-2">
        <input
          type="text"
          value={direccion}
          onChange={(e) => setDireccion(e.target.value)}
          onKeyDown={onKeyDown}
          placeholder="Ej: Av. Santa Fe 3345, CABA"
          className="min-w-0 flex-1 rounded border border-slate-300 px-3 py-1 text-sm focus:border-sky-500 focus:outline-none"
        />
        <button
          type="button"
          onClick={buscarDireccion}
          disabled={buscando || !direccion.trim()}
          className="shrink-0 rounded bg-sky-600 px-3 py-1 text-sm font-medium text-white hover:bg-sky-700 disabled:opacity-50"
        >
          {buscando ? 'Buscando…' : 'Ubicar'}
        </button>
      </div>
      {errorGeo && <p className="text-sm text-red-600">{errorGeo}</p>}

      <div className="flex items-center justify-between gap-2">
        <p className="text-sm text-slate-600">O tocá el mapa para marcar la ubicación, o:</p>
        <button
          type="button"
          onClick={usarMiUbicacion}
          className="shrink-0 rounded bg-slate-200 px-3 py-1 text-sm hover:bg-slate-300"
        >
          Usar mi ubicación
        </button>
      </div>

      <div className="h-64 w-full overflow-hidden rounded border border-slate-300">
        <MapContainer center={position || DEFAULT_CENTER} zoom={12} className="h-full w-full">
          <BaseTiles />
          <ClickHandler onPick={(lat, lng) => onChange({ latitud: lat, longitud: lng })} />
          {position && <Marker position={position} icon={iconUbicacion} />}
          {position && <Recenter position={position} zoom={geoZoom} />}
        </MapContainer>
      </div>

      {value && (
        <p className="text-xs text-slate-500">
          Lat {value.latitud.toFixed(5)}, Lng {value.longitud.toFixed(5)}
        </p>
      )}
    </div>
  )
}
