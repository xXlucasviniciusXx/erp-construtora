import { useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api, apiErrorMessage, getToken } from '@/lib/api'
import type { Client, Page, Property, Sale } from '@/lib/types'
import { useAuth } from '@/auth/AuthContext'
import { ActionsMenu } from '@/components/Menu'
import { Badge, Button, Field, Input, Modal, PageHeader, Select, Table } from '@/components/ui'
import { formatCurrency, formatDate } from '@/lib/utils'

interface SaleForm {
  clientId: string
  propertyId: string
  totalValue: number
  downPayment: number
  installmentsCount: number
  firstDueDate: string
  purchaseType: string
  paymentMethod: string
  correctionIndex: string
}

const EMPTY: SaleForm = {
  clientId: '', propertyId: '', totalValue: 0, downPayment: 0,
  installmentsCount: 12, firstDueDate: '', purchaseType: 'Entrada + parcelas',
  paymentMethod: 'Boleto', correctionIndex: 'Sem correção',
}

// TODO: futuramente carregar de um cadastro configurável.
const PURCHASE_TYPES = ['À vista', 'Entrada + parcelas', 'Financiamento próprio', 'Outro']
const PAYMENT_METHODS = ['Boleto', 'PIX', 'Transferência bancária', 'Cartão', 'Dinheiro', 'Outro']
const CORRECTION_INDEXES = ['Sem correção', 'INCC', 'IPCA', 'IGP-M', 'Juros fixo mensal', 'Outro']
const SALE_STATUS: Record<string, string> = { ACTIVE: 'Ativa', COMPLETED: 'Quitada', CANCELLED: 'Cancelada' }

