import { useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { CheckCircle2, XCircle, ArrowUpCircle } from 'lucide-react'
import { api, apiErrorMessage } from '@/lib/api'
import type { CostCenter, Development, Page, Supplier } from '@/lib/types'
import { useAuth } from '@/auth/AuthContext'
import { ActionsMenu } from '@/components/Menu'
import { useToast } from '@/components/Toast'
import { useConfirm } from '@/components/Confirm'
import { Badge, Button, EmptyState, Field, Input, Modal, PageHeader, Select, Table, TableSkeleton, Tr } from '@/components/ui'
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
  developmentId?: string
  developmentName?: string
}

const EMPTY: Partial<Payable> = { status: 'OPEN' }
const STATUS_COLOR: Record<string, string> = { OPEN: 'gray', PAID: 'green', OVERDUE: 'red', CANCELLED: 'gray' }
const STATUS_LABEL: Record<string, string> = { OPEN: 'Em aberto', PAID: 'Pago', OVERDUE: 'Atrasado', CANCELLED: 'Cancelado' }

export function PayablePage() {
  const { hasPermission } = useAuth()
  const queryClient = useQueryClient()
  const toast = useToast()
  const confirm = useConfirm()
  const [modalOpen, setModalOpen] = useState(false)
  const [form, setForm] = useState<Partial<Payable>>(EMPTY)
  const [view, setView] = useState<Payable | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [q, setQ] = useState('')
  const [statusFilter, setStatusFilter] = useState('')
  const [devFilter, setDevFilter] = useState('')
  const canWrite = hasPermission('PAYABLE_WRITE')

  const { data, isLoading } = useQuery({
    queryKey: ['payable'],
    queryFn: async () => (await api.get<Page<Payable>>('/accounts-payable', { params: { size: 200 } })).data,
  })
  const suppliers = useQuery({ queryKey: ['suppliers'], queryFn: async () => (await api.get<Supplier[]>('/suppliers')).data })
  const costCenters = useQuery({ queryKey: ['cost-centers'], queryFn: async () => (await api.get<CostCenter[]>('/cost-centers')).data })
  const developments = useQuery({ queryKey: ['developments'], queryFn: async () => (await api.get<Development[]>('/developments')).data })

  const invalidate = () => { queryClient.invalidateQueries({ queryKey: ['payable'] }); queryClient.invalidateQueries({ queryKey: ['dashboard-analytics'] }) }
  const save = useMutation({
    mutationFn: async (p: Partial<Payable>) => p.id ? api.put(`/accounts-payable/${p.id}`, p) : api.post('/accounts-payable', p),
    onSuccess: (_d, p) => { invalidate(); setModalOpen(false); setForm(EMPTY); toast.success(p.id ? 'Conta atualizada.' : 'Conta criada.') },
    onError: (e) => setError(apiErrorMessage(e)),
  })
  const pay = useMutation({ mutationFn: async (id: string) => api.post(`/accounts-payable/${id}/pay`), onSuccess: () => { invalidate(); toast.success('Pagamento confirmado.') }, onError: (e) => toast.error(apiErrorMessage(e)) })
  const cancel = useMutation({ mutationFn: async (id: string) => api.post(`/accounts-payable/${id}/cancel`), onSuccess: () => { invalidate(); toast.success('Conta cancelada.') }, onError: (e) => toast.error(apiErrorMessage(e)) })
  const remove = useMutation({ mutationFn: async (id: string) => api.delete(`/accounts-payable/${id}`), onSuccess: () => { invalidate(); toast.success('Conta excluída.') }, onError: (e) => toast.error(apiErrorMessage(e)) })

  async function confirmPay(p: Payable) {
    if (await confirm({ title: 'Confirmar pagamento', message: `Confirmar pagamento de "${p.supplier}" (${formatCurrency(p.amount)})?`, confirmLabel: 'Confirmar' })) pay.mutate(p.id)
  }
  async function confirmCancel(p: Payable) {
    if (await confirm({ title: 'Cancelar conta', message: `Cancelar a conta de "${p.supplier}"?`, confirmLabel: 'Cancelar conta', danger: true })) cancel.mutate(p.id)
  }
  async function confirmRemove(p: Payable) {
    if (await confirm({ title: 'Excluir conta', message: 'Excluir esta conta a pagar?', confirmLabel: 'Excluir', danger: true })) remove.mutate(p.id)
  }

  const filtered = useMemo(() => {
    let list = data?.content ?? []
    if (q) { const t = q.toLowerCase(); list = list.filter((p) => p.supplier.toLowerCase().includes(t) || (p.description ?? '').toLowerCase().includes(t)) }
    if (statusFilter) list = list.filter((p) => p.status === statusFilter)
    if (devFilter) list = list.filter((p) => (devFilter === '__none__' ? !p.developmentId : p.developmentId === devFilter))
    return list
  }, [data, q, statusFilter, devFilter])

  return (
    <div>
      <PageHeader title="Contas a Pagar" subtitle="Despesas e obrigações da empresa" action={canWrite && <Button onClick={() => { setForm(EMPTY); setError(null); setModalOpen(true) }}>Nova conta</Button>} />

      <div className="mb-4 grid grid-cols-1 gap-3 sm:grid-cols-3">
        <Input placeholder="Buscar por fornecedor ou descrição…" value={q} onChange={(e) => setQ(e.target.value)} />
        <Select value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)}>
          <option value="">Todos os status</option>
          <option value="OPEN">Em aberto</option>
          <option value="PAID">Pago</option>
          <option value="OVERDUE">Atrasado</option>
          <option value="CANCELLED">Cancelado</option>
        </Select>
        <Select value={devFilter} onChange={(e) => setDevFilter(e.target.value)}>
          <option value="">Todos os empreendimentos</option>
          <option value="__none__">Geral / Administrativo (sem empreend.)</option>
          {developments.data?.map((d) => <option key={d.id} value={d.id}>{d.name}</option>)}
        </Select>
      </div>

      {isLoading ? <TableSkeleton rows={6} cols={7} /> : (
        <Table headers={['Fornecedor', 'Empreendimento', 'Categoria', 'Valor', 'Vencimento', 'Status', 'Ações']}>
          {filtered.map((p) => (
            <Tr key={p.id}>
              <td className="px-4 py-2 font-medium">{p.supplier}</td>
              <td className="px-4 py-2">{p.developmentName ?? <span className="text-gray-400">Geral</span>}</td>
              <td className="px-4 py-2">{p.category ?? '—'}</td>
              <td className="px-4 py-2">{formatCurrency(p.amount)}</td>
              <td className="px-4 py-2">{formatDate(p.dueDate)}</td>
              <td className="px-4 py-2"><Badge dot color={STATUS_COLOR[p.status]}>{STATUS_LABEL[p.status]}</Badge></td>
              <td className="px-4 py-2">
                <div className="flex items-center justify-end gap-1">
                  {canWrite && (
                    <>
                      <button title="Confirmar pagamento" disabled={p.status === 'PAID' || p.status === 'CANCELLED'}
                        onClick={() => confirmPay(p)}
                        className="text-green-600 transition hover:text-green-700 disabled:opacity-30"><CheckCircle2 className="h-5 w-5" /></button>
                      <button title="Cancelar conta" disabled={p.status === 'CANCELLED'}
                        onClick={() => confirmCancel(p)}
                        className="text-red-600 transition hover:text-red-700 disabled:opacity-30"><XCircle className="h-5 w-5" /></button>
                    </>
                  )}
                  <ActionsMenu items={[
                    { label: 'Consultar', onClick: () => setView(p) },
                    ...(canWrite ? [
                      { label: 'Alterar', onClick: () => { setForm(p); setError(null); setModalOpen(true) } },
                      { label: 'Excluir', danger: true, onClick: () => confirmRemove(p) },
                    ] : []),
                  ]} />
                </div>
              </td>
            </Tr>
          ))}
          {filtered.length === 0 && (
            <tr><td colSpan={7} className="p-0">
              <EmptyState
                icon={ArrowUpCircle}
                title="Nenhuma conta a pagar"
                description={q || statusFilter || devFilter ? 'Ajuste a busca ou os filtros.' : 'Cadastre a primeira conta a pagar.'}
                action={canWrite && !q && !statusFilter && !devFilter ? <Button onClick={() => { setForm(EMPTY); setError(null); setModalOpen(true) }}>Nova conta</Button> : undefined}
              />
            </td></tr>
          )}
        </Table>
      )}

      {view && (
        <Modal open onClose={() => setView(null)} title="Conta a pagar">
          <div className="grid grid-cols-2 gap-3">
            <Info label="Fornecedor" value={view.supplier} />
            <Info label="Status" value={STATUS_LABEL[view.status]} />
            <Info label="Empreendimento" value={view.developmentName ?? 'Geral / Administrativo'} />
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
          <Field label="Empreendimento (opcional)">
            <Select value={form.developmentId ?? ''} onChange={(e) => setForm({ ...form, developmentId: e.target.value || undefined })}>
              <option value="">— Despesa geral / administrativa —</option>
              {developments.data?.map((d) => <option key={d.id} value={d.id}>{d.name}</option>)}
            </Select>
          </Field>
          {!form.developmentId && (
            <p className="rounded-md bg-amber-50 px-3 py-2 text-xs text-amber-700 dark:bg-amber-900/20 dark:text-amber-300">
              ⚠ Sem empreendimento vinculado: esta conta será registrada como <strong>despesa geral / administrativa</strong>.
            </p>
          )}
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
