import { useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api, apiErrorMessage } from '@/lib/api'
import { openApiPdf } from '@/lib/contracts'
import type { Client, ContractDocument, Lot, NamedItem, Page, Sale } from '@/lib/types'
import type { ComboOption } from '@/components/Combobox'
import { FileSignature, Plus } from 'lucide-react'
import { useAuth } from '@/auth/AuthContext'
import { ActionsMenu } from '@/components/Menu'
import { Combobox } from '@/components/Combobox'
import { QuickCreateClientModal } from '@/components/QuickCreateClientModal'
import { useToast } from '@/components/Toast'
import { Badge, Button, EmptyState, Field, Input, Modal, PageHeader, Pagination, Select, Table, TableSkeleton, Tr } from '@/components/ui'
import { formatCurrency, formatDate, formatDateTime } from '@/lib/utils'

interface DistratoForm { distratoDate: string; reason: string; refundAmount: number; retainedAmount: number; rule: string; ruleDetail: string }

const INSTALLMENT_STATUS: Record<string, { label: string; color: string }> = {
  PAID: { label: 'Paga', color: 'green' }, OVERDUE: { label: 'Vencida', color: 'red' },
  OPEN: { label: 'Aberta', color: 'gray' }, CANCELLED: { label: 'Cancelada', color: 'gray' },
}

const PURCHASE_WITH_DOWN = 'Entrada + parcelas'
const PURCHASE_TYPES = ['À vista', PURCHASE_WITH_DOWN, 'Financiamento próprio', 'Outro']
// Fallback enquanto a API ainda não respondeu
const PAYMENT_METHODS_FALLBACK = ['Boleto', 'PIX', 'Transferência bancária', 'Cartão', 'Dinheiro', 'Outro']
const CORRECTION_INDEXES_FALLBACK = ['Sem correção', 'INCC', 'IPCA', 'IGP-M', 'Juros fixo mensal', 'Outro']
const SALE_STATUS: Record<string, string> = { ACTIVE: 'Ativa', COMPLETED: 'Quitada', CANCELLED: 'Cancelada' }

interface SaleForm {
  clientId: string
  lotId: string
  totalValue: number
  downPayment: number
  installmentsCount: number
  firstDueDate: string
  purchaseType: string
  paymentMethod: string
  correctionIndex: string
  interestRate: number
  penaltyRate: number
}
const EMPTY: SaleForm = {
  clientId: '', lotId: '', totalValue: 0, downPayment: 0, installmentsCount: 12,
  firstDueDate: '', purchaseType: PURCHASE_WITH_DOWN, paymentMethod: 'Boleto', correctionIndex: 'Sem correção',
  interestRate: 1, penaltyRate: 2,
}

