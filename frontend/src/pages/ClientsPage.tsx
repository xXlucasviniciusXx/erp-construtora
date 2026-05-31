import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api, apiErrorMessage } from '@/lib/api'
import type { Client, Page } from '@/lib/types'
import { useAuth } from '@/auth/AuthContext'
import { Badge, Button, Field, Input, Modal, PageHeader, Select, Table } from '@/components/ui'

const EMPTY: Partial<Client> = { personType: 'PF', status: 'ACTIVE' }

export function ClientsPage() {
  const { hasPermission } = useAuth()
  const queryClient = useQueryClient()
  const [query, setQuery] = useState('')
  const [modalOpen, setModalOpen] = useState(false)
  const [form, setForm] = useState<Partial<Client>>(EMPTY)
  const [error, setError] = useState<string | null>(null)

  const canWrite = hasPermission('CLIENTS_WRITE')

  const { data, isLoading } = useQuery({
    queryKey: ['clients', query],
    queryFn: async () =>
      (await api.get<Page<Client>>('/clients', { params: { q: query || undefined, size: 50 } })).data,
  })

  const save = useMutation({
    mutationFn: async (payload: Partial<Client>) => {
      if (payload.id) return api.put(`/clients/${payload.id}`, payload)
      return api.post('/clients', payload)
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['clients'] })
      setModalOpen(false)
      setForm(EMPTY)
    },
    onError: (e) => setError(apiErrorMessage(e)),
  })

  function openNew() {
    setForm(EMPTY)
    setError(null)
    setModalOpen(true)
  }

  function openEdit(c: Client) {
    setForm(c)
    setError(null)
    setModalOpen(true)
  }

  return (
    <div>
      <PageHeader
        title="Clientes"
        action={canWrite && <Button onClick={openNew}>Novo cliente</Button>}
      />

      <div className="mb-4 max-w-sm">
        <Input placeholder="Buscar por nome ou documento…" value={query} onChange={(e) => setQuery(e.target.value)} />
      </div>

      {isLoading ? (
        <p className="text-gray-500">Carregando…</p>
      ) : (
        <Table headers={['Nome', 'Documento', 'Tipo', 'E-mail', 'Status', '']}>
          {data?.content.map((c) => (
            <tr key={c.id} className="hover:bg-gray-50">
              <td className="px-4 py-2 font-medium">{c.name}</td>
              <td className="px-4 py-2">{c.document}</td>
              <td className="px-4 py-2">{c.personType}</td>
              <td className="px-4 py-2">{c.email ?? '—'}</td>
              <td className="px-4 py-2">
                <Badge color={c.status === 'ACTIVE' ? 'green' : 'gray'}>{c.status}</Badge>
              </td>
              <td className="px-4 py-2 text-right">
                {canWrite && (
                  <Button variant="ghost" onClick={() => openEdit(c)}>Editar</Button>
                )}
              </td>
            </tr>
          ))}
          {data?.content.length === 0 && (
            <tr><td colSpan={6} className="px-4 py-6 text-center text-gray-400">Nenhum cliente.</td></tr>
          )}
        </Table>
      )}

      <Modal open={modalOpen} onClose={() => setModalOpen(false)} title={form.id ? 'Editar cliente' : 'Novo cliente'}>
        <form
          className="space-y-3"
          onSubmit={(e) => {
            e.preventDefault()
            save.mutate(form)
          }}
        >
          <Field label="Tipo de pessoa">
            <Select value={form.personType} onChange={(e) => setForm({ ...form, personType: e.target.value as Client['personType'] })}>
              <option value="PF">Pessoa Física</option>
              <option value="PJ">Pessoa Jurídica</option>
            </Select>
          </Field>
          <Field label="Nome / Razão social">
            <Input value={form.name ?? ''} onChange={(e) => setForm({ ...form, name: e.target.value })} required />
          </Field>
          <Field label="CPF / CNPJ">
            <Input value={form.document ?? ''} onChange={(e) => setForm({ ...form, document: e.target.value })} required />
          </Field>
          <div className="grid grid-cols-2 gap-3">
            <Field label="E-mail">
              <Input type="email" value={form.email ?? ''} onChange={(e) => setForm({ ...form, email: e.target.value })} />
            </Field>
            <Field label="Telefone">
              <Input value={form.phone ?? ''} onChange={(e) => setForm({ ...form, phone: e.target.value })} />
            </Field>
          </div>
          <Field label="Endereço">
            <Input value={form.address ?? ''} onChange={(e) => setForm({ ...form, address: e.target.value })} />
          </Field>
          <div className="grid grid-cols-3 gap-3">
            <Field label="Cidade">
              <Input value={form.city ?? ''} onChange={(e) => setForm({ ...form, city: e.target.value })} />
            </Field>
            <Field label="UF">
              <Input maxLength={2} value={form.state ?? ''} onChange={(e) => setForm({ ...form, state: e.target.value })} />
            </Field>
            <Field label="Status">
              <Select value={form.status} onChange={(e) => setForm({ ...form, status: e.target.value as Client['status'] })}>
                <option value="ACTIVE">Ativo</option>
                <option value="INACTIVE">Inativo</option>
              </Select>
            </Field>
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
