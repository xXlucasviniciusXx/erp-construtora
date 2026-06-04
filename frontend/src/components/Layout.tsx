import { Suspense, useState } from 'react'
import { NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom'
import {
  LayoutDashboard, Users, Building2, FileSignature, ArrowDownCircle,
  ArrowUpCircle, Banknote, Upload, BarChart3, Settings, ShieldCheck, LogOut, Menu,
  ChevronLeft, ChevronRight, Sun, Moon, Truck, Scale, Mail, AlertTriangle,
} from 'lucide-react'
import { useAuth } from '@/auth/AuthContext'
import { useLicensing } from '@/licensing/LicensingContext'
import { useSettings } from '@/theme/SettingsContext'
import { PageFallback } from '@/components/ui'
import { cn } from '@/lib/utils'
import type { ModuleCode } from '@/lib/types'

interface NavItem {
  to: string
  label: string
  icon: typeof LayoutDashboard
  permission?: string
  module?: ModuleCode
}

const NAV: NavItem[] = [
  { to: '/', label: 'Dashboard', icon: LayoutDashboard, permission: 'DASHBOARD_VIEW', module: 'DASHBOARD' },
  { to: '/clients', label: 'Clientes', icon: Users, permission: 'CLIENTES_VIEW', module: 'CLIENTES' },
  { to: '/properties', label: 'Imóveis / Lotes', icon: Building2, permission: 'EMPREENDIMENTOS_VIEW', module: 'EMPREENDIMENTOS' },
  { to: '/sales', label: 'Vendas', icon: FileSignature, permission: 'VENDAS_VIEW', module: 'VENDAS' },
  { to: '/payable', label: 'Contas a Pagar', icon: ArrowUpCircle, permission: 'CONTAS_PAGAR_VIEW', module: 'CONTAS_PAGAR' },
  { to: '/receivable', label: 'Contas a Receber', icon: ArrowDownCircle, permission: 'CONTAS_RECEBER_VIEW', module: 'CONTAS_RECEBER' },
  { to: '/suppliers', label: 'Fornecedores', icon: Truck, permission: 'FORNECEDORES_VIEW', module: 'FORNECEDORES' },
  { to: '/reconciliation', label: 'Conciliação', icon: Banknote, permission: 'CONCILIACAO_VIEW', module: 'CONCILIACAO' },
  { to: '/import', label: 'Importar Extrato', icon: Upload, permission: 'CONCILIACAO_EDIT', module: 'CONCILIACAO' },
  { to: '/dre', label: 'DRE', icon: Scale, permission: 'DRE_VIEW', module: 'DRE' },
  { to: '/reports', label: 'Relatórios', icon: BarChart3, permission: 'RELATORIOS_VIEW', module: 'RELATORIOS' },
  { to: '/notifications', label: 'Notificações', icon: Mail, permission: 'NOTIFICACOES_VIEW', module: 'NOTIFICACOES' },
  // Itens "core" (sem module): nunca são escondidos por licença.
  { to: '/users', label: 'Usuários', icon: ShieldCheck, permission: 'USERS_MANAGE' },
  { to: '/settings', label: 'Configurações', icon: Settings, permission: 'SETTINGS_MANAGE' },
]

const COLLAPSE_KEY = 'cf_sidebar_collapsed'

export function Layout() {
  const { user, logout, hasPermission } = useAuth()
  const { canAccess, license } = useLicensing()
  const { settings, theme, toggleTheme } = useSettings()
  const navigate = useNavigate()
  const location = useLocation()
  const [mobileOpen, setMobileOpen] = useState(false)
  const [collapsed, setCollapsed] = useState(() => localStorage.getItem(COLLAPSE_KEY) === '1')

  const items = NAV.filter((i) => (!i.permission || hasPermission(i.permission)) && canAccess(i.module))
  const current = NAV.find((i) => (i.to === '/' ? location.pathname === '/' : location.pathname.startsWith(i.to)))

  function handleLogout() {
    logout()
    navigate('/login')
  }

  function toggleCollapsed() {
    setCollapsed((c) => {
      localStorage.setItem(COLLAPSE_KEY, c ? '0' : '1')
      return !c
    })
  }

  const ThemeButton = ({ label }: { label?: boolean }) => (
    <button
      onClick={toggleTheme}
      title={theme === 'dark' ? 'Tema claro' : 'Tema escuro'}
      className="flex items-center gap-2 rounded-md px-2 py-1 text-slate-300 transition hover:bg-white/10 hover:text-white"
    >
      {theme === 'dark' ? <Sun className="h-4 w-4" /> : <Moon className="h-4 w-4" />}
      {label && <span className="text-xs">{theme === 'dark' ? 'Claro' : 'Escuro'}</span>}
    </button>
  )

  return (
    <div className="flex h-screen overflow-hidden">
      <aside
        className={cn(
          'flex flex-shrink-0 flex-col bg-slate-900 text-slate-100 transition-all duration-200',
          'fixed inset-y-0 z-40 md:static',
          collapsed ? 'w-16' : 'w-64',
          mobileOpen ? 'left-0' : '-left-64 md:left-0',
        )}
      >
        <div className={cn('flex items-center gap-2.5 px-4 py-4 text-lg font-bold', collapsed && 'justify-center px-2')}>
          {settings?.logoUrl ? (
            <img src={settings.logoUrl} alt="logo" className="h-8 w-8 flex-shrink-0 rounded" />
          ) : (
            <div
              className="flex h-8 w-8 flex-shrink-0 items-center justify-center rounded-lg"
              style={{ backgroundColor: 'var(--color-primary)' }}
            >
              <Building2 className="h-5 w-5 text-white" />
            </div>
          )}
          {!collapsed && <span className="truncate">{settings?.systemName ?? 'Construtora'}</span>}
        </div>

        <nav className="flex-1 space-y-0.5 overflow-y-auto px-2 py-2">
          {items.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.to === '/'}
              onClick={() => setMobileOpen(false)}
              title={collapsed ? item.label : undefined}
              className={({ isActive }) =>
                cn(
                  'group relative flex items-center gap-3 rounded-lg px-3 py-2 text-sm transition',
                  collapsed && 'justify-center px-2',
                  isActive ? 'font-medium text-white' : 'text-slate-300 hover:bg-white/5 hover:text-white',
                )
              }
              style={({ isActive }: { isActive: boolean }) =>
                isActive ? { backgroundColor: 'var(--color-primary)' } : undefined
              }
            >
              <item.icon className="h-[18px] w-[18px] flex-shrink-0" />
              {!collapsed && item.label}
            </NavLink>
          ))}
        </nav>

        {/* Botão de retrair/expandir (desktop) */}
        <button
          onClick={toggleCollapsed}
          title={collapsed ? 'Expandir menu' : 'Retrair menu'}
          className="hidden items-center justify-center border-t border-white/10 py-2 text-slate-400 transition hover:bg-white/5 hover:text-white md:flex"
        >
          {collapsed ? <ChevronRight className="h-4 w-4" /> : <ChevronLeft className="h-4 w-4" />}
        </button>

        <div className={cn('border-t border-white/10 px-3 py-3 text-xs', collapsed && 'flex flex-col items-center gap-2')}>
          {!collapsed && (
            <>
              <div className="font-medium">{user?.name}</div>
              <div className="text-slate-400">{user?.role}</div>
              <div className="mt-2 flex items-center justify-between">
                <ThemeButton label />
                <button onClick={handleLogout} className="flex items-center gap-1 text-slate-300 transition hover:text-white">
                  <LogOut className="h-3 w-3" /> Sair
                </button>
              </div>
            </>
          )}
          {collapsed && (
            <>
              <ThemeButton />
              <button onClick={handleLogout} title="Sair" className="text-slate-300 transition hover:text-white">
                <LogOut className="h-4 w-4" />
              </button>
            </>
          )}
        </div>
      </aside>

      {/* Overlay para fechar o drawer no mobile */}
      {mobileOpen && <div className="fixed inset-0 z-30 bg-black/40 md:hidden" onClick={() => setMobileOpen(false)} />}

      <div className="flex flex-1 flex-col overflow-hidden">
        {/* Header mobile */}
        <header className="flex items-center justify-between border-b border-gray-200 bg-white px-4 py-3 dark:border-gray-700 dark:bg-gray-800 md:hidden">
          <div className="flex items-center gap-3">
            <button onClick={() => setMobileOpen((v) => !v)} aria-label="Menu">
              <Menu className="h-5 w-5" />
            </button>
            <span className="font-semibold">{current?.label ?? settings?.systemName ?? 'Construtora'}</span>
          </div>
          <button onClick={toggleTheme} aria-label="Alternar tema" className="text-gray-600 dark:text-gray-300">
            {theme === 'dark' ? <Sun className="h-5 w-5" /> : <Moon className="h-5 w-5" />}
          </button>
        </header>

        {/* Header desktop */}
        <header className="hidden items-center justify-between border-b border-gray-200 bg-white/80 px-6 py-3 backdrop-blur dark:border-gray-700 dark:bg-gray-800/80 md:flex">
          <div className="flex items-center gap-2 text-sm">
            {current?.icon && <current.icon className="h-4 w-4 text-gray-400" />}
            <span className="font-medium text-gray-700 dark:text-gray-200">{current?.label ?? 'Painel'}</span>
          </div>
          <div className="flex items-center gap-3">
            <span className="text-xs text-gray-400">{user?.name} · {user?.role}</span>
          </div>
        </header>

        <main className="flex-1 overflow-y-auto p-4 md:p-6">
          <div className="mx-auto max-w-7xl">
            {license && (license.expired || (license.daysToExpire != null && license.daysToExpire <= 30)) && (
              <div
                className={cn(
                  'mb-4 flex items-center gap-2 rounded-lg border px-4 py-2.5 text-sm',
                  license.expired
                    ? 'border-red-200 bg-red-50 text-red-700 dark:border-red-900/50 dark:bg-red-900/20 dark:text-red-300'
                    : 'border-amber-200 bg-amber-50 text-amber-700 dark:border-amber-900/50 dark:bg-amber-900/20 dark:text-amber-300',
                )}
              >
                <AlertTriangle className="h-4 w-4 flex-shrink-0" />
                <span>
                  {license.expired
                    ? `Sua licença (${license.plan}) está vencida. Entre em contato para renovação.`
                    : `Sua licença (${license.plan}) vence em ${license.daysToExpire} dia(s). Entre em contato para renovação.`}
                </span>
              </div>
            )}
            <Suspense fallback={<PageFallback />}>
              <Outlet />
            </Suspense>
          </div>
        </main>

        <footer className="border-t border-gray-200 bg-white px-4 py-2 text-center text-xs text-gray-400 dark:border-gray-700 dark:bg-gray-800">
          {settings?.footerText ?? '© ERP Construtora — POC'}
        </footer>
      </div>
    </div>
  )
}
