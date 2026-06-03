import { useEffect, useState } from 'react'
import { useMutation, useQuery } from '@tanstack/react-query'
import { api, apiErrorMessage } from '@/lib/api'
import type { SystemSettings } from '@/lib/types'
import { useSettings } from '@/theme/SettingsContext'
import { Button, Card, Field, Input, PageHeader, Select } from '@/components/ui'
import { cn } from '@/lib/utils'
import { BankAccountsTab } from './settings/BankAccountsTab'
import { CostCentersTab } from './settings/CostCentersTab'
import { CategoriesTab } from './settings/CategoriesTab'
import { EmailTab } from './settings/EmailTab'

const TABS = [
  { key: 'general', label: 'Geral' },
  { key: 'email', label: 'Notificações / E-mail' },
  { key: 'banks', label: 'Contas Bancárias' },
  { key: 'categories', label: 'Categorias' },
  { key: 'cost-centers', label: 'Centros de Custo' },
] as const

type TabKey = (typeof TABS)[number]['key']

export function SettingsPage() {
  const [tab, setTab] = useState<TabKey>('general')

  return (
    <div>
      <PageHeader title="Configurações do sistema" />

      <div className="mb-4 flex gap-1 border-b border-gray-200 dark:border-gray-700">
        {TABS.map((t) => (
          <button
            key={t.key}
            onClick={() => setTab(t.key)}
            className={cn(
              'border-b-2 px-4 py-2 text-sm font-medium',
              tab === t.key
                ? 'border-primary text-primary'
                : 'border-transparent text-gray-500 hover:text-gray-700 dark:hover:text-gray-300',
            )}
          >
            {t.label}
          </button>
        ))}
      </div>

      {tab === 'general' && <GeneralTab />}
      {tab === 'email' && <EmailTab />}
      {tab === 'banks' && <BankAccountsTab />}
      {tab === 'categories' && <CategoriesTab />}
      {tab === 'cost-centers' && <CostCentersTab />}
    </div>
  )
}

function GeneralTab() {
  const { refresh } = useSettings()
  const [form, setForm] = useState<SystemSettings | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [saved, setSaved] = useState(false)

  const { data } = useQuery({
    queryKey: ['settings'],
    queryFn: async () => (await api.get<SystemSettings>('/settings')).data,
  })

  useEffect(() => { if (data) setForm(data) }, [data])

  const save = useMutation({
    mutationFn: async (p: SystemSettings) => api.put('/settings', p),
    onSuccess: async () => { await refresh(); setSaved(true); setTimeout(() => setSaved(false), 2000) },
    onError: (e) => setError(apiErrorMessage(e)),
  })

  if (!form) return <p className="text-gray-500">Carregando…</p>

  return (
    <Card className="max-w-2xl space-y-4">
      <form className="space-y-4" onSubmit={(e) => { e.preventDefault(); save.mutate(form) }}>
        <Field label="Nome do sistema">
          <Input value={form.systemName} onChange={(e) => setForm({ ...form, systemName: e.target.value })} required />
        </Field>
        <Field label="URL do logo">
          <Input value={form.logoUrl ?? ''} onChange={(e) => setForm({ ...form, logoUrl: e.target.value })} placeholder="https://…" />
        </Field>
        <div className="grid grid-cols-3 gap-3">
          <Field label="Cor primária">
            <Input type="color" value={form.primaryColor ?? '#1e40af'} onChange={(e) => setForm({ ...form, primaryColor: e.target.value })} />
          </Field>
          <Field label="Cor secundária">
            <Input type="color" value={form.secondaryColor ?? '#0f766e'} onChange={(e) => setForm({ ...form, secondaryColor: e.target.value })} />
          </Field>
          <Field label="Tema padrão">
            <Select value={form.theme ?? 'light'} onChange={(e) => setForm({ ...form, theme: e.target.value })}>
              <option value="light">Claro</option>
              <option value="dark">Escuro</option>
            </Select>
          </Field>
        </div>

        <h3 className="border-t pt-3 text-sm font-semibold text-gray-700 dark:border-gray-700 dark:text-gray-200">Dados da empresa</h3>
        <div className="grid grid-cols-2 gap-3">
          <Field label="Razão social"><Input value={form.companyName ?? ''} onChange={(e) => setForm({ ...form, companyName: e.target.value })} /></Field>
          <Field label="CNPJ"><Input value={form.companyDocument ?? ''} onChange={(e) => setForm({ ...form, companyDocument: e.target.value })} /></Field>
        </div>
        <Field label="Endereço"><Input value={form.companyAddress ?? ''} onChange={(e) => setForm({ ...form, companyAddress: e.target.value })} /></Field>
        <div className="grid grid-cols-2 gap-3">
          <Field label="Telefone"><Input value={form.companyPhone ?? ''} onChange={(e) => setForm({ ...form, companyPhone: e.target.value })} /></Field>
          <Field label="E-mail"><Input value={form.companyEmail ?? ''} onChange={(e) => setForm({ ...form, companyEmail: e.target.value })} /></Field>
        </div>
        <Field label="Rodapé"><Input value={form.footerText ?? ''} onChange={(e) => setForm({ ...form, footerText: e.target.value })} /></Field>

        {error && <p className="text-sm text-red-600">{error}</p>}
        <div className="flex items-center gap-3">
          <Button type="submit" disabled={save.isPending}>Salvar configurações</Button>
          {saved && <span className="text-sm text-green-600">Salvo! As mudanças já estão aplicadas.</span>}
        </div>
      </form>
    </Card>
  )
}
