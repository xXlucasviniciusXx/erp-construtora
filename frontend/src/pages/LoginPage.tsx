import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Building2 } from 'lucide-react'
import { useAuth } from '@/auth/AuthContext'
import { useSettings } from '@/theme/SettingsContext'
import { Button, Input, Field } from '@/components/ui'
import { apiErrorMessage } from '@/lib/api'

export function LoginPage() {
  const { login } = useAuth()
  const { settings } = useSettings()
  const navigate = useNavigate()
  const [email, setEmail] = useState('admin@construtora.com.br')
  const [password, setPassword] = useState('Admin@123')
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setError(null)
    setLoading(true)
    try {
      await login(email, password)
      navigate('/')
    } catch (err) {
      setError(apiErrorMessage(err))
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-gray-100 p-4 dark:bg-gray-950">
      <div className="w-full max-w-sm rounded-2xl border border-gray-100 bg-white p-8 shadow-xl dark:border-gray-700 dark:bg-gray-800">
        <div className="mb-6 flex flex-col items-center">
          {settings?.logoUrl ? (
            <img src={settings.logoUrl} alt="logo" className="mb-3 h-12 w-12 rounded-lg" />
          ) : (
            <div
              className="mb-3 flex h-12 w-12 items-center justify-center rounded-xl"
              style={{ backgroundColor: 'var(--color-primary)' }}
            >
              <Building2 className="h-6 w-6 text-white" />
            </div>
          )}
          <h1 className="text-center text-2xl font-bold tracking-tight text-gray-900 dark:text-gray-50">
            {settings?.systemName ?? 'ERP Construtora'}
          </h1>
          <p className="mt-1 text-center text-sm text-gray-500 dark:text-gray-400">
            Controle financeiro e conciliação bancária
          </p>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4">
          <Field label="E-mail">
            <Input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
          </Field>
          <Field label="Senha">
            <Input type="password" value={password} onChange={(e) => setPassword(e.target.value)} required />
          </Field>
          {error && (
            <p className="rounded-md bg-red-50 px-3 py-2 text-sm text-red-600 dark:bg-red-900/30 dark:text-red-300">
              {error}
            </p>
          )}
          <Button type="submit" className="w-full" loading={loading}>
            {loading ? 'Entrando…' : 'Entrar'}
          </Button>
        </form>

        <p className="mt-6 text-center text-xs text-gray-400 dark:text-gray-500">
          POC — use as credenciais de admin configuradas no backend.
        </p>
      </div>
    </div>
  )
}
