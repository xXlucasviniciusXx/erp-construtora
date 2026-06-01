import { useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { CheckCircle2, XCircle } from 'lucide-react'
import { api, apiErrorMessage } from '@/lib/api'
import type { CostCenter, Page, Supplier } from '@/lib/types'
import { useAuth } from '@/auth/AuthContext'
import { ActionsMenu } from '@/components/Menu'
import { Badge, Button, Field, Input, Modal, PageHeader, Select, Table } from '@/components/ui'
import { formatCurrency, formatDate } from '@/lib/utils'

interface Payable {
  id: string
  supplier: string
  category?: string
  description?: string
  amount: number
  dueDate: string
  paymentDate?: string
  status: 'OPEN' | 'PAID' | 'OVERDUE' | 'CANCELLED'
  paymentMethod?: string
  costCenter?: string
}

const EMPTY: Partial<Payable> = { status: 'OPEN' }
const STATUS_COLOR: Record<string, string> = { OPEN: 'gray', PAID: 'green', OVERDUE: 'red', CANCELLED: 'gray' }
const STATUS_LABEL: Record<string, string> = { OPEN: 'EM ABERTO', PAID: 'PAGO', OVERDUE: 'ATRASADO', CANCELLED: 'CANCELADO' }

export function PayablePage() {
  const { hasPermission } = useAuth()
  const queryClient = useQueryClient()
  const [modalOpen, setModalOpen] = useState(false)
  const [form, setForm] = useState<Partial<Payable>>(EMPTY)
  const [view, setView] = useState<Payable | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [q, setQ] = useState('')
  const [statusFilter, setStatusFilter] = useState('')
  const canWrite = hasPermission('PAYABLE_WRITE')

  const { data, isLoading } = useQuery({
    queryKey: ['payable'],
    queryFn: async () => (await api.get<Page<Payable>>('/accounts-payable', { params: { size: 200 } })).data,
  })
  const suppliers = useQuery({ queryKey: ['suppliers'], queryFn: async () => (await api.get<Supplier[]>('/suppliers')).data })
  const costCenters = useQuery({ queryKey: ['cost-centers'], queryFn: async () => (await api.get<CostCenter[]>('/cost-centers')).data })

  const invalidate = () => { queryClient.invalidateQueries({ queryKey: ['payable'] }); queryClient.invalidateQueries({ queryKey: ['dashboard-analytics'] }) }
  const save = useMutation({
    mutationFn: async (p: Partial<Payable>) => p.id ? api.put(`/accounts-payable/${p.id}`, p) : api.post('/accounts-payable', p),
    onSuccess: () => { invalidate(); setModalOpen(false); setForm(EMPTY) },
    onError: (e) => setError(apiErrorMessage(e)),
  })
  const pay = useMutation({ mutationFn: async (id: string) => api.post(`/accounts-payable/${id}/pay`), onSuccess: invalidate })
  const cancel = useMutation({ mutationFn: async (id: string) => api.post(`/accounts-payable/${id}/cancel`), onSuccess: invalidate })
  const remove = useMutation({ mutationFn: async (id: string) => api.delete(`/accounts-payable/${id}`), onSuccess: invalidate, onError: (e) => alert(apiErrorMessage(e)) })

  const filtered = useMemo(() => {
    let list = data?.content ?? []
    if (q) { const t = q.toLowerCase(); list = list.filter((p) => p.supplier.toLowerCase().includes(t) || (p.description ?? '').toLowerCase().includes(t)) }
    if (statusFilter) list = list.filter((p) => p.status === statusFilter)
    return list
  }, [data, q, statusFilter])

  return (
    <div>
      <PageHeader title="Contas a Pagar" action={canWrite && <Button onClick={() => { setForm(EMPTY); setError(null); setModalOpen(true) }}>Nova conta</Button>} />

      <div className="mb-4 grid grid-cols-1 gap-3 sm:grid-cols-3">
        <Input placeholder="Buscar por fornecedor ou descrição…" value={q} onChange={(e) => setQ(e.target.value)} />
        <Select value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)}>
          <option value="">Todos os status</option>
          <option value="OPEN">Em aberto</option>
          <option value="PAID">Pago</option>
          <option value="OVERDUE">Atrasado</option>
          <option value="CANCELLED">Cancelado</option>
        </Select>
      </div>

      {isLoading ? <p className="text-gray-500">Carregando…</p> : (
        <Table headers={['Fornecedor', 'Categoria', 'Valor', 'Vencimento', 'Status', 'Ações']}>
          {filtered.map((p) => (
            <tr key={p.id} className="hover:bg-gray-50">
              <td className="px-4 py-2 font-medium">{p.supplier}</td>
              <td className="px-4 py-2">{p.category ?? '—'}</td>
              <td className="px-4 py-2">{formatCurrency(p.amount)}</td>
              <td className="px-4 py-2">{formatDate(p.dueDate)}</td>
              <td className="px-4 py-2"><Badge color={STATUS_COLOR[p.status]}>{STATUS_LABEL[p.status]}</Badge></td>
              <td className="px-4 py-2">
                <div className="flex items-center justify-end gap-1">
                  {canWrite && (
                    <>
                      <button title="Confirmar pagamento" disabled={p.status === 'PAID' || p.status === 'CANCELLED'}
                        onClick={() => { if (window.confirm(`Confirmar pagamento de "${p.supplier}" (${formatCurrency(p.amount)})?`)) pay.mutate(p.id) }}
                        className="text-green-600 hover:text-green-700 disabled:opacity-30"><CheckCircle2 className="h-5 w-5" /></button>
                      <button title="Cancelar conta" disabled={p.status === 'CANCELLED'}
                        onClick={() => { if (window.confirm(`Cancelar a conta de "${p.supplier}"?`)) cancel.mutate(p.id) }}
                        className="text-red-600 hover:text-red-700 disabled:opacity-30"><XCircle className="h-5 w-5" /></button>
                    </>
                  )}
                  <ActionsMenu items={[
                    { label: 'Consultar', onClick: () => setView(p) },
                    ...(canWrite ? [
                      { label: 'Alterar', onClick: () => { setForm(p); setError(null); setModalOpen(true) } },
                      { label: 'Excluir', danger: true, onClick: () => { if (window.confirm('Excluir esta conta a pagar?')) remove.mutate(p.id) } },
                    ] : []),
                  ]} />
                </div>
              </td>
            </tr>
          ))}
          {filtered.length === 0 && <tr><td colSpan={6} className="px-4 py-6 text-center text-gray-400">Nenhuma conta.</td></tr>}
        </Table>
      )}

      {view && (
        <Modal open onClose={() => setView(null)} title="Conta a pagar">
          <div className="grid grid-cols-2 gap-3">
            <Info label="Fornecedor" value={view.supplier} />
            <Info label="Status" value={STATUS_LABEL[view.status]} />
            <Info label="Categoria" value={view.category} />
            <Info label="Centro de custo" value={view.costCenter} />
            <Info label="Valor" value={formatCurrency(view.amount)} />
            <Info label="Vencimento" value={formatDate(view.dueDate)} />
            <Info label="Pagamento" value={view.paymentDate ? formatDate(view.paymentDate) : '—'} />
            <Info label="Forma de pagamento" value={view.paymentMethod} />
          </div>
          {view.description && <div className="mt-3"><Info label="Descrição" value={view.description} /></div>}
        </Modal>
      )}

      <Modal open={modalOpen} onClose={() => setModalOpen(false)} title={form.id ? 'Alterar conta a pagar' : 'Nova conta a pagar'}>
        <form className="space-y-3" onSubmit={(e) => { e.preventDefault(); save.mutate(form) }}>
          <Field label="Fornecedor">
            <Input list="suppliers-list" placeholder="Selecione ou digite…" value={form.supplier ?? ''} onChange={(e) => setForm({ ...form, supplier: e.target.value })} required />
            <datalist id="suppliers-list">{suppliers.data?.map((s) => <option key={s.id} value={s.name} />)}</datalist>
          </Field>
          <div className="grid grid-cols-2 gap-3">
            <Field label="Categoria"><Input value={form.category ?? ''} onChange={(e) => setForm({ ...form, category: e.target.value })} /></Field>
            <Field label="Centro de custo">
              <Input list="cost-centers-list" placeholder="Selecione ou digite…" value={form.costCenter ?? ''} onChange={(e) => setForm({ ...form, costCenter: e.target.value })} />
              <datalist id="cost-centers-list">{costCenters.data?.map((c) => <option key={c.id} value={c.name} />)}</datalist>
            </Field>
          </div>
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
