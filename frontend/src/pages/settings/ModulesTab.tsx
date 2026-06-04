import { useEffect, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { KeyRound, Wand2, Copy } from 'lucide-react'
import { api, apiErrorMessage } from '@/lib/api'
import type { License, Module } from '@/lib/types'
import { useLicensing } from '@/licensing/LicensingContext'
import { useToast } from '@/components/Toast'
import { Badge, Button, Card, Field, Input, Select } from '@/components/ui'

const PLANS = ['ESSENCIAL', 'PROFISSIONAL', 'PREMIUM']
const STATUSES = ['ATIVA', 'SUSPENSA', 'CANCELADA']

export function ModulesTab() {
  const queryClient = useQueryClient()
  const { refresh } = useLicensing()
  // Invalida tudo que depende de licença/módulos após uma mudança.
  async function refreshAll() {
    await queryClient.invalidateQueries({ queryKey: ['licensing-modules'] })
    await queryClient.invalidateQueries({ queryKey: ['licensing-license'] })
    await refresh()
  }
  return (
    <div className="grid gap-6 lg:grid-cols-2">
      <div className="space-y-6">
        <PlanPresetCard onApplied={refreshAll} />
        <ModulesCard onChanged={refreshAll} />
      </div>
      <div className="space-y-6">
        <LicenseKeyCard onApplied={refreshAll} />
        <LicenseCard />
      </div>
    </div>
  )
}

function PlanPresetCard({ onApplied }: { onApplied: () => Promise<void> }) {
  const toast = useToast()
  const apply = useMutation({
    mutationFn: async (plan: string) => api.post('/licensing/plan', { plan }),
    onSuccess: async (_d, plan) => { await onApplied(); toast.success(`Pacote do plano ${plan} aplicado.`) },
    onError: (e) => toast.error(apiErrorMessage(e)),
  })
  return (
    <Card className="space-y-3">
      <div>
        <h3 className="text-sm font-semibold text-gray-700 dark:text-gray-200">Aplicar pacote de plano</h3>
        <p className="text-xs text-gray-500 dark:text-gray-400">
          Liga automaticamente os módulos do plano (e desliga os demais). Não altera a validade.
        </p>
      </div>
      <div className="flex flex-wrap gap-2">
        {PLANS.map((p) => (
          <Button key={p} variant="outline" disabled={apply.isPending} onClick={() => apply.mutate(p)}>{p}</Button>
        ))}
      </div>
    </Card>
  )
}

function ModulesCard({ onChanged }: { onChanged: () => Promise<void> }) {
  const toast = useToast()
  const { data, isLoading } = useQuery({
    queryKey: ['licensing-modules'],
    queryFn: async () => (await api.get<Module[]>('/licensing/modules')).data,
  })
  const toggle = useMutation({
    mutationFn: async (m: Module) => api.put(`/licensing/modules/${m.code}`, { active: !m.active }),
    onSuccess: async () => { await onChanged(); toast.success('Módulo atualizado.') },
    onError: (e) => toast.error(apiErrorMessage(e)),
  })

  return (
    <Card className="space-y-3">
      <div>
        <h3 className="text-sm font-semibold text-gray-700 dark:text-gray-200">Módulos do sistema</h3>
        <p className="text-xs text-gray-500 dark:text-gray-400">
          Ajuste fino. Desligar esconde o item e bloqueia a API para todos. Usuários e Configurações
          são sempre acessíveis.
        </p>
      </div>
      {isLoading ? (
        <p className="text-sm text-gray-500">Carregando…</p>
      ) : (
        <ul className="divide-y divide-gray-100 dark:divide-gray-700">
          {(data ?? []).map((m) => {
            const future = m.code === 'PORTAL_CLIENTE' || m.code === 'APP_MOBILE'
            return (
              <li key={m.code} className="flex items-center justify-between gap-3 py-2.5">
                <div className="min-w-0">
                  <div className="flex items-center gap-2">
                    <span className="truncate text-sm font-medium text-gray-800 dark:text-gray-100">{m.name}</span>
                    {future && <Badge color="gray">Em breve</Badge>}
                  </div>
                  {m.description && <p className="truncate text-xs text-gray-500 dark:text-gray-400">{m.description}</p>}
                </div>
                <button
                  type="button"
                  disabled={toggle.isPending || future}
                  onClick={() => toggle.mutate(m)}
                  title={future ? 'Módulo ainda não disponível' : m.active ? 'Desligar' : 'Ligar'}
                  className={[
                    'relative inline-flex h-6 w-11 flex-shrink-0 items-center rounded-full transition',
                    m.active ? 'bg-emerald-500' : 'bg-gray-300 dark:bg-gray-600',
                    future ? 'cursor-not-allowed opacity-40' : 'cursor-pointer',
                  ].join(' ')}
                >
                  <span className={['inline-block h-5 w-5 transform rounded-full bg-white shadow transition', m.active ? 'translate-x-5' : 'translate-x-0.5'].join(' ')} />
                </button>
              </li>
            )
          })}
        </ul>
      )}
    </Card>
  )
}

function LicenseKeyCard({ onApplied }: { onApplied: () => Promise<void> }) {
  const toast = useToast()
  const [key, setKey] = useState('')
  const [generated, setGenerated] = useState('')
  const [gen, setGen] = useState({ plan: 'PROFISSIONAL', customer: '', periodMonths: 12, maxUsers: '' as string | number })

  const apply = useMutation({
    mutationFn: async (k: string) => api.post('/licensing/license/key', { key: k }),
    onSuccess: async () => { await onApplied(); setKey(''); toast.success('Chave aplicada! Plano e módulos atualizados.') },
    onError: (e) => toast.error(apiErrorMessage(e)),
  })

  const generate = useMutation({
    mutationFn: async () => api.post<{ key: string }>('/licensing/license/key/generate', {
      plan: gen.plan,
      customer: gen.customer || null,
      periodMonths: Number(gen.periodMonths) || 12,
      maxUsers: gen.maxUsers ? Number(gen.maxUsers) : null,
    }),
    onSuccess: (r) => { setGenerated(r.data.key); toast.success('Chave gerada. Copie e aplique ou entregue ao cliente.') },
    onError: (e) => toast.error(apiErrorMessage(e)),
  })

  return (
    <Card className="space-y-4">
      <div className="flex items-center gap-2">
        <KeyRound className="h-4 w-4 text-gray-400" />
        <h3 className="text-sm font-semibold text-gray-700 dark:text-gray-200">Chave de licenciamento</h3>
      </div>

      {/* Aplicar uma chave recebida */}
      <div className="space-y-2">
        <Field label="Colar chave recebida">
          <textarea
            value={key}
            onChange={(e) => setKey(e.target.value)}
            rows={3}
            placeholder="cole aqui a chave (token) entregue para esta instalação"
            className="w-full rounded-md border border-gray-300 bg-white px-3 py-2 text-xs font-mono dark:border-gray-600 dark:bg-gray-700"
          />
        </Field>
        <Button disabled={!key.trim() || apply.isPending} loading={apply.isPending} onClick={() => apply.mutate(key.trim())}>
          Aplicar chave
        </Button>
      </div>

      {/* Gerar chave (teste / futuro painel da Fase 5) */}
      <details className="rounded-md border border-dashed border-gray-300 p-3 dark:border-gray-600">
        <summary className="flex cursor-pointer items-center gap-2 text-xs font-medium text-gray-600 dark:text-gray-300">
          <Wand2 className="h-3.5 w-3.5" /> Gerar uma chave (teste)
        </summary>
        <div className="mt-3 space-y-3">
          <div className="grid grid-cols-2 gap-2">
            <Field label="Plano">
              <Select value={gen.plan} onChange={(e) => setGen({ ...gen, plan: e.target.value })}>
                {PLANS.map((p) => <option key={p} value={p}>{p}</option>)}
              </Select>
            </Field>
            <Field label="Período (meses)">
              <Input type="number" min={1} value={gen.periodMonths} onChange={(e) => setGen({ ...gen, periodMonths: Number(e.target.value) })} />
            </Field>
          </div>
          <div className="grid grid-cols-2 gap-2">
            <Field label="Cliente">
              <Input value={gen.customer} onChange={(e) => setGen({ ...gen, customer: e.target.value })} placeholder="Nome do cliente" />
            </Field>
            <Field label="Máx. usuários">
              <Input type="number" min={1} value={gen.maxUsers} onChange={(e) => setGen({ ...gen, maxUsers: e.target.value })} placeholder="opcional" />
            </Field>
          </div>
          <Button variant="outline" loading={generate.isPending} onClick={() => generate.mutate()}>Gerar chave</Button>
          {generated && (
            <div className="space-y-1">
              <textarea readOnly value={generated} rows={3} className="w-full rounded-md border border-gray-300 bg-gray-50 px-3 py-2 text-xs font-mono dark:border-gray-600 dark:bg-gray-900" />
              <div className="flex gap-2">
                <Button variant="outline" onClick={() => { navigator.clipboard?.writeText(generated); toast.success('Chave copiada.') }}>
                  <Copy className="h-3.5 w-3.5" /> Copiar
                </Button>
                <Button variant="outline" onClick={() => setKey(generated)}>Usar acima</Button>
              </div>
            </div>
          )}
        </div>
      </details>
    </Card>
  )
}

function LicenseCard() {
  const toast = useToast()
  const { refresh } = useLicensing()
  const [form, setForm] = useState<License | null>(null)

  const { data } = useQuery({
    queryKey: ['licensing-license'],
    queryFn: async () => (await api.get<License>('/licensing/license')).data,
  })
  useEffect(() => { if (data) setForm(data) }, [data])

  const save = useMutation({
    mutationFn: async (p: License) =>
      api.put('/licensing/license', {
        plan: p.plan,
        status: ['ATIVA', 'SUSPENSA', 'CANCELADA'].includes(p.status) ? p.status : 'ATIVA',
        startDate: p.startDate,
        endDate: p.endDate,
        periodMonths: p.periodMonths,
        maxUsers: p.maxUsers,
        notes: p.notes,
      }),
    onSuccess: async () => { await refresh(); toast.success('Licença atualizada.') },
    onError: (e) => toast.error(apiErrorMessage(e)),
  })

  if (!form) return <Card><p className="text-sm text-gray-500">Carregando…</p></Card>

  const statusColor = form.status === 'ATIVA' ? 'green' : form.status === 'EXPIRADA' ? 'red' : 'gray'
  const editableStatus = ['ATIVA', 'SUSPENSA', 'CANCELADA'].includes(form.status) ? form.status : 'ATIVA'

  return (
    <Card className="space-y-4">
      <div className="flex items-center justify-between">
        <h3 className="text-sm font-semibold text-gray-700 dark:text-gray-200">Licença</h3>
        <div className="flex items-center gap-2">
          {form.hasKey && <Badge color="blue">por chave</Badge>}
          <Badge dot color={statusColor}>{form.status}</Badge>
        </div>
      </div>

      {form.customer && <p className="text-xs text-gray-500 dark:text-gray-400">Cliente: <span className="font-medium">{form.customer}</span></p>}

      <form className="space-y-3" onSubmit={(e) => { e.preventDefault(); save.mutate({ ...form, status: editableStatus }) }}>
        <div className="grid grid-cols-2 gap-3">
          <Field label="Plano">
            <Select value={form.plan} onChange={(e) => setForm({ ...form, plan: e.target.value })}>
              {PLANS.map((p) => <option key={p} value={p}>{p}</option>)}
              {!PLANS.includes(form.plan) && <option value={form.plan}>{form.plan}</option>}
            </Select>
          </Field>
          <Field label="Status">
            <Select value={editableStatus} onChange={(e) => setForm({ ...form, status: e.target.value })}>
              {STATUSES.map((s) => <option key={s} value={s}>{s}</option>)}
            </Select>
          </Field>
        </div>

        <div className="grid grid-cols-2 gap-3">
          <Field label="Início"><Input type="date" value={form.startDate ?? ''} onChange={(e) => setForm({ ...form, startDate: e.target.value })} /></Field>
          <Field label="Vencimento"><Input type="date" value={form.endDate ?? ''} onChange={(e) => setForm({ ...form, endDate: e.target.value })} /></Field>
        </div>

        <div className="grid grid-cols-2 gap-3">
          <Field label="Período (meses)"><Input type="number" min={1} value={form.periodMonths ?? 12} onChange={(e) => setForm({ ...form, periodMonths: Number(e.target.value) })} /></Field>
          <Field label="Máx. de usuários"><Input type="number" min={1} value={form.maxUsers ?? ''} onChange={(e) => setForm({ ...form, maxUsers: e.target.value ? Number(e.target.value) : undefined })} placeholder="Sem limite" /></Field>
        </div>

        <Field label="Observações"><Input value={form.notes ?? ''} onChange={(e) => setForm({ ...form, notes: e.target.value })} /></Field>

        {form.daysToExpire != null && (
          <p className="text-xs text-gray-500 dark:text-gray-400">
            {form.expired ? `Vencida há ${Math.abs(form.daysToExpire)} dia(s).` : `Faltam ${form.daysToExpire} dia(s) para o vencimento.`}
            {' '}Tolerância: {form.graceDays} dia(s).
          </p>
        )}

        <Button type="submit" loading={save.isPending}>Salvar licença</Button>
      </form>
    </Card>
  )
}
