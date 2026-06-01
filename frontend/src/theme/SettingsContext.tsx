import { createContext, useContext, useEffect, useState, type ReactNode } from 'react'
import { api } from '@/lib/api'
import type { SystemSettings } from '@/lib/types'

type Theme = 'light' | 'dark'

interface SettingsContextValue {
  settings: SystemSettings | null
  theme: Theme
  toggleTheme: () => void
  refresh: () => Promise<void>
}

const SettingsContext = createContext<SettingsContextValue | undefined>(undefined)
const THEME_KEY = 'cf_theme'

const DEFAULTS: SystemSettings = {
  systemName: 'Construtora Financeiro',
  primaryColor: '#1e40af',
  secondaryColor: '#0f766e',
  theme: 'light',
}

/** Carrega as configurações públicas, aplica branding (cores) e controla o tema claro/escuro. */
export function SettingsProvider({ children }: { children: ReactNode }) {
  const [settings, setSettings] = useState<SystemSettings | null>(null)
  // Preferência do usuário (localStorage) tem prioridade sobre o default da empresa.
  const [theme, setTheme] = useState<Theme>(() => (localStorage.getItem(THEME_KEY) as Theme) || 'light')

  async function refresh() {
    try {
      const { data } = await api.get<SystemSettings>('/settings/public')
      applyColors(data)
      setSettings(data)
      // Se o usuário ainda não escolheu um tema, segue o padrão da empresa.
      if (!localStorage.getItem(THEME_KEY) && (data.theme === 'light' || data.theme === 'dark')) {
        setTheme(data.theme)
      }
    } catch {
      applyColors(DEFAULTS)
      setSettings(DEFAULTS)
    }
  }

  function applyColors(s: SystemSettings) {
    const root = document.documentElement
    root.style.setProperty('--color-primary', s.primaryColor ?? DEFAULTS.primaryColor!)
    root.style.setProperty('--color-secondary', s.secondaryColor ?? DEFAULTS.secondaryColor!)
    document.title = s.systemName ?? DEFAULTS.systemName
  }

  function toggleTheme() {
    setTheme((t) => (t === 'dark' ? 'light' : 'dark'))
  }

  // Aplica a classe `dark` no <html> sempre que o tema mudar (fixo: light/dark).
  useEffect(() => {
    document.documentElement.classList.toggle('dark', theme === 'dark')
    localStorage.setItem(THEME_KEY, theme)
  }, [theme])

  useEffect(() => {
    refresh()
  }, [])

  return (
    <SettingsContext.Provider value={{ settings, theme, toggleTheme, refresh }}>
      {children}
    </SettingsContext.Provider>
  )
}

export function useSettings() {
  const ctx = useContext(SettingsContext)
  if (!ctx) throw new Error('useSettings deve ser usado dentro de SettingsProvider')
  return ctx
}
