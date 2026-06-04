import { useEffect, useRef, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api, apiErrorMessage } from '@/lib/api'
import type { SystemSettings } from '@/lib/types'
import { useSettings } from '@/theme/SettingsContext'
import { Button, Card, Field, Input, PageHeader, Select } from '@/components/ui'
import { cn } from '@/lib/utils'
import { BankAccountsTab } from './settings/BankAccountsTab'
import { CostCentersTab } from './settings/CostCentersTab'
import { CategoriesTab } from './settings/CategoriesTab'
import { EmailTab } from './settings/EmailTab'
import { ModulesTab } from './settings/ModulesTab'
import { AccessProfilesTab } from './settings/AccessProfilesTab'
import { ListsTab } from './settings/ListsTab'

const TABS = [
  { key: 'general', label: 'Geral' },
  { key: 'modules', label: 'Módulos & Licença' },
  { key: 'profiles', label: 'Perfis de Acesso' },
  { key: 'email', label: 'Notificações / E-mail' },
  { key: 'banks', label: 'Contas Bancárias' },
  { key: 'categories', label: 'Categorias' },
  { key: 'cost-centers', label: 'Centros de Custo' },
  { key: 'lists', label: 'Listas' },
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
      {tab === 'modules' && <ModulesTab />}
      {tab === 'profiles' && <AccessProfilesTab />}
      {tab === 'email' && <EmailTab />}
      {tab === 'banks' && <BankAccountsTab />}
      {tab === 'categories' && <CategoriesTab />}
      {tab === 'cost-centers' && <CostCentersTab />}
      {tab === 'lists' && <ListsTab />}
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
        <LogoField form={form} setForm={setForm} />

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

/** Campo de logo: URL manual + upload de arquivo armazenado no banco. */
function LogoField({ form, setForm }: { form: SystemSettings; setForm: (f: SystemSettings) => void }) {
  const queryClient = useQueryClient()
  const { refetch: refetchSettings } = useQuery({ queryKey: ['settings'], queryFn: () => null, enabled: false })
  const fileRef = useRef<HTMLInputElement>(null)
  const [uploading, setUploading] = useState(false)
  const [uploadError, setUploadError] = useState<string | null>(null)

  async function handleFileChange(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0]
    if (!file) return
    setUploading(true); setUploadError(null)
    try {
      const fd = new FormData(); fd.append('file', file)
      const { data } = await api.post<SystemSettings>('/settings/logo', fd, {
        headers: { 'Content-Type': 'multipart/form-data' },
      })
      setForm({ ...form, logoUrl: data.logoUrl ?? form.logoUrl })
      queryClient.invalidateQueries({ queryKey: ['settings'] })
      queryClient.invalidateQueries({ queryKey: ['public-settings'] })
    } catch (err) {
      setUploadError(apiErrorMessage(err))
    } finally {
      setUploading(false)
      if (fileRef.current) fileRef.current.value = ''
    }
  }

  async function removeLogo() {
    try {
      const { data } = await api.delete<SystemSettings>('/settings/logo')
      setForm({ ...form, logoUrl: data.logoUrl ?? '' })
      queryClient.invalidateQueries({ queryKey: ['settings'] })
    } catch (err) {
      setUploadError(apiErrorMessage(err))
    }
  }

  const isUploaded = form.logoUrl === '/api/assets/logo'

  return (
    <div className="space-y-1.5">
      <Field label="Logo do sistema">
        <div className="flex gap-2">
          <Input
            value={isUploaded ? '' : (form.logoUrl ?? '')}
            onChange={(e) => setForm({ ...form, logoUrl: e.target.value })}
            placeholder={isUploaded ? '← logo enviado pelo sistema' : 'https://…'}
            disabled={isUploaded}
          />
          <Button type="button" variant="outline" onClick={() => fileRef.current?.click()} loading={uploading}>
            {isUploaded ? 'Trocar' : 'Upload'}
          </Button>
          {isUploaded && (
            <Button type="button" variant="outline" onClick={removeLogo}>Remover</Button>
          )}
        </div>
        <input ref={fileRef} type="file" accept="image/*" className="hidden" onChange={handleFileChange} />
      </Field>
      {isUploaded && (
        <p className="text-xs text-green-600 dark:text-green-400">
          ✓ Logo personalizado armazenado no sistema.
        </p>
      )}
      {uploadError && <p className="text-xs text-red-600">{uploadError}</p>}
      {form.logoUrl && !isUploaded && (
        <img src={form.logoUrl} alt="Preview do logo" className="mt-1 h-10 rounded border object-contain dark:border-gray-700" />
      )}
      {isUploaded && (
        <img src="/api/assets/logo" alt="Logo do sistema" className="mt-1 h-10 rounded border object-contain dark:border-gray-700" />
      )}
    </div>
  )
}
