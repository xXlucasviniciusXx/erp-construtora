import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api, apiErrorMessage } from '@/lib/api'
import type { BankAccount } from '@/lib/types'
import { ActionsMenu } from '@/components/Menu'
import { useToast } from '@/components/Toast'
import { useConfirm } from '@/components/Confirm'
import { Badge, Button, Field, Input, Modal, Table, TableSkeleton, Tr } from '@/components/ui'
import { formatCurrency } from '@/lib/utils'

const EMPTY: Partial<BankAccount> = { active: true, initialBalance: 0 }

export function BankAccountsTab() {
  const queryClient = useQueryClient()
  const toast = useToast()
  const confirm = useConfirm()
  const [modalOpen, setModalOpen] = useState(false)
  const [form, setForm] = useState<Partial<BankAccount>>(EMPTY)
  const [error, setError] = useState<string | null>(null)

  const { data, isLoading } = useQuery({
    queryKey: ['bank-accounts'],
    queryFn: async () => (await api.get<BankAccount[]>('/bank-accounts')).data,
  })

  const save = useMutation({
    mutationFn: async (p: Partial<BankAccount>) =>
      p.id ? api.put(`/bank-accounts/${p.id}`, p) : api.post('/bank-accounts', p),
    onSuccess: (_d, p) => { queryClient.invalidateQueries({ queryKey: ['bank-accounts'] }); setModalOpen(false); setForm(EMPTY); toast.success(p.id ? 'Conta bancária atualizada.' : 'Conta bancária criada.') },
    onError: (e) => setError(apiErrorMessage(e)),
  })
  const remove = useMutation({
    mutationFn: async (id: string) => api.delete(`/bank-accounts/${id}`),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['bank-accounts'] }); toast.success('Conta bancária removida.') },
    onError: (e) => toast.error(apiErrorMessage(e)),
  })

  async function confirmRemove(a: BankAccount) {
    if (await confirm({ title: 'Remover conta bancária', message: `Remover "${a.name}"?`, confirmLabel: 'Remover', danger: true })) remove.mutate(a.id)
  }

  return (
    <div>
      <div className="mb-3 flex justify-end">
        <Button onClick={() => { setForm(EMPTY); setError(null); setModalOpen(true) }}>Nova conta bancária</Button>
      </div>
      {isLoading ? <TableSkeleton rows={4} cols={7} /> : (
        <Table headers={['Nome', 'Banco', 'Agência', 'Conta', 'Saldo inicial', 'Status', 'Ações']}>
          {data?.map((a) => (
            <Tr key={a.id}>
              <td className="px-4 py-2 font-medium">{a.name}</td>
              <td className="px-4 py-2">{[a.bankCode, a.bankName].filter(Boolean).join(' - ') || '—'}</td>
              <td className="px-4 py-2">{a.agency ?? '—'}</td>
              <td className="px-4 py-2">{a.accountNumber ?? '—'}</td>
              <td className="px-4 py-2">{formatCurrency(a.initialBalance)}</td>
              <td className="px-4 py-2"><Badge dot color={a.active ? 'green' : 'gray'}>{a.active ? 'Ativa' : 'Inativa'}</Badge></td>
              <td className="px-4 py-2 text-right">
                <ActionsMenu items={[
                  { label: 'Editar', onClick: () => { setForm(a); setError(null); setModalOpen(true) } },
                  { label: 'Remover', danger: true, onClick: () => confirmRemove(a) },
                ]} />
              </td>
            </Tr>
          ))}
          {data?.length === 0 && <tr><td colSpan={7} className="px-4 py-6 text-center text-sm text-gray-400">Nenhuma conta bancária.</td></tr>}
        </Table>
      )}

      <Modal open={modalOpen} onClose={() => setModalOpen(false)} title={form.id ? 'Editar conta bancária' : 'Nova conta bancária'}>
        <form className="space-y-3" onSubmit={(e) => { e.preventDefault(); save.mutate(form) }}>
          <Field label="Nome (apelido)"><Input value={form.name ?? ''} onChange={(e) => setForm({ ...form, name: e.target.value })} required /></Field>
          <div className="grid grid-cols-2 gap-3">
            <Field label="Código do banco"><Input value={form.bankCode ?? ''} onChange={(e) => setForm({ ...form, bankCode: e.target.value })} /></Field>
            <Field label="Nome do banco"><Input value={form.bankName ?? ''} onChange={(e) => setForm({ ...form, bankName: e.target.value })} /></Field>
          </div>
          <div className="grid grid-cols-3 gap-3">
            <Field label="Agência"><Input value={form.agency ?? ''} onChange={(e) => setForm({ ...form, agency: e.target.value })} /></Field>
            <Field label="Conta"><Input value={form.accountNumber ?? ''} onChange={(e) => setForm({ ...form, accountNumber: e.target.value })} /></Field>
            <Field label="Saldo inicial"><Input type="number" step="0.01" value={form.initialBalance ?? 0} onChange={(e) => setForm({ ...form, initialBalance: Number(e.target.value) })} /></Field>
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
