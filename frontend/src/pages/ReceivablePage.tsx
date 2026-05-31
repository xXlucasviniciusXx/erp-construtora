import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api, apiErrorMessage } from '@/lib/api'
import type { Client, Page } from '@/lib/types'
import { useAuth } from '@/auth/AuthContext'
import { Badge, Button, Field, Input, Modal, PageHeader, Select, Table } from '@/components/ui'
import { formatCurrency, formatDate } from '@/lib/utils'

interface Receivable {
  id: string
  clientId?: string
  clientName?: string
  description?: string
  amount: number
  dueDate: string
  receiveDate?: string
  status: 'OPEN' | 'RECEIVED' | 'OVERDUE' | 'CANCELLED'
}

const EMPTY: Partial<Receivable> = { status: 'OPEN' }

export function ReceivablePage() {
  const { hasPermission } = useAuth()
  const queryClient = useQueryClient()
  const [modalOpen, setModalOpen] = useState(false)
  const [form, setForm] = useState<Partial<Receivable>>(EMPTY)
  const [error, setError] = useState<string | null>(null)
  const canWrite = hasPermission('RECEIVABLE_WRITE')

  const { data, isLoading } = useQuery({
    queryKey: ['receivable'],
    queryFn: async () => (await api.get<Page<Receivable>>('/accounts-receivable', { params: { size: 100 } })).data,
  })
  const clients = useQuery({
    queryKey: ['clients-all'],
    queryFn: async () => (await api.get<Page<Client>>('/clients', { params: { size: 100 } })).data.content,
  })

  const save = useMutation({
    mutationFn: async (p: Partial<Receivable>) => api.post('/accounts-receivable', p),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['receivable'] }); setModalOpen(false); setForm(EMPTY) },
    onError: (e) => setError(apiErrorMessage(e)),
  })
  const receive = useMutation({
    mutationFn: async (id: string) => api.post(`/accounts-receivable/${id}/receive`),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['receivable'] }),
  })

  return (
    <div>
      <PageHeader title="Contas a Receber" action={canWrite && <Button onClick={() => { setForm(EMPTY); setModalOpen(true) }}>Nova conta</Button>} />
      {isLoading ? <p className="text-gray-500">Carregando…</p> : (
        <Table headers={['Cliente', 'Descrição', 'Valor', 'Vencimento', 'Status', '']}>
          {data?.content.map((r) => (
            <tr key={r.id} className="hover:bg-gray-50">
              <td className="px-4 py-2 font-medium">{r.clientName ?? '—'}</td>
              <td className="px-4 py-2">{r.description ?? '—'}</td>
              <td className="px-4 py-2">{formatCurrency(r.amount)}</td>
              <td className="px-4 py-2">{formatDate(r.dueDate)}</td>
              <td className="px-4 py-2"><Badge color={r.status === 'RECEIVED' ? 'green' : r.status === 'OVERDUE' ? 'red' : 'gray'}>{r.status}</Badge></td>
              <td className="px-4 py-2 text-right">
                {canWrite && r.status !== 'RECEIVED' && <Button variant="ghost" onClick={() => receive.mutate(r.id)}>Confirmar receb.</Button>}
              </td>
            </tr>
          ))}
          {data?.content.length === 0 && <tr><td colSpan={6} className="px-4 py-6 text-center text-gray-400">Nenhuma conta.</td></tr>}
        </Table>
      )}

      <Modal open={modalOpen} onClose={() => setModalOpen(false)} title="Nova conta a receber">
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
