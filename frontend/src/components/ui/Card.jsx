// Contenedor estándar y ÚNICA fuente de verdad del "marco": superficie,
// encuadre (borde + radius) y profundidad sutil con padding consistente.
export default function Card({ className = '', children, ...props }) {
  return (
    <div
      className={`rounded-lg border border-border bg-surface p-4 shadow-sm ${className}`}
      {...props}
    >
      {children}
    </div>
  )
}
