// Estado vacío consistente para listas/colecciones sin datos.
// `icon` opcional (emoji o nodo), `action` opcional (ej. un <Button>).
export default function EmptyState({ icon, title, description, action, className = '' }) {
  return (
    <div className={`flex flex-col items-center justify-center gap-2 py-12 text-center ${className}`}>
      {icon && <div className="text-3xl text-slate-300">{icon}</div>}
      <p className="font-medium text-slate-600">{title}</p>
      {description && <p className="max-w-sm text-sm text-slate-400">{description}</p>}
      {action && <div className="mt-2">{action}</div>}
    </div>
  )
}
