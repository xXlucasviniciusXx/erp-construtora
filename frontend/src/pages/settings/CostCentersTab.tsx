import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api, apiErrorMessage } from '@/lib/api'
import type { CostCenter } from '@/lib/types'
import { ActionsMenu } from '@/components/Menu'
import { useToast } from '@/components/Toast'
import { useConfirm } from '@/components/Confirm'
import { Badge, Button, Field, Input, Modal, Table, TableSkeleton, Tr } from '@/components/ui'

const EMPTY: Partial<CostCenter> = { active: true }

export function CostCentersTab() {
  const queryClient = useQueryClient()
  const toast = useToast()
  const confirm = useConfirm()
  const [modalOpen, setModalOpen] = useState(false)
  const [form, setForm] = useState<Partial<CostCenter>>(EMPTY)
  const [error, setError] = useState<string | null>(null)

  const { data, isLoading } = useQuery({
    queryKey: ['cost-centers'],
    queryFn: async () => (await api.get<CostCenter[]>('/cost-centers')).data,
  })

  const save = useMutation({
    mutationFn: async (p: Partial<CostCenter>) =>
      p.id ? api.put(`/cost-centers/${p.id}`, p) : api.post('/cost-centers', p),
    onSuccess: (_d, p) => { queryClient.invalidateQueries({ queryKey: ['cost-centers'] }); setModalOpen(false); setForm(EMPTY); toast.success(p.id ? 'Centro de custo atualizado.' : 'Centro de custo criado.') },
    onError: (e) => setError(apiErrorMessage(e)),
  })
  const remove = useMutation({
    mutationFn: async (id: string) => api.delete(`/cost-centers/${id}`),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['cost-centers'] }); toast.success('Centro de custo removido.') },
    onError: (e) => toast.error(apiErrorMessage(e)),
  })

  async function confirmRemove(c: CostCenter) {
    if (await confirm({ title: 'Remover centro de custo', message: `Remover "${c.name}"?`, confirmLabel: 'Remover', danger: true })) remove.mutate(c.id)
  }

  return (
    <div>
      <div className="mb-3 flex justify-end">
        <Button onClick={() => { setForm(EMPTY); setError(null); setModalOpen(true) }}>Novo centro de custo</Button>
      </div>
      {isLoading ? <TableSkeleton rows={4} cols={4} /> : (
        <Table headers={['Nome', 'Descrição', 'Status', 'Ações']}>
          {data?.map((c) => (
            <Tr key={c.id}>
              <td className="px-4 py-2 font-medium">{c.name}</td>
              <td className="px-4 py-2">{c.description ?? '—'}</td>
              <td className="px-4 py-2"><Badge dot color={c.active ? 'green' : 'gray'}>{c.active ? 'Ativo' : 'Inativo'}</Badge></td>
              <td className="px-4 py-2 text-right">
                <ActionsMenu items={[
                  { label: 'Editar', onClick: () => { setForm(c); setError(null); setModalOpen(true) } },
                  { label: 'Remover', danger: true, onClick: () => confirmRemove(c) },
                ]} />
              </td>
            </Tr>
          ))}
          {data?.length === 0 && <tr><td colSpan={4} className="px-4 py-6 text-center text-sm text-gray-400">Nenhum centro de custo.</td></tr>}
        </Table>
      )}

      <Modal open={modalOpen} onClose={() => setModalOpen(false)} title={form.id ? 'Editar centro de custo' : 'Novo centro de custo'}>
        <form className="space-y-3" onSubmit={(e) => { e.preventDefault(); save.mutate(form) }}>
          <Field label="Nome"><Input value={form.name ?? ''} onChange={(e) => setForm({ ...form, name: e.target.value })} required /></Field>
          <Field label="Descrição"><Input value={form.description ?? ''} onChange={(e) => setForm({ ...form, description: e.target.value })} /></Field>
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
