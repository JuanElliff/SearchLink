import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { apiFetch } from '../api/client'
import LocationPicker from '../components/LocationPicker'
import Field from '../components/ui/Field'

const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

function validar({ nombre, email, password, ubicacion }) {
  const e = {}
  if (!nombre.trim())           e.nombre   = 'El nombre es obligatorio'
  if (!email.trim())            e.email    = 'El email es obligatorio'
  else if (!EMAIL_RE.test(email)) e.email  = 'El email no tiene un formato válido'
  if (!password)                e.password = 'La contraseña es obligatoria'
  else if (password.length < 8) e.password = 'La contraseña debe tener al menos 8 caracteres'
  if (!ubicacion)               e.ubicacion = 'La ubicación es obligatoria'
  return e
}

// Los únicos roles que puede crear un ADMIN por estos endpoints.
const ROLES_CREABLES = ['OPERADOR', 'ADMIN']

export default function CrearUsuarioPage() {
  const navigate = useNavigate()
  const [rol, setRol]           = useState('OPERADOR')
  const [nombre, setNombre]     = useState('')
  const [email, setEmail]       = useState('')
  const [password, setPassword] = useState('')
  const [ubicacion, setUbicacion] = useState(null)
  const [errores, setErrores]   = useState({})
  const [errorServidor, setErrorServidor] = useState(null)
  const [exito, setExito]       = useState(null)
  const [enviando, setEnviando] = useState(false)

  const onSubmit = async (ev) => {
    ev.preventDefault()
    setErrorServidor(null)
    setExito(null)
    const e = validar({ nombre, email, password, ubicacion })
    setErrores(e)
    if (Object.keys(e).length > 0) return

    // El endpoint depende del rol elegido.
    const endpoint = rol === 'ADMIN' ? '/api/usuarios/admin' : '/api/usuarios/operador'
    setEnviando(true)
    try {
      // Body FLAT: latitud/longitud top-level (RegistroUsuarioRequest). NO anidado,
      // distinto del body de alertas/avistamientos donde la ubicación va anidada.
      const creado = await apiFetch(endpoint, {
        method: 'POST',
        body: {
          nombre:   nombre.trim(),
          email:    email.trim(),
          password,
          latitud:  ubicacion.latitud,
          longitud: ubicacion.longitud,
        },
      })
      setExito(`${creado.nombre} creado como ${creado.rol}`)
      setNombre(''); setEmail(''); setPassword(''); setUbicacion(null); setErrores({})
    } catch (err) {
      setErrorServidor(
        err.status === 409 ? 'Ya existe un usuario con ese email' : err.message,
      )
    } finally {
      setEnviando(false)
    }
  }

  return (
    <div className="mx-auto max-w-lg space-y-6">
      <div className="flex items-center gap-3">
        <button onClick={() => navigate('/admin')} className="text-slate-400 hover:text-slate-700">
          ← Volver
        </button>
        <h1 className="text-xl font-bold text-slate-900">Crear usuario</h1>
      </div>

      <form onSubmit={onSubmit} className="space-y-5" noValidate>
        {/* Selector de rol */}
        <div>
          <label className="mb-1 block text-sm font-medium" htmlFor="rol">Rol</label>
          <select
            id="rol" value={rol} onChange={(e) => setRol(e.target.value)}
            className="w-full rounded border border-slate-300 px-3 py-2 focus:border-sky-500 focus:outline-none"
          >
            {ROLES_CREABLES.map((r) => <option key={r} value={r}>{r}</option>)}
          </select>
        </div>

        <Field id="nombre" label="Nombre" required value={nombre} onChange={setNombre} error={errores.nombre} />
        <Field id="email"  label="Email"  required type="email" value={email} onChange={setEmail} error={errores.email} />
        <Field id="password" label="Contraseña" required type="password" value={password} onChange={setPassword}
          error={errores.password} hint="Mínimo 8 caracteres" />

        <div>
          <label className="mb-1 block text-sm font-medium">
            Ubicación <span className="text-red-500">*</span>
          </label>
          <LocationPicker value={ubicacion} onChange={setUbicacion} />
          {errores.ubicacion && <p className="mt-1 text-sm text-red-600">{errores.ubicacion}</p>}
        </div>

        {exito && (
          <p className="rounded bg-green-50 px-3 py-2 text-sm font-medium text-green-700">
            ✓ {exito}
          </p>
        )}
        {errorServidor && <p className="text-sm text-red-600">{errorServidor}</p>}

        <div className="flex gap-3">
          <button
            type="submit" disabled={enviando}
            className="rounded bg-sky-600 px-5 py-2 font-medium text-white hover:bg-sky-700 disabled:opacity-60"
          >
            {enviando ? 'Creando…' : 'Crear usuario'}
          </button>
          <button
            type="button" onClick={() => navigate('/admin')}
            className="rounded border border-slate-300 px-5 py-2 text-slate-700 hover:bg-slate-50"
          >
            {exito ? 'Volver a la lista' : 'Cancelar'}
          </button>
        </div>
      </form>
    </div>
  )
}
