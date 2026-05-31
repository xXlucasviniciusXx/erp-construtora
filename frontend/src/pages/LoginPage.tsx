import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
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
    <div className="flex min-h-screen items-center justify-center bg-gray-100 p-4">
      <div className="w-full max-w-sm rounded-xl bg-white p-8 shadow-lg">
        <h1 className="mb-1 text-center text-2xl font-bold" style={{ color: 'var(--color-primary)' }}>
          {settings?.systemName ?? 'Construtora Financeiro'}
        </h1>
        <p className="mb-6 text-center text-sm text-gray-500">Controle financeiro e conciliação bancária</p>

        <form onSubmit={handleSubmit} className="space-y-4">
          <Field label="E-mail">
            <Input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
          </Field>
          <Field label="Senha">
            <Input type="password" value={password} onChange={(e) => setPassword(e.target.value)} required />
          </Field>
          {error && <p className="text-sm text-red-600">{error}</p>}
          <Button type="submit" className="w-full" disabled={loading}>
            {loading ? 'Entrando…' : 'Entrar'}
          </Button>
        </form>

        <p className="mt-4 text-center text-xs text-gray-400">
          POC — use as credenciais de admin configuradas no backend.
        </p>
      </div>
    </div>
  )
}
