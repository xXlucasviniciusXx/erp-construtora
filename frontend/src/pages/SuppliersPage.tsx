import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api, apiErrorMessage } from '@/lib/api'
import type { Page, Supplier } from '@/lib/types'
import { Truck } from 'lucide-react'
import { useAuth } from '@/auth/AuthContext'
import { ActionsMenu } from '@/components/Menu'
import { useToast } from '@/components/Toast'
import { useConfirm } from '@/components/Confirm'
import { Badge, Button, EmptyState, Field, Input, Modal, PageHeader, Pagination, Table, TableSkeleton, Tr } from '@/components/ui'

const EMPTY: Partial<Supplier> = { active: true }

export function SuppliersPage() {
  const { hasPermission } = useAuth()
  const queryClient = useQueryClient()
  const toast = useToast()
  const confirm = useConfirm()
  const [q, setQ] = useState('')
  const [page, setPage] = useState(0)
  const [modalOpen, setModalOpen] = useState(false)
  const [form, setForm] = useState<Partial<Supplier>>(EMPTY)
  const [error, setError] = useState<string | null>(null)
  const canWrite = hasPermission('PAYABLE_WRITE')

  const { data, isLoading } = useQuery({
    queryKey: ['suppliers', q, page],
    queryFn: async () => (await api.get<Page<Supplier>>('/suppliers', { params: { q: q || undefined, page, size: 20 } })).data,
  })

  const save = useMutation({
    mutationFn: async (p: Partial<Supplier>) =>
      p.id ? api.put(`/suppliers/${p.id}`, p) : api.post('/suppliers', p),
    onSuccess: (_d, p) => {
      queryClient.invalidateQueries({ queryKey: ['suppliers'] }); setModalOpen(false); setForm(EMPTY)
      toast.success(p.id ? 'Fornecedor atualizado.' : 'Fornecedor criado.')
    },
    onError: (e) => setError(apiErrorMessage(e)),
  })
  const remove = useMutation({
    mutationFn: async (id: string) => api.delete(`/suppliers/${id}`),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['suppliers'] }); toast.success('Fornecedor removido.') },
    onError: (e) => toast.error(apiErrorMessage(e)),
  })

  async function confirmRemove(s: Supplier) {
    const ok = await confirm({ title: 'Remover fornecedor', message: `Remover "${s.name}"?`, confirmLabel: 'Remover', danger: true })
    if (ok) remove.mutate(s.id)
  }

  return (
    <div>
      <PageHeader
        title="Fornecedores"
        subtitle="Cadastro de fornecedores e prestadores"
        action={canWrite && <Button onClick={() => { setForm(EMPTY); setError(null); setModalOpen(true) }}>Novo fornecedor</Button>}
      />

      <div className="mb-4 max-w-sm">
        <Input placeholder="Buscar fornecedor…" value={q} onChange={(e) => { setQ(e.target.value); setPage(0) }} />
      </div>

      {isLoading ? <TableSkeleton rows={5} cols={7} /> : (
        <Table headers={['Nome', 'Documento', 'Categoria', 'Telefone', 'Cidade/UF', 'Status', 'Ações']}>
          {data?.content.map((s) => (
            <Tr key={s.id}>
              <td className="px-4 py-2 font-medium">{s.name}</td>
              <td className="px-4 py-2">{s.document ?? '—'}</td>
              <td className="px-4 py-2">{s.category ?? '—'}</td>
              <td className="px-4 py-2">{s.phone ?? '—'}</td>
              <td className="px-4 py-2">{[s.city, s.state].filter(Boolean).join(' / ') || '—'}</td>
              <td className="px-4 py-2"><Badge dot color={s.active ? 'green' : 'gray'}>{s.active ? 'Ativo' : 'Inativo'}</Badge></td>
              <td className="px-4 py-2 text-right">
                {canWrite && (
                  <ActionsMenu
                    items={[
                      { label: 'Editar', onClick: () => { setForm(s); setError(null); setModalOpen(true) } },
                      { label: 'Remover', danger: true, onClick: () => confirmRemove(s) },
                    ]}
                  />
                )}
              </td>
            </Tr>
          ))}
          {data?.content.length === 0 && (
            <tr><td colSpan={7} className="p-0">
              <EmptyState
                icon={Truck}
                title="Nenhum fornecedor"
                description={q ? 'Ajuste a busca.' : 'Cadastre o primeiro fornecedor.'}
                action={canWrite && !q ? <Button onClick={() => { setForm(EMPTY); setError(null); setModalOpen(true) }}>Novo fornecedor</Button> : undefined}
              />
            </td></tr>
          )}
        </Table>
      )}
      {data && <Pagination page={data.number} totalPages={data.totalPages} totalElements={data.totalElements} onChange={setPage} />}

      <Modal open={modalOpen} onClose={() => setModalOpen(false)} title={form.id ? 'Editar fornecedor' : 'Novo fornecedor'}>
        <form className="space-y-3" onSubmit={(e) => { e.preventDefault(); save.mutate(form) }}>
          <Field label="Nome / Razão social">
            <Input value={form.name ?? ''} onChange={(e) => setForm({ ...form, name: e.target.value })} required />
          </Field>
          <div className="grid grid-cols-2 gap-3">
            <Field label="CNPJ / CPF"><Input value={form.document ?? ''} onChange={(e) => setForm({ ...form, document: e.target.value })} /></Field>
            <Field label="Categoria"><Input value={form.category ?? ''} onChange={(e) => setForm({ ...form, category: e.target.value })} /></Field>
          </div>
          <div className="grid grid-cols-2 gap-3">
            <Field label="E-mail"><Input type="email" value={form.email ?? ''} onChange={(e) => setForm({ ...form, email: e.target.value })} /></Field>
            <Field label="Telefone"><Input value={form.phone ?? ''} onChange={(e) => setForm({ ...form, phone: e.target.value })} /></Field>
          </div>
          <Field label="Endereço"><Input value={form.address ?? ''} onChange={(e) => setForm({ ...form, address: e.target.value })} /></Field>
          <div className="grid grid-cols-3 gap-3">
            <Field label="Cidade"><Input value={form.city ?? ''} onChange={(e) => setForm({ ...form, city: e.target.value })} /></Field>
            <Field label="UF"><Input maxLength={2} value={form.state ?? ''} onChange={(e) => setForm({ ...form, state: e.target.value })} /></Field>
          </div>
          <Field label="Observações"><Input value={form.notes ?? ''} onChange={(e) => setForm({ ...form, notes: e.target.value })} /></Field>
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
