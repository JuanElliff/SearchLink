import Input from './Input'

/**
 * Campo de formulario unificado: label + Input + hint/error inline.
 * Reemplaza el componente `Campo` que estaba triplicado en RegistroPage,
 * AlertaCrearPage y CrearUsuarioPage. Mantiene su mismo contrato para que la
 * sustitución sea mecánica: `onChange` recibe el valor (no el evento), y los
 * props extra (min, step, placeholder, …) se reenvían al <input>.
 */
export default function Field({
  id,
  label,
  type = 'text',
  required = false,
  value,
  onChange,
  error,
  hint,
  ...rest
}) {
  return (
    <div>
      {label && (
        <label className="mb-1 block text-sm font-medium text-slate-700" htmlFor={id}>
          {label}
          {required && <span className="ml-0.5 text-danger">*</span>}
        </label>
      )}
      <Input
        id={id}
        type={type}
        value={value}
        invalid={Boolean(error)}
        onChange={(e) => onChange(e.target.value)}
        {...rest}
      />
      {hint && !error && <p className="mt-1 text-xs text-slate-400">{hint}</p>}
      {error && <p className="mt-1 text-sm text-danger">{error}</p>}
    </div>
  )
}
