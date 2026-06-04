import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import { api, setToken, getToken, setRefreshToken, getRefreshToken } from '@/lib/api'
import type { AuthResponse } from '@/lib/types'

interface AuthState {
  userId: string
  name: string
  email: string
  role: string
  permissions: string[]
}

interface AuthContextValue {
  user: AuthState | null
  loading: boolean
  login: (email: string, password: string) => Promise<void>
  logout: () => void
  hasPermission: (perm: string) => boolean
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined)
const USER_KEY = 'cf_user'

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthState | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const token = getToken()
    const stored = localStorage.getItem(USER_KEY)
    if (token && stored) {
      try {
        setUser(JSON.parse(stored))
      } catch {
        // dados corrompidos — força re-login
        setToken(null)
        setRefreshToken(null)
      }
    }
    setLoading(false)
  }, [])

  async function login(email: string, password: string) {
    const { data } = await api.post<AuthResponse>('/auth/login', { email, password })
    setToken(data.token)
    if (data.refreshToken) setRefreshToken(data.refreshToken)
    const state: AuthState = {
      userId: data.userId,
      name: data.name,
      email: data.email,
      role: data.role,
      permissions: data.permissions,
    }
    localStorage.setItem(USER_KEY, JSON.stringify(state))
    setUser(state)
  }

  function logout() {
    // Revoga o refresh token no servidor (best-effort: não bloqueia o logout local)
    const rt = getRefreshToken()
    if (rt) {
      api.post('/auth/logout').catch(() => {/* ignora erros de rede no logout */})
    }
    setToken(null)
    setRefreshToken(null)
    localStorage.removeItem(USER_KEY)
    setUser(null)
  }

  function hasPermission(perm: string) {
    if (!user) return false
    if (user.role === 'ADMIN') return true
    return user.permissions.includes(perm)
  }

  const value = useMemo(
    () => ({ user, loading, login, logout, hasPermission }),
    [user, loading],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth deve ser usado dentro de AuthProvider')
  return ctx
}
