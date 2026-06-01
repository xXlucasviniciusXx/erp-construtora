import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api, apiErrorMessage } from '@/lib/api'
import type { Supplier } from '@/lib/types'
import { useAuth } from '@/auth/AuthContext'
import { ActionsMenu } from '@/components/Menu'
import { Badge, Button, Field, Input, Modal, PageHeader, Table } from '@/components/ui'

const EMPTY: Partial<Supplier> = { active: true }

export function SuppliersPage() {
  const { hasPermission } = useAuth()
  const queryClient = useQueryClient()
  const [q, setQ] = useState('')
  const [modalOpen, setModalOpen] = useState(false)
  const [form, setForm] = useState<Partial<Supplier>>(EMPTY)
  const [error, setError] = useState<string | null>(null)
  const canWrite = hasPermission('PAYABLE_WRITE')

  const { data, isLoading } = useQuery({
    queryKey: ['suppliers', q],
    queryFn: async () => (await api.get<Supplier[]>('/suppliers', { params: { q: q || undefined } })).data,
  })

  const save = useMutation({
    mutationFn: async (p: Partial<Supplier>) =>
      p.id ? api.put(`/suppliers/${p.id}`, p) : api.post('/suppliers', p),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['suppliers'] }); setModalOpen(false); setForm(EMPTY) },
    onError: (e) => setError(apiErrorMessage(e)),
  })
  const remove = useMutation({
    mutationFn: async (id: string) => api.delete(`/suppliers/${id}`),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['suppliers'] }),
  })

  return (
    <div>
      <PageHeader
        title="Fornecedores"
        action={canWrite && <Button onClick={() => { setForm(EMPTY); setError(null); setModalOpen(true) }}>Novo fornecedor</Button>}
      />

      <div className="mb-4 max-w-sm">
        <Input placeholder="Buscar fornecedor…" value={q} onChange={(e) => setQ(e.target.value)} />
      </div>

      {isLoading ? <p className="text-gray-500">Carregando…</p> : (
        <Table headers={['Nome', 'Documento', 'Categoria', 'Telefone', 'Cidade/UF', 'Status', 'Ações']}>
          {data?.map((s) => (
            <tr key={s.id} className="hover:bg-gray-50">
              <td className="px-4 py-2 font-medium">{s.name}</td>
              <td className="px-4 py-2">{s.document ?? '—'}</td>
              <td className="px-4 py-2">{s.category ?? '—'}</td>
              <td className="px-4 py-2">{s.phone ?? '—'}</td>
              <td className="px-4 py-2">{[s.city, s.state].filter(Boolean).join(' / ') || '—'}</td>
              <td className="px-4 py-2"><Badge color={s.active ? 'green' : 'gray'}>{s.active ? 'ATIVO' : 'INATIVO'}</Badge></td>
              <td className="px-4 py-2 text-right">
                {canWrite && (
                  <ActionsMenu
                    items={[
                      { label: 'Editar', onClick: () => { setForm(s); setError(null); setModalOpen(true) } },
                      { label: 'Remover', danger: true, onClick: () => { if (window.confirm(`Remover "${s.name}"?`)) remove.mutate(s.id) } },
                    ]}
                  />
                )}
              </td>
            </tr>
          ))}
          {data?.length === 0 && <tr><td colSpan={7} className="px-4 py-6 text-center text-gray-400">Nenhum fornecedor.</td></tr>}
        </Table>
      )}

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
            <Button type="submit" disabled={save.isPending}>Salvar</Button>
          </div>
        </form>
      </Modal>
    </div>
  )
}
