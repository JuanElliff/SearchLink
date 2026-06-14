import { useEffect } from 'react'
import { MapContainer, TileLayer, Marker, useMapEvents, useMap } from 'react-leaflet'
import L from 'leaflet'
import markerIcon from 'leaflet/dist/images/marker-icon.png'
import markerIcon2x from 'leaflet/dist/images/marker-icon-2x.png'
import markerShadow from 'leaflet/dist/images/marker-shadow.png'

// Fix del icono por defecto de Leaflet con bundlers (Vite resuelve los PNG a URLs).
L.Icon.Default.mergeOptions({
  iconUrl: markerIcon,
  iconRetinaUrl: markerIcon2x,
  shadowUrl: markerShadow,
})

const DEFAULT_CENTER = [-34.6037, -58.3816] // Buenos Aires

function ClickHandler({ onPick }) {
  useMapEvents({
    click(e) {
      onPick(e.latlng.lat, e.latlng.lng)
    },
  })
  return null
}

function Recenter({ position }) {
  const map = useMap()
  useEffect(() => {
    if (position) map.setView(position)
  }, [position, map])
  return null
}

/**
 * Selector de ubicación sobre un mapa Leaflet. El valor es { latitud, longitud } o null.
 * Se setea tocando el mapa o con el botón "Usar mi ubicación" (GPS del navegador).
 */
export default function LocationPicker({ value, onChange }) {
  const position = value ? [value.latitud, value.longitud] : null

  const usarMiUbicacion = () => {
    if (!navigator.geolocation) return
    navigator.geolocation.getCurrentPosition(
      (pos) => onChange({ latitud: pos.coords.latitude, longitud: pos.coords.longitude }),
      () => {}, // si se deniega/falla, el usuario puede tocar el mapa
    )
  }

  return (
    <div className="space-y-2">
      <div className="flex items-center justify-between gap-2">
        <p className="text-sm text-slate-600">Tocá el mapa para marcar tu ubicación, o:</p>
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
          <TileLayer
            attribution="&copy; OpenStreetMap"
            url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
          />
          <ClickHandler onPick={(lat, lng) => onChange({ latitud: lat, longitud: lng })} />
          {position && <Marker position={position} />}
          {position && <Recenter position={position} />}
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
