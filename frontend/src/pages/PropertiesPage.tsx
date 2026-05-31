import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api, apiErrorMessage } from '@/lib/api'
import type { Page, Property } from '@/lib/types'
import { useAuth } from '@/auth/AuthContext'
import { Badge, Button, Field, Input, Modal, PageHeader, Select, Table } from '@/components/ui'
import { formatCurrency } from '@/lib/utils'

const EMPTY: Partial<Property> = { status: 'AVAILABLE' }
const STATUS_COLOR: Record<Property['status'], string> = {
  AVAILABLE: 'green', RESERVED: 'yellow', SOLD: 'blue', CANCELLED: 'gray',
}

export function PropertiesPage() {
  const { hasPermission } = useAuth()
  const queryClient = useQueryClient()
  const [modalOpen, setModalOpen] = useState(false)
  const [form, setForm] = useState<Partial<Property>>(EMPTY)
  const [error, setError] = useState<string | null>(null)
  const canWrite = hasPermission('PROPERTIES_WRITE')

  const { data, isLoading } = useQuery({
    queryKey: ['properties'],
    queryFn: async () => (await api.get<Page<Property>>('/properties', { params: { size: 50 } })).data,
  })

  const save = useMutation({
    mutationFn: async (payload: Partial<Property>) =>
      payload.id ? api.put(`/properties/${payload.id}`, payload) : api.post('/properties', payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['properties'] })
      setModalOpen(false)
      setForm(EMPTY)
    },
    onError: (e) => setError(apiErrorMessage(e)),
  })

  return (
    <div>
      <PageHeader
        title="Imóveis / Lotes"
        action={canWrite && <Button onClick={() => { setForm(EMPTY); setError(null); setModalOpen(true) }}>Novo imóvel</Button>}
      />

      {isLoading ? (
        <p className="text-gray-500">Carregando…</p>
      ) : (
        <Table headers={['Empreendimento', 'Quadra/Lote/Un.', 'Matrícula', 'Valor', 'Status', '']}>
          {data?.content.map((p) => (
            <tr key={p.id} className="hover:bg-gray-50">
              <td className="px-4 py-2 font-medium">{p.development}</td>
              <td className="px-4 py-2">{[p.block, p.lot, p.unit].filter(Boolean).join(' / ') || '—'}</td>
              <td className="px-4 py-2">{p.registration ?? '—'}</td>
              <td className="px-4 py-2">{formatCurrency(p.saleValue)}</td>
              <td className="px-4 py-2"><Badge color={STATUS_COLOR[p.status]}>{p.status}</Badge></td>
              <td className="px-4 py-2 text-right">
                {canWrite && <Button variant="ghost" onClick={() => { setForm(p); setError(null); setModalOpen(true) }}>Editar</Button>}
              </td>
            </tr>
          ))}
          {data?.content.length === 0 && (
            <tr><td colSpan={6} className="px-4 py-6 text-center text-gray-400">Nenhum imóvel.</td></tr>
          )}
        </Table>
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
