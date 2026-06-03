import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api, apiErrorMessage } from '@/lib/api'
import { lookupCep, lookupCnpj } from '@/lib/brasilapi'
import type { Client, Page, Sale } from '@/lib/types'
import { Users } from 'lucide-react'
import { useAuth } from '@/auth/AuthContext'
import { ActionsMenu } from '@/components/Menu'
import { useToast } from '@/components/Toast'
import { useConfirm } from '@/components/Confirm'
import { Badge, Button, EmptyState, Field, Input, Modal, PageHeader, Pagination, Select, Table, TableSkeleton, Tr } from '@/components/ui'
import { formatCurrency, formatDate } from '@/lib/utils'

const EMPTY: Partial<Client> = { personType: 'PF', status: 'ACTIVE' }

export function ClientsPage() {
  const { hasPermission } = useAuth()
  const queryClient = useQueryClient()
  const toast = useToast()
  const confirm = useConfirm()
  const [query, setQuery] = useState('')
  const [statusFilter, setStatusFilter] = useState('')
  const [page, setPage] = useState(0)
  const [editOpen, setEditOpen] = useState(false)
  const [form, setForm] = useState<Partial<Client>>(EMPTY)
  const [error, setError] = useState<string | null>(null)
  const [pageError, setPageError] = useState<string | null>(null)
  const [viewClient, setViewClient] = useState<Client | null>(null)
  const [lookupMsg, setLookupMsg] = useState<string | null>(null)

  const canWrite = hasPermission('CLIENTS_WRITE')

  async function handleCepBlur(cep: string) {
    if (!cep) return
    const r = await lookupCep(cep)
    if (!r) { setLookupMsg('Não foi possível localizar o CEP. Preencha as informações manualmente.'); return }
    setLookupMsg(null)
    setForm((f) => ({
      ...f, zipCode: cep,
      address: [r.street, r.neighborhood].filter(Boolean).join(' - ') || f.address,
      city: r.city, state: r.state,
    }))
  }

  async function handleCnpjBlur(doc: string) {
    if (form.personType !== 'PJ' || !doc) return
    const r = await lookupCnpj(doc)
    if (!r) { setLookupMsg('Não foi possível localizar os dados automaticamente. Preencha as informações manualmente.'); return }
    setLookupMsg(null)
    setForm((f) => ({
      ...f,
      name: r.razao_social || f.name,
      zipCode: r.cep || f.zipCode,
      address: [r.logradouro, r.numero, r.bairro].filter(Boolean).join(', ') || f.address,
      city: r.municipio || f.city, state: r.uf || f.state,
    }))
  }

  const { data, isLoading } = useQuery({
    queryKey: ['clients', query, statusFilter, page],
    queryFn: async () =>
      (await api.get<Page<Client>>('/clients', {
        params: { q: query || undefined, status: statusFilter || undefined, page, size: 20 },
      })).data,
  })

  const save = useMutation({
    mutationFn: async (payload: Partial<Client>) =>
      payload.id ? api.put(`/clients/${payload.id}`, payload) : api.post('/clients', payload),
    onSuccess: (_d, payload) => {
      queryClient.invalidateQueries({ queryKey: ['clients'] })
      setEditOpen(false)
      setForm(EMPTY)
      toast.success(payload.id ? 'Cliente atualizado com sucesso.' : 'Cliente criado com sucesso.')
    },
    onError: (e) => setError(apiErrorMessage(e)),
  })

  const inactivate = useMutation({
    mutationFn: async (id: string) => api.patch(`/clients/${id}/inactivate`),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['clients'] })
      toast.success('Cliente inativado.')
    },
    onError: (e) => { setPageError(apiErrorMessage(e)); toast.error(apiErrorMessage(e)) },
  })

  function openNew() {
    setForm(EMPTY); setError(null); setLookupMsg(null); setEditOpen(true)
  }
  function openEdit(c: Client) {
    setForm(c); setError(null); setLookupMsg(null); setEditOpen(true)
  }
  async function confirmInactivate(c: Client) {
    setPageError(null)
    const ok = await confirm({
      title: 'Inativar cliente',
      message: `Tem certeza que deseja inativar o cliente "${c.name}"?`,
      confirmLabel: 'Inativar',
      danger: true,
    })
    if (ok) inactivate.mutate(c.id)
  }

  return (
    <div>
      <PageHeader title="Clientes" subtitle="Cadastro de pessoas físicas e jurídicas" action={canWrite && <Button onClick={openNew}>Novo cliente</Button>} />

      {pageError && (
        <div className="mb-4 rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">
          {pageError}
        </div>
      )}

      <div className="mb-4 grid grid-cols-1 gap-3 sm:grid-cols-3">
        <Input placeholder="Buscar por nome ou documento…" value={query} onChange={(e) => { setQuery(e.target.value); setPage(0) }} />
        <Select value={statusFilter} onChange={(e) => { setStatusFilter(e.target.value); setPage(0) }}>
          <option value="">Todos os status</option>
          <option value="ACTIVE">Ativos</option>
          <option value="INACTIVE">Inativos</option>
        </Select>
      </div>

      {isLoading ? (
        <TableSkeleton rows={6} cols={6} />
      ) : (
        <Table headers={['Nome', 'Documento', 'Tipo', 'E-mail', 'Status', 'Ações']}>
          {data?.content.map((c) => (
            <Tr key={c.id}>
              <td className="px-4 py-2 font-medium">{c.name}</td>
              <td className="px-4 py-2">{c.document}</td>
              <td className="px-4 py-2">{c.personType}</td>
              <td className="px-4 py-2">{c.email ?? '—'}</td>
              <td className="px-4 py-2">
                <Badge dot color={c.status === 'ACTIVE' ? 'green' : 'gray'}>
                  {c.status === 'ACTIVE' ? 'Ativo' : 'Inativo'}
                </Badge>
              </td>
              <td className="px-4 py-2 text-right">
                <ActionsMenu
                  items={[
                    { label: 'Visualizar', onClick: () => setViewClient(c) },
                    ...(canWrite
                      ? [
                          { label: 'Editar', onClick: () => openEdit(c) },
                          {
                            label: 'Inativar',
                            danger: true,
                            disabled: c.status === 'INACTIVE',
                            onClick: () => confirmInactivate(c),
                          },
                        ]
                      : []),
                  ]}
                />
              </td>
            </Tr>
          ))}
          {data?.content.length === 0 && (
            <tr>
              <td colSpan={6} className="p-0">
                <EmptyState
                  icon={Users}
                  title="Nenhum cliente encontrado"
                  description={query ? 'Tente ajustar a busca ou os filtros.' : 'Cadastre o primeiro cliente para começar.'}
                  action={canWrite && !query ? <Button onClick={openNew}>Novo cliente</Button> : undefined}
                />
              </td>
            </tr>
          )}
        </Table>
      )}
      {data && <Pagination page={data.number} totalPages={data.totalPages} totalElements={data.totalElements} onChange={setPage} />}

      {/* ---- Modal: criar/editar ---- */}
      <Modal open={editOpen} onClose={() => setEditOpen(false)} title={form.id ? 'Editar cliente' : 'Novo cliente'}>
        <form className="space-y-3" onSubmit={(e) => { e.preventDefault(); save.mutate(form) }}>
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
            <Input
              value={form.document ?? ''}
              onChange={(e) => setForm({ ...form, document: e.target.value })}
              onBlur={(e) => handleCnpjBlur(e.target.value)}
              placeholder={form.personType === 'PJ' ? 'CNPJ — preenche automático' : ''}
              required
            />
          </Field>
          <Field label="CEP">
            <Input
              value={form.zipCode ?? ''}
              onChange={(e) => setForm({ ...form, zipCode: e.target.value })}
              onBlur={(e) => handleCepBlur(e.target.value)}
              placeholder="00000-000 — preenche endereço"
            />
          </Field>
          {lookupMsg && <p className="text-xs text-amber-600">{lookupMsg}</p>}
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
            <Button type="button" variant="outline" onClick={() => setEditOpen(false)}>Cancelar</Button>
            <Button type="submit" loading={save.isPending}>Salvar</Button>
          </div>
        </form>
      </Modal>

      {/* ---- Modal: visualizar (somente leitura, com compras e saldos) ---- */}
      {viewClient && (
        <Modal open onClose={() => setViewClient(null)} title={`Cliente — ${viewClient.name}`}>
          <ClientView client={viewClient} />
        </Modal>
      )}
    </div>
  )
}

