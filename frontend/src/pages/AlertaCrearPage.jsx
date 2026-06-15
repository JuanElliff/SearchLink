import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { apiFetch } from '../api/client'
import LocationPicker from '../components/LocationPicker'

function validar({ nombreMenor, radioKm, ubicacion }) {
  const e = {}
  if (!nombreMenor.trim())   e.nombreMenor = 'El nombre del menor es obligatorio'
  if (!radioKm)              e.radioKm     = 'El radio es obligatorio'
  else if (Number(radioKm) <= 0) e.radioKm = 'El radio debe ser mayor a 0'
  if (!ubicacion)            e.ubicacion   = 'La ubicación es obligatoria'
  return e
}

export default function AlertaCrearPage() {
  const navigate = useNavigate()
  const [nombreMenor, setNombreMenor]   = useState('')
  const [edad, setEdad]                 = useState('')
  const [descripcion, setDescripcion]   = useState('')
  const [fotoUrl, setFotoUrl]           = useState('')
  const [ubicacion, setUbicacion]       = useState(null)
  const [radioKm, setRadioKm]           = useState('')
  const [errores, setErrores]           = useState({})
  const [errorServidor, setErrorServidor] = useState(null)
  const [enviando, setEnviando]         = useState(false)

  const onSubmit = async (ev) => {
    ev.preventDefault()
    setErrorServidor(null)
    const e = validar({ nombreMenor, radioKm, ubicacion })
    setErrores(e)
    if (Object.keys(e).length > 0) return

    setEnviando(true)
    try {
      // edad es Integer opcional: cadena vacía → null; de lo contrario entero.
      // ubicacion va ANIDADA ({latitud, longitud}), igual que el registro y el avistamiento.
      await apiFetch('/api/alertas', {
        method: 'POST',
        body: {
          nombreMenor:  nombreMenor.trim(),
          edad:         edad !== '' ? parseInt(edad, 10) : null,
          descripcion:  descripcion.trim() || null,
          fotoUrl:      fotoUrl.trim()     || null,
          ubicacion:    { latitud: ubicacion.latitud, longitud: ubicacion.longitud },
          radioKm:      Number(radioKm),
        },
      })
      navigate('/operador')
    } catch (err) {
      setErrorServidor(err.message)
    } finally {
      setEnviando(false)
    }
  }

  return (
    <div className="mx-auto max-w-lg space-y-6">
      <div className="flex items-center gap-3">
        <button onClick={() => navigate('/operador')} className="text-slate-400 hover:text-slate-700">
          ← Volver
        </button>
        <h1 className="text-xl font-bold text-slate-900">Nueva alerta</h1>
      </div>

      <form onSubmit={onSubmit} className="space-y-5" noValidate>
        <Campo id="nombreMenor" label="Nombre del menor" required
          value={nombreMenor} onChange={setNombreMenor} error={errores.nombreMenor} />

        <Campo id="edad" label="Edad" type="number" min="0"
          value={edad} onChange={setEdad} hint="Opcional" />

        <div>
          <label className="mb-1 block text-sm font-medium" htmlFor="descripcion">Descripción</label>
          <textarea id="descripcion" rows={3} value={descripcion}
            onChange={(e) => setDescripcion(e.target.value)}
            placeholder="Ropa que llevaba, señas particulares…"
            className="w-full rounded border border-slate-300 px-3 py-2 focus:border-sky-500 focus:outline-none"
          />
        </div>

        <Campo id="fotoUrl" label="URL de foto" value={fotoUrl} onChange={setFotoUrl}
          hint="Opcional. El upload de archivos es un bloque posterior." />

        <div>
          <label className="mb-1 block text-sm font-medium">
            Ubicación <span className="text-red-500">*</span>
          </label>
          <LocationPicker value={ubicacion} onChange={setUbicacion} />
          {errores.ubicacion && <p className="mt-1 text-sm text-red-600">{errores.ubicacion}</p>}
        </div>

        <Campo id="radioKm" label="Radio de búsqueda (km)" type="number" min="0.1" step="0.5"
          required value={radioKm} onChange={setRadioKm} error={errores.radioKm} />

        {errorServidor && <p className="text-sm text-red-600">{errorServidor}</p>}

        <div className="flex gap-3">
          <button type="submit" disabled={enviando}
            className="rounded bg-sky-600 px-5 py-2 font-medium text-white hover:bg-sky-700 disabled:opacity-60">
            {enviando ? 'Creando…' : 'Crear alerta'}
          </button>
          <button type="button" onClick={() => navigate('/operador')}
            className="rounded border border-slate-300 px-5 py-2 text-slate-700 hover:bg-slate-50">
            Cancelar
          </button>
        </div>
      </form>
    </div>
  )
}

function Campo({ id, label, type = 'text', required, value, onChange, error, hint, ...rest }) {
  return (
    <div>
      <label className="mb-1 block text-sm font-medium" htmlFor={id}>
        {label}{required && <span className="ml-0.5 text-red-500">*</span>}
      </label>
      <input id={id} type={type} value={value}
        onChange={(e) => onChange(e.target.value)}
        className="w-full rounded border border-slate-300 px-3 py-2 focus:border-sky-500 focus:outline-none"
        {...rest} />
      {hint  && !error && <p className="mt-1 text-xs text-slate-400">{hint}</p>}
      {error && <p className="mt-1 text-sm text-red-600">{error}</p>}
    </div>
  )
}
