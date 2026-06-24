import { useState } from 'react'
import { useAuth } from '../auth/AuthContext'
import { apiFetch } from '../api/client'
import Field from '../components/ui/Field'
import LocationPicker from '../components/LocationPicker'

export default function PerfilPage() {
  const { usuario } = useAuth()

  const [nombre, setNombre] = useState(usuario?.nombre ?? '')
  const [ubicacion, setUbicacion] = useState(null)
  const [passwordActual, setPasswordActual] = useState('')
  const [nuevaPassword, setNuevaPassword] = useState('')
  const [confirmarPassword, setConfirmarPassword] = useState('')

  const [errores, setErrores] = useState({})
  const [errorServidor, setErrorServidor] = useState(null)
  const [guardado, setGuardado] = useState(false)
  const [enviando, setEnviando] = useState(false)

  const validar = () => {
    const e = {}
    if (!nombre.trim()) e.nombre = 'El nombre es obligatorio'
    if (nuevaPassword) {
      if (nuevaPassword.length < 8) e.nuevaPassword = 'La contraseña debe tener al menos 8 caracteres'
      if (nuevaPassword !== confirmarPassword) e.confirmarPassword = 'Las contraseñas no coinciden'
      if (!passwordActual) e.passwordActual = 'Ingresá tu contraseña actual para cambiarla'
    }
    return e
  }

  const onSubmit = async (e) => {
    e.preventDefault()
    setErrorServidor(null)
    setGuardado(false)
    const errs = validar()
    setErrores(errs)
    if (Object.keys(errs).length > 0) return

    const body = { nombre: nombre.trim() }
    if (ubicacion) {
      body.latitud = ubicacion.latitud
      body.longitud = ubicacion.longitud
    }
    if (nuevaPassword) {
      body.nuevaPassword = nuevaPassword
      body.passwordActual = passwordActual
    }

    setEnviando(true)
    try {
      await apiFetch(`/api/usuarios/${usuario.id}/perfil`, { method: 'PATCH', body })
      setGuardado(true)
      setPasswordActual('')
      setNuevaPassword('')
      setConfirmarPassword('')
      setUbicacion(null)
    } catch (err) {
      setErrorServidor(err.message)
    } finally {
      setEnviando(false)
    }
  }

  return (
    <div className="mx-auto max-w-lg space-y-6">
      <h1 className="text-xl font-bold text-slate-900">Mi perfil</h1>

      <form onSubmit={onSubmit} className="space-y-5" noValidate>
        <Field
          id="nombre"
          label="Nombre"
          value={nombre}
          onChange={setNombre}
          error={errores.nombre}
        />

        <div>
          <p className="mb-1 text-sm font-medium text-slate-700">
            Actualizar ubicación
          </p>
          <p className="mb-2 text-xs text-slate-500">
            Dejá el mapa sin selección para no cambiar tu ubicación actual.
          </p>
          <LocationPicker value={ubicacion} onChange={setUbicacion} />
        </div>

        <div className="rounded border border-slate-200 p-4 space-y-4">
          <p className="text-sm font-medium text-slate-700">Cambiar contraseña</p>
          <p className="text-xs text-slate-500">Completá estos campos solo si querés cambiar la contraseña.</p>
          <Field
            id="passwordActual"
            label="Contraseña actual"
            type="password"
            value={passwordActual}
            onChange={setPasswordActual}
            error={errores.passwordActual}
          />
          <Field
            id="nuevaPassword"
            label="Nueva contraseña"
            type="password"
            value={nuevaPassword}
            onChange={setNuevaPassword}
            error={errores.nuevaPassword}
            hint="Mínimo 8 caracteres"
          />
          <Field
            id="confirmarPassword"
            label="Confirmar nueva contraseña"
            type="password"
            value={confirmarPassword}
            onChange={setConfirmarPassword}
            error={errores.confirmarPassword}
          />
        </div>

        {errorServidor && <p className="text-sm text-red-600">{errorServidor}</p>}
        {guardado && <p className="text-sm font-medium text-green-700">✓ Perfil actualizado</p>}

        <button
          type="submit"
          disabled={enviando}
          className="rounded bg-sky-600 px-5 py-2 font-medium text-white hover:bg-sky-700 disabled:opacity-60"
        >
          {enviando ? 'Guardando…' : 'Guardar cambios'}
        </button>
      </form>
    </div>
  )
}
