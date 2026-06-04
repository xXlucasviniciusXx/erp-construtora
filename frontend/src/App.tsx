import { lazy, Suspense } from 'react'
import { Routes, Route } from 'react-router-dom'
import { ProtectedRoute } from '@/auth/ProtectedRoute'
import { Layout } from '@/components/Layout'
import { ModuleGuard } from '@/components/ModuleGuard'
import { PageFallback } from '@/components/ui'

// Páginas carregadas sob demanda (code-splitting por rota).
// As páginas usam named exports, então mapeamos para `default` no import dinâmico.
const LoginPage = lazy(() => import('@/pages/LoginPage').then((m) => ({ default: m.LoginPage })))
const DashboardPage = lazy(() => import('@/pages/DashboardPage').then((m) => ({ default: m.DashboardPage })))
const ClientsPage = lazy(() => import('@/pages/ClientsPage').then((m) => ({ default: m.ClientsPage })))
const DevelopmentsPage = lazy(() => import('@/pages/DevelopmentsPage').then((m) => ({ default: m.DevelopmentsPage })))
const SalesPage = lazy(() => import('@/pages/SalesPage').then((m) => ({ default: m.SalesPage })))
const PayablePage = lazy(() => import('@/pages/PayablePage').then((m) => ({ default: m.PayablePage })))
const ReceivablePage = lazy(() => import('@/pages/ReceivablePage').then((m) => ({ default: m.ReceivablePage })))
const SuppliersPage = lazy(() => import('@/pages/SuppliersPage').then((m) => ({ default: m.SuppliersPage })))
const ReconciliationPage = lazy(() => import('@/pages/ReconciliationPage').then((m) => ({ default: m.ReconciliationPage })))
const ImportPage = lazy(() => import('@/pages/ImportPage').then((m) => ({ default: m.ImportPage })))
const ReportsPage = lazy(() => import('@/pages/ReportsPage').then((m) => ({ default: m.ReportsPage })))
const DrePage = lazy(() => import('@/pages/DrePage').then((m) => ({ default: m.DrePage })))
const UsersPage = lazy(() => import('@/pages/UsersPage').then((m) => ({ default: m.UsersPage })))
const SettingsPage = lazy(() => import('@/pages/SettingsPage').then((m) => ({ default: m.SettingsPage })))
const NotificationsPage = lazy(() => import('@/pages/NotificationsPage').then((m) => ({ default: m.NotificationsPage })))

export function App() {
  return (
    <Routes>
      <Route
        path="/login"
        element={
          <Suspense fallback={<PageFallback />}>
            <LoginPage />
          </Suspense>
        }
      />
      <Route element={<ProtectedRoute />}>
        <Route element={<Layout />}>
          <Route index element={<ModuleGuard code="DASHBOARD"><DashboardPage /></ModuleGuard>} />
          <Route path="clients" element={<ModuleGuard code="CLIENTES"><ClientsPage /></ModuleGuard>} />
          <Route path="properties" element={<ModuleGuard code="EMPREENDIMENTOS"><DevelopmentsPage /></ModuleGuard>} />
          <Route path="sales" element={<ModuleGuard code="VENDAS"><SalesPage /></ModuleGuard>} />
          <Route path="payable" element={<ModuleGuard code="CONTAS_PAGAR"><PayablePage /></ModuleGuard>} />
          <Route path="receivable" element={<ModuleGuard code="CONTAS_RECEBER"><ReceivablePage /></ModuleGuard>} />
          <Route path="suppliers" element={<ModuleGuard code="FORNECEDORES"><SuppliersPage /></ModuleGuard>} />
          <Route path="reconciliation" element={<ModuleGuard code="CONCILIACAO"><ReconciliationPage /></ModuleGuard>} />
          <Route path="import" element={<ModuleGuard code="CONCILIACAO"><ImportPage /></ModuleGuard>} />
          <Route path="reports" element={<ModuleGuard code="RELATORIOS"><ReportsPage /></ModuleGuard>} />
          <Route path="dre" element={<ModuleGuard code="DRE"><DrePage /></ModuleGuard>} />
        </Route>
      </Route>
      <Route element={<ProtectedRoute permission="USERS_MANAGE" />}>
        <Route element={<Layout />}>
          <Route path="users" element={<UsersPage />} />
        </Route>
      </Route>
      <Route element={<ProtectedRoute permission="SETTINGS_MANAGE" />}>
        <Route element={<Layout />}>
          <Route path="settings" element={<SettingsPage />} />
          <Route path="notifications" element={<ModuleGuard code="NOTIFICACOES"><NotificationsPage /></ModuleGuard>} />
        </Route>
      </Route>
    </Routes>
  )
}
