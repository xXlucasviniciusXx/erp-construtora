import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ChevronRight, Building2, ArrowLeft } from 'lucide-react'
import { api, apiErrorMessage } from '@/lib/api'
import type { Block, Development, Lot, LotStatus } from '@/lib/types'
import { useAuth } from '@/auth/AuthContext'
import { ActionsMenu } from '@/components/Menu'
import { useToast } from '@/components/Toast'
import { useConfirm } from '@/components/Confirm'
import { Badge, Button, Card, EmptyState, Field, Input, Modal, PageHeader, Select, Table, TableSkeleton, Tr } from '@/components/ui'
import { cn, formatCurrency } from '@/lib/utils'

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
  const toast = useToast()
  const confirm = useConfirm()
  const [open, setOpen] = useState(false)
  const [form, setForm] = useState<Partial<Development>>({})
  const [error, setError] = useState<string | null>(null)

  const { data, isLoading } = useQuery({
    queryKey: ['developments'],
    queryFn: async () => (await api.get<Development[]>('/developments')).data,
  })
  const save = useMutation({
    mutationFn: async (d: Partial<Development>) => d.id ? api.put(`/developments/${d.id}`, d) : api.post('/developments', d),
    onSuccess: (_r, d) => { queryClient.invalidateQueries({ queryKey: ['developments'] }); setOpen(false); setForm({}); toast.success(d.id ? 'Empreendimento atualizado.' : 'Empreendimento criado.') },
    onError: (e) => setError(apiErrorMessage(e)),
  })
  const remove = useMutation({
    mutationFn: async (id: string) => api.delete(`/developments/${id}`),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['developments'] }); toast.success('Empreendimento excluído.') },
    onError: (e) => toast.error(apiErrorMessage(e)),
  })

  async function confirmRemove(d: Development) {
    if (await confirm({ title: 'Excluir empreendimento', message: `Excluir "${d.name}" e tudo dentro dele (quadras e lotes)?`, confirmLabel: 'Excluir', danger: true })) remove.mutate(d.id)
  }

  return (
    <div>
      <PageHeader title="Empreendimentos" subtitle="Hierarquia Empreendimento → Quadra → Lote" action={canWrite && <Button onClick={() => { setForm({}); setError(null); setOpen(true) }}>Novo empreendimento</Button>} />
      {isLoading ? <TableSkeleton rows={5} cols={8} /> : (
        <Table headers={['Empreendimento', 'Código', 'Valor previsto', 'Expectativa', 'Recebido', 'Quadras', 'Lotes', 'Ações']}>
          {data?.map((d) => (
            <Tr key={d.id}>
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
                    { label: 'Excluir', danger: true, onClick: () => confirmRemove(d) },
                  ] : []),
                ]} />
              </td>
            </Tr>
          ))}
          {data?.length === 0 && (
            <tr><td colSpan={8} className="p-0">
              <EmptyState
                icon={Building2}
                title="Nenhum empreendimento"
                description="Cadastre o primeiro empreendimento para criar quadras e lotes."
                action={canWrite ? <Button onClick={() => { setForm({}); setError(null); setOpen(true) }}>Novo empreendimento</Button> : undefined}
              />
            </td></tr>
          )}
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
            <Button type="submit" loading={save.isPending}>Salvar</Button>
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
      <button onClick={onBack} className="mb-2 inline-flex items-center gap-1 text-sm text-primary hover:underline">
        <ArrowLeft className="h-4 w-4" /> Empreendimentos
      </button>
      <PageHeader title={d.name} subtitle={`Código ${d.internalCode}`} />

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
  const toast = useToast()
  const confirm = useConfirm()
  const [open, setOpen] = useState(false)
  const [form, setForm] = useState<Partial<Block>>({})
  const [error, setError] = useState<string | null>(null)
  const atLimit = development.blocksCount != null && blocks.length >= development.blocksCount

  const save = useMutation({
    mutationFn: async (b: Partial<Block>) => b.id
      ? api.put(`/blocks/${b.id}`, { ...b, developmentId: development.id })
      : api.post('/blocks', { ...b, developmentId: development.id }),
    onSuccess: (_r, b) => { queryClient.invalidateQueries({ queryKey: ['blocks', development.id] }); onChange(); setOpen(false); setForm({}); toast.success(b.id ? 'Quadra atualizada.' : 'Quadra criada.') },
    onError: (e) => setError(apiErrorMessage(e)),
  })
  const remove = useMutation({
    mutationFn: async (id: string) => api.delete(`/blocks/${id}`),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['blocks', development.id] }); onChange(); toast.success('Quadra excluída.') },
    onError: (e) => toast.error(apiErrorMessage(e)),
  })

  async function confirmRemove(b: Block) {
    if (await confirm({ title: 'Excluir quadra', message: `Excluir a quadra "${b.name}" e seus lotes?`, confirmLabel: 'Excluir', danger: true })) remove.mutate(b.id)
  }

  return (
    <Card className="mb-4">
      <div className="mb-3 flex items-center justify-between">
        <h3 className="text-sm font-semibold text-gray-700 dark:text-gray-200">Quadras ({blocks.length}{development.blocksCount != null ? `/${development.blocksCount}` : ''})</h3>
        {canWrite && <Button disabled={atLimit} onClick={() => { setForm({}); setError(null); setOpen(true) }}>{atLimit ? 'Limite atingido' : 'Nova quadra'}</Button>}
      </div>
      <Table headers={['Quadra', 'Código', 'Matrícula', 'Área', 'Lotes', 'Ações']}>
        {blocks.map((b) => (
          <tr
            key={b.id}
            className={cn('row-hover cursor-pointer', selectedBlock?.id === b.id && 'bg-primary/5')}
            onClick={() => onSelect(b)}
          >
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
                  { label: 'Excluir', danger: true, onClick: () => confirmRemove(b) },
                ]} />}
              </div>
            </td>
          </tr>
        ))}
        {blocks.length === 0 && <tr><td colSpan={6} className="px-4 py-4 text-center text-sm text-gray-400">Nenhuma quadra cadastrada.</td></tr>}
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
            <Button type="submit" loading={save.isPending}>Salvar</Button>
          </div>
        </form>
      </Modal>
    </Card>
  )
}

