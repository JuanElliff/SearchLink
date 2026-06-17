import { TileLayer } from 'react-leaflet'

// Config centralizada del basemap (antes repetida literal en 4 archivos).
// CartoDB Positron: tiles monocromáticos y sobrios; {r} sirve retina (@2x).
export const TILE_URL = 'https://{s}.basemaps.cartocdn.com/light_all/{z}/{x}/{y}{r}.png'
export const TILE_ATTRIBUTION =
  '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors &copy; <a href="https://carto.com/attributions">CARTO</a>'

// Capa base lista para usar dentro de cualquier <MapContainer>.
export default function BaseTiles() {
  return <TileLayer attribution={TILE_ATTRIBUTION} url={TILE_URL} />
}
