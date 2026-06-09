import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ChevronRight, Building2, ArrowLeft, LayoutList, Grid3x3, Clock } from 'lucide-react'
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
  const canWrite = hasPermission('EMPREENDIMENTOS_EDIT')
  const [manage, setManage] = useState<Development | null>(null)
  const [tab, setTab] = useState<'devs' | 'lots'>('devs')

  if (manage) return <DevelopmentManager development={manage} canWrite={canWrite} onBack={() => setManage(null)} />
  return (
    <div className="space-y-4">
      <div className="inline-flex rounded-lg border border-gray-200 bg-gray-50 p-0.5 dark:border-gray-700 dark:bg-gray-800">
        {([['devs', 'Empreendimentos'], ['lots', 'Lotes']] as const).map(([key, label]) => (
          <button
            key={key}
            onClick={() => setTab(key)}
            className={cn(
              'rounded-md px-4 py-1.5 text-sm font-medium transition',
              tab === key ? 'bg-white text-gray-900 shadow-sm dark:bg-gray-900 dark:text-white' : 'text-gray-500 hover:text-gray-700 dark:hover:text-gray-300',
            )}
          >
            {label}
          </button>
        ))}
      </div>
      {tab === 'devs'
        ? <DevelopmentsList canWrite={canWrite} onManage={setManage} />
        : <LotsExplorer />}
    </div>
  )
}

