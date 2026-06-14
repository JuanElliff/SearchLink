import { Routes, Route } from 'react-router-dom'
import Layout from './components/Layout'
import ProtectedRoute from './routes/ProtectedRoute'
import LoginPage from './pages/LoginPage'
import RegistroPage from './pages/RegistroPage'
import AdminPage from './pages/AdminPage'
import OperadorPage from './pages/OperadorPage'
import EstandarHomePage from './pages/EstandarHomePage'
import NotFoundPage from './pages/NotFoundPage'

export default function App() {
  return (
    <Routes>
      {/* Públicas */}
      <Route path="/login" element={<LoginPage />} />
      <Route path="/registro" element={<RegistroPage />} />

      {/* Protegidas (dentro del shell con nav + logout) */}
      <Route element={<Layout />}>
        <Route
          path="/"
          element={
            <ProtectedRoute roles={['ESTANDAR']}>
              <EstandarHomePage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/operador"
          element={
            <ProtectedRoute roles={['OPERADOR']}>
              <OperadorPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/admin"
          element={
            <ProtectedRoute roles={['ADMIN']}>
              <AdminPage />
            </ProtectedRoute>
          }
        />
      </Route>

      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  )
}
