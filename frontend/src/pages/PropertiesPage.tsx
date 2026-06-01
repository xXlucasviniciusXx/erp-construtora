import { useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api, apiErrorMessage } from '@/lib/api'
import type { Page, Property } from '@/lib/types'
import { useAuth } from '@/auth/AuthContext'
import { ActionsMenu } from '@/components/Menu'
import { Badge, Button, Card, Field, Input, Modal, PageHeader, Select, Table } from '@/components/ui'
import { formatCurrency } from '@/lib/utils'

const EMPTY: Partial<Property> = { status: 'AVAILABLE' }
const STATUS_COLOR: Record<Property['status'], string> = {
  AVAILABLE: 'green', RESERVED: 'yellow', SOLD: 'blue', CANCELLED: 'gray',
}
const STATUS_LABEL: Record<Property['status'], string> = {
  AVAILABLE: 'Disponível', RESERVED: 'Reservado', SOLD: 'Vendido', CANCELLED: 'Cancelado',
}

export function PropertiesPage() {
  const { hasPermission } = useAuth()
  const queryClient = useQueryClient()
  const [modalOpen, setModalOpen] = useState(false)
  const [form, setForm] = useState<Partial<Property>>(EMPTY)
  const [error, setError] = useState<string | null>(null)
  const [viewProp, setViewProp] = useState<Property | null>(null)
  const [q, setQ] = useState('')
  const [statusFilter, setStatusFilter] = useState('')
  const canWrite = hasPermission('PROPERTIES_WRITE')

  const { data, isLoading } = useQuery({
    queryKey: ['properties'],
    queryFn: async () => (await api.get<Page<Property>>('/properties', { params: { size: 200 } })).data,
  })

  const save = useMutation({
    mutationFn: async (payload: Partial<Property>) =>
      payload.id ? api.put(`/properties/${payload.id}`, payload) : api.post('/properties', payload),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['properties'] }); setModalOpen(false); setForm(EMPTY) },
    onError: (e) => setError(apiErrorMessage(e)),
  })
  const cancel = useMutation({
    mutationFn: async (id: string) => api.patch(`/properties/${id}/cancel`),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['properties'] }),
    onError: (e) => alert(apiErrorMessage(e)),
  })

  const all = data?.content ?? []
  const counts = useMemo(() => {
    const c = { AVAILABLE: 0, RESERVED: 0, SOLD: 0, CANCELLED: 0 } as Record<Property['status'], number>
    all.forEach((p) => { c[p.status]++ })
    return c
  }, [all])

  const filtered = useMemo(() => {
    let list = all
    if (q) {
      const t = q.toLowerCase()
      list = list.filter((p) =>
        p.development.toLowerCase().includes(t) ||
        [p.block, p.lot, p.unit, p.registration].filter(Boolean).join(' ').toLowerCase().includes(t))
    }
    if (statusFilter) list = list.filter((p) => p.status === statusFilter)
    return list
  }, [all, q, statusFilter])

  return (
    <div>
      <PageHeader
        title="Imóveis / Lotes"
        action={canWrite && <Button onClick={() => { setForm(EMPTY); setError(null); setModalOpen(true) }}>Novo imóvel</Button>}
      />

      {/* Resumo de lotes */}
      <div className="mb-4 grid grid-cols-2 gap-3 sm:grid-cols-4">
        <Summary label="Disponíveis" value={counts.AVAILABLE} accent="text-green-600" />
        <Summary label="Reservados" value={counts.RESERVED} accent="text-amber-600" />
        <Summary label="Vendidos" value={counts.SOLD} accent="text-blue-700" />
        <Summary label="Cancelados" value={counts.CANCELLED} accent="text-gray-500" />
      </div>

      <div className="mb-4 grid grid-cols-1 gap-3 sm:grid-cols-3">
        <Input placeholder="Buscar por empreendimento, lote, matrícula…" value={q} onChange={(e) => setQ(e.target.value)} />
        <Select value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)}>
          <option value="">Todos os status</option>
          <option value="AVAILABLE">Disponível</option>
          <option value="RESERVED">Reservado</option>
          <option value="SOLD">Vendido</option>
          <option value="CANCELLED">Cancelado</option>
        </Select>
      </div>

      {isLoading ? (
        <p className="text-gray-500">Carregando…</p>
      ) : (
        <Table headers={['Empreendimento', 'Quadra/Lote/Un.', 'Matrícula', 'Valor', 'Status', 'Ações']}>
          {filtered.map((p) => (
            <tr key={p.id} className="hover:bg-gray-50">
              <td className="px-4 py-2 font-medium">{p.development}</td>
              <td className="px-4 py-2">{[p.block, p.lot, p.unit].filter(Boolean).join(' / ') || '—'}</td>
              <td className="px-4 py-2">{p.registration ?? '—'}</td>
              <td className="px-4 py-2">{formatCurrency(p.saleValue)}</td>
              <td className="px-4 py-2"><Badge color={STATUS_COLOR[p.status]}>{STATUS_LABEL[p.status]}</Badge></td>
              <td className="px-4 py-2 text-right">
                <ActionsMenu items={[
                  { label: 'Visualizar', onClick: () => setViewProp(p) },
                  ...(canWrite ? [
                    { label: 'Editar', onClick: () => { setForm(p); setError(null); setModalOpen(true) } },
                    { label: 'Inativar', danger: true, disabled: p.status === 'CANCELLED', onClick: () => { if (window.confirm(`Inativar/cancelar o lote "${p.development} ${[p.block, p.lot].filter(Boolean).join('/')}"?`)) cancel.mutate(p.id) } },
                  ] : []),
                ]} />
              </td>
            </tr>
          ))}
          {filtered.length === 0 && (
            <tr><td colSpan={6} className="px-4 py-6 text-center text-gray-400">Nenhum imóvel.</td></tr>
          )}
        </Table>
      )}

      {/* Modal: visualizar */}
      {viewProp && (
        <Modal open onClose={() => setViewProp(null)} title={`Imóvel — ${viewProp.development}`}>
          <div className="grid grid-cols-2 gap-3">
            <Info label="Empreendimento" value={viewProp.development} />
            <Info label="Status" value={STATUS_LABEL[viewProp.status]} />
            <Info label="Quadra" value={viewProp.block} />
            <Info label="Lote" value={viewProp.lot} />
            <Info label="Unidade" value={viewProp.unit} />
            <Info label="Matrícula" value={viewProp.registration} />
            <Info label="Endereço" value={viewProp.address} />
            <Info label="Valor de venda" value={viewProp.saleValue != null ? formatCurrency(viewProp.saleValue) : null} />
            <Info label="Área total" value={viewProp.totalArea != null ? `${viewProp.totalArea} m²` : null} />
            <Info label="Área construída" value={viewProp.builtArea != null ? `${viewProp.builtArea} m²` : null} />
          </div>
          {viewProp.notes && <div className="mt-3"><Info label="Observações" value={viewProp.notes} /></div>}
        </Modal>
      )}

      <Modal open={modalOpen} onClose={() => setModalOpen(false)} title={form.id ? 'Editar imóvel' : 'Novo imóvel'}>
        <form className="space-y-3" onSubmit={(e) => { e.preventDefault(); save.mutate(form) }}>
          <Field label="Empreendimento">
            <Input value={form.development ?? ''} onChange={(e) => setForm({ ...form, development: e.target.value })} required />
          </Field>
          <div className="grid grid-cols-3 gap-3">
            <Field label="Quadra"><Input value={form.block ?? ''} onChange={(e) => setForm({ ...form, block: e.target.value })} /></Field>
            <Field label="Lote"><Input value={form.lot ?? ''} onChange={(e) => setForm({ ...form, lot: e.target.value })} /></Field>
            <Field label="Unidade"><Input value={form.unit ?? ''} onChange={(e) => setForm({ ...form, unit: e.target.value })} /></Field>
          </div>
          <div className="grid grid-cols-2 gap-3">
            <Field label="Matrícula"><Input value={form.registration ?? ''} onChange={(e) => setForm({ ...form, registration: e.target.value })} /></Field>
            <Field label="Valor de venda">
              <Input type="number" step="0.01" value={form.saleValue ?? ''} onChange={(e) => setForm({ ...form, saleValue: Number(e.target.value) })} />
            </Field>
          </div>
          <Field label="Endereço"><Input value={form.address ?? ''} onChange={(e) => setForm({ ...form, address: e.target.value })} /></Field>
          <div className="grid grid-cols-3 gap-3">
            <Field label="Área total (m²)"><Input type="number" step="0.01" value={form.totalArea ?? ''} onChange={(e) => setForm({ ...form, totalArea: Number(e.target.value) })} /></Field>
            <Field label="Área construída (m²)"><Input type="number" step="0.01" value={form.builtArea ?? ''} onChange={(e) => setForm({ ...form, builtArea: Number(e.target.value) })} /></Field>
            <Field label="Status">
              <Select value={form.status} onChange={(e) => setForm({ ...form, status: e.target.value as Property['status'] })}>
                <option value="AVAILABLE">Disponível</option>
                <option value="RESERVED">Reservado</option>
                <option value="SOLD">Vendido</option>
                <option value="CANCELLED">Cancelado</option>
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

function Summary({ label, value, accent }: { label: string; value: number; accent: string }) {
  return (
    <Card>
      <div className="text-xs uppercase tracking-wide text-gray-500 dark:text-gray-400">{label}</div>
      <div className={`mt-1 text-2xl font-semibold ${accent}`}>{value}</div>
    </Card>
  )
}

function Info({ label, value }: { label: string; value?: string | number | null }) {
  return (
    <div>
      <div className="text-xs text-gray-400">{label}</div>
      <div className="text-sm text-gray-800 dark:text-gray-100">{value || '—'}</div>
    </div>
  )
}