export function SalesPage() {
  const { hasPermission } = useAuth()
  const queryClient = useQueryClient()
  const [modalOpen, setModalOpen] = useState(false)
  const [form, setForm] = useState<SaleForm>(EMPTY)
  const [error, setError] = useState<string | null>(null)
  const [viewSale, setViewSale] = useState<Sale | null>(null)
  const [q, setQ] = useState('')
  const [statusFilter, setStatusFilter] = useState('')
  const canWrite = hasPermission('SALES_WRITE')
  const canContract = hasPermission('CONTRACTS_GENERATE')

  const sales = useQuery({ queryKey: ['sales'], queryFn: async () => (await api.get<Sale[]>('/sales')).data })
  const clients = useQuery({
    queryKey: ['clients-all'],
    queryFn: async () => (await api.get<Page<Client>>('/clients', { params: { size: 200 } })).data.content,
  })
  const properties = useQuery({
    queryKey: ['properties-all'],
    queryFn: async () => (await api.get<Page<Property>>('/properties', { params: { size: 200 } })).data.content,
  })

  const save = useMutation({
    mutationFn: async (payload: SaleForm) => api.post('/sales', payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['sales'] })
      queryClient.invalidateQueries({ queryKey: ['properties-all'] })
      setModalOpen(false); setForm(EMPTY)
    },
    onError: (e) => setError(apiErrorMessage(e)),
  })

  const filtered = useMemo(() => {
    let list = sales.data ?? []
    if (q) {
      const t = q.toLowerCase()
      list = list.filter((s) => s.clientName.toLowerCase().includes(t) || s.propertyLabel.toLowerCase().includes(t))
    }
    if (statusFilter) list = list.filter((s) => s.status === statusFilter)
    return list
  }, [sales.data, q, statusFilter])

  function onSelectProperty(id: string) {
    const prop = properties.data?.find((p) => p.id === id)
    setForm((f) => ({ ...f, propertyId: id, totalValue: prop?.saleValue ?? f.totalValue }))
  }

  function downloadContract(saleId: string) {
    fetch(`${api.defaults.baseURL}/contracts/sales/${saleId}/pdf`, { headers: { Authorization: `Bearer ${getToken()}` } })
      .then((r) => r.blob()).then((blob) => window.open(URL.createObjectURL(blob)))
  }

  return (
    <div>
      <PageHeader
        title="Vendas"
        action={canWrite && <Button onClick={() => { setForm(EMPTY); setError(null); setModalOpen(true) }}>Nova venda</Button>}
      />

      <div className="mb-4 grid grid-cols-1 gap-3 sm:grid-cols-3">
        <Input placeholder="Buscar por cliente ou imóvel…" value={q} onChange={(e) => setQ(e.target.value)} />
        <Select value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)}>
          <option value="">Todos os status</option>
          <option value="ACTIVE">Ativa</option>
          <option value="COMPLETED">Quitada</option>
          <option value="CANCELLED">Cancelada</option>
        </Select>
      </div>

      {sales.isLoading ? <p className="text-gray-500">Carregando…</p> : (
        <Table headers={['Cliente', 'Imóvel', 'Total', 'Parcelas (Qtd / Pagas)', 'Status', 'Ações']}>
          {filtered.map((s) => (
            <tr key={s.id} className="hover:bg-gray-50">
              <td className="px-4 py-2 font-medium">{s.clientName}</td>
              <td className="px-4 py-2">{s.propertyLabel}</td>
              <td className="px-4 py-2">{formatCurrency(s.totalValue)}</td>
              <td className="px-4 py-2">{s.installmentsCount} / <span className="font-medium text-green-600">{s.paidInstallments ?? 0}</span></td>
              <td className="px-4 py-2"><Badge color="blue">{SALE_STATUS[s.status] ?? s.status}</Badge></td>
              <td className="px-4 py-2 text-right">
                <ActionsMenu items={[
                  { label: 'Ver parcelas', onClick: () => setViewSale(s) },
                  { label: 'Gerar contrato', onClick: () => downloadContract(s.id), disabled: !canContract },
                ]} />
              </td>
            </tr>
          ))}
          {filtered.length === 0 && (
            <tr><td colSpan={6} className="px-4 py-6 text-center text-gray-400">Nenhuma venda encontrada.</td></tr>
          )}
        </Table>
      )}

      {/* Modal: ver parcelas (consulta à venda) */}
      {viewSale && (
        <Modal open onClose={() => setViewSale(null)} title={`Parcelas — ${viewSale.clientName}`}>
          <div className="mb-3 text-sm text-gray-500 dark:text-gray-400">
            {viewSale.propertyLabel} · {formatCurrency(viewSale.totalValue)} ·
            {' '}Pago: <span className="font-medium text-green-600">{formatCurrency(viewSale.paidAmount ?? 0)}</span> ·
            {' '}Saldo devedor: <span className="font-medium text-amber-600">{formatCurrency(viewSale.openAmount ?? 0)}</span>
          </div>
          <Table headers={['Nº', 'Vencimento', 'Valor', 'Status']}>
            {viewSale.installments.map((i) => (
              <tr key={i.id} className="hover:bg-gray-50">
                <td className="px-4 py-2">#{i.number}</td>
                <td className="px-4 py-2">{formatDate(i.dueDate)}</td>
                <td className="px-4 py-2">{formatCurrency(i.amount)}</td>
                <td className="px-4 py-2"><Badge color={i.status === 'PAID' ? 'green' : i.status === 'OVERDUE' ? 'red' : 'gray'}>{i.status}</Badge></td>
              </tr>
            ))}
          </Table>
        </Modal>
      )}

      {/* Modal: nova venda */}
      <Modal open={modalOpen} onClose={() => setModalOpen(false)} title="Nova venda">
        <form className="space-y-3" onSubmit={(e) => { e.preventDefault(); save.mutate(form) }}>
          <Field label="Cliente">
            <Select value={form.clientId} onChange={(e) => setForm({ ...form, clientId: e.target.value })} required>
              <option value="">Selecione…</option>
              {clients.data?.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
            </Select>
          </Field>
          <Field label="Imóvel (disponíveis)">
            <Select value={form.propertyId} onChange={(e) => onSelectProperty(e.target.value)} required>
              <option value="">Selecione…</option>
              {properties.data?.filter((p) => p.status === 'AVAILABLE').map((p) => (
                <option key={p.id} value={p.id}>{p.development} — {[p.block, p.lot, p.unit].filter(Boolean).join('/')}</option>
              ))}
            </Select>
          </Field>
          <div className="grid grid-cols-2 gap-3">
            <Field label="Valor total"><Input type="number" step="0.01" value={form.totalValue} onChange={(e) => setForm({ ...form, totalValue: Number(e.target.value) })} required /></Field>
            <Field label="Entrada"><Input type="number" step="0.01" value={form.downPayment} onChange={(e) => setForm({ ...form, downPayment: Number(e.target.value) })} /></Field>
          </div>
          <div className="grid grid-cols-2 gap-3">
            <Field label="Qtd. parcelas"><Input type="number" min={0} value={form.installmentsCount} onChange={(e) => setForm({ ...form, installmentsCount: Number(e.target.value) })} required /></Field>
            <Field label="1º vencimento"><Input type="date" value={form.firstDueDate} onChange={(e) => setForm({ ...form, firstDueDate: e.target.value })} required /></Field>
          </div>
          <div className="grid grid-cols-3 gap-3">
            <Field label="Forma de compra">
              <Select value={form.purchaseType} onChange={(e) => setForm({ ...form, purchaseType: e.target.value })}>
                {PURCHASE_TYPES.map((m) => <option key={m} value={m}>{m}</option>)}
              </Select>
            </Field>
            <Field label="Forma de pagamento">
              <Select value={form.paymentMethod} onChange={(e) => setForm({ ...form, paymentMethod: e.target.value })}>
                {PAYMENT_METHODS.map((m) => <option key={m} value={m}>{m}</option>)}
              </Select>
            </Field>
            <Field label="Índice de correção">
              <Select value={form.correctionIndex} onChange={(e) => setForm({ ...form, correctionIndex: e.target.value })}>
                {CORRECTION_INDEXES.map((m) => <option key={m} value={m}>{m}</option>)}
              </Select>
            </Field>
          </div>
          {error && <p className="text-sm text-red-600">{error}</p>}
          <div className="flex justify-end gap-2 pt-2">
            <Button type="button" variant="outline" onClick={() => setModalOpen(false)}>Cancelar</Button>
            <Button type="submit" disabled={save.isPending}>Gerar venda e parcelas</Button>
          </div>
        </form>
      </Modal>
    </div>
  )
}