/* ---------------- Explorador de Lotes (lista plana filtrável) ---------------- */
function LotsExplorer() {
  const [developmentId, setDevelopmentId] = useState('')
  const [clientId, setClientId] = useState('')
  const [status, setStatus] = useState<LotStatus | ''>('')

  const developments = useQuery({
    queryKey: ['developments'],
    queryFn: async () => (await api.get<Development[]>('/developments')).data,
  })

  const lots = useQuery({
    queryKey: ['lots-explorer', developmentId],
    queryFn: async () => (await api.get<Lot[]>('/lots', { params: { developmentId } })).data,
    enabled: !!developmentId,
  })

  // Opções de cliente derivadas dos compradores presentes nos lotes carregados.
  const clientOptions = (() => {
    const map = new Map<string, string>()
    for (const l of lots.data ?? []) if (l.clientId && l.clientName) map.set(l.clientId, l.clientName)
    return [...map.entries()].sort((a, b) => a[1].localeCompare(b[1]))
  })()

  const filtered = (lots.data ?? [])
    .filter((l) => !clientId || l.clientId === clientId)
    .filter((l) => !status || l.status === status)

  return (
    <div>
      <PageHeader title="Lotes" subtitle="Selecione um empreendimento para listar seus lotes e compradores" />
      <Card className="mb-4">
        <div className="grid grid-cols-1 gap-3 sm:grid-cols-3">
          <Field label="Empreendimento *">
            <Select value={developmentId} onChange={(e) => { setDevelopmentId(e.target.value); setClientId('') }}>
              <option value="">— selecione —</option>
              {(developments.data ?? []).map((d) => <option key={d.id} value={d.id}>{d.name}</option>)}
            </Select>
          </Field>
          <Field label="Cliente (comprador)">
            <Select value={clientId} onChange={(e) => setClientId(e.target.value)} disabled={!developmentId}>
              <option value="">Todos</option>
              {clientOptions.map(([id, name]) => <option key={id} value={id}>{name}</option>)}
            </Select>
          </Field>
          <Field label="Status">
            <Select value={status} onChange={(e) => setStatus(e.target.value as LotStatus | '')} disabled={!developmentId}>
              <option value="">Todos</option>
              {(['AVAILABLE', 'RESERVED', 'SOLD', 'CANCELLED'] as LotStatus[]).map((s) => <option key={s} value={s}>{LOT_LABEL[s]}</option>)}
            </Select>
          </Field>
        </div>
      </Card>

      {!developmentId ? (
        <EmptyState icon={Building2} title="Selecione um empreendimento" description="O filtro de empreendimento é obrigatório para listar os lotes." />
      ) : lots.isLoading ? (
        <TableSkeleton rows={6} cols={6} />
      ) : (
        <Table headers={['Lote', 'Quadra', 'Cliente', 'Status', 'Valor previsto', 'Valor vendido']}>
          {filtered.map((l) => (
            <Tr key={l.id}>
              <td className="px-4 py-2 font-medium">{l.name}<div className="text-xs text-gray-400">{l.internalCode}</div></td>
              <td className="px-4 py-2">{l.blockName}</td>
              <td className="px-4 py-2">{l.clientName ?? <span className="text-gray-400">—</span>}</td>
              <td className="px-4 py-2"><Badge dot color={LOT_COLOR[l.status]}>{LOT_LABEL[l.status]}</Badge></td>
              <td className="px-4 py-2">{formatCurrency(l.plannedValue)}</td>
              <td className="px-4 py-2 text-green-600">{l.saleValue != null ? formatCurrency(l.saleValue) : '—'}</td>
            </Tr>
          ))}
          {filtered.length === 0 && (
            <tr><td colSpan={6} className="p-0">
              <EmptyState icon={Building2} title="Nenhum lote encontrado" description="Ajuste os filtros de cliente/status." />
            </td></tr>
          )}
        </Table>
      )}
    </div>
  )
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
          <div className="grid grid-cols-3 gap-3">
            <Field label="Categoria">
              <Select value={form.category ?? 'CORRETORA'} onChange={(e) => setForm({ ...form, category: e.target.value as Development['category'] })}>
                <option value="CORRETORA">Corretora</option>
                <option value="TERRENISTA">Terrenista</option>
              </Select>
            </Field>
            {form.category === 'TERRENISTA' && (
              <>
                <Field label="Qtd. de terrenistas">
                  <Input type="number" min={0} value={form.terrenistaCount ?? ''}
                    onChange={(e) => setForm({ ...form, terrenistaCount: e.target.value ? Number(e.target.value) : null })} />
                </Field>
                <Field label="Percentual de divisão (%)">
                  <Input type="number" step="0.01" min={0} max={100} value={form.divisionPercent ?? ''}
                    onChange={(e) => setForm({ ...form, divisionPercent: e.target.value ? Number(e.target.value) : null })} />
                </Field>
              </>
            )}
          </div>
          {form.category === 'TERRENISTA' && (
            <p className="rounded-md bg-blue-50 px-3 py-2 text-xs text-blue-700 dark:bg-blue-900/20 dark:text-blue-300">
              Empreendimento <strong>Terrenista</strong>: os campos de quantidade e percentual ficam disponíveis para as regras de divisão financeira (uso futuro).
            </p>
          )}
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
  const [view, setView] = useState<'manage' | 'map'>('manage')

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
      <PageHeader
        title={d.name}
        subtitle={`Código ${d.internalCode}`}
        action={
          <div className="inline-flex rounded-lg border border-gray-200 bg-gray-50 p-0.5 dark:border-gray-700 dark:bg-gray-800">
            {(['manage', 'map'] as const).map((v) => (
              <button
                key={v}
                onClick={() => setView(v)}
                className={cn(
                  'flex items-center gap-1.5 rounded-md px-3 py-1.5 text-sm font-medium transition',
                  view === v ? 'bg-white text-gray-900 shadow-sm dark:bg-gray-700 dark:text-gray-100' : 'text-gray-500 hover:text-gray-700 dark:hover:text-gray-300',
                )}
              >
                {v === 'manage' ? <><LayoutList className="h-4 w-4" /> Gestão</> : <><Grid3x3 className="h-4 w-4" /> Mapa</>}
              </button>
            ))}
          </div>
        }
      />

      <div className="mb-4 grid grid-cols-2 gap-3 sm:grid-cols-3">
        <Card><div className="text-xs uppercase text-gray-500 dark:text-gray-400">Valor previsto total</div><div className="mt-1 text-lg font-semibold text-blue-700">{formatCurrency(d.plannedTotal)}</div></Card>
        <Card><div className="text-xs uppercase text-gray-500 dark:text-gray-400">Valor expectativa</div><div className="mt-1 text-lg font-semibold text-amber-600">{formatCurrency(d.expectedValue)}</div></Card>
        <Card><div className="text-xs uppercase text-gray-500 dark:text-gray-400">Valor recebido</div><div className="mt-1 text-lg font-semibold text-green-600">{formatCurrency(d.receivedTotal)}</div></Card>
      </div>

      {view === 'map' ? (
        <DevelopmentMap development={d} blocks={blocks.data ?? []} />
      ) : (
        <>
          {/* Quadras */}
          <BlocksSection
            development={d} blocks={blocks.data ?? []} canWrite={canWrite}
            selectedBlock={selectedBlock} onSelect={setSelectedBlock} onChange={refreshAll}
          />

          {/* Lotes da quadra selecionada */}
          {selectedBlock && (
            <LotsSection development={d} block={selectedBlock} canWrite={canWrite} onChange={refreshAll} />
          )}
        </>
      )}
    </div>
  )
}

