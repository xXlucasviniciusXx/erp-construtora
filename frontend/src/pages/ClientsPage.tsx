import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api, apiErrorMessage } from '@/lib/api'
import { lookupCep, lookupCnpj } from '@/lib/brasilapi'
import type { Client, Page, Sale } from '@/lib/types'
import { useAuth } from '@/auth/AuthContext'
import { ActionsMenu } from '@/components/Menu'
import { Badge, Button, Field, Input, Modal, PageHeader, Select, Table } from '@/components/ui'
import { formatCurrency, formatDate } from '@/lib/utils'

const EMPTY: Partial<Client> = { personType: 'PF', status: 'ACTIVE' }

export function ClientsPage() {
  const { hasPermission } = useAuth()
  const queryClient = useQueryClient()
  const [query, setQuery] = useState('')
  const [editOpen, setEditOpen] = useState(false)
  const [form, setForm] = useState<Partial<Client>>(EMPTY)
  const [error, setError] = useState<string | null>(null)
  const [pageError, setPageError] = useState<string | null>(null)
  const [viewClient, setViewClient] = useState<Client | null>(null)
  const [lotsClient, setLotsClient] = useState<Client | null>(null)
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
    queryKey: ['clients', query],
    queryFn: async () =>
      (await api.get<Page<Client>>('/clients', { params: { q: query || undefined, size: 50 } })).data,
  })

  const save = useMutation({
    mutationFn: async (payload: Partial<Client>) =>
      payload.id ? api.put(`/clients/${payload.id}`, payload) : api.post('/clients', payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['clients'] })
      setEditOpen(false)
      setForm(EMPTY)
    },
    onError: (e) => setError(apiErrorMessage(e)),
  })

  const inactivate = useMutation({
    mutationFn: async (id: string) => api.patch(`/clients/${id}/inactivate`),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['clients'] }),
    onError: (e) => setPageError(apiErrorMessage(e)),
  })

  function openNew() {
    setForm(EMPTY); setError(null); setLookupMsg(null); setEditOpen(true)
  }
  function openEdit(c: Client) {
    setForm(c); setError(null); setLookupMsg(null); setEditOpen(true)
  }
  function confirmInactivate(c: Client) {
    setPageError(null)
    if (window.confirm(`Tem certeza que deseja inativar o cliente "${c.name}"?`)) {
      inactivate.mutate(c.id)
    }
  }

  return (
    <div>
      <PageHeader title="Clientes" action={canWrite && <Button onClick={openNew}>Novo cliente</Button>} />

      {pageError && (
        <div className="mb-4 rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">
          {pageError}
        </div>
      )}

      <div className="mb-4 max-w-sm">
        <Input placeholder="Buscar por nome ou documento…" value={query} onChange={(e) => setQuery(e.target.value)} />
      </div>

      {isLoading ? (
        <p className="text-gray-500">Carregando…</p>
      ) : (
        <Table headers={['Nome', 'Documento', 'Tipo', 'E-mail', 'Status', 'Ações']}>
          {data?.content.map((c) => (
            <tr key={c.id} className="hover:bg-gray-50">
              <td className="px-4 py-2 font-medium">{c.name}</td>
              <td className="px-4 py-2">{c.document}</td>
              <td className="px-4 py-2">{c.personType}</td>
              <td className="px-4 py-2">{c.email ?? '—'}</td>
              <td className="px-4 py-2">
                <Badge color={c.status === 'ACTIVE' ? 'green' : 'gray'}>
                  {c.status === 'ACTIVE' ? 'ATIVO' : 'INATIVO'}
                </Badge>
              </td>
              <td className="px-4 py-2 text-right">
                <ActionsMenu
                  items={[
                    { label: 'Visualizar', onClick: () => setViewClient(c) },
                    { label: 'Visualizar Lotes', onClick: () => setLotsClient(c) },
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
            </tr>
          ))}
          {data?.content.length === 0 && (
            <tr><td colSpan={6} className="px-4 py-6 text-center text-gray-400">Nenhum cliente.</td></tr>
          )}
        </Table>
      )}

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
            <Button type="submit" disabled={save.isPending}>Salvar</Button>
          </div>
        </form>
      </Modal>

      {/* ---- Modal: visualizar (somente leitura) ---- */}
      {viewClient && (
        <Modal open onClose={() => setViewClient(null)} title={`Cliente — ${viewClient.name}`}>
          <ClientView client={viewClient} />
        </Modal>
      )}

      {/* ---- Modal: visualizar lotes ---- */}
      {lotsClient && (
        <Modal open onClose={() => setLotsClient(null)} title={`Lotes/Imóveis — ${lotsClient.name}`}>
          <ClientLots clientId={lotsClient.id} />
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
    queryFn: async () => (await api.get<Sale[]>('/sales', { params: { clientId: client.id } })).data,
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
        <div className="mb-1 text-sm font-semibold text-gray-700 dark:text-gray-200">Compras / Parcelas</div>
        {sales.isLoading ? (
          <p className="text-xs text-gray-400">Carregando…</p>
        ) : sales.data?.length ? (
          <div className="space-y-1">
            {sales.data.map((s) => {
              const paid = s.installments.filter((i) => i.status === 'PAID').length
              return (
                <div key={s.id} className="rounded border border-gray-200 px-3 py-2 text-xs dark:border-gray-700">
                  <div className="font-medium">{s.propertyLabel}</div>
                  <div className="text-gray-500 dark:text-gray-400">
                    {formatCurrency(s.totalValue)} · {paid}/{s.installmentsCount} parcelas pagas · venda {formatDate(s.saleDate)}
                  </div>
                </div>
              )
            })}
          </div>
        ) : (
          <p className="text-xs text-gray-400">Nenhuma compra registrada.</p>
        )}
      </div>
    </div>
  )
}

function ClientLots({ clientId }: { clientId: string }) {
  const sales = useQuery({
    queryKey: ['client-sales', clientId],
    queryFn: async () => (await api.get<Sale[]>('/sales', { params: { clientId } })).data,
  })
  if (sales.isLoading) return <p className="text-sm text-gray-500">Carregando…</p>
  if (!sales.data?.length) return <p className="text-sm text-gray-400">Este cliente não possui lotes/imóveis vinculados.</p>
  return (
    <Table headers={['Imóvel (Empr./Quadra/Lote)', 'Situação', 'Data compra', 'Valor']}>
      {sales.data.map((s) => (
        <tr key={s.id} className="hover:bg-gray-50">
          <td className="px-4 py-2 font-medium">{s.propertyLabel}</td>
          <td className="px-4 py-2"><Badge color="blue">{s.status}</Badge></td>
          <td className="px-4 py-2">{formatDate(s.saleDate)}</td>
          <td className="px-4 py-2">{formatCurrency(s.totalValue)}</td>
        </tr>
      ))}
    </Table>
  )
}
