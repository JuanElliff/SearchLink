// Etiqueta de estado. Variantes por significado semántico, no por color crudo.
// Convención de uso en SearchLink:
//   alerta ACTIVA → danger · RESUELTA → success · CANCELADA → neutral
//   avistamiento PENDIENTE → warning · VERIFICADO → success · DESCARTADO → neutral
//   rol ADMIN → danger · OPERADOR → primary · ESTANDAR → neutral
// Cada variante = fondo tenue + texto sólido + borde, para que COMUNIQUE estado
// (no que adorne). Respaldado por los tokens semánticos -50/-200/-700.
const VARIANTS = {
  primary: 'bg-primary-50 text-primary-700 border-primary-200',
  danger: 'bg-danger-50 text-danger-700 border-danger-200',
  success: 'bg-success-50 text-success-700 border-success-200',
  warning: 'bg-warning-50 text-warning-700 border-warning-200',
  neutral: 'bg-slate-50 text-slate-600 border-slate-200',
}

export default function Badge({ variant = 'neutral', className = '', children }) {
  return (
    <span
      className={`inline-block rounded-full border px-2 py-0.5 text-xs font-medium ${
        VARIANTS[variant] ?? VARIANTS.neutral
      } ${className}`}
    >
      {children}
    </span>
  )
}
