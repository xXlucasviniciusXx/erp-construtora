import { useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api, apiErrorMessage, getToken } from '@/lib/api'
import type { Client, InstallmentDetail, Page } from '@/lib/types'
import { useAuth } from '@/auth/AuthContext'
import { ActionsMenu } from '@/components/Menu'
import { Badge, Button, Field, Input, Modal, PageHeader, Select, Table } from '@/components/ui'
import { cn, formatCurrency, formatDate } from '@/lib/utils'

const TABS = [
  { key: 'installments', label: 'Parcelas de Vendas' },
  { key: 'standalone', label: 'Avulsas' },
] as const
type TabKey = (typeof TABS)[number]['key']

export function ReceivablePage() {
  const [tab, setTab] = useState<TabKey>('installments')
  return (
    <div>
      <PageHeader title="Contas a Receber" />
      <div className="mb-4 flex gap-1 border-b border-gray-200 dark:border-gray-700">
        {TABS.map((t) => (
          <button
            key={t.key}
            onClick={() => setTab(t.key)}
            className={cn('border-b-2 px-4 py-2 text-sm font-medium',
              tab === t.key ? 'border-primary text-primary' : 'border-transparent text-gray-500 hover:text-gray-700 dark:hover:text-gray-300')}
          >
            {t.label}
          </button>
        ))}
      </div>
      {tab === 'installments' ? <InstallmentsTab /> : <StandaloneTab />}
    </div>
  )
}

/* ---------------- Aba: parcelas de vendas (antiga tela "Parcelas") ---------------- */

async function openContract(saleId: string) {
  const res = await fetch(`${api.defaults.baseURL}/contracts/sales/${saleId}/pdf`, { headers: { Authorization: `Bearer ${getToken()}` } })
  if (!res.ok) { alert('Falha ao gerar o contrato'); return }
  window.open(URL.createObjectURL(await res.blob()))
}
const INST_COLOR: Record<string, string> = { OPEN: 'gray', PAID: 'green', OVERDUE: 'red', CANCELLED: 'gray' }

function InstallmentsTab() {
  const { hasPermission } = useAuth()
  const queryClient = useQueryClient()
  const canPay = hasPermission('RECEIVABLE_WRITE') || hasPermission('SALES_WRITE')
  const canContract = hasPermission('CONTRACTS_GENERATE')
  const [q, setQ] = useState('')
  const [status, setStatus] = useState('')
  const [dueFrom, setDueFrom] = useState('')
  const [dueTo, setDueTo] = useState('')

  const { data, isLoading } = useQuery({
    queryKey: ['installments-search', q, status, dueFrom, dueTo],
    queryFn: async () => (await api.get<InstallmentDetail[]>('/installments', {
      params: { q: q || undefined, status: status || undefined, dueFrom: dueFrom || undefined, dueTo: dueTo || undefined },
    })).data,
  })

  const pay = useMutation({
    mutationFn: async (id: string) => api.post(`/installments/${id}/pay`, { paymentDate: new Date().toISOString().slice(0, 10) }),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['installments-search'] }); queryClient.invalidateQueries({ queryKey: ['dashboard-analytics'] }) },
  })

  return (
    <div>
      <div className="mb-4 grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-4">
        <Input placeholder="Cliente (nome / CPF / CNPJ)" value={q} onChange={(e) => setQ(e.target.value)} />
        <Select value={status} onChange={(e) => setStatus(e.target.value)}>
          <option value="">Todos os status</option>
          <option value="OVERDUE">Atrasada</option>
          <option value="OPEN">Em aberto</option>
          <option value="PAID">Paga</option>
        </Select>
        <Input type="date" value={dueFrom} onChange={(e) => setDueFrom(e.target.value)} />
        <Input type="date" value={dueTo} onChange={(e) => setDueTo(e.target.value)} />
      </div>
      {isLoading ? <p className="text-gray-500">Carregando…</p> : (
        <Table headers={['Cliente', 'Documento', 'Telefone', 'Parcela', 'Valor', 'Vencimento', 'Status', 'Ações']}>
          {data?.map((i) => (
            <tr key={i.id} className="hover:bg-gray-50">
              <td className="px-4 py-2 font-medium">{i.clientName}</td>
              <td className="px-4 py-2">{i.clientDocument}</td>
              <td className="px-4 py-2">{i.clientPhone ?? '—'}</td>
              <td className="px-4 py-2">#{i.number}</td>
              <td className="px-4 py-2">{formatCurrency(i.amount)}</td>
              <td className="px-4 py-2">{formatDate(i.dueDate)}</td>
              <td className="px-4 py-2"><Badge color={INST_COLOR[i.status]}>{i.status}</Badge></td>
              <td className="px-4 py-2 text-right">
                <ActionsMenu items={[
                  ...(canPay && i.status !== 'PAID' ? [{ label: 'Registrar recebimento', onClick: () => pay.mutate(i.id) }] : []),
                  { label: 'Gerar contrato', onClick: () => openContract(i.saleId), disabled: !canContract },
                ]} />
              </td>
            </tr>
          ))}
          {data?.length === 0 && <tr><td colSpan={8} className="px-4 py-6 text-center text-gray-400">Nenhuma parcela encontrada.</td></tr>}
        </Table>
      )}
    </div>
  )
}

