import { createContext, useContext, useEffect, useState, type ReactNode } from 'react'
import { api } from '@/lib/api'
import type { SystemSettings } from '@/lib/types'

interface SettingsContextValue {
  settings: SystemSettings | null
  refresh: () => Promise<void>
}

const SettingsContext = createContext<SettingsContextValue | undefined>(undefined)

const DEFAULTS: SystemSettings = {
  systemName: 'Construtora Financeiro',
  primaryColor: '#1e40af',
  secondaryColor: '#0f766e',
  theme: 'light',
}

/** Carrega as configurações públicas e aplica branding (cores/tema) globalmente. */
export function SettingsProvider({ children }: { children: ReactNode }) {
  const [settings, setSettings] = useState<SystemSettings | null>(null)

  async function refresh() {
    try {
      const { data } = await api.get<SystemSettings>('/settings/public')
      apply(data)
      setSettings(data)
    } catch {
      apply(DEFAULTS)
      setSettings(DEFAULTS)
    }
  }

  function apply(s: SystemSettings) {
    const root = document.documentElement
    root.style.setProperty('--color-primary', s.primaryColor ?? DEFAULTS.primaryColor!)
    root.style.setProperty('--color-secondary', s.secondaryColor ?? DEFAULTS.secondaryColor!)
    root.classList.toggle('dark', s.theme === 'dark')
    document.title = s.systemName ?? DEFAULTS.systemName
  }

  useEffect(() => {
    refresh()
  }, [])

  return (
    <SettingsContext.Provider value={{ settings, refresh }}>{children}</SettingsContext.Provider>
  )
}

export function useSettings() {
  const ctx = useContext(SettingsContext)
  if (!ctx) throw new Error('useSettings deve ser usado dentro de SettingsProvider')
  return ctx
}
