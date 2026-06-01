import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ChevronRight } from 'lucide-react'
import { api, apiErrorMessage } from '@/lib/api'
import type { Block, Development, Lot, LotStatus } from '@/lib/types'
import { useAuth } from '@/auth/AuthContext'
import { ActionsMenu } from '@/components/Menu'
import { Badge, Button, Card, Field, Input, Modal, PageHeader, Select, Table } from '@/components/ui'
import { formatCurrency } from '@/lib/utils'

const LOT_COLOR: Record<LotStatus, string> = { AVAILABLE: 'green', RESERVED: 'yellow', SOLD: 'blue', CANCELLED: 'gray' }
const LOT_LABEL: Record<LotStatus, string> = { AVAILABLE: 'Disponível', RESERVED: 'Reservado', SOLD: 'Vendido', CANCELLED: 'Cancelado' }

export function DevelopmentsPage() {
  const { hasPermission } = useAuth()
  const canWrite = hasPermission('PROPERTIES_WRITE')
  const [manage, setManage] = useState<Development | null>(null)

  if (manage) return <DevelopmentManager development={manage} canWrite={canWrite} onBack={() => setManage(null)} />
  return <DevelopmentsList canWrite={canWrite} onManage={setManage} />
}

/* ---------------- Lista de empreendimentos ---------------- */
function DevelopmentsList({ canWrite, onManage }: { canWrite: boolean; onManage: (d: Development) => void }) {
  const queryClient = useQueryClient()
  const [open, setOpen] = useState(false)
  const [form, setForm] = useState<Partial<Development>>({})
  const [error, setError] = useState<string | null>(null)

  const { data, isLoading } = useQuery({
    queryKey: ['developments'],
    queryFn: async () => (await api.get<Development[]>('/developments')).data,
  })
  const save = useMutation({
    mutationFn: async (d: Partial<Development>) => d.id ? api.put(`/developments/${d.id}`, d) : api.post('/developments', d),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['developments'] }); setOpen(false); setForm({}) },
    onError: (e) => setError(apiErrorMessage(e)),
  })
  const remove = useMutation({
    mutationFn: async (id: string) => api.delete(`/developments/${id}`),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['developments'] }),
    onError: (e) => alert(apiErrorMessage(e)),
  })

  return (
    <div>
      <PageHeader title="Empreendimentos" action={canWrite && <Button onClick={() => { setForm({}); setError(null); setOpen(true) }}>Novo empreendimento</Button>} />
      {isLoading ? <p className="text-gray-500">Carregando…</p> : (
        <Table headers={['Empreendimento', 'Código', 'Valor previsto', 'Expectativa', 'Recebido', 'Quadras', 'Lotes', 'Ações']}>
          {data?.map((d) => (
            <tr key={d.id} className="hover:bg-gray-50">
              <td className="px-4 py-2 font-medium">{d.name}</td>
              <td className="px-4 py-2">{d.internalCode}</td>
              <td className="px-4 py-2 text-blue-700">{formatCurrency(d.plannedTotal)}</td>
              <td className="px-4 py-2 text-amber-600">{formatCurrency(d.expectedValue)}</td>
              <td className="px-4 py-2 text-green-600">{formatCurrency(d.receivedTotal)}</td>
              <td className="px-4 py-2">{d.actualBlocks}{d.blocksCount != null ? `/${d.blocksCount}` : ''}</td>
              <td className="px-4 py-2">{d.actualLots}{d.lotsCount != null ? `/${d.lotsCount}` : ''}</td>
              <td className="px-4 py-2 text-right">
                <ActionsMenu items={[
                  { label: 'Gerenciar quadras/lotes', onClick: () => onManage(d) },
                  ...(canWrite ? [
                    { label: 'Editar', onClick: () => { setForm(d); setError(null); setOpen(true) } },
                    { label: 'Excluir', danger: true, onClick: () => { if (window.confirm(`Excluir "${d.name}" e tudo dentro dele?`)) remove.mutate(d.id) } },
                  ] : []),
                ]} />
              </td>
            </tr>
          ))}
          {data?.length === 0 && <tr><td colSpan={8} className="px-4 py-6 text-center text-gray-400">Nenhum empreendimento.</td></tr>}
        </Table>
      )}

      <Modal open={open} onClose={() => setOpen(false)} title={form.id ? 'Editar empreendimento' : 'Novo empreendimento'}>
        <form className="space-y-3" onSubmit={(e) => { e.preventDefault(); save.mutate(form) }}>
          <Field label="Nome"><Input value={form.name ?? ''} onChange={(e) => setForm({ ...form, name: e.target.value })} required /></Field>
          <div className="grid grid-cols-2 gap-3">
            <Field label="Qtd. de quadras"><Input type="number" min={0} value={form.blocksCount ?? ''} onChange={(e) => setForm({ ...form, blocksCount: Number(e.target.value) })} /></Field>
            <Field label="Qtd. de lotes (limite total)"><Input type="number" min={0} value={form.lotsCount ?? ''} onChange={(e) => setForm({ ...form, lotsCount: Number(e.target.value) })} /></Field>
          </div>
          <Field label="Valor expectativa (manual)"><Input type="number" step="0.01" value={form.expectedValue ?? ''} onChange={(e) => setForm({ ...form, expectedValue: Number(e.target.value) })} /></Field>
          <div className="grid grid-cols-2 gap-3">
            <Field label="Endereço"><Input value={form.address ?? ''} onChange={(e) => setForm({ ...form, address: e.target.value })} /></Field>
            <Field label="Dimensões"><Input value={form.dimensions ?? ''} onChange={(e) => setForm({ ...form, dimensions: e.target.value })} /></Field>
          </div>
          <p className="text-xs text-gray-400">Valor previsto total e valor recebido são calculados automaticamente a partir dos lotes.</p>
          {error && <p className="text-sm text-red-600">{error}</p>}
          <div className="flex justify-end gap-2 pt-2">
            <Button type="button" variant="outline" onClick={() => setOpen(false)}>Cancelar</Button>
            <Button type="submit" disabled={save.isPending}>Salvar</Button>
          </div>
        </form>
      </Modal>
    </div>
  )
}

