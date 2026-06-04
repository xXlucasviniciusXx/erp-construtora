import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api, apiErrorMessage, getToken } from '@/lib/api'
import type { Client, Lot, NamedItem, Page, Sale } from '@/lib/types'
import { FileSignature } from 'lucide-react'
import { useAuth } from '@/auth/AuthContext'
import { ActionsMenu } from '@/components/Menu'
import { Combobox } from '@/components/Combobox'
import { useToast } from '@/components/Toast'
import { Badge, Button, EmptyState, Field, Input, Modal, PageHeader, Pagination, Select, Table, TableSkeleton, Tr } from '@/components/ui'
import { formatCurrency, formatDate } from '@/lib/utils'

const INSTALLMENT_STATUS: Record<string, { label: string; color: string }> = {
  PAID: { label: 'Paga', color: 'green' }, OVERDUE: { label: 'Vencida', color: 'red' },
  OPEN: { label: 'Aberta', color: 'gray' }, CANCELLED: { label: 'Cancelada', color: 'gray' },
}

const PURCHASE_WITH_DOWN = 'Entrada + parcelas'
const PURCHASE_TYPES = ['À vista', PURCHASE_WITH_DOWN, 'Financiamento próprio', 'Outro']
// Fallback enquanto a API ainda não respondeu
const PAYMENT_METHODS_FALLBACK = ['Boleto', 'PIX', 'Transferência bancária', 'Cartão', 'Dinheiro', 'Outro']
const CORRECTION_INDEXES_FALLBACK = ['Sem correção', 'INCC', 'IPCA', 'IGP-M', 'Juros fixo mensal', 'Outro']
const SALE_STATUS: Record<string, string> = { ACTIVE: 'Ativa', COMPLETED: 'Quitada', CANCELLED: 'Cancelada' }

interface SaleForm {
  clientId: string
  lotId: string
  totalValue: number
  downPayment: number
  installmentsCount: number
  firstDueDate: string
  purchaseType: string
  paymentMethod: string
  correctionIndex: string
  interestRate: number
  penaltyRate: number
}
const EMPTY: SaleForm = {
  clientId: '', lotId: '', totalValue: 0, downPayment: 0, installmentsCount: 12,
  firstDueDate: '', purchaseType: PURCHASE_WITH_DOWN, paymentMethod: 'Boleto', correctionIndex: 'Sem correção',
  interestRate: 1, penaltyRate: 2,
}

