import { useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ArrowDownCircle } from 'lucide-react'
import { api, apiErrorMessage, getToken } from '@/lib/api'
import type { Client, InstallmentDetail, Page } from '@/lib/types'
import { useAuth } from '@/auth/AuthContext'
import { ActionsMenu } from '@/components/Menu'
import { useToast } from '@/components/Toast'
import { useConfirm } from '@/components/Confirm'
import { Badge, Button, EmptyState, Field, Input, Modal, PageHeader, Select, Table, TableSkeleton, Tr } from '@/components/ui'
import { cn, formatCurrency, formatDate } from '@/lib/utils'

const INST_LABEL: Record<string, string> = { OPEN: 'Aberta', PAID: 'Paga', OVERDUE: 'Vencida', CANCELLED: 'Cancelada' }
const RECV_LABEL: Record<string, string> = { OPEN: 'Em aberto', RECEIVED: 'Recebida', OVERDUE: 'Atrasada', CANCELLED: 'Cancelada' }

const TABS = [
  { key: 'installments', label: 'Parcelas de Vendas' },
  { key: 'standalone', label: 'Avulsas' },
] as const
type TabKey = (typeof TABS)[number]['key']

export function ReceivablePage() {
  const [tab, setTab] = useState<TabKey>('installments')
  return (
    <div>
      <PageHeader title="Contas a Receber" subtitle="Parcelas de vendas e recebíveis avulsos" />
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
  if (!res.ok) { return }
  window.open(URL.createObjectURL(await res.blob()))
}
const INST_COLOR: Record<string, string> = { OPEN: 'gray', PAID: 'green', OVERDUE: 'red', CANCELLED: 'gray' }

function InstallmentsTab() {
  const { hasPermission } = useAuth()
  const queryClient = useQueryClient()
  const toast = useToast()
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
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['installments-search'] }); queryClient.invalidateQueries({ queryKey: ['dashboard-analytics'] }); toast.success('Recebimento registrado.') },
    onError: (e) => toast.error(apiErrorMessage(e)),
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
      {isLoading ? <TableSkeleton rows={6} cols={8} /> : (
        <Table headers={['Cliente', 'Documento', 'Telefone', 'Parcela', 'Valor', 'Vencimento', 'Status', 'Ações']}>
          {data?.map((i) => (
            <Tr key={i.id}>
              <td className="px-4 py-2 font-medium">{i.clientName}</td>
              <td className="px-4 py-2">{i.clientDocument}</td>
              <td className="px-4 py-2">{i.clientPhone ?? '—'}</td>
              <td className="px-4 py-2">#{i.number}</td>
              <td className="px-4 py-2">{formatCurrency(i.amount)}</td>
              <td className="px-4 py-2">{formatDate(i.dueDate)}</td>
              <td className="px-4 py-2"><Badge dot color={INST_COLOR[i.status]}>{INST_LABEL[i.status] ?? i.status}</Badge></td>
              <td className="px-4 py-2 text-right">
                <ActionsMenu items={[
                  ...(canPay && i.status !== 'PAID' ? [{ label: 'Registrar recebimento', onClick: () => pay.mutate(i.id) }] : []),
                  { label: 'Gerar contrato', onClick: () => openContract(i.saleId), disabled: !canContract },
                ]} />
              </td>
            </Tr>
          ))}
          {data?.length === 0 && (
            <tr><td colSpan={8} className="p-0">
              <EmptyState icon={ArrowDownCircle} title="Nenhuma parcela encontrada" description="Ajuste a busca ou os filtros de período/status." />
            </td></tr>
          )}
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
  const toast = useToast()
  const confirm = useConfirm()
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
    onSuccess: (_d, p) => { queryClient.invalidateQueries({ queryKey: ['receivable'] }); setModalOpen(false); setForm(EMPTY); toast.success(p.id ? 'Conta atualizada.' : 'Conta criada.') },
    onError: (e) => setError(apiErrorMessage(e)),
  })
  const receive = useMutation({
    mutationFn: async (id: string) => api.post(`/accounts-receivable/${id}/receive`),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['receivable'] }); toast.success('Recebimento confirmado.') },
    onError: (e) => toast.error(apiErrorMessage(e)),
  })
  const remove = useMutation({
    mutationFn: async (id: string) => api.delete(`/accounts-receivable/${id}`),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['receivable'] }); toast.success('Conta excluída.') },
    onError: (e) => toast.error(apiErrorMessage(e)),
  })

  async function confirmRemove(id: string) {
    if (await confirm({ title: 'Excluir conta', message: 'Excluir esta conta a receber?', confirmLabel: 'Excluir', danger: true })) remove.mutate(id)
  }

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

      {isLoading ? <TableSkeleton rows={6} cols={6} /> : (
        <Table headers={['Cliente', 'Descrição', 'Valor', 'Vencimento', 'Status', 'Ações']}>
          {filtered.map((r) => (
            <Tr key={r.id}>
              <td className="px-4 py-2 font-medium">{r.clientName ?? '—'}</td>
              <td className="px-4 py-2">{r.description ?? '—'}</td>
              <td className="px-4 py-2">{formatCurrency(r.amount)}</td>
              <td className="px-4 py-2">{formatDate(r.dueDate)}</td>
              <td className="px-4 py-2"><Badge dot color={RECV_COLOR[r.status]}>{RECV_LABEL[r.status] ?? r.status}</Badge></td>
              <td className="px-4 py-2 text-right">
                <ActionsMenu items={[
                  { label: 'Consultar', onClick: () => setView(r) },
                  ...(canWrite ? [
                    ...(r.status !== 'RECEIVED' ? [{ label: 'Confirmar recebimento', onClick: () => receive.mutate(r.id) }] : []),
                    { label: 'Alterar', onClick: () => { setForm(r); setError(null); setModalOpen(true) } },
                    { label: 'Excluir', danger: true, onClick: () => confirmRemove(r.id) },
                  ] : []),
                ]} />
              </td>
            </Tr>
          ))}
          {filtered.length === 0 && (
            <tr><td colSpan={6} className="p-0">
              <EmptyState
                icon={ArrowDownCircle}
                title="Nenhuma conta avulsa"
                description={q || statusFilter ? 'Ajuste a busca ou os filtros.' : 'Cadastre a primeira conta a receber avulsa.'}
                action={canWrite && !q && !statusFilter ? <Button onClick={() => { setForm(EMPTY); setError(null); setModalOpen(true) }}>Nova conta</Button> : undefined}
              />
            </td></tr>
          )}
        </Table>
      )}

      {view && (
        <Modal open onClose={() => setView(null)} title="Conta a receber">
          <div className="grid grid-cols-2 gap-3 text-sm">
            <Info label="Cliente" value={view.clientName} />
            <Info label="Status" value={RECV_LABEL[view.status] ?? view.status} />
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
            <Button type="submit" loading={save.isPending}>Salvar</Button>
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