/* ---------------- Gerenciar quadras e lotes de um empreendimento ---------------- */
function DevelopmentManager({ development, canWrite, onBack }: { development: Development; canWrite: boolean; onBack: () => void }) {
  const queryClient = useQueryClient()
  const [selectedBlock, setSelectedBlock] = useState<Block | null>(null)

  // Recarrega o empreendimento (derivados atualizam ao criar lotes)
  const dev = useQuery({
    queryKey: ['development', development.id],
    queryFn: async () => (await api.get<Development>(`/developments/${development.id}`)).data,
    initialData: development,
  })
  const blocks = useQuery({
    queryKey: ['blocks', development.id],
    queryFn: async () => (await api.get<Block[]>('/blocks', { params: { developmentId: development.id } })).data,
  })

  const d = dev.data
  const refreshAll = () => {
    queryClient.invalidateQueries({ queryKey: ['development', development.id] })
    queryClient.invalidateQueries({ queryKey: ['blocks', development.id] })
    queryClient.invalidateQueries({ queryKey: ['developments'] })
  }

  return (
    <div>
      <button onClick={onBack} className="mb-2 text-sm text-primary hover:underline">← Empreendimentos</button>
      <PageHeader title={`${d.name} (${d.internalCode})`} />

      <div className="mb-4 grid grid-cols-2 gap-3 sm:grid-cols-3">
        <Card><div className="text-xs uppercase text-gray-500 dark:text-gray-400">Valor previsto total</div><div className="mt-1 text-lg font-semibold text-blue-700">{formatCurrency(d.plannedTotal)}</div></Card>
        <Card><div className="text-xs uppercase text-gray-500 dark:text-gray-400">Valor expectativa</div><div className="mt-1 text-lg font-semibold text-amber-600">{formatCurrency(d.expectedValue)}</div></Card>
        <Card><div className="text-xs uppercase text-gray-500 dark:text-gray-400">Valor recebido</div><div className="mt-1 text-lg font-semibold text-green-600">{formatCurrency(d.receivedTotal)}</div></Card>
      </div>

      {/* Quadras */}
      <BlocksSection
        development={d} blocks={blocks.data ?? []} canWrite={canWrite}
        selectedBlock={selectedBlock} onSelect={setSelectedBlock} onChange={refreshAll}
      />

      {/* Lotes da quadra selecionada */}
      {selectedBlock && (
        <LotsSection development={d} block={selectedBlock} canWrite={canWrite} onChange={refreshAll} />
      )}
    </div>
  )
}

