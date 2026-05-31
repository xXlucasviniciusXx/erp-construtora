import { Fragment, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api, apiErrorMessage, getToken } from '@/lib/api'
import type { Client, Page, Property, Sale } from '@/lib/types'
import { useAuth } from '@/auth/AuthContext'
import { Badge, Button, Field, Input, Modal, PageHeader, Select, Table } from '@/components/ui'
import { formatCurrency, formatDate } from '@/lib/utils'

interface SaleForm {
  clientId: string
  propertyId: string
  totalValue: number
  downPayment: number
  installmentsCount: number
  firstDueDate: string
  paymentMethod: string
  correctionIndex: string
}

const EMPTY: SaleForm = {
  clientId: '', propertyId: '', totalValue: 0, downPayment: 0,
  installmentsCount: 12, firstDueDate: '', paymentMethod: 'Boleto', correctionIndex: '',
}

export function SalesPage() {
  const { hasPermission } = useAuth()
  const queryClient = useQueryClient()
  const [modalOpen, setModalOpen] = useState(false)
  const [form, setForm] = useState<SaleForm>(EMPTY)
  const [error, setError] = useState<string | null>(null)
  const [expanded, setExpanded] = useState<string | null>(null)
  const canWrite = hasPermission('SALES_WRITE')

  const sales = useQuery({ queryKey: ['sales'], queryFn: async () => (await api.get<Sale[]>('/sales')).data })
  const clients = useQuery({
    queryKey: ['clients-all'],
    queryFn: async () => (await api.get<Page<Client>>('/clients', { params: { size: 100 } })).data.content,
  })
  const properties = useQuery({
    queryKey: ['properties-all'],
    queryFn: async () => (await api.get<Page<Property>>('/properties', { params: { size: 100 } })).data.content,
  })

  const save = useMutation({
    mutationFn: async (payload: SaleForm) => api.post('/sales', payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['sales'] })
      queryClient.invalidateQueries({ queryKey: ['properties-all'] })
      setModalOpen(false)
      setForm(EMPTY)
    },
    onError: (e) => setError(apiErrorMessage(e)),
  })

  function onSelectProperty(id: string) {
    const prop = properties.data?.find((p) => p.id === id)
    setForm((f) => ({ ...f, propertyId: id, totalValue: prop?.saleValue ?? f.totalValue }))
  }

  function downloadContract(saleId: string) {
    // PDF protegido por JWT: abre via fetch com Authorization e blob
    fetch(`${api.defaults.baseURL}/contracts/sales/${saleId}/pdf`, {
      headers: { Authorization: `Bearer ${getToken()}` },
    })
      .then((r) => r.blob())
      .then((blob) => window.open(URL.createObjectURL(blob)))
  }

  return (
    <div>
      <PageHeader
        title="Vendas"
        action={canWrite && <Button onClick={() => { setForm(EMPTY); setError(null); setModalOpen(true) }}>Nova venda</Button>}
      />

      {sales.isLoading ? (
        <p className="text-gray-500">Carregando…</p>
      ) : (
        <Table headers={['Cliente', 'Imóvel', 'Total', 'Parcelas', 'Status', '']}>
          {sales.data?.map((s) => (
            <Fragment key={s.id}>
              <tr className="hover:bg-gray-50">
                <td className="px-4 py-2 font-medium">{s.clientName}</td>
                <td className="px-4 py-2">{s.propertyLabel}</td>
                <td className="px-4 py-2">{formatCurrency(s.totalValue)}</td>
                <td className="px-4 py-2">{s.installmentsCount}x</td>
                <td className="px-4 py-2"><Badge color="blue">{s.status}</Badge></td>
                <td className="px-4 py-2 text-right">
                  <Button variant="ghost" onClick={() => setExpanded(expanded === s.id ? null : s.id)}>
                    {expanded === s.id ? 'Ocultar' : 'Parcelas'}
                  </Button>
                  <Button variant="ghost" onClick={() => downloadContract(s.id)}>Contrato</Button>
                </td>
              </tr>
              {expanded === s.id && (
                <tr>
                  <td colSpan={6} className="bg-gray-50 px-4 py-3">
                    <div className="grid grid-cols-1 gap-1 sm:grid-cols-2 lg:grid-cols-3">
                      {s.installments.map((i) => (
                        <div key={i.id} className="flex justify-between rounded border border-gray-200 bg-white px-3 py-1 text-xs">
                          <span>#{i.number} — {formatDate(i.dueDate)}</span>
                          <span className="font-medium">{formatCurrency(i.amount)}</span>
                          <Badge color={i.status === 'PAID' ? 'green' : i.status === 'OVERDUE' ? 'red' : 'gray'}>{i.status}</Badge>
                        </div>
                      ))}
                    </div>
                  </td>
                </tr>
              )}
            </Fragment>
          ))}
          {sales.data?.length === 0 && (
            <tr><td colSpan={6} className="px-4 py-6 text-center text-gray-400">Nenhuma venda registrada.</td></tr>
          )}
        </Table>
      )}

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
            <Field label="Valor total">
              <Input type="number" step="0.01" value={form.totalValue} onChange={(e) => setForm({ ...form, totalValue: Number(e.target.value) })} required />
            </Field>
            <Field label="Entrada">
              <Input type="number" step="0.01" value={form.downPayment} onChange={(e) => setForm({ ...form, downPayment: Number(e.target.value) })} />
            </Field>
          </div>
          <div className="grid grid-cols-2 gap-3">
            <Field label="Qtd. parcelas">
              <Input type="number" min={0} value={form.installmentsCount} onChange={(e) => setForm({ ...form, installmentsCount: Number(e.target.value) })} required />
            </Field>
            <Field label="1º vencimento">
              <Input type="date" value={form.firstDueDate} onChange={(e) => setForm({ ...form, firstDueDate: e.target.value })} required />
            </Field>
          </div>
          <div className="grid grid-cols-2 gap-3">
            <Field label="Forma de pagamento">
              <Input value={form.paymentMethod} onChange={(e) => setForm({ ...form, paymentMethod: e.target.value })} />
            </Field>
            <Field label="Índice de correção">
              <Input placeholder="IGPM, INCC…" value={form.correctionIndex} onChange={(e) => setForm({ ...form, correctionIndex: e.target.value })} />
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
