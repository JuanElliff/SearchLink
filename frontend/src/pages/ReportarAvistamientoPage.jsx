import { useEffect, useMemo, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { apiFetch } from '../api/client'
import LocationPicker from '../components/LocationPicker'

export default function ReportarAvistamientoPage() {
  const { id: alertaId } = useParams()
  const navigate = useNavigate()
  const [descripcion, setDescripcion] = useState('')
  const [foto, setFoto] = useState(null)
  const [ubicacion, setUbicacion] = useState(null)
  const [errores, setErrores] = useState({})
  const [errorServidor, setErrorServidor] = useState(null)
  const [exito, setExito] = useState(false)
  const [enviando, setEnviando] = useState(false)

  // Preview local del archivo elegido, derivado de `foto`. El object URL se revoca al
  // cambiar de archivo o al desmontar (cleanup del effect), para no fugar memoria.
  const previewUrl = useMemo(() => (foto ? URL.createObjectURL(foto) : null), [foto])
  useEffect(() => () => { if (previewUrl) URL.revokeObjectURL(previewUrl) }, [previewUrl])

  const validar = () => {
    const e = {}
    if (!descripcion.trim()) e.descripcion = 'La descripción es obligatoria'
    if (!ubicacion) e.ubicacion = 'La ubicación es obligatoria'
    return e
  }

  const onSubmit = async (ev) => {
    ev.preventDefault()
    setErrorServidor(null)
    const e = validar()
    setErrores(e)
    if (Object.keys(e).length > 0) return

    setEnviando(true)
    try {
      // Si hay foto, se sube PRIMERO (endpoint separado) y se obtiene su URL pública.
      let fotoUrl = null
      if (foto) {
        const fd = new FormData()
        fd.append('archivo', foto)
        const subida = await apiFetch('/api/uploads/avistamientos', { method: 'POST', body: fd })
        fotoUrl = subida.url
      }

      // La ubicación va ANIDADA ({latitud, longitud}), no plana.
      await apiFetch('/api/avistamientos', {
        method: 'POST',
        body: {
          alertaId,
          descripcion: descripcion.trim(),
          ubicacion: { latitud: ubicacion.latitud, longitud: ubicacion.longitud },
          fotoUrl,
        },
      })
      setExito(true)
    } catch (err) {
      setErrorServidor(err.message)
    } finally {
      setEnviando(false)
    }
  }

  if (exito) {
    return (
      <div className="mx-auto max-w-md space-y-4 pt-10 text-center">
        <p className="text-lg font-semibold text-green-700">✓ Avistamiento reportado</p>
        <p className="text-slate-600">Gracias por tu colaboración.</p>
        <button
          onClick={() => navigate(`/alerta/${alertaId}`)}
          className="rounded bg-sky-600 px-5 py-2 font-medium text-white hover:bg-sky-700"
        >
          Volver al detalle de la alerta
        </button>
      </div>
    )
  }

  return (
    <div className="mx-auto max-w-lg space-y-6">
      <div>
        <h1 className="text-xl font-bold text-slate-900">Reportar avistamiento</h1>
        <p className="mt-1 text-sm text-slate-500">
          Tu identidad se registra automáticamente desde tu sesión.
        </p>
      </div>

      <form onSubmit={onSubmit} className="space-y-5" noValidate>
        <div>
          <label className="mb-1 block text-sm font-medium" htmlFor="descripcion">
            Descripción <span className="text-red-500">*</span>
          </label>
          <textarea
            id="descripcion"
            rows={3}
            value={descripcion}
            onChange={(e) => setDescripcion(e.target.value)}
            placeholder="Describí dónde y cuándo lo viste, qué ropa llevaba…"
            className="w-full rounded border border-slate-300 px-3 py-2 focus:border-sky-500 focus:outline-none"
          />
          {errores.descripcion && (
            <p className="mt-1 text-sm text-red-600">{errores.descripcion}</p>
          )}
        </div>

        <div>
          <label className="mb-1 block text-sm font-medium">
            Ubicación donde lo viste <span className="text-red-500">*</span>
          </label>
          <LocationPicker value={ubicacion} onChange={setUbicacion} />
          {errores.ubicacion && (
            <p className="mt-1 text-sm text-red-600">{errores.ubicacion}</p>
          )}
        </div>

        <div>
          <label className="mb-1 block text-sm font-medium" htmlFor="foto">Foto</label>
          <input id="foto" type="file" accept="image/jpeg,image/png,image/webp" capture="environment"
            onChange={(e) => setFoto(e.target.files?.[0] ?? null)}
            className="block w-full text-sm text-slate-600 file:mr-3 file:rounded file:border-0 file:bg-sky-50 file:px-4 file:py-2 file:text-sky-700 hover:file:bg-sky-100" />
          <p className="mt-1 text-xs text-slate-400">
            Opcional. Podés tomar una foto en el momento. JPEG, PNG o WebP, hasta 5 MB.
          </p>
          {previewUrl && (
            <img src={previewUrl} alt="Vista previa" className="mt-2 max-h-48 rounded object-cover" />
          )}
        </div>

        {errorServidor && <p className="text-sm text-red-600">{errorServidor}</p>}

        <div className="flex gap-3">
          <button
            type="submit"
            disabled={enviando}
            className="rounded bg-sky-600 px-5 py-2 font-medium text-white hover:bg-sky-700 disabled:opacity-60"
          >
            {enviando ? 'Enviando…' : 'Enviar avistamiento'}
          </button>
          <button
            type="button"
            onClick={() => navigate(`/alerta/${alertaId}`)}
            className="rounded border border-slate-300 px-5 py-2 text-slate-700 hover:bg-slate-50"
          >
            Cancelar
          </button>
        </div>
      </form>
    </div>
  )
}