function BlocksSection({ development, blocks, canWrite, selectedBlock, onSelect, onChange }: {
  development: Development; blocks: Block[]; canWrite: boolean
  selectedBlock: Block | null; onSelect: (b: Block) => void; onChange: () => void
}) {
  const queryClient = useQueryClient()
  const [open, setOpen] = useState(false)
  const [form, setForm] = useState<Partial<Block>>({})
  const [error, setError] = useState<string | null>(null)
  const atLimit = development.blocksCount != null && blocks.length >= development.blocksCount

  const save = useMutation({
    mutationFn: async (b: Partial<Block>) => b.id
      ? api.put(`/blocks/${b.id}`, { ...b, developmentId: development.id })
      : api.post('/blocks', { ...b, developmentId: development.id }),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['blocks', development.id] }); onChange(); setOpen(false); setForm({}) },
    onError: (e) => setError(apiErrorMessage(e)),
  })
  const remove = useMutation({
    mutationFn: async (id: string) => api.delete(`/blocks/${id}`),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['blocks', development.id] }); onChange() },
    onError: (e) => alert(apiErrorMessage(e)),
  })

  return (
    <Card className="mb-4">
      <div className="mb-3 flex items-center justify-between">
        <h3 className="text-sm font-semibold text-gray-700 dark:text-gray-200">Quadras ({blocks.length}{development.blocksCount != null ? `/${development.blocksCount}` : ''})</h3>
        {canWrite && <Button disabled={atLimit} onClick={() => { setForm({}); setError(null); setOpen(true) }}>{atLimit ? 'Limite atingido' : 'Nova quadra'}</Button>}
      </div>
      <Table headers={['Quadra', 'Código', 'Matrícula', 'Área', 'Lotes', 'Ações']}>
        {blocks.map((b) => (
          <tr key={b.id} className={`cursor-pointer hover:bg-gray-50 ${selectedBlock?.id === b.id ? 'bg-primary/5' : ''}`} onClick={() => onSelect(b)}>
            <td className="px-4 py-2 font-medium">{b.name}</td>
            <td className="px-4 py-2">{b.internalCode}</td>
            <td className="px-4 py-2">{b.registration ?? '—'}</td>
            <td className="px-4 py-2">{b.area ?? '—'}</td>
            <td className="px-4 py-2">{b.lotsCount}</td>
            <td className="px-4 py-2 text-right" onClick={(e) => e.stopPropagation()}>
              <div className="flex items-center justify-end gap-1">
                <Button variant="outline" className="px-2 py-1 text-xs" onClick={() => onSelect(b)}>Ver lotes <ChevronRight className="inline h-3 w-3" /></Button>
                {canWrite && <ActionsMenu items={[
                  { label: 'Editar', onClick: () => { setForm(b); setError(null); setOpen(true) } },
                  { label: 'Excluir', danger: true, onClick: () => { if (window.confirm(`Excluir a quadra "${b.name}" e seus lotes?`)) remove.mutate(b.id) } },
                ]} />}
              </div>
            </td>
          </tr>
        ))}
        {blocks.length === 0 && <tr><td colSpan={6} className="px-4 py-4 text-center text-gray-400">Nenhuma quadra.</td></tr>}
      </Table>

      <Modal open={open} onClose={() => setOpen(false)} title={form.id ? 'Editar quadra' : 'Nova quadra'}>
        <form className="space-y-3" onSubmit={(e) => { e.preventDefault(); save.mutate(form) }}>
          <Field label="Nome da quadra"><Input value={form.name ?? ''} onChange={(e) => setForm({ ...form, name: e.target.value })} required /></Field>
          <div className="grid grid-cols-2 gap-3">
            <Field label="Matrícula do cartório (opcional)"><Input value={form.registration ?? ''} onChange={(e) => setForm({ ...form, registration: e.target.value })} /></Field>
            <Field label="Área"><Input type="number" step="0.01" value={form.area ?? ''} onChange={(e) => setForm({ ...form, area: Number(e.target.value) })} /></Field>
          </div>
          {!form.id && <p className="text-xs text-gray-400">O código interno é gerado automaticamente.</p>}
          {error && <p className="text-sm text-red-600">{error}</p>}
          <div className="flex justify-end gap-2 pt-2">
            <Button type="button" variant="outline" onClick={() => setOpen(false)}>Cancelar</Button>
            <Button type="submit" disabled={save.isPending}>Salvar</Button>
          </div>
        </form>
      </Modal>
    </Card>
  )
}

