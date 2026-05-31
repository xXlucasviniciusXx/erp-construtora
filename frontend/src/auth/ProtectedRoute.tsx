import { Navigate, Outlet } from 'react-router-dom'
import { useAuth } from './AuthContext'

/** Bloqueia rotas para usuários não autenticados; opcionalmente exige permissão. */
export function ProtectedRoute({ permission }: { permission?: string }) {
  const { user, loading, hasPermission } = useAuth()

  if (loading) {
    return <div className="p-8 text-gray-500">Carregando…</div>
  }
  if (!user) {
    return <Navigate to="/login" replace />
  }
  if (permission && !hasPermission(permission)) {
    return (
      <div className="p-8">
        <h2 className="text-lg font-semibold text-red-600">Acesso negado</h2>
        <p className="text-gray-600">Você não tem permissão para acessar esta página.</p>
      </div>
    )
  }
  return <Outlet />
}