/* ---------------- Mapa visual de lotes (espelho de vendas) ---------------- */
const CELL_STYLE: Record<LotStatus, string> = {
  AVAILABLE: 'bg-green-100 text-green-800 border-green-300 hover:bg-green-200 dark:bg-green-900/30 dark:text-green-300 dark:border-green-800',
  RESERVED: 'bg-amber-100 text-amber-800 border-amber-300 hover:bg-amber-200 dark:bg-amber-900/30 dark:text-amber-300 dark:border-amber-800',
  SOLD: 'bg-blue-100 text-blue-800 border-blue-300 hover:bg-blue-200 dark:bg-blue-900/30 dark:text-blue-300 dark:border-blue-800',
  CANCELLED: 'bg-gray-100 text-gray-400 border-gray-300 line-through hover:bg-gray-200 dark:bg-gray-700/40 dark:text-gray-500 dark:border-gray-600',
}

function DevelopmentMap({ development, blocks }: { development: Development; blocks: Block[] }) {
  const [selectedLot, setSelectedLot] = useState<Lot | null>(null)
  const lots = useQuery({
    queryKey: ['lots-dev', development.id],
    queryFn: async () => (await api.get<Lot[]>('/lots', { params: { developmentId: development.id } })).data,
  })

  const counts = (lots.data ?? []).reduce(
    (acc, l) => { acc[l.status] = (acc[l.status] ?? 0) + 1; return acc },
    {} as Record<string, number>,
  )

  if (lots.isLoading) return <Card><div className="skeleton h-40 w-full" /></Card>

  return (
    <Card>
      {/* Legenda + contadores */}
      <div className="mb-4 flex flex-wrap items-center gap-4 text-xs">
        {(Object.keys(LOT_LABEL) as LotStatus[]).map((s) => (
          <span key={s} className="inline-flex items-center gap-1.5">
            <span className={cn('inline-block h-3 w-3 rounded border', CELL_STYLE[s])} />
            {LOT_LABEL[s]} <span className="font-semibold text-gray-700 dark:text-gray-200">({counts[s] ?? 0})</span>
          </span>
        ))}
      </div>

      {blocks.length === 0 && <p className="py-6 text-center text-sm text-gray-400">Nenhuma quadra cadastrada ainda.</p>}

      <div className="space-y-4">
        {blocks.map((b) => {
          const blockLots = (lots.data ?? []).filter((l) => l.blockId === b.id)
          return (
            <div key={b.id}>
              <div className="mb-2 flex items-center gap-2 text-sm font-medium text-gray-700 dark:text-gray-200">
                <span>Quadra {b.name}</span>
                <span className="text-xs font-normal text-gray-400">{b.internalCode} · {blockLots.length} lote(s)</span>
              </div>
              {blockLots.length === 0 ? (
                <p className="text-xs text-gray-400">Sem lotes nesta quadra.</p>
              ) : (
                <div className="flex flex-wrap gap-2">
                  {blockLots.map((l) => (
                    <button
                      key={l.id}
                      onClick={() => setSelectedLot(l)}
                      title={`${l.name} · ${LOT_LABEL[l.status]}${l.reservationExpiresAt ? ' · expira em ' + reservationCountdown(l.reservationExpiresAt) : ''}${l.saleValue ? ' · ' + formatCurrency(l.saleValue) : ''}`}
                      className={cn(
                        'flex h-16 w-16 flex-col items-center justify-center rounded-lg border text-center text-xs font-medium transition',
                        CELL_STYLE[l.status],
                      )}
                    >
                      <span className="truncate px-1">{l.name}</span>
                      {l.status === 'RESERVED' && l.reservationExpiresAt && (
                        <span className="flex items-center gap-0.5 text-[9px] opacity-80">
                          <Clock className="h-2.5 w-2.5" />{reservationCountdown(l.reservationExpiresAt)}
                        </span>
                      )}
                    </button>
                  ))}
                </div>
              )}
            </div>
          )
        })}
      </div>

      {/* Detalhe do lote ao clicar */}
      {selectedLot && (
        <Modal open onClose={() => setSelectedLot(null)} title={`Lote ${selectedLot.name}`} size="sm">
          <div className="grid grid-cols-2 gap-3 text-sm">
            <div><div className="text-xs text-gray-400">Código</div><div>{selectedLot.internalCode}</div></div>
            <div><div className="text-xs text-gray-400">Status</div><Badge dot color={LOT_COLOR[selectedLot.status]}>{LOT_LABEL[selectedLot.status]}</Badge></div>
            <div><div className="text-xs text-gray-400">Matrícula</div><div>{selectedLot.registration ?? '—'}</div></div>
            <div><div className="text-xs text-gray-400">Área total</div><div>{selectedLot.totalArea ?? '—'}</div></div>
            <div><div className="text-xs text-gray-400">Valor previsto</div><div>{formatCurrency(selectedLot.plannedValue)}</div></div>
            <div><div className="text-xs text-gray-400">Valor vendido</div><div className="text-green-600">{selectedLot.saleValue != null ? formatCurrency(selectedLot.saleValue) : '—'}</div></div>
          </div>
        </Modal>
      )}
    </Card>
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

/** Formata tempo restante de uma reserva (ex: "2h 15min" ou "expirada"). */
function reservationCountdown(expiresAt: string | null | undefined): string {
  if (!expiresAt) return ''
  const diff = new Date(expiresAt).getTime() - Date.now()
  if (diff <= 0) return 'expirada'
  const h = Math.floor(diff / 3_600_000)
  const m = Math.floor((diff % 3_600_000) / 60_000)
  return h > 0 ? `${h}h ${m}min` : `${m}min`
}

function LotsSection({ development, block, canWrite, onChange }: { development: Development; block: Block; canWrite: boolean; onChange: () => void }) {
  const queryClient = useQueryClient()
  const toast = useToast()
  const confirm = useConfirm()
  const [open, setOpen] = useState(false)
  const [reserveOpen, setReserveOpen] = useState(false)
  const [reserveLot, setReserveLot] = useState<Lot | null>(null)
  const [reserveHours, setReserveHours] = useState(24)
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
  const doReserve = useMutation({
    mutationFn: async ({ id, hours }: { id: string; hours: number }) =>
      api.patch(`/lots/${id}/reserve`, { hours }),
    onSuccess: () => { refresh(); setReserveOpen(false); toast.success('Lote reservado.') },
    onError: (e) => toast.error(apiErrorMessage(e)),
  })
  const doRelease = useMutation({
    mutationFn: async (id: string) => api.patch(`/lots/${id}/release`),
    onSuccess: () => { refresh(); toast.success('Reserva liberada.') },
    onError: (e) => toast.error(apiErrorMessage(e)),
  })

  async function confirmCancel(l: Lot) {
    if (await confirm({ title: 'Inativar lote', message: `Inativar o lote "${l.name}"?`, confirmLabel: 'Inativar', danger: true })) cancel.mutate(l.id)
  }
  async function confirmRemove(l: Lot) {
    if (await confirm({ title: 'Excluir lote', message: `Excluir o lote "${l.name}"?`, confirmLabel: 'Excluir', danger: true })) remove.mutate(l.id)
  }
  async function confirmRelease(l: Lot) {
    if (await confirm({ title: 'Liberar reserva', message: `Liberar a reserva do lote "${l.name}"?`, confirmLabel: 'Liberar' }))
      doRelease.mutate(l.id)
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
              <td className="px-4 py-2">
                <Badge dot color={LOT_COLOR[l.status]}>{LOT_LABEL[l.status]}</Badge>
                {l.status === 'RESERVED' && l.reservationExpiresAt && (
                  <span className="ml-1.5 inline-flex items-center gap-0.5 text-[10px] text-amber-600 dark:text-amber-400">
                    <Clock className="h-3 w-3" />{reservationCountdown(l.reservationExpiresAt)}
                  </span>
                )}
              </td>
              <td className="px-4 py-2 text-right">
                {canWrite && <ActionsMenu items={[
                  { label: 'Editar', onClick: () => { setForm(l); setError(null); setOpen(true) } },
                  { label: 'Reservar', disabled: l.status !== 'AVAILABLE', onClick: () => { setReserveLot(l); setReserveHours(24); setReserveOpen(true) } },
                  { label: 'Liberar reserva', disabled: l.status !== 'RESERVED', onClick: () => confirmRelease(l) },
                  { label: 'Inativar', danger: true, disabled: l.status === 'CANCELLED', onClick: () => confirmCancel(l) },
                  { label: 'Excluir', danger: true, onClick: () => confirmRemove(l) },
                ]} />}
              </td>
            </Tr>
          ))}
          {lots.data?.length === 0 && <tr><td colSpan={7} className="px-4 py-4 text-center text-sm text-gray-400">Nenhum lote nesta quadra.</td></tr>}
        </Table>
      )}

      {/* Modal de reserva */}
      <Modal open={reserveOpen} onClose={() => setReserveOpen(false)} title={`Reservar lote ${reserveLot?.name}`} size="sm">
        <div className="space-y-3">
          <p className="text-sm text-gray-500 dark:text-gray-400">
            Reserva por tempo determinado. Após o prazo, o lote volta a <strong>Disponível</strong> automaticamente.
          </p>
          <Field label="Duração da reserva (horas)">
            <Input type="number" min={1} max={720} value={reserveHours}
              onChange={(e) => setReserveHours(Number(e.target.value))} />
          </Field>
          <div className="flex justify-end gap-2 pt-1">
            <Button type="button" variant="outline" onClick={() => setReserveOpen(false)}>Cancelar</Button>
            <Button loading={doReserve.isPending} onClick={() => reserveLot && doReserve.mutate({ id: reserveLot.id, hours: reserveHours })}>
              Reservar
            </Button>
          </div>
        </div>
      </Modal>

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