function LotsSection({ development, block, canWrite, onChange }: { development: Development; block: Block; canWrite: boolean; onChange: () => void }) {
  const queryClient = useQueryClient()
  const [open, setOpen] = useState(false)
  const [form, setForm] = useState<Partial<Lot>>({ status: 'AVAILABLE' })
  const [error, setError] = useState<string | null>(null)
  const atLimit = development.lotsCount != null && development.actualLots >= development.lotsCount

  const lots = useQuery({
    queryKey: ['lots', block.id],
    queryFn: async () => (await api.get<Lot[]>('/lots', { params: { blockId: block.id } })).data,
  })
  const refresh = () => { queryClient.invalidateQueries({ queryKey: ['lots', block.id] }); onChange() }
  const save = useMutation({
    mutationFn: async (l: Partial<Lot>) => l.id
      ? api.put(`/lots/${l.id}`, { ...l, blockId: block.id })
      : api.post('/lots', { ...l, blockId: block.id }),
    onSuccess: () => { refresh(); setOpen(false); setForm({ status: 'AVAILABLE' }) },
    onError: (e) => setError(apiErrorMessage(e)),
  })
  const cancel = useMutation({ mutationFn: async (id: string) => api.patch(`/lots/${id}/cancel`), onSuccess: refresh, onError: (e) => alert(apiErrorMessage(e)) })
  const remove = useMutation({ mutationFn: async (id: string) => api.delete(`/lots/${id}`), onSuccess: refresh, onError: (e) => alert(apiErrorMessage(e)) })

  return (
    <Card>
      <div className="mb-3 flex items-center justify-between">
        <h3 className="text-sm font-semibold text-gray-700 dark:text-gray-200">Lotes da quadra {block.name}</h3>
        {canWrite && <Button disabled={atLimit} onClick={() => { setForm({ status: 'AVAILABLE' }); setError(null); setOpen(true) }}>{atLimit ? 'Limite de lotes atingido' : 'Novo lote'}</Button>}
      </div>
      {lots.isLoading ? <p className="text-gray-500">Carregando…</p> : (
        <Table headers={['Lote', 'Código', 'Matrícula', 'Previsto', 'Vendido', 'Status', 'Ações']}>
          {lots.data?.map((l) => (
            <tr key={l.id} className="hover:bg-gray-50">
              <td className="px-4 py-2 font-medium">{l.name}</td>
              <td className="px-4 py-2">{l.internalCode}</td>
              <td className="px-4 py-2">{l.registration ?? '—'}</td>
              <td className="px-4 py-2">{formatCurrency(l.plannedValue)}</td>
              <td className="px-4 py-2 text-green-600">{l.saleValue != null ? formatCurrency(l.saleValue) : '—'}</td>
              <td className="px-4 py-2"><Badge color={LOT_COLOR[l.status]}>{LOT_LABEL[l.status]}</Badge></td>
              <td className="px-4 py-2 text-right">
                {canWrite && <ActionsMenu items={[
                  { label: 'Editar', onClick: () => { setForm(l); setError(null); setOpen(true) } },
                  { label: 'Inativar', danger: true, disabled: l.status === 'CANCELLED', onClick: () => { if (window.confirm(`Inativar o lote "${l.name}"?`)) cancel.mutate(l.id) } },
                  { label: 'Excluir', danger: true, onClick: () => { if (window.confirm(`Excluir o lote "${l.name}"?`)) remove.mutate(l.id) } },
                ]} />}
              </td>
            </tr>
          ))}
          {lots.data?.length === 0 && <tr><td colSpan={7} className="px-4 py-4 text-center text-gray-400">Nenhum lote nesta quadra.</td></tr>}
        </Table>
      )}

      <Modal open={open} onClose={() => setOpen(false)} title={form.id ? 'Editar lote' : 'Novo lote'}>
        <form className="space-y-3" onSubmit={(e) => { e.preventDefault(); save.mutate(form) }}>
          <Field label="Nome do lote"><Input value={form.name ?? ''} onChange={(e) => setForm({ ...form, name: e.target.value })} required /></Field>
          <div className="grid grid-cols-2 gap-3">
            <Field label="Matrícula do cartório (opcional)"><Input value={form.registration ?? ''} onChange={(e) => setForm({ ...form, registration: e.target.value })} /></Field>
            <Field label="Valor previsto"><Input type="number" step="0.01" value={form.plannedValue ?? ''} onChange={(e) => setForm({ ...form, plannedValue: Number(e.target.value) })} /></Field>
          </div>
          <div className="grid grid-cols-3 gap-3">
            <Field label="Unidade"><Input value={form.unit ?? ''} onChange={(e) => setForm({ ...form, unit: e.target.value })} /></Field>
            <Field label="Área total"><Input type="number" step="0.01" value={form.totalArea ?? ''} onChange={(e) => setForm({ ...form, totalArea: Number(e.target.value) })} /></Field>
            <Field label="Status">
              <Select value={form.status} onChange={(e) => setForm({ ...form, status: e.target.value as LotStatus })}>
                <option value="AVAILABLE">Disponível</option>
                <option value="RESERVED">Reservado</option>
                <option value="SOLD">Vendido</option>
                <option value="CANCELLED">Cancelado</option>
              </Select>
            </Field>
          </div>
          <Field label="Observação"><Input value={form.notes ?? ''} onChange={(e) => setForm({ ...form, notes: e.target.value })} /></Field>
          {!form.id && <p className="text-xs text-gray-400">Código interno gerado automaticamente. O "valor da venda" é preenchido pela tela de Vendas.</p>}
          {error && <p className="text-sm text-red-600">{error}</p>}
          <div className="flex justify-end gap-2 pt-2">
            <Button type="button" variant="outline" onClick={() => setOpen(false)}>Cancelar</Button>
            <Button type="submit" disabled={save.isPending}>Salvar</Button>
          </div>
        </form>
      </Modal>
    </Card>
  )
}
