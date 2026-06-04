import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api, apiErrorMessage } from '@/lib/api'
import type { NamedItem } from '@/lib/types'
import { ActionsMenu } from '@/components/Menu'
import { useToast } from '@/components/Toast'
import { useConfirm } from '@/components/Confirm'
import { Badge, Button, Field, Input, Modal, Select, Table, TableSkeleton, Tr } from '@/components/ui'
import { cn } from '@/lib/utils'

/** Definição de cada lista configurável (endpoint + rótulos). */
const LISTS = [
  { key: 'payment-methods',    label: 'Formas de pagamento', singular: 'forma de pagamento' },
  { key: 'correction-indexes', label: 'Índices de correção', singular: 'índice de correção' },
  { key: 'supplier-categories', label: 'Categorias de fornecedor', singular: 'categoria de fornecedor' },
] as const

type ListKey = (typeof LISTS)[number]['key']

export function ListsTab() {
  const [active, setActive] = useState<ListKey>('payment-methods')
  const current = LISTS.find((l) => l.key === active)!

  return (
    <div>
      <p className="mb-3 text-sm text-gray-500 dark:text-gray-400">
        Listas usadas nos formulários do sistema. Adicione, edite ou desative os itens.
      </p>

      {/* Sub-abas */}
      <div className="mb-4 inline-flex rounded-lg border border-gray-200 bg-gray-50 p-0.5 dark:border-gray-700 dark:bg-gray-800">
        {LISTS.map((l) => (
          <button
            key={l.key}
            onClick={() => setActive(l.key)}
            className={cn(
              'rounded-md px-3 py-1.5 text-sm font-medium transition',
              active === l.key
                ? 'bg-white text-gray-900 shadow-sm dark:bg-gray-700 dark:text-gray-100'
                : 'text-gray-500 hover:text-gray-700 dark:hover:text-gray-300',
            )}
          >
            {l.label}
          </button>
        ))}
      </div>

      <NamedItemList key={active} endpoint={active} singular={current.singular} />
    </div>
  )
}

/** CRUD genérico de uma lista NamedItem (id, name, active, sortOrder). */
function NamedItemList({ endpoint, singular }: { endpoint: ListKey; singular: string }) {
  const queryClient = useQueryClient()
  const toast = useToast()
  const confirm = useConfirm()
  const [modalOpen, setModalOpen] = useState(false)
  const [form, setForm] = useState<Partial<NamedItem>>({ active: true, sortOrder: 0 })
  const [error, setError] = useState<string | null>(null)

  const qKey = ['lists', endpoint, 'all']
  const { data, isLoading } = useQuery({
    queryKey: qKey,
    queryFn: async () => (await api.get<NamedItem[]>(`/lists/${endpoint}/all`)).data,
  })

  const invalidate = () => {
    queryClient.invalidateQueries({ queryKey: qKey })
    // invalida também a lista "ativos" consumida pelos formulários
    queryClient.invalidateQueries({ queryKey: ['lists', endpoint] })
    queryClient.invalidateQueries({ queryKey: [endpoint] })
  }

  const save = useMutation({
    mutationFn: async (p: Partial<NamedItem>) =>
      p.id ? api.put(`/lists/${endpoint}/${p.id}`, p) : api.post(`/lists/${endpoint}`, p),
    onSuccess: (_d, p) => { invalidate(); setModalOpen(false); toast.success(p.id ? 'Item atualizado.' : 'Item criado.') },
    onError: (e) => setError(apiErrorMessage(e)),
  })
  const remove = useMutation({
    mutationFn: async (id: string) => api.delete(`/lists/${endpoint}/${id}`),
    onSuccess: () => { invalidate(); toast.success('Item removido.') },
    onError: (e) => toast.error(apiErrorMessage(e)),
  })

  async function confirmRemove(item: NamedItem) {
    if (await confirm({ title: 'Remover item', message: `Remover "${item.name}"?`, confirmLabel: 'Remover', danger: true }))
      remove.mutate(item.id)
  }

  function openNew() { setForm({ active: true, sortOrder: ((data?.length ?? 0) + 1) }); setError(null); setModalOpen(true) }
  function openEdit(item: NamedItem) { setForm({ ...item }); setError(null); setModalOpen(true) }

  return (
    <div>
      <div className="mb-3 flex justify-end">
        <Button onClick={openNew}>Novo item</Button>
      </div>
      {isLoading ? <TableSkeleton rows={5} cols={4} /> : (
        <Table headers={['Nome', 'Ordem', 'Status', 'Ações']}>
          {(data ?? []).map((item) => (
            <Tr key={item.id}>
              <td className="px-4 py-2 font-medium">{item.name}</td>
              <td className="px-4 py-2 text-gray-500">{item.sortOrder}</td>
              <td className="px-4 py-2"><Badge dot color={item.active ? 'green' : 'gray'}>{item.active ? 'Ativo' : 'Inativo'}</Badge></td>
              <td className="px-4 py-2 text-right">
                <ActionsMenu items={[
                  { label: 'Editar', onClick: () => openEdit(item) },
                  { label: 'Remover', danger: true, onClick: () => confirmRemove(item) },
                ]} />
              </td>
            </Tr>
          ))}
          {(data ?? []).length === 0 && <tr><td colSpan={4} className="px-4 py-6 text-center text-sm text-gray-400">Nenhum item.</td></tr>}
        </Table>
      )}

      <Modal open={modalOpen} onClose={() => setModalOpen(false)} title={form.id ? `Editar ${singular}` : `Nova ${singular}`}>
        <form className="space-y-3" onSubmit={(e) => { e.preventDefault(); save.mutate(form) }}>
          <Field label="Nome">
            <Input value={form.name ?? ''} onChange={(e) => setForm({ ...form, name: e.target.value })} required autoFocus />
          </Field>
          <div className="grid grid-cols-2 gap-3">
            <Field label="Ordem de exibição">
              <Input type="number" value={form.sortOrder ?? 0} onChange={(e) => setForm({ ...form, sortOrder: Number(e.target.value) })} />
            </Field>
            <Field label="Status">
              <Select value={form.active ? '1' : '0'} onChange={(e) => setForm({ ...form, active: e.target.value === '1' })}>
                <option value="1">Ativo</option>
                <option value="0">Inativo</option>
              </Select>
            </Field>
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
