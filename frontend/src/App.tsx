import { Routes, Route } from 'react-router-dom'
import { ProtectedRoute } from '@/auth/ProtectedRoute'
import { Layout } from '@/components/Layout'
import { LoginPage } from '@/pages/LoginPage'
import { DashboardPage } from '@/pages/DashboardPage'
import { ClientsPage } from '@/pages/ClientsPage'
import { PropertiesPage } from '@/pages/PropertiesPage'
import { SalesPage } from '@/pages/SalesPage'
import { PayablePage } from '@/pages/PayablePage'
import { ReceivablePage } from '@/pages/ReceivablePage'
import { SuppliersPage } from '@/pages/SuppliersPage'
import { ReconciliationPage } from '@/pages/ReconciliationPage'
import { ImportPage } from '@/pages/ImportPage'
import { ReportsPage } from '@/pages/ReportsPage'
import { UsersPage } from '@/pages/UsersPage'
import { SettingsPage } from '@/pages/SettingsPage'

export function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route element={<ProtectedRoute />}>
        <Route element={<Layout />}>
          <Route index element={<DashboardPage />} />
          <Route path="clients" element={<ClientsPage />} />
          <Route path="properties" element={<PropertiesPage />} />
          <Route path="sales" element={<SalesPage />} />
          <Route path="payable" element={<PayablePage />} />
          <Route path="receivable" element={<ReceivablePage />} />
          <Route path="suppliers" element={<SuppliersPage />} />
          <Route path="reconciliation" element={<ReconciliationPage />} />
          <Route path="import" element={<ImportPage />} />
          <Route path="reports" element={<ReportsPage />} />
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
        </Route>
      </Route>
    </Routes>
  )
}
