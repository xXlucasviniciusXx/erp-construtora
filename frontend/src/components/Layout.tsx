import { useState } from 'react'
import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import {
  LayoutDashboard, Users, Building2, FileSignature, Wallet, ArrowDownCircle,
  ArrowUpCircle, Banknote, Upload, FileText, BarChart3, Settings, ShieldCheck, LogOut, Menu,
} from 'lucide-react'
import { useAuth } from '@/auth/AuthContext'
import { useSettings } from '@/theme/SettingsContext'
import { cn } from '@/lib/utils'

interface NavItem {
  to: string
  label: string
  icon: typeof LayoutDashboard
  permission?: string
}

const NAV: NavItem[] = [
  { to: '/', label: 'Dashboard', icon: LayoutDashboard },
  { to: '/clients', label: 'Clientes', icon: Users, permission: 'READ' },
  { to: '/properties', label: 'Imóveis / Lotes', icon: Building2, permission: 'READ' },
  { to: '/sales', label: 'Vendas', icon: FileSignature, permission: 'READ' },
  { to: '/installments', label: 'Parcelas', icon: Wallet, permission: 'READ' },
  { to: '/payable', label: 'Contas a Pagar', icon: ArrowUpCircle, permission: 'READ' },
  { to: '/receivable', label: 'Contas a Receber', icon: ArrowDownCircle, permission: 'READ' },
  { to: '/reconciliation', label: 'Conciliação', icon: Banknote, permission: 'READ' },
  { to: '/import', label: 'Importar Extrato', icon: Upload, permission: 'RECONCILIATION_WRITE' },
  { to: '/reports', label: 'Relatórios', icon: BarChart3, permission: 'READ' },
  { to: '/users', label: 'Usuários', icon: ShieldCheck, permission: 'USERS_MANAGE' },
  { to: '/settings', label: 'Configurações', icon: Settings, permission: 'SETTINGS_MANAGE' },
]

export function Layout() {
  const { user, logout, hasPermission } = useAuth()
  const { settings } = useSettings()
  const navigate = useNavigate()
  const [open, setOpen] = useState(false)

  const items = NAV.filter((i) => !i.permission || hasPermission(i.permission))

  function handleLogout() {
    logout()
    navigate('/login')
  }

  return (
    <div className="flex h-screen overflow-hidden">
      <aside
        className={cn(
          'flex w-64 flex-shrink-0 flex-col bg-gray-900 text-gray-100 transition-all',
          'fixed inset-y-0 z-40 md:static',
          open ? 'left-0' : '-left-64 md:left-0',
        )}
      >
        <div className="flex items-center gap-2 px-4 py-4 text-lg font-bold">
          {settings?.logoUrl ? (
            <img src={settings.logoUrl} alt="logo" className="h-8 w-8 rounded" />
          ) : (
            <Building2 className="h-6 w-6" style={{ color: 'var(--color-secondary)' }} />
          )}
          <span className="truncate">{settings?.systemName ?? 'Construtora'}</span>
        </div>
        <nav className="flex-1 space-y-1 overflow-y-auto px-2 py-2">
          {items.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.to === '/'}
              onClick={() => setOpen(false)}
              className={({ isActive }) =>
                cn(
                  'flex items-center gap-3 rounded-md px-3 py-2 text-sm transition',
                  isActive ? 'bg-white/10 font-medium text-white' : 'text-gray-300 hover:bg-white/5',
                )
              }
            >
              <item.icon className="h-4 w-4" />
              {item.label}
            </NavLink>
          ))}
        </nav>
        <div className="border-t border-white/10 px-4 py-3 text-xs">
          <div className="font-medium">{user?.name}</div>
          <div className="text-gray-400">{user?.role}</div>
          <button onClick={handleLogout} className="mt-2 flex items-center gap-1 text-gray-300 hover:text-white">
            <LogOut className="h-3 w-3" /> Sair
          </button>
        </div>
      </aside>

      <div className="flex flex-1 flex-col overflow-hidden">
        <header className="flex items-center gap-3 border-b border-gray-200 bg-white px-4 py-3 md:hidden">
          <button onClick={() => setOpen((v) => !v)}>
            <Menu className="h-5 w-5" />
          </button>
          <span className="font-semibold">{settings?.systemName ?? 'Construtora'}</span>
        </header>
        <main className="flex-1 overflow-y-auto p-4 md:p-6">
          <Outlet />
        </main>
        <footer className="border-t border-gray-200 bg-white px-4 py-2 text-center text-xs text-gray-400">
          {settings?.footerText ?? '© Construtora Financeiro — POC'}
        </footer>
      </div>
    </div>
  )
}
