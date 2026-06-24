import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { apiFetch } from '../api/client'
import LocationPicker from '../components/LocationPicker'
import Field from '../components/ui/Field'

const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

function validar({ nombre, email, password, confirmarPassword, ubicacion }) {
  const errores = {}
  if (!nombre.trim()) errores.nombre = 'El nombre es obligatorio'
  if (!email.trim()) errores.email = 'El email es obligatorio'
  else if (!EMAIL_RE.test(email)) errores.email = 'El email no tiene un formato válido'
  if (!password) errores.password = 'La contraseña es obligatoria'
  else if (password.length < 8) errores.password = 'La contraseña debe tener al menos 8 caracteres'
  if (!confirmarPassword) errores.confirmarPassword = 'Confirmá la contraseña'
  else if (password && password !== confirmarPassword) errores.confirmarPassword = 'Las contraseñas no coinciden'
  if (!ubicacion) errores.ubicacion = 'La ubicación es obligatoria'
  else if (
    ubicacion.latitud < -90 || ubicacion.latitud > 90 ||
    ubicacion.longitud < -180 || ubicacion.longitud > 180
  ) {
    errores.ubicacion = 'La ubicación está fuera de rango'
  }
  return errores
}

export default function RegistroPage() {
  const navigate = useNavigate()
  const [nombre, setNombre] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [confirmarPassword, setConfirmarPassword] = useState('')
  const [ubicacion, setUbicacion] = useState(null)
  const [errores, setErrores] = useState({})
  const [errorServidor, setErrorServidor] = useState(null)
  const [enviando, setEnviando] = useState(false)

  const onSubmit = async (e) => {
    e.preventDefault()
    setErrorServidor(null)
    const errs = validar({ nombre, email, password, confirmarPassword, ubicacion })
    setErrores(errs)
    if (Object.keys(errs).length > 0) return

    setEnviando(true)
    try {
      // El backend espera latitud/longitud planos (no anidados).
      await apiFetch('/api/usuarios', {
        method: 'POST',
        auth: false,
        body: {
          nombre,
          email,
          password,
          latitud: ubicacion.latitud,
          longitud: ubicacion.longitud,
        },
      })
      navigate('/login', { replace: true })
    } catch (err) {
      setErrorServidor(
        err.status === 409 ? 'Ya existe una cuenta con ese email' : err.message,
      )
    } finally {
      setEnviando(false)
    }
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-slate-50 px-4 py-8">
      <div className="w-full max-w-md rounded-lg bg-white p-6 shadow">
        <h1 className="mb-1 text-2xl font-bold text-slate-900">Crear cuenta</h1>
        <p className="mb-6 text-sm text-slate-500">Te registrás como usuario estándar (ciudadanía).</p>

        <form onSubmit={onSubmit} className="space-y-4" noValidate>
          <Field id="nombre" label="Nombre" value={nombre} onChange={setNombre} error={errores.nombre} />
          <Field id="email" label="Email" type="email" value={email} onChange={setEmail} error={errores.email} />
          <Field id="password" label="Contraseña" type="password" value={password} onChange={setPassword} error={errores.password} hint="Mínimo 8 caracteres" />
          <Field id="confirmarPassword" label="Confirmar contraseña" type="password" value={confirmarPassword} onChange={setConfirmarPassword} error={errores.confirmarPassword} />

          <div>
            <label className="mb-1 block text-sm font-medium">Ubicación</label>
            <LocationPicker value={ubicacion} onChange={setUbicacion} />
            {errores.ubicacion && <p className="mt-1 text-sm text-red-600">{errores.ubicacion}</p>}
          </div>

          {errorServidor && <p className="text-sm text-red-600">{errorServidor}</p>}

          <button
            type="submit" disabled={enviando}
            className="w-full rounded bg-sky-600 py-2 font-medium text-white hover:bg-sky-700 disabled:opacity-60"
          >
            {enviando ? 'Creando…' : 'Crear cuenta'}
          </button>
        </form>

        <p className="mt-4 text-center text-sm text-slate-600">
          ¿Ya tenés cuenta?{' '}
          <Link to="/login" className="font-medium text-sky-600 hover:underline">
            Ingresá
          </Link>
        </p>
      </div>
    </div>
  )
}
