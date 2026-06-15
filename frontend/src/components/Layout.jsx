import { useState, useEffect } from 'react'
import { Outlet, Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { fcmConfigurado, fcmRegistrado, suscribirFCM, escucharMensajesForeground } from '../lib/fcm'

function estadoNotifInicial() {
  if (!fcmConfigurado()) return 'no-configurado'
  if (!('Notification' in window)) return 'no-soportado'
  if (Notification.permission === 'denied') return 'denegado'
  if (Notification.permission === 'granted' && fcmRegistrado()) return 'activo'
  return 'default'
}

export default function Layout() {
  const { usuario, rol, logout } = useAuth()
  const navigate = useNavigate()
  const [estadoNotif, setEstadoNotif] = useState(estadoNotifInicial)
  const [cargando, setCargando] = useState(false)
  const [toast, setToast] = useState(null)

  useEffect(() => {
    const unsub = escucharMensajesForeground(payload => {
      const title = payload.notification?.title ?? 'SearchLink'
      const body = payload.notification?.body ?? ''
      setToast({ title, body })
      setTimeout(() => setToast(null), 5000)
    })
    return unsub
  }, [])

  const onActivar = async () => {
    setCargando(true)
    try {
      const resultado = await suscribirFCM()
      if (resultado.estado === 'activo') setEstadoNotif('activo')
      else if (resultado.estado === 'denegado') setEstadoNotif('denegado')
      // sin-token / no-configurado / error de red → mantener 'default' para reintentar
    } catch {
      // error de red o del servidor al registrar el token → no bloquear el botón
    } finally {
      setCargando(false)
    }
  }

  const onLogout = () => {
    logout()
    navigate('/login', { replace: true })
  }

  return (
    <div className="min-h-screen bg-slate-50 text-slate-900">
      <header className="bg-slate-900 text-white">
        <div className="mx-auto flex max-w-5xl items-center justify-between px-4 py-3">
          <Link to="/" className="text-lg font-bold">
            SearchLink
          </Link>
          <div className="flex items-center gap-3 text-sm">
            {estadoNotif === 'default' && (
              <button
                onClick={onActivar}
                disabled={cargando}
                className="rounded bg-blue-600 px-3 py-1 font-medium text-white hover:bg-blue-500 disabled:opacity-50"
              >
                {cargando ? 'Activando…' : 'Activar notificaciones'}
              </button>
            )}
            {estadoNotif === 'activo' && (
              <span className="text-xs text-green-400">&#10003; Notificaciones activas</span>
            )}
            {estadoNotif === 'denegado' && (
              <span
                className="cursor-help text-xs text-yellow-300"
                title="Para reactivarlas: hacé clic en el ícono de candado o info en la barra del navegador → Permisos del sitio → Notificaciones → Permitir, y luego recargá la página."
              >
                Notif. bloqueadas
              </span>
            )}
            <span className="hidden sm:inline">{usuario?.nombre}</span>
            <span className="rounded-full bg-slate-700 px-2 py-0.5 text-xs font-medium">
              {rol}
            </span>
            <button
              onClick={onLogout}
              className="rounded bg-slate-100 px-3 py-1 font-medium text-slate-900 hover:bg-white"
            >
              Salir
            </button>
          </div>
        </div>
      </header>
      <main className="mx-auto max-w-5xl px-4 py-6">
        <Outlet />
      </main>
      {toast && (
        <div className="fixed right-4 top-4 z-50 max-w-xs rounded-lg bg-slate-900 p-4 text-white shadow-lg">
          <p className="font-semibold">{toast.title}</p>
          {toast.body && <p className="mt-1 text-sm text-slate-300">{toast.body}</p>}
        </div>
      )}
    </div>
  )
}
