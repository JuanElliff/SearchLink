import L from 'leaflet'
import markerIcon from 'leaflet/dist/images/marker-icon.png'
import markerIcon2x from 'leaflet/dist/images/marker-icon-2x.png'
import markerShadow from 'leaflet/dist/images/marker-shadow.png'

// Fix del icono por defecto de Leaflet con bundlers (Vite resuelve los PNG a URLs).
// Importado una sola vez; los componentes que usen Leaflet importan este módulo.
L.Icon.Default.mergeOptions({
  iconUrl: markerIcon,
  iconRetinaUrl: markerIcon2x,
  shadowUrl: markerShadow,
})

// ─────────────────────────────────────────────────────────────────────────
// Marcadores custom (divIcon SVG, sin dependencias de imagen ni CSS extra).
// Colores literales = tokens de paleta (ver src/index.css): danger / primary / muted.
// Disponibles para que las páginas los pasen como prop `icon` de <Marker>.
// ─────────────────────────────────────────────────────────────────────────
function pinIcon(color) {
  const svg = `
    <svg width="26" height="36" viewBox="0 0 26 36" xmlns="http://www.w3.org/2000/svg">
      <path d="M13 0C5.82 0 0 5.82 0 13c0 9.1 13 23 13 23s13-13.9 13-23C26 5.82 20.18 0 13 0z" fill="${color}"/>
      <circle cx="13" cy="13" r="5" fill="#ffffff"/>
    </svg>`
  return L.divIcon({
    html: svg,
    className: '', // sin clase => evita el fondo/borde blanco del divIcon por defecto
    iconSize: [26, 36],
    iconAnchor: [13, 36],
    popupAnchor: [0, -32],
  })
}

export const iconAlerta = pinIcon('#dc2626')        // danger — alerta de menor
export const iconAvistamiento = pinIcon('#1d4ed8')  // primary — reporte de avistamiento
export const iconUbicacion = pinIcon('#64748b')     // muted — ubicación seleccionada
