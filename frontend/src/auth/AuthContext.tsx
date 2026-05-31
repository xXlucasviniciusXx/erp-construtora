import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import { api, setToken, getToken } from '@/lib/api'
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
      setUser(JSON.parse(stored))
    }
    setLoading(false)
  }, [])

  async function login(email: string, password: string) {
    const { data } = await api.post<AuthResponse>('/auth/login', { email, password })
    setToken(data.token)
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
    setToken(null)
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
