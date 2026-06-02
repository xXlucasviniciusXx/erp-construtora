import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api, apiErrorMessage } from '@/lib/api'
import type { Category } from '@/lib/types'
import { ActionsMenu } from '@/components/Menu'
import { useToast } from '@/components/Toast'
import { useConfirm } from '@/components/Confirm'
import { Badge, Button, Field, Input, Modal, Table, TableSkeleton, Tr } from '@/components/ui'

const EMPTY: Partial<Category> = { active: true }

export function CategoriesTab() {
  const queryClient = useQueryClient()
  const toast = useToast()
  const confirm = useConfirm()
  const [modalOpen, setModalOpen] = useState(false)
  const [form, setForm] = useState<Partial<Category>>(EMPTY)
  const [error, setError] = useState<string | null>(null)
  const [q, setQ] = useState('')

  const { data, isLoading } = useQuery({
    queryKey: ['categories'],
    queryFn: async () => (await api.get<Category[]>('/categories')).data,
  })

  const save = useMutation({
    mutationFn: async (p: Partial<Category>) =>
      p.id ? api.put(`/categories/${p.id}`, p) : api.post('/categories', p),
    onSuccess: (_d, p) => { queryClient.invalidateQueries({ queryKey: ['categories'] }); setModalOpen(false); setForm(EMPTY); toast.success(p.id ? 'Categoria atualizada.' : 'Categoria criada.') },
    onError: (e) => setError(apiErrorMessage(e)),
  })
  const remove = useMutation({
    mutationFn: async (id: string) => api.delete(`/categories/${id}`),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['categories'] }); toast.success('Categoria removida.') },
    onError: (e) => toast.error(apiErrorMessage(e)),
  })

  async function confirmRemove(c: Category) {
    if (await confirm({ title: 'Remover categoria', message: `Remover "${c.grupo} / ${c.name}"?`, confirmLabel: 'Remover', danger: true })) remove.mutate(c.id)
  }

  const filtered = (data ?? []).filter((c) => {
    if (!q) return true
    const t = q.toLowerCase()
    return c.name.toLowerCase().includes(t) || c.grupo.toLowerCase().includes(t)
  })

  return (
    <div>
      <div className="mb-3 flex items-center justify-between gap-3">
        <Input className="max-w-xs" placeholder="Buscar por grupo ou categoria…" value={q} onChange={(e) => setQ(e.target.value)} />
        <Button onClick={() => { setForm(EMPTY); setError(null); setModalOpen(true) }}>Nova categoria</Button>
      </div>
      {isLoading ? <TableSkeleton rows={6} cols={4} /> : (
        <Table headers={['Grupo', 'Categoria', 'Status', 'Ações']}>
          {filtered.map((c) => (
            <Tr key={c.id}>
              <td className="px-4 py-2 text-gray-500">{c.grupo}</td>
              <td className="px-4 py-2 font-medium">{c.name}</td>
              <td className="px-4 py-2"><Badge dot color={c.active ? 'green' : 'gray'}>{c.active ? 'Ativa' : 'Inativa'}</Badge></td>
              <td className="px-4 py-2 text-right">
                <ActionsMenu items={[
                  { label: 'Editar', onClick: () => { setForm(c); setError(null); setModalOpen(true) } },
                  { label: 'Remover', danger: true, onClick: () => confirmRemove(c) },
                ]} />
              </td>
            </Tr>
          ))}
          {filtered.length === 0 && <tr><td colSpan={4} className="px-4 py-6 text-center text-sm text-gray-400">Nenhuma categoria.</td></tr>}
        </Table>
      )}

      <Modal open={modalOpen} onClose={() => setModalOpen(false)} title={form.id ? 'Editar categoria' : 'Nova categoria'}>
        <form className="space-y-3" onSubmit={(e) => { e.preventDefault(); save.mutate(form) }}>
          <Field label="Grupo">
            <Input list="category-groups" value={form.grupo ?? ''} onChange={(e) => setForm({ ...form, grupo: e.target.value })} placeholder="Ex.: Infraestrutura e Obras" required />
            <datalist id="category-groups">
              {[...new Set((data ?? []).map((c) => c.grupo))].map((g) => <option key={g} value={g} />)}
            </datalist>
          </Field>
          <Field label="Categoria (item)"><Input value={form.name ?? ''} onChange={(e) => setForm({ ...form, name: e.target.value })} placeholder="Ex.: Terraplanagem" required /></Field>
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