function Info({ label, value }: { label: string; value?: string | null }) {
  return (
    <div>
      <div className="text-xs text-gray-400">{label}</div>
      <div className="text-sm text-gray-800 dark:text-gray-100">{value || '—'}</div>
    </div>
  )
}

function ClientView({ client }: { client: Client }) {
  const sales = useQuery({
    queryKey: ['client-sales', client.id],
    queryFn: async () => (await api.get<Page<Sale>>('/sales', { params: { clientId: client.id, size: 500 } })).data.content,
  })
  return (
    <div className="space-y-4">
      <div className="grid grid-cols-2 gap-3">
        <Info label="Tipo" value={client.personType === 'PF' ? 'Pessoa Física' : 'Pessoa Jurídica'} />
        <Info label="CPF / CNPJ" value={client.document} />
        <Info label="RG / Inscrição" value={client.stateRegistration} />
        <Info label="Status" value={client.status === 'ACTIVE' ? 'Ativo' : 'Inativo'} />
        <Info label="E-mail" value={client.email} />
        <Info label="Telefone" value={client.phone} />
        <Info label="Endereço" value={client.address} />
        <Info label="Cidade/UF" value={[client.city, client.state].filter(Boolean).join(' / ')} />
        <Info label="Estado civil" value={client.maritalStatus} />
        <Info label="Profissão" value={client.occupation} />
      </div>
      {client.notes && <Info label="Observações" value={client.notes} />}

      <div>
        <div className="mb-1 text-sm font-semibold text-gray-700 dark:text-gray-200">Compras / Lotes</div>
        {sales.isLoading ? (
          <p className="text-xs text-gray-400">Carregando…</p>
        ) : sales.data?.length ? (
          <Table headers={['Imóvel', 'Total', 'Pagas', 'Saldo pago', 'Saldo devedor', 'Data']}>
            {sales.data.map((s) => (
              <Tr key={s.id}>
                <td className="px-4 py-2 font-medium">{s.propertyLabel}</td>
                <td className="px-4 py-2">{formatCurrency(s.totalValue)}</td>
                <td className="px-4 py-2">{s.paidInstallments ?? 0}/{s.installmentsCount}</td>
                <td className="px-4 py-2 text-green-600">{formatCurrency(s.paidAmount ?? 0)}</td>
                <td className="px-4 py-2 text-amber-600">{formatCurrency(s.openAmount ?? 0)}</td>
                <td className="px-4 py-2">{formatDate(s.saleDate)}</td>
              </Tr>
            ))}
          </Table>
        ) : (
          <p className="text-xs text-gray-400">Nenhuma compra registrada.</p>
        )}
      </div>
    </div>
  )
}
