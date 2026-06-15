import { Routes, Route } from 'react-router-dom'
import Layout from './components/Layout'
import ProtectedRoute from './routes/ProtectedRoute'
import LoginPage from './pages/LoginPage'
import RegistroPage from './pages/RegistroPage'
import AdminPage from './pages/AdminPage'
import CrearUsuarioPage from './pages/CrearUsuarioPage'
import OperadorPage from './pages/OperadorPage'
import AlertaCrearPage from './pages/AlertaCrearPage'
import AlertaEditarPage from './pages/AlertaEditarPage'
import ModerarAvistamientosPage from './pages/ModerarAvistamientosPage'
import EstandarHomePage from './pages/EstandarHomePage'
import AlertaDetallePage from './pages/AlertaDetallePage'
import ReportarAvistamientoPage from './pages/ReportarAvistamientoPage'
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
          path="/operador/nueva"
          element={
            <ProtectedRoute roles={['OPERADOR']}>
              <AlertaCrearPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/operador/alerta/:id/editar"
          element={
            <ProtectedRoute roles={['OPERADOR']}>
              <AlertaEditarPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/operador/alerta/:id/avistamientos"
          element={
            <ProtectedRoute roles={['OPERADOR']}>
              <ModerarAvistamientosPage />
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
        <Route
          path="/admin/crear"
          element={
            <ProtectedRoute roles={['ADMIN']}>
              <CrearUsuarioPage />
            </ProtectedRoute>
          }
        />
        {/* Detalle de alerta y reporte: cualquier autenticado */}
        <Route
          path="/alerta/:id"
          element={
            <ProtectedRoute>
              <AlertaDetallePage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/alerta/:id/avistamiento"
          element={
            <ProtectedRoute>
              <ReportarAvistamientoPage />
            </ProtectedRoute>
          }
        />
      </Route>

      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  )
}
