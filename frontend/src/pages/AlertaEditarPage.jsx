import { useEffect, useState } from 'react'
import { useParams, useNavigate, useLocation } from 'react-router-dom'
import { apiFetch } from '../api/client'

// Valores válidos de EstadoAlerta (verificados en backend/model/EstadoAlerta.java).
const ESTADOS = ['ACTIVA', 'RESUELTA', 'CANCELADA']

function validar({ radioKm }) {
  const e = {}
  if (!radioKm)                e.radioKm = 'El radio es obligatorio'
  else if (Number(radioKm) <= 0) e.radioKm = 'El radio debe ser mayor a 0'
  return e
}

export default function AlertaEditarPage() {
  const { id } = useParams()
  const navigate = useNavigate()

  const [alerta, setAlerta]           = useState(null)
  const [cargando, setCargando]       = useState(true)
  const [errorCarga, setErrorCarga]   = useState(null)

  const [estado, setEstado]           = useState('')
  const [radioKm, setRadioKm]         = useState('')
  const [errores, setErrores]         = useState({})
  const [errorServidor, setErrorServidor] = useState(null)
  const [enviando, setEnviando]       = useState(false)
  const [guardado, setGuardado]       = useState(false)

  useEffect(() => {
    apiFetch(`/api/alertas/${id}`)
      .then((a) => {
        setAlerta(a)
        setEstado(a.estado)
        setRadioKm(String(a.radioKm))
      })
      .catch((e) => setErrorCarga(e.status === 404 ? 'Alerta no encontrada.' : e.message))
      .finally(() => setCargando(false))
  }, [id])

  const onSubmit = async (ev) => {
    ev.preventDefault()
    setErrorServidor(null)
    setGuardado(false)
    const e = validar({ radioKm })
    setErrores(e)
    if (Object.keys(e).length > 0) return

    setEnviando(true)
    try {
      // PATCH acepta SOLO estado y radioKm (ActualizarAlertaRequest del backend).
      // Enviamos ambos siempre; el service aplica los no-nulos.
      await apiFetch(`/api/alertas/${id}`, {
        method: 'PATCH',
        body: { estado, radioKm: Number(radioKm) },
      })
      setGuardado(true)
    } catch (err) {
      setErrorServidor(err.message)
    } finally {
      setEnviando(false)
    }
  }

  if (cargando)   return <p className="text-slate-500">Cargando…</p>
  if (errorCarga) return <p className="text-red-600">{errorCarga}</p>

  return (
    <div className="mx-auto max-w-md space-y-6">
      <div className="flex items-center gap-3">
        <button onClick={() => navigate('/operador')} className="text-slate-400 hover:text-slate-700">
          ← Volver
        </button>
        <div>
          <h1 className="text-xl font-bold text-slate-900">{alerta.nombreMenor}</h1>
          <p className="text-sm text-slate-500">
            Solo se puede cambiar el estado y el radio de búsqueda.
          </p>
        </div>
      </div>

      <form onSubmit={onSubmit} className="space-y-5" noValidate>
        <div>
          <label className="mb-1 block text-sm font-medium" htmlFor="estado">Estado</label>
          <select id="estado" value={estado} onChange={(e) => setEstado(e.target.value)}
            className="w-full rounded border border-slate-300 px-3 py-2 focus:border-sky-500 focus:outline-none">
            {ESTADOS.map((s) => (
              <option key={s} value={s}>{s}</option>
            ))}
          </select>
        </div>

        <div>
          <label className="mb-1 block text-sm font-medium" htmlFor="radioKm">
            Radio de búsqueda (km) <span className="text-red-500">*</span>
          </label>
          <input id="radioKm" type="number" min="0.1" step="0.5" value={radioKm}
            onChange={(e) => setRadioKm(e.target.value)}
            className="w-full rounded border border-slate-300 px-3 py-2 focus:border-sky-500 focus:outline-none" />
          {errores.radioKm && <p className="mt-1 text-sm text-red-600">{errores.radioKm}</p>}
        </div>

        {errorServidor && <p className="text-sm text-red-600">{errorServidor}</p>}
        {guardado && (
          <p className="text-sm font-medium text-green-700">✓ Cambios guardados</p>
        )}

        <div className="flex gap-3">
          <button type="submit" disabled={enviando}
            className="rounded bg-sky-600 px-5 py-2 font-medium text-white hover:bg-sky-700 disabled:opacity-60">
            {enviando ? 'Guardando…' : 'Guardar cambios'}
          </button>
          <button type="button" onClick={() => navigate('/operador', { state: { ts: Date.now() } })}
            className="rounded border border-slate-300 px-5 py-2 text-slate-700 hover:bg-slate-50">
            Volver a la lista
          </button>
        </div>
      </form>
    </div>
  )
}
