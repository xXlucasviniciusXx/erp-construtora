import { useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { FileX2 } from 'lucide-react'
import { api, apiErrorMessage } from '@/lib/api'
import type {
  Distrato, DistratoFinancialRule, DistratoSimulation, DistratoStatus, Page, Sale,
} from '@/lib/types'
import { useAuth } from '@/auth/AuthContext'
import { useToast } from '@/components/Toast'
import { useConfirm } from '@/components/Confirm'
import {
  Badge, Button, Card, EmptyState, Field, Input, Modal, PageHeader, Select, Table, TableSkeleton, Tr,
} from '@/components/ui'
import { formatCurrency, formatDate } from '@/lib/utils'

const RULE_LABEL: Record<DistratoFinancialRule, string> = {
  APENAS_RETENCAO_SOBRE_VALOR_PAGO: 'Apenas retenção sobre o valor pago',
  RETENCAO_MAIS_PARCELAS_VENCIDAS: 'Retenção + parcelas vencidas',
  RETENCAO_MAIS_PARCELAS_VENCIDAS_E_ENCARGOS: 'Retenção + parcelas vencidas + encargos',
  RETENCAO_MAIS_SALDO_DEVEDOR_TOTAL: 'Retenção + saldo devedor total',
}
const RULES = Object.keys(RULE_LABEL) as DistratoFinancialRule[]

const STATUS_LABEL: Record<DistratoStatus, string> = {
  SOLICITADO: 'Solicitado', APROVADO: 'Aprovado',
  AGUARDANDO_QUITACAO_FINANCEIRA: 'Aguardando quitação', CONCLUIDO: 'Concluído', CANCELADO: 'Cancelado',
}
const STATUS_COLOR: Record<DistratoStatus, string> = {
  SOLICITADO: 'yellow', APROVADO: 'blue', AGUARDANDO_QUITACAO_FINANCEIRA: 'orange', CONCLUIDO: 'green', CANCELADO: 'gray',
}

function outcomeText(d: { financialOutcome: string; financialEntryAmount: number }) {
  if (d.financialOutcome === 'PAYABLE') return `Devolver ao cliente · ${formatCurrency(d.financialEntryAmount)} (Contas a Pagar)`
  if (d.financialOutcome === 'RECEIVABLE') return `Cobrar do cliente · ${formatCurrency(d.financialEntryAmount)} (Contas a Receber)`
  return 'Saldo zero — sem lançamento financeiro'
}

export function DistratosPage() {
  const { hasPermission } = useAuth()
  const canEdit = hasPermission('VENDAS_EDIT')
  const queryClient = useQueryClient()
  const [statusFilter, setStatusFilter] = useState<DistratoStatus | ''>('')
  const [novo, setNovo] = useState(false)
  const [detail, setDetail] = useState<Distrato | null>(null)

  const list = useQuery({
    queryKey: ['distratos', statusFilter],
    queryFn: async () => (await api.get<Distrato[]>('/distratos', {
      params: { status: statusFilter || undefined },
    })).data,
  })

  const refresh = () => queryClient.invalidateQueries({ queryKey: ['distratos'] })

  return (
    <div>
      <PageHeader
        title="Distratos"
        subtitle="Rescisão de contratos de lote — cálculo, aprovação e quitação financeira"
        action={canEdit && <Button onClick={() => setNovo(true)}>Novo distrato</Button>}
      />

      <Card className="mb-4">
        <div className="flex items-end gap-3">
          <Field label="Status">
            <Select value={statusFilter} onChange={(e) => setStatusFilter(e.target.value as DistratoStatus | '')}>
              <option value="">Todos</option>
              {(Object.keys(STATUS_LABEL) as DistratoStatus[]).map((s) => <option key={s} value={s}>{STATUS_LABEL[s]}</option>)}
            </Select>
          </Field>
        </div>
      </Card>

      {list.isLoading ? <TableSkeleton rows={6} cols={7} /> : (
        <Table headers={['Contrato', 'Cliente', 'Lote', 'Regra', 'Resultado', 'Status', '']}>
          {list.data?.map((d) => (
            <Tr key={d.id} className="cursor-pointer" onClick={() => setDetail(d)}>
              <td className="px-4 py-2 font-medium">{d.contractNumber ?? '—'}</td>
              <td className="px-4 py-2">{d.clientName}</td>
              <td className="px-4 py-2">{d.developmentName} / {d.blockName} / {d.lotName}</td>
              <td className="px-4 py-2 text-xs">{RULE_LABEL[d.financialRule]}</td>
              <td className="px-4 py-2 text-xs">{outcomeText(d)}</td>
              <td className="px-4 py-2"><Badge dot color={STATUS_COLOR[d.status]}>{STATUS_LABEL[d.status]}</Badge></td>
              <td className="px-4 py-2 text-right text-xs text-blue-600">ver</td>
            </Tr>
          ))}
          {list.data?.length === 0 && (
            <tr><td colSpan={7} className="p-0">
              <EmptyState icon={FileX2} title="Nenhum distrato" description="Inicie um distrato a partir de uma venda ativa." />
            </td></tr>
          )}
        </Table>
      )}

      {novo && <NovoDistratoModal onClose={() => setNovo(false)} onCreated={() => { setNovo(false); refresh() }} />}
      {detail && (
        <DetailModal
          distrato={detail}
          canEdit={canEdit}
          onClose={() => setDetail(null)}
          onChanged={(updated) => { setDetail(updated); refresh() }}
        />
      )}
    </div>
  )
}

/* ----------------- Novo distrato (seleção + simulação + solicitação) ----------------- */
function NovoDistratoModal({ onClose, onCreated }: { onClose: () => void; onCreated: () => void }) {
  const toast = useToast()
  const [saleId, setSaleId] = useState('')
  const [rule, setRule] = useState<DistratoFinancialRule | ''>('')
  const [percent, setPercent] = useState<string>('')
  const [reason, setReason] = useState('')
  const [changeReason, setChangeReason] = useState('')
  const [sim, setSim] = useState<DistratoSimulation | null>(null)
  const [error, setError] = useState<string | null>(null)

  const sales = useQuery({
    queryKey: ['sales-active-distrato'],
    queryFn: async () => (await api.get<Page<Sale>>('/sales', { params: { status: 'ACTIVE', size: 100 } })).data.content,
  })

  const simulate = useMutation({
    mutationFn: async () => (await api.post<DistratoSimulation>('/distratos/simulate', {
      saleId,
      financialRule: rule || undefined,
      usedRetentionPercent: percent === '' ? undefined : Number(percent),
    })).data,
    onSuccess: (data) => { setSim(data); setError(null); if (percent === '') setPercent(String(data.usedRetentionPercent ?? 0)) },
    onError: (e) => setError(apiErrorMessage(e)),
  })

  const create = useMutation({
    mutationFn: async () => (await api.post<Distrato>('/distratos', {
      saleId,
      reason,
      financialRule: rule || undefined,
      usedRetentionPercent: percent === '' ? undefined : Number(percent),
      retentionChangeReason: changeReason || undefined,
    })).data,
    onSuccess: () => { toast.success('Distrato solicitado.'); onCreated() },
    onError: (e) => setError(apiErrorMessage(e)),
  })

  const defaultPct = sim?.defaultRetentionPercent ?? null
  const usedPct = percent === '' ? (sim?.usedRetentionPercent ?? 0) : Number(percent)
  const percentChanged = sim != null && Number(usedPct) !== Number(defaultPct ?? 0)
  const needsChangeReason = percentChanged && !changeReason.trim()

  return (
    <Modal open onClose={onClose} title="Novo distrato" size="lg">
      <div className="space-y-3">
        <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
          <Field label="Venda (contrato ativo) *">
            <Select value={saleId} onChange={(e) => { setSaleId(e.target.value); setSim(null) }}>
              <option value="">— selecione —</option>
              {(sales.data ?? []).map((s) => (
                <option key={s.id} value={s.id}>{s.contractNumber} · {s.clientName} · {s.propertyLabel}</option>
              ))}
            </Select>
          </Field>
          <Field label="Regra financeira">
            <Select value={rule} onChange={(e) => { setRule(e.target.value as DistratoFinancialRule | ''); setSim(null) }}>
              <option value="">Usar regra configurada (empreendimento/global)</option>
              {RULES.map((r) => <option key={r} value={r}>{RULE_LABEL[r]}</option>)}
            </Select>
          </Field>
        </div>

        <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
          <Field label={`% de retenção${defaultPct != null ? ` (padrão do lote: ${defaultPct}%)` : ''}`}>
            <Input type="number" step="0.01" min={0} max={100} value={percent}
              onChange={(e) => setPercent(e.target.value)} placeholder="usa o padrão do lote" />
          </Field>
          <Field label="Motivo do distrato">
            <Input value={reason} onChange={(e) => setReason(e.target.value)} placeholder="Ex.: desistência do comprador" />
          </Field>
        </div>

        {percentChanged && (
          <Field label="Motivo da alteração do percentual (obrigatório)">
            <Input value={changeReason} onChange={(e) => setChangeReason(e.target.value)}
              placeholder="Justifique por que o percentual difere do padrão do lote" />
          </Field>
        )}

        <div className="flex gap-2">
          <Button type="button" variant="outline" disabled={!saleId || simulate.isPending} loading={simulate.isPending}
            onClick={() => simulate.mutate()}>Simular</Button>
        </div>

        {sim && <SimulationView sim={{ ...sim, usedRetentionPercent: Number(usedPct) }} />}

        {error && <p className="text-sm text-red-600">{error}</p>}

        <div className="flex justify-end gap-2 border-t border-gray-100 pt-3 dark:border-gray-700">
          <Button type="button" variant="outline" onClick={onClose}>Cancelar</Button>
          <Button type="button" variant="danger" disabled={!sim || needsChangeReason || create.isPending}
            loading={create.isPending} onClick={() => create.mutate()}>
            Solicitar distrato
          </Button>
        </div>
        {needsChangeReason && <p className="text-right text-xs text-amber-600">Informe o motivo da alteração do percentual para prosseguir.</p>}
      </div>
    </Modal>
  )
}

/* ----------------- Resumo da simulação / memória de cálculo ----------------- */
function SimulationView({ sim }: { sim: DistratoSimulation }) {
  return (
    <div className="rounded-lg border border-gray-200 bg-gray-50 p-3 text-sm dark:border-gray-700 dark:bg-gray-800/50">
      <div className="mb-2 font-semibold text-gray-700 dark:text-gray-200">Memória de cálculo</div>
      <div className="grid grid-cols-2 gap-x-6 gap-y-1 sm:grid-cols-3">
        <Row label="Valor total do contrato" value={formatCurrency(sim.contractTotal)} />
        <Row label="Valor pago pelo cliente" value={formatCurrency(sim.paidAmount)} />
        <Row label="% retenção (padrão)" value={sim.defaultRetentionPercent != null ? `${sim.defaultRetentionPercent}%` : '—'} />
        <Row label="% retenção (usado)" value={`${sim.usedRetentionPercent}%`} />
        <Row label="Valor retido" value={formatCurrency(sim.retentionAmount)} />
        <Row label="Regra aplicada" value={RULE_LABEL[sim.financialRule]} />
        <Row label="Parcelas vencidas" value={formatCurrency(sim.overdueAmount)} />
        <Row label="Encargos" value={formatCurrency(sim.chargesAmount)} />
        <Row label="Saldo devedor total" value={formatCurrency(sim.totalDebtAmount)} />
        <Row label="Descontos aplicados" value={formatCurrency(sim.deductions)} />
        <Row label="Saldo final do distrato" value={formatCurrency(sim.finalBalance)} strong />
      </div>
      <div className="mt-2 rounded-md bg-white px-3 py-2 text-xs font-medium dark:bg-gray-900">
        Resultado: <span className={sim.financialOutcome === 'RECEIVABLE' ? 'text-red-600' : sim.financialOutcome === 'PAYABLE' ? 'text-blue-600' : 'text-gray-600'}>
          {outcomeText(sim)}
        </span>
      </div>
    </div>
  )
}

function Row({ label, value, strong }: { label: string; value: string; strong?: boolean }) {
  return (
    <div>
      <div className="text-xs text-gray-400">{label}</div>
      <div className={strong ? 'font-semibold text-gray-900 dark:text-white' : 'text-gray-700 dark:text-gray-200'}>{value}</div>
    </div>
  )
}

/* ----------------- Detalhe + ações de fluxo ----------------- */
function DetailModal({ distrato, canEdit, onClose, onChanged }: {
  distrato: Distrato; canEdit: boolean; onClose: () => void; onChanged: (d: Distrato) => void
}) {
  const toast = useToast()
  const confirm = useConfirm()
  const [error, setError] = useState<string | null>(null)

  function useAction(path: string, body: unknown, ok: string) {
    return useMutation({
      mutationFn: async () => (await api.post<Distrato>(`/distratos/${distrato.id}/${path}`, body)).data,
      onSuccess: (d) => { toast.success(ok); onChanged(d) },
      onError: (e) => setError(apiErrorMessage(e)),
    })
  }
  const approve = useAction('approve', undefined, 'Distrato aprovado.')
  const settle = useAction('settle', {}, 'Quitação registrada — distrato concluído.')
  const cancel = useAction('cancel', undefined, 'Distrato cancelado.')

  const d = distrato
  const memory = useMemo(() => {
    try { return d.calculationMemory ? JSON.parse(d.calculationMemory) : null } catch { return null }
  }, [d.calculationMemory])

  return (
    <Modal open onClose={onClose} title={`Distrato · ${d.contractNumber ?? ''}`} size="lg">
      <div className="space-y-3 text-sm">
        <div className="flex items-center gap-2">
          <Badge dot color={STATUS_COLOR[d.status]}>{STATUS_LABEL[d.status]}</Badge>
          <span className="text-gray-500">{d.developmentName} / {d.blockName} / {d.lotName}</span>
        </div>

        <div className="grid grid-cols-2 gap-x-6 gap-y-1 sm:grid-cols-3">
          <Row label="Cliente" value={d.clientName} />
          <Row label="Valor total do contrato" value={formatCurrency(d.contractTotal)} />
          <Row label="Valor pago" value={formatCurrency(d.paidAmount)} />
          <Row label="% retenção (padrão)" value={d.defaultRetentionPercent != null ? `${d.defaultRetentionPercent}%` : '—'} />
          <Row label="% retenção (usado)" value={`${d.usedRetentionPercent}%`} />
          <Row label="Valor retido" value={formatCurrency(d.retentionAmount)} />
          <Row label="Regra" value={RULE_LABEL[d.financialRule]} />
          <Row label="Parcelas vencidas" value={formatCurrency(d.overdueAmount)} />
          <Row label="Encargos" value={formatCurrency(d.chargesAmount)} />
          <Row label="Saldo devedor total" value={formatCurrency(d.totalDebtAmount)} />
          <Row label="Saldo final" value={formatCurrency(d.finalBalance)} strong />
        </div>

        {d.retentionChangeReason && (
          <p className="rounded-md bg-amber-50 px-3 py-2 text-xs text-amber-800 dark:bg-amber-900/20 dark:text-amber-300">
            Percentual alterado — motivo: {d.retentionChangeReason}
          </p>
        )}

        <div className="rounded-md bg-gray-50 px-3 py-2 text-xs font-medium dark:bg-gray-800/50">
          {outcomeText(d)}
          {d.payableId && <span className="ml-2 text-gray-500">· Contas a Pagar #{d.payableId.slice(0, 8)}</span>}
          {d.receivableId && <span className="ml-2 text-gray-500">· Contas a Receber #{d.receivableId.slice(0, 8)}</span>}
        </div>

        {d.reason && <div className="text-xs text-gray-600 dark:text-gray-300">Motivo: {d.reason}</div>}

        <div className="grid grid-cols-3 gap-2 text-xs text-gray-500">
          <span>Solicitado: {formatDate(d.requestedAt)}</span>
          <span>Aprovado: {formatDate(d.approvedAt)}</span>
          <span>Concluído: {formatDate(d.concludedAt)}</span>
        </div>

        {memory && (
          <details className="rounded-md border border-gray-200 dark:border-gray-700">
            <summary className="cursor-pointer px-3 py-1.5 text-xs font-medium text-gray-600 dark:text-gray-300">Memória de cálculo (JSON)</summary>
            <pre className="overflow-auto px-3 py-2 text-[11px] text-gray-600 dark:text-gray-300">{JSON.stringify(memory, null, 2)}</pre>
          </details>
        )}

        {error && <p className="text-sm text-red-600">{error}</p>}

        {canEdit && (
          <div className="flex flex-wrap justify-end gap-2 border-t border-gray-100 pt-3 dark:border-gray-700">
            {d.status === 'SOLICITADO' && (
              <>
                <Button variant="outline" onClick={async () => {
                  if (await confirm({ title: 'Cancelar distrato', message: 'O lote volta para Vendido. Confirma?', confirmLabel: 'Cancelar distrato', danger: true })) cancel.mutate()
                }}>Cancelar distrato</Button>
                <Button loading={approve.isPending} onClick={() => approve.mutate()}>Aprovar</Button>
              </>
            )}
            {d.status === 'AGUARDANDO_QUITACAO_FINANCEIRA' && (
              <>
                <Button variant="outline" onClick={async () => {
                  if (await confirm({ title: 'Cancelar distrato', message: 'Cancela o lançamento financeiro e devolve o lote para Vendido. Confirma?', confirmLabel: 'Cancelar distrato', danger: true })) cancel.mutate()
                }}>Cancelar distrato</Button>
                <Button variant="danger" loading={settle.isPending} onClick={async () => {
                  if (await confirm({ title: 'Registrar quitação', message: 'Confirma a baixa financeira e a conclusão do distrato? O lote será liberado.', confirmLabel: 'Registrar quitação' })) settle.mutate()
                }}>Registrar quitação financeira</Button>
              </>
            )}
          </div>
        )}
      </div>
    </Modal>
  )
}
