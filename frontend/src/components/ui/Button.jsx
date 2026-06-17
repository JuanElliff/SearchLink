import Spinner from './Spinner'

// Única fuente de verdad para los estilos de botón (antes repetidos en ~10 archivos).
// Variantes ancladas a tokens (primary/brand/danger). Sin variantes nuevas.
const VARIANTS = {
  primary: 'bg-primary text-white hover:bg-primary-hover active:bg-primary-active',
  secondary: 'border border-border bg-surface text-slate-700 hover:bg-slate-50',
  danger: 'bg-danger text-white hover:bg-danger-hover',
  ghost: 'text-slate-500 hover:bg-slate-100 hover:text-slate-800',
}

export default function Button({
  variant = 'primary',
  loading = false,
  disabled = false,
  type = 'button',
  className = '',
  children,
  ...props
}) {
  return (
    <button
      type={type}
      disabled={disabled || loading}
      className={`inline-flex items-center justify-center gap-2 rounded-md px-4 py-2 text-sm font-medium transition-colors focus:outline-none focus:ring-2 focus:ring-primary-200 disabled:cursor-not-allowed disabled:opacity-60 ${VARIANTS[variant] ?? VARIANTS.primary} ${className}`}
      {...props}
    >
      {loading && <Spinner size="sm" />}
      {children}
    </button>
  )
}
