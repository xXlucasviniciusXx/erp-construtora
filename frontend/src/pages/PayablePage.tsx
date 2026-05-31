import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api, apiErrorMessage } from '@/lib/api'
import type { Page } from '@/lib/types'
import { useAuth } from '@/auth/AuthContext'
import { Badge, Button, Field, Input, Modal, PageHeader, Table } from '@/components/ui'
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
  costCenter?: string
}

const EMPTY: Partial<Payable> = { status: 'OPEN' }

export function PayablePage() {
  const { hasPermission } = useAuth()
  const queryClient = useQueryClient()
  const [modalOpen, setModalOpen] = useState(false)
  const [form, setForm] = useState<Partial<Payable>>(EMPTY)
  const [error, setError] = useState<string | null>(null)
  const canWrite = hasPermission('PAYABLE_WRITE')

  const { data, isLoading } = useQuery({
    queryKey: ['payable'],
    queryFn: async () => (await api.get<Page<Payable>>('/accounts-payable', { params: { size: 100 } })).data,
  })

  const save = useMutation({
    mutationFn: async (p: Partial<Payable>) => api.post('/accounts-payable', p),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['payable'] }); setModalOpen(false); setForm(EMPTY) },
    onError: (e) => setError(apiErrorMessage(e)),
  })

  const pay = useMutation({
    mutationFn: async (id: string) => api.post(`/accounts-payable/${id}/pay`),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['payable'] }),
  })

  return (
    <div>
      <PageHeader title="Contas a Pagar" action={canWrite && <Button onClick={() => { setForm(EMPTY); setModalOpen(true) }}>Nova conta</Button>} />
      {isLoading ? <p className="text-gray-500">Carregando…</p> : (
        <Table headers={['Fornecedor', 'Categoria', 'Valor', 'Vencimento', 'Status', '']}>
          {data?.content.map((p) => (
            <tr key={p.id} className="hover:bg-gray-50">
              <td className="px-4 py-2 font-medium">{p.supplier}</td>
              <td className="px-4 py-2">{p.category ?? '—'}</td>
              <td className="px-4 py-2">{formatCurrency(p.amount)}</td>
              <td className="px-4 py-2">{formatDate(p.dueDate)}</td>
              <td className="px-4 py-2"><Badge color={p.status === 'PAID' ? 'green' : p.status === 'OVERDUE' ? 'red' : 'gray'}>{p.status}</Badge></td>
              <td className="px-4 py-2 text-right">
                {canWrite && p.status !== 'PAID' && <Button variant="ghost" onClick={() => pay.mutate(p.id)}>Confirmar pgto.</Button>}
              </td>
            </tr>
          ))}
          {data?.content.length === 0 && <tr><td colSpan={6} className="px-4 py-6 text-center text-gray-400">Nenhuma conta.</td></tr>}
        </Table>
      )}

      <Modal open={modalOpen} onClose={() => setModalOpen(false)} title="Nova conta a pagar">
        <form className="space-y-3" onSubmit={(e) => { e.preventDefault(); save.mutate(form) }}>
          <Field label="Fornecedor"><Input value={form.supplier ?? ''} onChange={(e) => setForm({ ...form, supplier: e.target.value })} required /></Field>
          <div className="grid grid-cols-2 gap-3">
            <Field label="Categoria"><Input value={form.category ?? ''} onChange={(e) => setForm({ ...form, category: e.target.value })} /></Field>
            <Field label="Centro de custo"><Input value={form.costCenter ?? ''} onChange={(e) => setForm({ ...form, costCenter: e.target.value })} /></Field>
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
