// Input de texto base. `invalid` pinta el borde en danger (se conecta con Field).
export default function Input({ invalid = false, className = '', ...props }) {
  return (
    <input
      className={`w-full rounded-md border px-3 py-2 text-slate-900 placeholder:text-slate-400 focus:outline-none focus:ring-2 focus:ring-blue-100 disabled:bg-slate-50 ${
        invalid ? 'border-danger focus:border-danger' : 'border-slate-300 focus:border-primary'
      } ${className}`}
      {...props}
    />
  )
}