function LotsSection({ development, block, canWrite, onChange }: { development: Development; block: Block; canWrite: boolean; onChange: () => void }) {
  const queryClient = useQueryClient()
  const toast = useToast()
  const confirm = useConfirm()
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
    onSuccess: (_r, l) => { refresh(); setOpen(false); setForm({ status: 'AVAILABLE' }); toast.success(l.id ? 'Lote atualizado.' : 'Lote criado.') },
    onError: (e) => setError(apiErrorMessage(e)),
  })
  const cancel = useMutation({ mutationFn: async (id: string) => api.patch(`/lots/${id}/cancel`), onSuccess: () => { refresh(); toast.success('Lote inativado.') }, onError: (e) => toast.error(apiErrorMessage(e)) })
  const remove = useMutation({ mutationFn: async (id: string) => api.delete(`/lots/${id}`), onSuccess: () => { refresh(); toast.success('Lote excluído.') }, onError: (e) => toast.error(apiErrorMessage(e)) })

  async function confirmCancel(l: Lot) {
    if (await confirm({ title: 'Inativar lote', message: `Inativar o lote "${l.name}"?`, confirmLabel: 'Inativar', danger: true })) cancel.mutate(l.id)
  }
  async function confirmRemove(l: Lot) {
    if (await confirm({ title: 'Excluir lote', message: `Excluir o lote "${l.name}"?`, confirmLabel: 'Excluir', danger: true })) remove.mutate(l.id)
  }

  return (
    <Card>
      <div className="mb-3 flex items-center justify-between">
        <h3 className="text-sm font-semibold text-gray-700 dark:text-gray-200">Lotes da quadra {block.name}</h3>
        {canWrite && <Button disabled={atLimit} onClick={() => { setForm({ status: 'AVAILABLE' }); setError(null); setOpen(true) }}>{atLimit ? 'Limite de lotes atingido' : 'Novo lote'}</Button>}
      </div>
      {lots.isLoading ? <TableSkeleton rows={4} cols={7} /> : (
        <Table headers={['Lote', 'Código', 'Matrícula', 'Previsto', 'Vendido', 'Status', 'Ações']}>
          {lots.data?.map((l) => (
            <Tr key={l.id}>
              <td className="px-4 py-2 font-medium">{l.name}</td>
              <td className="px-4 py-2">{l.internalCode}</td>
              <td className="px-4 py-2">{l.registration ?? '—'}</td>
              <td className="px-4 py-2">{formatCurrency(l.plannedValue)}</td>
              <td className="px-4 py-2 text-green-600">{l.saleValue != null ? formatCurrency(l.saleValue) : '—'}</td>
              <td className="px-4 py-2"><Badge dot color={LOT_COLOR[l.status]}>{LOT_LABEL[l.status]}</Badge></td>
              <td className="px-4 py-2 text-right">
                {canWrite && <ActionsMenu items={[
                  { label: 'Editar', onClick: () => { setForm(l); setError(null); setOpen(true) } },
                  { label: 'Inativar', danger: true, disabled: l.status === 'CANCELLED', onClick: () => confirmCancel(l) },
                  { label: 'Excluir', danger: true, onClick: () => confirmRemove(l) },
                ]} />}
              </td>
            </Tr>
          ))}
          {lots.data?.length === 0 && <tr><td colSpan={7} className="px-4 py-4 text-center text-sm text-gray-400">Nenhum lote nesta quadra.</td></tr>}
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
            <Button type="submit" loading={save.isPending}>Salvar</Button>
          </div>
        </form>
      </Modal>
    </Card>
  )
}