/* ---------------- Aba: avulsas (accounts_receivable) ---------------- */

interface Receivable {
  id: string
  clientId?: string
  clientName?: string
  description?: string
  amount: number
  dueDate: string
  receiveDate?: string
  status: 'OPEN' | 'RECEIVED' | 'OVERDUE' | 'CANCELLED'
  paymentMethod?: string
  notes?: string
}
const EMPTY: Partial<Receivable> = { status: 'OPEN' }
const RECV_COLOR: Record<string, string> = { OPEN: 'gray', RECEIVED: 'green', OVERDUE: 'red', CANCELLED: 'gray' }

function StandaloneTab() {
  const { hasPermission } = useAuth()
  const queryClient = useQueryClient()
  const [modalOpen, setModalOpen] = useState(false)
  const [form, setForm] = useState<Partial<Receivable>>(EMPTY)
  const [view, setView] = useState<Receivable | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [q, setQ] = useState('')
  const [statusFilter, setStatusFilter] = useState('')
  const canWrite = hasPermission('RECEIVABLE_WRITE')

  const { data, isLoading } = useQuery({
    queryKey: ['receivable'],
    queryFn: async () => (await api.get<Page<Receivable>>('/accounts-receivable', { params: { size: 200 } })).data,
  })
  const clients = useQuery({
    queryKey: ['clients-all'],
    queryFn: async () => (await api.get<Page<Client>>('/clients', { params: { size: 200 } })).data.content,
  })

  const save = useMutation({
    mutationFn: async (p: Partial<Receivable>) => p.id ? api.put(`/accounts-receivable/${p.id}`, p) : api.post('/accounts-receivable', p),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['receivable'] }); setModalOpen(false); setForm(EMPTY) },
    onError: (e) => setError(apiErrorMessage(e)),
  })
  const receive = useMutation({
    mutationFn: async (id: string) => api.post(`/accounts-receivable/${id}/receive`),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['receivable'] }),
  })
  const remove = useMutation({
    mutationFn: async (id: string) => api.delete(`/accounts-receivable/${id}`),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['receivable'] }),
    onError: (e) => alert(apiErrorMessage(e)),
  })

  const filtered = useMemo(() => {
    let list = data?.content ?? []
    if (q) { const t = q.toLowerCase(); list = list.filter((r) => (r.clientName ?? '').toLowerCase().includes(t) || (r.description ?? '').toLowerCase().includes(t)) }
    if (statusFilter) list = list.filter((r) => r.status === statusFilter)
    return list
  }, [data, q, statusFilter])

  return (
    <div>
      <div className="mb-4 flex flex-wrap items-end justify-between gap-3">
        <div className="grid flex-1 grid-cols-1 gap-3 sm:grid-cols-2 lg:max-w-lg">
          <Input placeholder="Buscar por cliente ou descrição…" value={q} onChange={(e) => setQ(e.target.value)} />
          <Select value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)}>
            <option value="">Todos os status</option>
            <option value="OPEN">Em aberto</option>
            <option value="RECEIVED">Recebida</option>
            <option value="OVERDUE">Atrasada</option>
            <option value="CANCELLED">Cancelada</option>
          </Select>
        </div>
        {canWrite && <Button onClick={() => { setForm(EMPTY); setError(null); setModalOpen(true) }}>Nova conta</Button>}
      </div>

      {isLoading ? <p className="text-gray-500">Carregando…</p> : (
        <Table headers={['Cliente', 'Descrição', 'Valor', 'Vencimento', 'Status', 'Ações']}>
          {filtered.map((r) => (
            <tr key={r.id} className="hover:bg-gray-50">
              <td className="px-4 py-2 font-medium">{r.clientName ?? '—'}</td>
              <td className="px-4 py-2">{r.description ?? '—'}</td>
              <td className="px-4 py-2">{formatCurrency(r.amount)}</td>
              <td className="px-4 py-2">{formatDate(r.dueDate)}</td>
              <td className="px-4 py-2"><Badge color={RECV_COLOR[r.status]}>{r.status}</Badge></td>
              <td className="px-4 py-2 text-right">
                <ActionsMenu items={[
                  { label: 'Consultar', onClick: () => setView(r) },
                  ...(canWrite ? [
                    ...(r.status !== 'RECEIVED' ? [{ label: 'Confirmar recebimento', onClick: () => receive.mutate(r.id) }] : []),
                    { label: 'Alterar', onClick: () => { setForm(r); setError(null); setModalOpen(true) } },
                    { label: 'Excluir', danger: true, onClick: () => { if (window.confirm('Excluir esta conta a receber?')) remove.mutate(r.id) } },
                  ] : []),
                ]} />
              </td>
            </tr>
          ))}
          {filtered.length === 0 && <tr><td colSpan={6} className="px-4 py-6 text-center text-gray-400">Nenhuma conta.</td></tr>}
        </Table>
      )}

      {view && (
        <Modal open onClose={() => setView(null)} title="Conta a receber">
          <div className="grid grid-cols-2 gap-3 text-sm">
            <Info label="Cliente" value={view.clientName} />
            <Info label="Status" value={view.status} />
            <Info label="Descrição" value={view.description} />
            <Info label="Valor" value={formatCurrency(view.amount)} />
            <Info label="Vencimento" value={formatDate(view.dueDate)} />
            <Info label="Recebimento" value={view.receiveDate ? formatDate(view.receiveDate) : '—'} />
            <Info label="Forma de pagamento" value={view.paymentMethod} />
          </div>
          {view.notes && <div className="mt-3"><Info label="Observações" value={view.notes} /></div>}
        </Modal>
      )}

      <Modal open={modalOpen} onClose={() => setModalOpen(false)} title={form.id ? 'Alterar conta a receber' : 'Nova conta a receber'}>
        <form className="space-y-3" onSubmit={(e) => { e.preventDefault(); save.mutate(form) }}>
          <Field label="Cliente">
            <Select value={form.clientId ?? ''} onChange={(e) => setForm({ ...form, clientId: e.target.value })}>
              <option value="">—</option>
              {clients.data?.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
            </Select>
          </Field>
          <Field label="Descrição"><Input value={form.description ?? ''} onChange={(e) => setForm({ ...form, description: e.target.value })} /></Field>
          <div className="grid grid-cols-2 gap-3">
            <Field label="Valor"><Input type="number" step="0.01" value={form.amount ?? ''} onChange={(e) => setForm({ ...form, amount: Number(e.target.value) })} required /></Field>
            <Field label="Vencimento"><Input type="date" value={form.dueDate ?? ''} onChange={(e) => setForm({ ...form, dueDate: e.target.value })} required /></Field>
          </div>
          {error && <p className="text-sm text-red-600">{error}</p>}
          <div className="flex justify-end gap-2 pt-2">
            <Button type="button" variant="outline" onClick={() => setModalOpen(false)}>Cancelar</Button>
            <Button type="submit" disabled={save.isPending}>Salvar</Button>
          </div>
        </form>
      </Modal>
    </div>
  )
}

function Info({ label, value }: { label: string; value?: string | null }) {
  return (
    <div>
      <div className="text-xs text-gray-400">{label}</div>
      <div className="text-sm text-gray-800 dark:text-gray-100">{value || '—'}</div>
    </div>
  )
}
