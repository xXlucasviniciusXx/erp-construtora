import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import { api } from '@/lib/api'
import { useAuth } from '@/auth/AuthContext'
import type { LicensingInfo, License, Module } from '@/lib/types'

interface LicensingContextValue {
  modules: Module[]
  license: License | null
  loading: boolean
  /** True se o módulo está ativo. Códigos desconhecidos (ex.: itens "core"
   *  como Usuários/Configurações) são sempre liberados. Durante o carregamento
   *  também libera, para não esconder o menu indevidamente. */
  canAccess: (code?: string) => boolean
  refresh: () => Promise<void>
}

const LicensingContext = createContext<LicensingContextValue | undefined>(undefined)

export function LicensingProvider({ children }: { children: ReactNode }) {
  const { user } = useAuth()
  const [modules, setModules] = useState<Module[]>([])
  const [license, setLicense] = useState<License | null>(null)
  const [loaded, setLoaded] = useState(false)

  const refresh = useCallback(async () => {
    try {
      const { data } = await api.get<LicensingInfo>('/licensing/me')
      setModules(data.modules ?? [])
      setLicense(data.license ?? null)
    } catch {
      // Falha (offline/sem permissão): não gateia nada para não travar a UI.
      setModules([])
      setLicense(null)
    } finally {
      setLoaded(true)
    }
  }, [])

  useEffect(() => {
    if (user) {
      refresh()
    } else {
      setModules([])
      setLicense(null)
      setLoaded(false)
    }
  }, [user, refresh])

  const activeByCode = useMemo(() => {
    const map = new Map<string, boolean>()
    modules.forEach((m) => map.set(m.code, m.active))
    return map
  }, [modules])

  const canAccess = useCallback(
    (code?: string) => {
      if (!code) return true
      if (!loaded) return true // evita "piscar" itens sumindo durante o load
      if (!activeByCode.has(code)) return true // código não gerenciado (core)
      return activeByCode.get(code) === true
    },
    [activeByCode, loaded],
  )

  const value = useMemo(
    () => ({ modules, license, loading: !loaded, canAccess, refresh }),
    [modules, license, loaded, canAccess, refresh],
  )

  return <LicensingContext.Provider value={value}>{children}</LicensingContext.Provider>
}

export function useLicensing() {
  const ctx = useContext(LicensingContext)
  if (!ctx) throw new Error('useLicensing deve ser usado dentro de LicensingProvider')
  return ctx
}
