import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { homePathForRol } from '../auth/roles'

export default function LoginPage() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState(null)
  const [enviando, setEnviando] = useState(false)

  const onSubmit = async (e) => {
    e.preventDefault()
    setError(null)
    setEnviando(true)
    try {
      const usuario = await login(email, password)
      navigate(homePathForRol(usuario.rol), { replace: true })
    } catch (err) {
      setError(err.status === 401 ? 'Email o contraseña incorrectos' : err.message)
    } finally {
      setEnviando(false)
    }
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-slate-50 px-4">
      <div className="w-full max-w-sm rounded-lg bg-white p-6 shadow">
        <h1 className="mb-1 text-2xl font-bold text-slate-900">SearchLink</h1>
        <p className="mb-6 text-sm text-slate-500">Ingresá a tu cuenta</p>

        <form onSubmit={onSubmit} className="space-y-4">
          <div>
            <label className="mb-1 block text-sm font-medium" htmlFor="email">Email</label>
            <input
              id="email" type="email" required value={email}
              onChange={(e) => setEmail(e.target.value)}
              className="w-full rounded border border-slate-300 px-3 py-2 focus:border-sky-500 focus:outline-none"
            />
          </div>
          <div>
            <label className="mb-1 block text-sm font-medium" htmlFor="password">Contraseña</label>
            <input
              id="password" type="password" required value={password}
              onChange={(e) => setPassword(e.target.value)}
              className="w-full rounded border border-slate-300 px-3 py-2 focus:border-sky-500 focus:outline-none"
            />
          </div>

          {error && <p className="text-sm text-red-600">{error}</p>}

          <button
            type="submit" disabled={enviando}
            className="w-full rounded bg-sky-600 py-2 font-medium text-white hover:bg-sky-700 disabled:opacity-60"
          >
            {enviando ? 'Ingresando…' : 'Ingresar'}
          </button>
        </form>

        <p className="mt-4 text-center text-sm text-slate-600">
          ¿No tenés cuenta?{' '}
          <Link to="/registro" className="font-medium text-sky-600 hover:underline">
            Registrate
          </Link>
        </p>
      </div>
    </div>
  )
}
