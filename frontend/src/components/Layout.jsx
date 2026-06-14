import { Outlet, Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'

export default function Layout() {
  const { usuario, rol, logout } = useAuth()
  const navigate = useNavigate()

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
    </div>
  )
}