export function SalesPage() {
  const { hasPermission } = useAuth()
  const queryClient = useQueryClient()
  const toast = useToast()
  const canWrite = hasPermission('VENDAS_EDIT')
  const canContract = hasPermission('VENDAS_EDIT')
  const canCreateClient = hasPermission('CLIENTES_EDIT')

  const [quickClient, setQuickClient] = useState(false)
  const [modalOpen, setModalOpen] = useState(false)
  const [editingId, setEditingId] = useState<string | null>(null)
  const [form, setForm] = useState<SaleForm>(EMPTY)
  const [error, setError] = useState<string | null>(null)
  const [view, setView] = useState<Sale | null>(null)
  const [q, setQ] = useState('')
  const [statusFilter, setStatusFilter] = useState('')
  const [page, setPage] = useState(0)

  // Distrato + histórico de documentos
  const [distratoFor, setDistratoFor] = useState<Sale | null>(null)
  const [distratoForm, setDistratoForm] = useState<DistratoForm>({ distratoDate: '', reason: '', refundAmount: 0, retainedAmount: 0, rule: '', ruleDetail: '' })
  const [distratoError, setDistratoError] = useState<string | null>(null)
  const [docsFor, setDocsFor] = useState<Sale | null>(null)

  // ---- search state para os comboboxes (server-side) ----
  const [clientSearch, setClientSearch] = useState('')
  const [lotSearch, setLotSearch] = useState('')
  /** Semente: item selecionado atualmente (para mostrar a label mesmo antes de buscar) */
  const [clientSeed, setClientSeed] = useState<ComboOption | null>(null)
  const [lotSeed, setLotSeed] = useState<ComboOption | null>(null)

  const sales = useQuery({
    queryKey: ['sales', q, statusFilter, page],
    queryFn: async () => (await api.get<Page<Sale>>('/sales', {
      params: { q: q || undefined, status: statusFilter || undefined, page, size: 20 },
    })).data,
  })

  // Clientes — server-side: busca apenas quando o modal está aberto
  const clientsQuery = useQuery({
    queryKey: ['clients-search', clientSearch],
    queryFn: async () => (await api.get<Page<Client>>('/clients', {
      params: { q: clientSearch || undefined, size: 20 },
    })).data.content,
    enabled: modalOpen,
    staleTime: 10_000,
  })

  // Lotes — server-side: busca apenas quando o modal está aberto
  const lotsQuery = useQuery({
    queryKey: ['lots-search', lotSearch],
    queryFn: async () => (await api.get<Lot[]>('/lots', {
      params: { q: lotSearch || undefined },
    })).data,
    enabled: modalOpen,
    staleTime: 10_000,
  })

  // Opções com semente: garante que a seleção atual apareça mesmo antes de digitar
  const clientOptions = useMemo<ComboOption[]>(() => {
    const opts: ComboOption[] = (clientsQuery.data ?? []).map((c) => ({ value: c.id, label: c.name, hint: c.document }))
    if (clientSeed && !opts.find((o) => o.value === clientSeed.value)) return [clientSeed, ...opts]
    return opts
  }, [clientsQuery.data, clientSeed])

  const lotOptions = useMemo<ComboOption[]>(() => {
    const opts: ComboOption[] = (lotsQuery.data ?? [])
      .filter((l) => l.status === 'AVAILABLE' || l.id === form.lotId)
      .map((l) => ({ value: l.id, label: l.label, hint: `${l.internalCode} · previsto ${formatCurrency(l.plannedValue)}` }))
    if (lotSeed && !opts.find((o) => o.value === lotSeed.value)) return [lotSeed, ...opts]
    return opts
  }, [lotsQuery.data, lotSeed, form.lotId])

  const paymentMethods = useQuery({
    queryKey: ['payment-methods'],
    queryFn: async () => (await api.get<NamedItem[]>('/lists/payment-methods')).data,
  })
  const correctionIndexes = useQuery({
    queryKey: ['correction-indexes'],
    queryFn: async () => (await api.get<NamedItem[]>('/lists/correction-indexes')).data,
  })
  const pmList = paymentMethods.data?.map((p) => p.name) ?? PAYMENT_METHODS_FALLBACK
  const ciList = correctionIndexes.data?.map((c) => c.name) ?? CORRECTION_INDEXES_FALLBACK

  const save = useMutation({
    mutationFn: async (payload: SaleForm) =>
      editingId ? api.put(`/sales/${editingId}`, payload) : api.post('/sales', payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['sales'] })
      queryClient.invalidateQueries({ queryKey: ['lots-search'] })
      queryClient.invalidateQueries({ queryKey: ['dashboard-analytics'] })
      setModalOpen(false); setForm(EMPTY)
      toast.success(editingId ? 'Venda atualizada com sucesso.' : 'Venda registrada e parcelas geradas.')
      setEditingId(null)
    },
    onError: (e) => setError(apiErrorMessage(e)),
  })

  const distrato = useMutation({
    mutationFn: async ({ id, payload }: { id: string; payload: DistratoForm }) =>
      api.post(`/sales/${id}/distrato`, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['sales'] })
      queryClient.invalidateQueries({ queryKey: ['lots-search'] })
      queryClient.invalidateQueries({ queryKey: ['dashboard-analytics'] })
      setDistratoFor(null)
      toast.success('Venda distratada. O lote foi liberado.')
    },
    onError: (e) => setDistratoError(apiErrorMessage(e)),
  })

  // Documentos arquivados da venda selecionada
  const documents = useQuery({
    queryKey: ['contract-documents', docsFor?.id],
    queryFn: async () => (await api.get<ContractDocument[]>(`/contracts/sales/${docsFor!.id}/documents`)).data,
    enabled: !!docsFor,
  })

  const filtered = sales.data?.content ?? []  // filtrado/paginado no servidor

  // ---- helpers do formulário ----
  // plannedValue do lote selecionado (para exibir o "valor esperado")
  const expectedValue = lotsQuery.data?.find((l) => l.id === form.lotId)?.plannedValue ?? 0
  const entradaDisabled = form.purchaseType !== PURCHASE_WITH_DOWN

  function openNew() {
    setEditingId(null); setForm(EMPTY); setError(null)
    setClientSearch(''); setLotSearch('')
    setClientSeed(null); setLotSeed(null)
    setModalOpen(true)
  }
  function openEdit(s: Sale) {
    setEditingId(s.id)
    setForm({
      clientId: s.clientId, lotId: s.lotId, totalValue: s.totalValue, downPayment: s.downPayment,
      installmentsCount: s.installmentsCount, firstDueDate: s.firstDueDate.slice(0, 10),
      purchaseType: s.purchaseType ?? PURCHASE_WITH_DOWN, paymentMethod: s.paymentMethod ?? 'Boleto',
      correctionIndex: s.correctionIndex ?? 'Sem correção',
      interestRate: s.interestRate ?? 0, penaltyRate: s.penaltyRate ?? 0,
    })
    // Semear os comboboxes com a seleção atual para exibir a label sem precisar buscar
    setClientSeed({ value: s.clientId, label: s.clientName, hint: '' })
    setLotSeed({ value: s.lotId, label: s.propertyLabel, hint: '' })
    setClientSearch(''); setLotSearch('')
    setError(null); setModalOpen(true)
  }
  function onSelectLot(id: string) {
    const lot = lotsQuery.data?.find((l) => l.id === id)
    setLotSeed(lot ? { value: lot.id, label: lot.label, hint: '' } : null)
    setForm((f) => ({ ...f, lotId: id, totalValue: f.totalValue || Number(lot?.plannedValue ?? 0) }))
  }
  function openPdf(path: string) {
    openApiPdf(path).catch(() => toast.error('Não foi possível gerar o documento.'))
  }
  function downloadContract(saleId: string) { openPdf(`/contracts/sales/${saleId}/pdf`) }
  function openDistrato(s: Sale) {
    setDistratoFor(s)
    setDistratoForm({ distratoDate: new Date().toISOString().slice(0, 10), reason: '', refundAmount: s.paidAmount ?? 0, retainedAmount: 0, rule: '', ruleDetail: '' })
    setDistratoError(null)
  }

  return (
    <div>
      <PageHeader title="Vendas" subtitle="Vendas de lotes com geração automática de parcelas" action={canWrite && <Button onClick={openNew}>Nova venda</Button>} />

      <div className="mb-4 grid grid-cols-1 gap-3 sm:grid-cols-3">
        <Input placeholder="Buscar por cliente ou lote…" value={q} onChange={(e) => { setQ(e.target.value); setPage(0) }} />
        <Select value={statusFilter} onChange={(e) => { setStatusFilter(e.target.value); setPage(0) }}>
          <option value="">Todos os status</option>
          <option value="ACTIVE">Ativa</option>
          <option value="COMPLETED">Quitada</option>
          <option value="CANCELLED">Cancelada</option>
        </Select>
      </div>

      {sales.isLoading ? <TableSkeleton rows={6} cols={6} /> : (
        <Table headers={['Cliente', 'Lote', 'Total', 'Parcelas (Qtd / Pagas)', 'Status', 'Ações']}>
          {filtered.map((s) => (
            <Tr key={s.id}>
              <td className="px-4 py-2 font-medium">
                {s.clientName}
                {s.contractNumber && <div className="text-[11px] font-normal text-gray-400">{s.contractNumber}</div>}
              </td>
              <td className="px-4 py-2">{s.propertyLabel}</td>
              <td className="px-4 py-2">{formatCurrency(s.totalValue)}</td>
              <td className="px-4 py-2">{s.installmentsCount} / <span className="font-medium text-green-600">{s.paidInstallments ?? 0}</span></td>
              <td className="px-4 py-2">
                <Badge dot color={s.status === 'ACTIVE' ? 'blue' : s.status === 'COMPLETED' ? 'green' : 'gray'}>
                  {SALE_STATUS[s.status] ?? s.status}
                </Badge>
              </td>
              <td className="px-4 py-2 text-right">
                <ActionsMenu items={[
                  { label: 'Visualizar venda', onClick: () => setView(s) },
                  ...(canWrite && s.status !== 'CANCELLED' ? [{ label: 'Editar venda', onClick: () => openEdit(s) }] : []),
                  { label: 'Gerar contrato', onClick: () => downloadContract(s.id), disabled: !canContract },
                  { label: 'Histórico de documentos', onClick: () => setDocsFor(s) },
                  ...(canWrite && s.status !== 'CANCELLED'
                    ? [{ label: 'Distratar venda', danger: true, onClick: () => openDistrato(s) }]
                    : []),
                  ...(s.status === 'CANCELLED' && s.distratoDate
                    ? [{ label: 'Gerar distrato (PDF)', onClick: () => openPdf(`/contracts/sales/${s.id}/distrato/pdf`), disabled: !canContract }]
                    : []),
                ]} />
              </td>
            </Tr>
          ))}
          {filtered.length === 0 && (
            <tr><td colSpan={6} className="p-0">
              <EmptyState
                icon={FileSignature}
                title="Nenhuma venda encontrada"
                description={q || statusFilter ? 'Ajuste a busca ou os filtros.' : 'Registre a primeira venda para gerar as parcelas automaticamente.'}
                action={canWrite && !q && !statusFilter ? <Button onClick={openNew}>Nova venda</Button> : undefined}
              />
            </td></tr>
          )}
        </Table>
      )}
      {sales.data && <Pagination page={sales.data.number} totalPages={sales.data.totalPages} totalElements={sales.data.totalElements} onChange={setPage} />}

      {/* Modal: visualizar venda (detalhes + parcelas + saldos) */}
      {view && (
        <Modal open onClose={() => setView(null)} title={`Venda — ${view.clientName}`}>
          <div className="mb-3 grid grid-cols-2 gap-2 text-sm">
            <Info label="Nº do contrato" value={view.contractNumber} />
            <Info label="Lote" value={view.propertyLabel} />
            <Info label="Forma de compra" value={view.purchaseType} />
            <Info label="Valor esperado" value={formatCurrency(view.expectedValue)} />
            <Info label="Valor vendido" value={formatCurrency(view.totalValue)} />
            <Info label="Entrada" value={formatCurrency(view.downPayment)} />
            <Info label="Forma de pagamento" value={view.paymentMethod} />
          </div>
          {view.status === 'CANCELLED' && view.distratoDate && (
            <div className="mb-3 rounded-lg border border-amber-200 bg-amber-50 p-3 text-sm dark:border-amber-900/40 dark:bg-amber-900/20">
              <div className="mb-1 font-medium text-amber-700 dark:text-amber-300">Distrato em {formatDate(view.distratoDate)}</div>
              <div className="grid grid-cols-2 gap-2">
                <Info label="Devolvido ao comprador" value={formatCurrency(view.distratoRefundAmount)} />
                <Info label="Retido pela vendedora" value={formatCurrency(view.distratoRetainedAmount)} />
              </div>
              {view.distratoReason && <div className="mt-1 text-xs text-gray-600 dark:text-gray-300">Motivo: {view.distratoReason}</div>}
              {view.distratoRule && <div className="mt-1 text-xs text-amber-800 dark:text-amber-300">Regra aplicada: <strong>{view.distratoRule}</strong></div>}
              {view.distratoRuleDetail && <div className="mt-0.5 whitespace-pre-line text-xs text-gray-600 dark:text-gray-300">Memória de cálculo: {view.distratoRuleDetail}</div>}
            </div>
          )}
          <div className="mb-2 text-sm">
            Pago: <span className="font-medium text-green-600">{formatCurrency(view.paidAmount ?? 0)}</span> ·
            {' '}Saldo devedor: <span className="font-medium text-amber-600">{formatCurrency(view.openAmount ?? 0)}</span>
          </div>
          <Table headers={['Nº', 'Vencimento', 'Valor', 'Atraso', 'Total atualizado', 'Status']}>
            {view.installments.map((i) => (
              <Tr key={i.id}>
                <td className="px-4 py-2">#{i.number}</td>
                <td className="px-4 py-2">{formatDate(i.dueDate)}</td>
                <td className="px-4 py-2">{formatCurrency(i.amount)}</td>
                <td className="px-4 py-2">{i.daysLate > 0 ? <span className="text-red-600">{i.daysLate}d</span> : '—'}</td>
                <td className="px-4 py-2 font-medium">
                  {i.daysLate > 0
                    ? <span title={`Multa ${formatCurrency(i.penaltyAmount)} + juros ${formatCurrency(i.interestAmount)}`}>{formatCurrency(i.updatedAmount)}</span>
                    : '—'}
                </td>
                <td className="px-4 py-2">
                  <Badge dot color={INSTALLMENT_STATUS[i.status]?.color ?? 'gray'}>
                    {INSTALLMENT_STATUS[i.status]?.label ?? i.status}
                  </Badge>
                </td>
              </Tr>
            ))}
          </Table>
        </Modal>
      )}

      {/* Modal: nova/editar venda */}
      <Modal open={modalOpen} onClose={() => setModalOpen(false)} title={editingId ? 'Editar venda' : 'Nova venda'}>
        <form className="space-y-3" onSubmit={(e) => { e.preventDefault(); save.mutate(form) }}>
          <Field label="Cliente">
            <div className="flex gap-2">
              <div className="flex-1">
                <Combobox
                  options={clientOptions}
                  value={form.clientId}
                  disabled={!!editingId}
                  onChange={(v) => {
                    const opt = clientOptions.find((o) => o.value === v)
                    setClientSeed(opt ?? null)
                    setForm({ ...form, clientId: v })
                  }}
                  placeholder="Buscar cliente…"
                  onSearch={setClientSearch}
                  loading={clientsQuery.isFetching}
                />
              </div>
              {canCreateClient && !editingId && (
                <Button type="button" variant="outline" title="Cadastrar novo cliente" onClick={() => setQuickClient(true)}>
                  <Plus className="h-4 w-4" />
                </Button>
              )}
            </div>
          </Field>
          <Field label="Lote (disponíveis)">
            <Combobox
              options={lotOptions}
              value={form.lotId}
              disabled={!!editingId}
              onChange={onSelectLot}
              placeholder="Buscar lote…"
              onSearch={setLotSearch}
              loading={lotsQuery.isFetching}
            />
          </Field>
          <div className="grid grid-cols-2 gap-3">
            <Field label="Valor esperado de venda (do lote)">
              <Input value={expectedValue ? formatCurrency(expectedValue) : '—'} readOnly disabled />
            </Field>
            <Field label="Valor que foi vendido">
              <Input type="number" step="0.01" value={form.totalValue} onChange={(e) => setForm({ ...form, totalValue: Number(e.target.value) })} required />
            </Field>
          </div>
          <div className="grid grid-cols-3 gap-3">
            <Field label="Forma de compra">
              <Select value={form.purchaseType} onChange={(e) => setForm({ ...form, purchaseType: e.target.value })}>
                {PURCHASE_TYPES.map((m) => <option key={m} value={m}>{m}</option>)}
              </Select>
            </Field>
            <Field label={`Entrada${entradaDisabled ? ' (só p/ entrada + parcelas)' : ''}`}>
              <Input type="number" step="0.01" value={entradaDisabled ? 0 : form.downPayment} disabled={entradaDisabled}
                onChange={(e) => setForm({ ...form, downPayment: Number(e.target.value) })} />
            </Field>
            <Field label="Qtd. parcelas">
              <Input type="number" min={0} value={form.installmentsCount} onChange={(e) => setForm({ ...form, installmentsCount: Number(e.target.value) })} required />
            </Field>
          </div>
          <div className="grid grid-cols-3 gap-3">
            <Field label="1º vencimento">
              <Input type="date" value={form.firstDueDate} onChange={(e) => setForm({ ...form, firstDueDate: e.target.value })} required />
            </Field>
            <Field label="Forma de pagamento">
              <Select value={form.paymentMethod} onChange={(e) => setForm({ ...form, paymentMethod: e.target.value })}>
                {pmList.map((m) => <option key={m} value={m}>{m}</option>)}
              </Select>
            </Field>
            <Field label="Índice de correção">
              <Select value={form.correctionIndex} onChange={(e) => setForm({ ...form, correctionIndex: e.target.value })}>
                {ciList.map((m) => <option key={m} value={m}>{m}</option>)}
              </Select>
            </Field>
          </div>
          <div className="grid grid-cols-2 gap-3">
            <Field label="Juros de mora (% ao mês)">
              <Input type="number" step="0.01" min={0} value={form.interestRate}
                onChange={(e) => setForm({ ...form, interestRate: Number(e.target.value) })} />
            </Field>
            <Field label="Multa por atraso (%)">
              <Input type="number" step="0.01" min={0} value={form.penaltyRate}
                onChange={(e) => setForm({ ...form, penaltyRate: Number(e.target.value) })} />
            </Field>
          </div>
          <p className="text-xs text-gray-400">
            Encargos calculados automaticamente nas parcelas vencidas: multa fixa + juros proporcionais aos dias de atraso.
          </p>
          {error && <p className="text-sm text-red-600">{error}</p>}
          <div className="flex justify-end gap-2 pt-2">
            <Button type="button" variant="outline" onClick={() => setModalOpen(false)}>Cancelar</Button>
            <Button type="submit" loading={save.isPending}>{editingId ? 'Salvar alterações' : 'Gerar venda e parcelas'}</Button>
          </div>
        </form>
      </Modal>

      {/* Modal: distrato (rescisão amigável) */}
      {distratoFor && (
        <Modal open onClose={() => setDistratoFor(null)} title={`Distratar venda — ${distratoFor.clientName}`}>
          <form className="space-y-3" onSubmit={(e) => { e.preventDefault(); distrato.mutate({ id: distratoFor.id, payload: distratoForm }) }}>
            <p className="rounded-md bg-amber-50 px-3 py-2 text-xs text-amber-700 dark:bg-amber-900/20 dark:text-amber-300">
              A venda será <strong>cancelada</strong> (mantida no histórico) e o lote <strong>{distratoFor.propertyLabel}</strong> será liberado.
              O documento de distrato pode ser gerado em seguida.
            </p>
            <div className="grid grid-cols-3 gap-3">
              <Field label="Data do distrato">
                <Input type="date" value={distratoForm.distratoDate} onChange={(e) => setDistratoForm({ ...distratoForm, distratoDate: e.target.value })} required />
              </Field>
              <Field label="Valor a devolver">
                <Input type="number" step="0.01" min={0} value={distratoForm.refundAmount} onChange={(e) => setDistratoForm({ ...distratoForm, refundAmount: Number(e.target.value) })} />
              </Field>
              <Field label="Valor retido">
                <Input type="number" step="0.01" min={0} value={distratoForm.retainedAmount} onChange={(e) => setDistratoForm({ ...distratoForm, retainedAmount: Number(e.target.value) })} />
              </Field>
            </div>
            <Field label="Motivo do distrato">
              <textarea
                className="min-h-[60px] w-full rounded-md border border-gray-300 bg-white px-3 py-2 text-sm dark:border-gray-600 dark:bg-gray-800"
                value={distratoForm.reason}
                onChange={(e) => setDistratoForm({ ...distratoForm, reason: e.target.value })}
                placeholder="Ex.: desistência do comprador, inadimplência…"
              />
            </Field>
            <Field label="Regra aplicada (registrada no histórico p/ auditoria)">
              <Input list="distrato-rules" value={distratoForm.rule} required
                onChange={(e) => setDistratoForm({ ...distratoForm, rule: e.target.value })}
                placeholder="Ex.: Retenção de 20% sobre o valor pago" />
              <datalist id="distrato-rules">
                <option value="Retenção de 20% sobre o valor pago" />
                <option value="Retenção de 10% sobre o valor pago" />
                <option value="Retenção de cláusula penal (Lei 13.786/2018)" />
                <option value="Devolução integral (sem retenção)" />
              </datalist>
            </Field>
            <Field label="Memória de cálculo (opcional)">
              <textarea
                className="min-h-[48px] w-full rounded-md border border-gray-300 bg-white px-3 py-2 text-sm dark:border-gray-600 dark:bg-gray-800"
                value={distratoForm.ruleDetail}
                onChange={(e) => setDistratoForm({ ...distratoForm, ruleDetail: e.target.value })}
                placeholder="Ex.: 20% de R$ 24.000 pagos = R$ 4.800 retidos; devolução de R$ 19.200."
              />
            </Field>
            {distratoForm.rule && (
              <p className="rounded-md border border-amber-200 bg-amber-50 px-3 py-2 text-xs text-amber-800 dark:border-amber-900/40 dark:bg-amber-900/20 dark:text-amber-300">
                Regra que será aplicada e registrada: <strong>{distratoForm.rule}</strong>
              </p>
            )}
            {distratoError && <p className="text-sm text-red-600">{distratoError}</p>}
            <div className="flex justify-end gap-2 pt-2">
              <Button type="button" variant="outline" onClick={() => setDistratoFor(null)}>Cancelar</Button>
              <Button type="submit" variant="danger" loading={distrato.isPending}>Confirmar distrato</Button>
            </div>
          </form>
        </Modal>
      )}

      {/* Modal: histórico de documentos arquivados */}
      {docsFor && (
        <Modal open onClose={() => setDocsFor(null)} title={`Documentos — ${docsFor.clientName}`}>
          {documents.isLoading ? (
            <p className="text-sm text-gray-500">Carregando…</p>
          ) : (documents.data ?? []).length === 0 ? (
            <EmptyState icon={FileSignature} title="Nenhum documento gerado"
              description="Gere o contrato ou o distrato para arquivar uma versão aqui." />
          ) : (
            <Table headers={['Tipo', 'Versão', 'Gerado em', 'Por', 'Ações']}>
              {(documents.data ?? []).map((d) => (
                <Tr key={d.id}>
                  <td className="px-4 py-2">
                    <Badge color={d.type === 'DISTRATO' ? 'yellow' : 'blue'}>{d.type === 'DISTRATO' ? 'Distrato' : 'Contrato'}</Badge>
                  </td>
                  <td className="px-4 py-2">v{d.version}</td>
                  <td className="px-4 py-2">{formatDateTime(d.generatedAt)}</td>
                  <td className="px-4 py-2 text-gray-500">{d.generatedBy ?? '—'}</td>
                  <td className="px-4 py-2 text-right">
                    <Button variant="outline" onClick={() => openPdf(`/contracts/documents/${d.id}`)}>Baixar</Button>
                  </td>
                </Tr>
              ))}
            </Table>
          )}
        </Modal>
      )}

      {/* Cadastro rápido de cliente a partir da nova venda */}
      {quickClient && (
        <QuickCreateClientModal
          onClose={() => setQuickClient(false)}
          onCreated={(c) => {
            setClientSeed({ value: c.id, label: c.name, hint: c.document })
            setForm((f) => ({ ...f, clientId: c.id }))
          }}
        />
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
