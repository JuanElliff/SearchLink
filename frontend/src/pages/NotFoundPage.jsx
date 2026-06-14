import { Link } from 'react-router-dom'

export default function NotFoundPage() {
  return (
    <div className="flex min-h-screen flex-col items-center justify-center gap-3 bg-slate-50 px-4 text-center">
      <h1 className="text-3xl font-bold text-slate-900">404</h1>
      <p className="text-slate-600">La página que buscás no existe.</p>
      <Link to="/" className="font-medium text-sky-600 hover:underline">
        Volver al inicio
      </Link>
    </div>
  )
}