export function SalesPage() {
  const { hasPermission } = useAuth()
  const queryClient = useQueryClient()
  const toast = useToast()
  const canWrite = hasPermission('VENDAS_EDIT')
  const canContract = hasPermission('VENDAS_EDIT')

  const [modalOpen, setModalOpen] = useState(false)
  const [editingId, setEditingId] = useState<string | null>(null)
  const [form, setForm] = useState<SaleForm>(EMPTY)
  const [error, setError] = useState<string | null>(null)
  const [view, setView] = useState<Sale | null>(null)
  const [q, setQ] = useState('')
  const [statusFilter, setStatusFilter] = useState('')
  const [page, setPage] = useState(0)

  const sales = useQuery({
    queryKey: ['sales', q, statusFilter, page],
    queryFn: async () => (await api.get<Page<Sale>>('/sales', {
      params: { q: q || undefined, status: statusFilter || undefined, page, size: 20 },
    })).data,
  })
  const clients = useQuery({
    queryKey: ['clients-all'],
    queryFn: async () => (await api.get<Page<Client>>('/clients', { params: { size: 500 } })).data.content,
  })
  const lots = useQuery({ queryKey: ['lots-all'], queryFn: async () => (await api.get<Lot[]>('/lots')).data })
  const paymentMethods = useQuery({
    queryKey: ['payment-methods'],
    queryFn: async () => (await api.get<NamedItem[]>('/lists/payment-methods')).data,
  })
  const correctionIndexes = useQuery({
    queryKey: ['correction-indexes'],
    queryFn: async () => (await api.get<NamedItem[]>('/lists/correction-indexes')).data,
  })
  const pmList = paymentMethods.data?.map((p) => p.name) ?? PAYMENT_METHODS_FALLBACK
  const ciList = correctionIndexes.data?.map((c) => c.name) ?? CORRECTION_INDEXES_FALLBACK

  const save = useMutation({
    mutationFn: async (payload: SaleForm) =>
      editingId ? api.put(`/sales/${editingId}`, payload) : api.post('/sales', payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['sales'] })
      queryClient.invalidateQueries({ queryKey: ['lots-all'] })
      queryClient.invalidateQueries({ queryKey: ['dashboard-analytics'] })
      setModalOpen(false); setForm(EMPTY)
      toast.success(editingId ? 'Venda atualizada com sucesso.' : 'Venda registrada e parcelas geradas.')
      setEditingId(null)
    },
    onError: (e) => setError(apiErrorMessage(e)),
  })

  const filtered = sales.data?.content ?? []  // filtrado/paginado no servidor

  // ---- helpers do formulário ----
  const selectedLot = lots.data?.find((l) => l.id === form.lotId)
  const expectedValue = selectedLot?.plannedValue ?? 0
  const entradaDisabled = form.purchaseType !== PURCHASE_WITH_DOWN

  function openNew() {
    setEditingId(null); setForm(EMPTY); setError(null); setModalOpen(true)
  }
  function openEdit(s: Sale) {
    setEditingId(s.id)
    setForm({
      clientId: s.clientId, lotId: s.lotId, totalValue: s.totalValue, downPayment: s.downPayment,
      installmentsCount: s.installmentsCount, firstDueDate: s.firstDueDate.slice(0, 10),
      purchaseType: s.purchaseType ?? PURCHASE_WITH_DOWN, paymentMethod: s.paymentMethod ?? 'Boleto',
      correctionIndex: s.correctionIndex ?? 'Sem correção',
      interestRate: s.interestRate ?? 0, penaltyRate: s.penaltyRate ?? 0,
    })
    setError(null); setModalOpen(true)
  }
  function onSelectLot(id: string) {
    const lot = lots.data?.find((l) => l.id === id)
    setForm((f) => ({ ...f, lotId: id, totalValue: f.totalValue || (lot?.plannedValue ?? 0) }))
  }
  function downloadContract(saleId: string) {
    fetch(`${api.defaults.baseURL}/contracts/sales/${saleId}/pdf`, { headers: { Authorization: `Bearer ${getToken()}` } })
      .then((r) => r.blob()).then((blob) => window.open(URL.createObjectURL(blob)))
  }

  const availableLotOptions = (lots.data ?? [])
    .filter((l) => l.status === 'AVAILABLE' || l.id === form.lotId)
    .map((l) => ({ value: l.id, label: l.label, hint: `${l.internalCode} · previsto ${formatCurrency(l.plannedValue)}` }))
  const clientOptions = (clients.data ?? []).map((c) => ({ value: c.id, label: c.name, hint: c.document }))

  return (
    <div>
      <PageHeader title="Vendas" subtitle="Vendas de lotes com geração automática de parcelas" action={canWrite && <Button onClick={openNew}>Nova venda</Button>} />

      <div className="mb-4 grid grid-cols-1 gap-3 sm:grid-cols-3">
        <Input placeholder="Buscar por cliente ou lote…" value={q} onChange={(e) => { setQ(e.target.value); setPage(0) }} />
        <Select value={statusFilter} onChange={(e) => { setStatusFilter(e.target.value); setPage(0) }}>
          <option value="">Todos os status</option>
          <option value="ACTIVE">Ativa</option>
          <option value="COMPLETED">Quitada</option>
          <option value="CANCELLED">Cancelada</option>
        </Select>
      </div>

      {sales.isLoading ? <TableSkeleton rows={6} cols={6} /> : (
        <Table headers={['Cliente', 'Lote', 'Total', 'Parcelas (Qtd / Pagas)', 'Status', 'Ações']}>
          {filtered.map((s) => (
            <Tr key={s.id}>
              <td className="px-4 py-2 font-medium">{s.clientName}</td>
              <td className="px-4 py-2">{s.propertyLabel}</td>
              <td className="px-4 py-2">{formatCurrency(s.totalValue)}</td>
              <td className="px-4 py-2">{s.installmentsCount} / <span className="font-medium text-green-600">{s.paidInstallments ?? 0}</span></td>
              <td className="px-4 py-2">
                <Badge dot color={s.status === 'ACTIVE' ? 'blue' : s.status === 'COMPLETED' ? 'green' : 'gray'}>
                  {SALE_STATUS[s.status] ?? s.status}
                </Badge>
              </td>
              <td className="px-4 py-2 text-right">
                <ActionsMenu items={[
                  { label: 'Visualizar venda', onClick: () => setView(s) },
                  ...(canWrite ? [{ label: 'Editar venda', onClick: () => openEdit(s) }] : []),
                  { label: 'Gerar contrato', onClick: () => downloadContract(s.id), disabled: !canContract },
                ]} />
              </td>
            </Tr>
          ))}
          {filtered.length === 0 && (
            <tr><td colSpan={6} className="p-0">
              <EmptyState
                icon={FileSignature}
                title="Nenhuma venda encontrada"
                description={q || statusFilter ? 'Ajuste a busca ou os filtros.' : 'Registre a primeira venda para gerar as parcelas automaticamente.'}
                action={canWrite && !q && !statusFilter ? <Button onClick={openNew}>Nova venda</Button> : undefined}
              />
            </td></tr>
          )}
        </Table>
      )}
      {sales.data && <Pagination page={sales.data.number} totalPages={sales.data.totalPages} totalElements={sales.data.totalElements} onChange={setPage} />}

      {/* Modal: visualizar venda (detalhes + parcelas + saldos) */}
      {view && (
        <Modal open onClose={() => setView(null)} title={`Venda — ${view.clientName}`}>
          <div className="mb-3 grid grid-cols-2 gap-2 text-sm">
            <Info label="Lote" value={view.propertyLabel} />
            <Info label="Forma de compra" value={view.purchaseType} />
            <Info label="Valor esperado" value={formatCurrency(view.expectedValue)} />
            <Info label="Valor vendido" value={formatCurrency(view.totalValue)} />
            <Info label="Entrada" value={formatCurrency(view.downPayment)} />
            <Info label="Forma de pagamento" value={view.paymentMethod} />
          </div>
          <div className="mb-2 text-sm">
            Pago: <span className="font-medium text-green-600">{formatCurrency(view.paidAmount ?? 0)}</span> ·
            {' '}Saldo devedor: <span className="font-medium text-amber-600">{formatCurrency(view.openAmount ?? 0)}</span>
          </div>
          <Table headers={['Nº', 'Vencimento', 'Valor', 'Atraso', 'Total atualizado', 'Status']}>
            {view.installments.map((i) => (
              <Tr key={i.id}>
                <td className="px-4 py-2">#{i.number}</td>
                <td className="px-4 py-2">{formatDate(i.dueDate)}</td>
                <td className="px-4 py-2">{formatCurrency(i.amount)}</td>
                <td className="px-4 py-2">{i.daysLate > 0 ? <span className="text-red-600">{i.daysLate}d</span> : '—'}</td>
                <td className="px-4 py-2 font-medium">
                  {i.daysLate > 0
                    ? <span title={`Multa ${formatCurrency(i.penaltyAmount)} + juros ${formatCurrency(i.interestAmount)}`}>{formatCurrency(i.updatedAmount)}</span>
                    : '—'}
                </td>
                <td className="px-4 py-2">
                  <Badge dot color={INSTALLMENT_STATUS[i.status]?.color ?? 'gray'}>
                    {INSTALLMENT_STATUS[i.status]?.label ?? i.status}
                  </Badge>
                </td>
              </Tr>
            ))}
          </Table>
        </Modal>
      )}

      {/* Modal: nova/editar venda */}
      <Modal open={modalOpen} onClose={() => setModalOpen(false)} title={editingId ? 'Editar venda' : 'Nova venda'}>
        <form className="space-y-3" onSubmit={(e) => { e.preventDefault(); save.mutate(form) }}>
          <Field label="Cliente">
            <Combobox options={clientOptions} value={form.clientId} disabled={!!editingId}
              onChange={(v) => setForm({ ...form, clientId: v })} placeholder="Buscar cliente…" />
          </Field>
          <Field label="Lote (disponíveis)">
            <Combobox options={availableLotOptions} value={form.lotId} disabled={!!editingId}
              onChange={onSelectLot} placeholder="Buscar lote…" />
          </Field>
          <div className="grid grid-cols-2 gap-3">
            <Field label="Valor esperado de venda (do lote)">
              <Input value={expectedValue ? formatCurrency(expectedValue) : '—'} readOnly disabled />
            </Field>
            <Field label="Valor que foi vendido">
              <Input type="number" step="0.01" value={form.totalValue} onChange={(e) => setForm({ ...form, totalValue: Number(e.target.value) })} required />
            </Field>
          </div>
          <div className="grid grid-cols-3 gap-3">
            <Field label="Forma de compra">
              <Select value={form.purchaseType} onChange={(e) => setForm({ ...form, purchaseType: e.target.value })}>
                {PURCHASE_TYPES.map((m) => <option key={m} value={m}>{m}</option>)}
              </Select>
            </Field>
            <Field label={`Entrada${entradaDisabled ? ' (só p/ entrada + parcelas)' : ''}`}>
              <Input type="number" step="0.01" value={entradaDisabled ? 0 : form.downPayment} disabled={entradaDisabled}
                onChange={(e) => setForm({ ...form, downPayment: Number(e.target.value) })} />
            </Field>
            <Field label="Qtd. parcelas">
              <Input type="number" min={0} value={form.installmentsCount} onChange={(e) => setForm({ ...form, installmentsCount: Number(e.target.value) })} required />
            </Field>
          </div>
          <div className="grid grid-cols-3 gap-3">
            <Field label="1º vencimento">
              <Input type="date" value={form.firstDueDate} onChange={(e) => setForm({ ...form, firstDueDate: e.target.value })} required />
            </Field>
            <Field label="Forma de pagamento">
              <Select value={form.paymentMethod} onChange={(e) => setForm({ ...form, paymentMethod: e.target.value })}>
                {pmList.map((m) => <option key={m} value={m}>{m}</option>)}
              </Select>
            </Field>
            <Field label="Índice de correção">
              <Select value={form.correctionIndex} onChange={(e) => setForm({ ...form, correctionIndex: e.target.value })}>
                {ciList.map((m) => <option key={m} value={m}>{m}</option>)}
              </Select>
            </Field>
          </div>
          <div className="grid grid-cols-2 gap-3">
            <Field label="Juros de mora (% ao mês)">
              <Input type="number" step="0.01" min={0} value={form.interestRate}
                onChange={(e) => setForm({ ...form, interestRate: Number(e.target.value) })} />
            </Field>
            <Field label="Multa por atraso (%)">
              <Input type="number" step="0.01" min={0} value={form.penaltyRate}
                onChange={(e) => setForm({ ...form, penaltyRate: Number(e.target.value) })} />
            </Field>
          </div>
          <p className="text-xs text-gray-400">
            Encargos calculados automaticamente nas parcelas vencidas: multa fixa + juros proporcionais aos dias de atraso.
          </p>
          {error && <p className="text-sm text-red-600">{error}</p>}
          <div className="flex justify-end gap-2 pt-2">
            <Button type="button" variant="outline" onClick={() => setModalOpen(false)}>Cancelar</Button>
            <Button type="submit" loading={save.isPending}>{editingId ? 'Salvar alterações' : 'Gerar venda e parcelas'}</Button>
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
