import { Fragment, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Banknote } from 'lucide-react'
import { api, apiErrorMessage } from '@/lib/api'
import type { BankTransaction, ManualTarget, Suggestion } from '@/lib/types'
import { useAuth } from '@/auth/AuthContext'
import { useToast } from '@/components/Toast'
import { Badge, Button, Card, EmptyState, PageHeader, Select, Table, TableSkeleton, Tr } from '@/components/ui'
import { cn, formatCurrency, formatDate } from '@/lib/utils'

const TABS = [
  { key: 'PENDING', label: 'Pendentes' },
  { key: 'RECONCILED', label: 'Conciliadas' },
  { key: 'IGNORED', label: 'Ignoradas' },
  { key: 'DIVERGENT', label: 'Divergentes' },
] as const

type Status = (typeof TABS)[number]['key']

export function ReconciliationPage() {
  const { hasPermission } = useAuth()
  const queryClient = useQueryClient()
  const toast = useToast()
  const canWrite = hasPermission('RECONCILIATION_WRITE')

  const [status, setStatus] = useState<Status>('PENDING')
  const [expand, setExpand] = useState<{ id: string; mode: 'suggest' | 'manual' } | null>(null)
  const [suggestions, setSuggestions] = useState<Record<string, Suggestion[]>>({})
  const [targets, setTargets] = useState<Record<string, ManualTarget[]>>({})
  const [pick, setPick] = useState<Record<string, string>>({})
  const [error, setError] = useState<string | null>(null)

  const txns = useQuery({
    queryKey: ['recon-txns', status],
    queryFn: async () => (await api.get<BankTransaction[]>('/bank-transactions', { params: { status } })).data,
  })

  const invalidate = () => {
    queryClient.invalidateQueries({ queryKey: ['recon-txns'] })
    queryClient.invalidateQueries({ queryKey: ['reconciliation-history'] })
    queryClient.invalidateQueries({ queryKey: ['dashboard'] })
  }

  const loadSuggestions = useMutation({
    mutationFn: async (id: string) =>
      (await api.get<Suggestion[]>(`/reconciliation/transactions/${id}/suggestions`)).data,
    onSuccess: (data, id) => setSuggestions((s) => ({ ...s, [id]: data })),
    onError: (e) => setError(apiErrorMessage(e)),
  })

  const loadTargets = useMutation({
    mutationFn: async (id: string) =>
      (await api.get<ManualTarget[]>(`/reconciliation/transactions/${id}/targets`)).data,
    onSuccess: (data, id) => setTargets((t) => ({ ...t, [id]: data })),
    onError: (e) => setError(apiErrorMessage(e)),
  })

  const reconcile = useMutation({
    mutationFn: async ({ id, targetType, targetId }: { id: string; targetType: string; targetId: string }) =>
      api.post(`/reconciliation/transactions/${id}/reconcile`, { targetType, targetId }),
    onSuccess: () => { invalidate(); setExpand(null); toast.success('Transação conciliada com sucesso.') },
    onError: (e) => { setError(apiErrorMessage(e)); toast.error(apiErrorMessage(e)) },
  })

  const setStatusM = useMutation({
    mutationFn: async ({ id, newStatus, notes }: { id: string; newStatus: string; notes?: string }) =>
      api.patch(`/reconciliation/transactions/${id}/status`, null, { params: { status: newStatus, notes } }),
    onSuccess: () => { invalidate(); toast.success('Status atualizado.') },
    onError: (e) => { setError(apiErrorMessage(e)); toast.error(apiErrorMessage(e)) },
  })

  function toggle(id: string, mode: 'suggest' | 'manual') {
    if (expand?.id === id && expand.mode === mode) { setExpand(null); return }
    setExpand({ id, mode })
    if (mode === 'suggest' && !suggestions[id]) loadSuggestions.mutate(id)
    if (mode === 'manual' && !targets[id]) loadTargets.mutate(id)
  }

  function reconcileSuggestion(id: string, s: Suggestion) {
    reconcile.mutate({ id, targetType: s.targetType, targetId: s.targetId })
  }
  function reconcileManual(id: string) {
    const sel = pick[id]
    if (!sel) return
    const [targetType, targetId] = sel.split('|')
    reconcile.mutate({ id, targetType, targetId })
  }
  function markDivergent(id: string) {
    const reason = window.prompt('Motivo da divergência (opcional):') ?? ''
    setStatusM.mutate({ id, newStatus: 'DIVERGENT', notes: reason })
  }

  return (
    <div className="space-y-6">
      <PageHeader title="Conciliação bancária" subtitle="Vincule transações do extrato aos lançamentos" />
      {error && <p className="text-sm text-red-600">{error}</p>}

      {/* Abas de status */}
      <div className="flex gap-1 border-b border-gray-200 dark:border-gray-700">
        {TABS.map((t) => (
          <button
            key={t.key}
            onClick={() => { setStatus(t.key); setExpand(null) }}
            className={cn(
              'border-b-2 px-4 py-2 text-sm font-medium transition',
              status === t.key ? 'border-primary text-primary' : 'border-transparent text-gray-500 hover:text-gray-700 dark:hover:text-gray-300',
            )}
          >
            {t.label}
          </button>
        ))}
      </div>

      <Card>
        {txns.isLoading ? (
          <TableSkeleton rows={5} cols={6} />
        ) : (
          <Table headers={['Data', 'Descrição', 'Documento', 'Valor', 'Tipo', 'Ações']}>
            {txns.data?.map((t) => (
              <Fragment key={t.id}>
                <Tr className="align-top">
                  <td className="px-4 py-2">{formatDate(t.transactionDate)}</td>
                  <td className="px-4 py-2">
                    {t.description ?? '—'}
                    {t.status === 'DIVERGENT' && t.notes && (
                      <div className="text-xs text-red-500">Divergência: {t.notes}</div>
                    )}

                    {expand?.id === t.id && expand.mode === 'suggest' && (
                      <div className="mt-2 space-y-1">
                        {loadSuggestions.isPending && <p className="text-xs text-gray-400">Buscando sugestões…</p>}
                        {suggestions[t.id]?.length === 0 && (
                          <p className="text-xs text-gray-400">Nenhuma sugestão automática — use "Manual".</p>
                        )}
                        {suggestions[t.id]?.map((s) => (
                          <div key={s.targetId} className="flex items-center justify-between rounded border border-gray-200 bg-white px-2 py-1 text-xs text-gray-700 dark:border-gray-700 dark:bg-gray-800 dark:text-gray-200">
                            <div><span className="font-medium">{s.label}</span><span className="ml-2 text-gray-400">{s.reason}</span></div>
                            <div className="flex items-center gap-2">
                              <Badge color={s.score >= 80 ? 'green' : s.score >= 60 ? 'yellow' : 'gray'}>{Math.round(s.score)}%</Badge>
                              {canWrite && <Button className="px-2 py-1" onClick={() => reconcileSuggestion(t.id, s)}>Conciliar</Button>}
                            </div>
                          </div>
                        ))}
                      </div>
                    )}

                    {expand?.id === t.id && expand.mode === 'manual' && (
                      <div className="mt-2 flex flex-wrap items-center gap-2">
                        {loadTargets.isPending && <p className="text-xs text-gray-400">Carregando lançamentos…</p>}
                        {targets[t.id] && (
                          <>
                            <Select
                              className="max-w-xs text-xs"
                              value={pick[t.id] ?? ''}
                              onChange={(e) => setPick((p) => ({ ...p, [t.id]: e.target.value }))}
                            >
                              <option value="">Escolha um lançamento…</option>
                              {targets[t.id].map((m) => (
                                <option key={m.targetId} value={`${m.targetType}|${m.targetId}`}>
                                  {m.label} — {formatCurrency(m.amount)}
                                </option>
                              ))}
                            </Select>
                            <Button className="px-2 py-1" disabled={!pick[t.id]} onClick={() => reconcileManual(t.id)}>
                              Conciliar
                            </Button>
                            {pick[t.id] && (() => {
                              const m = targets[t.id].find((x) => `${x.targetType}|${x.targetId}` === pick[t.id])
                              return m && Math.abs(m.amount) !== Math.abs(t.amount) ? (
                                <span className="text-xs text-amber-600">⚠ valor difere ({formatCurrency(m.amount)} vs {formatCurrency(Math.abs(t.amount))}) — será registrado</span>
                              ) : null
                            })()}
                          </>
                        )}
                      </div>
                    )}
                  </td>
                  <td className="px-4 py-2">{t.documentNumber ?? '—'}</td>
                  <td className="px-4 py-2 font-medium">{formatCurrency(Math.abs(t.amount))}</td>
                  <td className="px-4 py-2"><Badge color={t.type === 'CREDIT' ? 'green' : 'red'}>{t.type === 'CREDIT' ? 'Crédito' : 'Débito'}</Badge></td>
                  <td className="px-4 py-2">
                    <div className="flex flex-wrap gap-1">
                      {t.status === 'PENDING' && (
                        <>
                          <Button variant="outline" className="px-2 py-1" onClick={() => toggle(t.id, 'suggest')}>Sugestões</Button>
                          {canWrite && (
                            <>
                              <Button variant="outline" className="px-2 py-1" onClick={() => toggle(t.id, 'manual')}>Manual</Button>
                              <Button variant="ghost" className="px-2 py-1" onClick={() => setStatusM.mutate({ id: t.id, newStatus: 'IGNORED' })}>Ignorar</Button>
                              <Button variant="ghost" className="px-2 py-1" onClick={() => markDivergent(t.id)}>Divergente</Button>
                            </>
                          )}
                        </>
                      )}
                      {(t.status === 'IGNORED' || t.status === 'DIVERGENT') && canWrite && (
                        <Button variant="ghost" className="px-2 py-1" onClick={() => setStatusM.mutate({ id: t.id, newStatus: 'PENDING' })}>
                          Voltar p/ pendente
                        </Button>
                      )}
                      {t.status === 'RECONCILED' && <span className="text-xs text-green-600">Conciliada</span>}
                    </div>
                  </td>
                </Tr>
              </Fragment>
            ))}
            {txns.data?.length === 0 && (
              <tr><td colSpan={6} className="p-0">
                <EmptyState icon={Banknote} title="Nenhuma transação" description="Não há transações nesta aba. Importe um extrato para iniciar a conciliação." />
              </td></tr>
            )}
          </Table>
        )}
      </Card>

      <ReconciliationHistory />
    </div>
  )
}

function ReconciliationHistory() {
  const queryClient = useQueryClient()
  const toast = useToast()
  const { hasPermission } = useAuth()
  const canWrite = hasPermission('RECONCILIATION_WRITE')

  const history = useQuery({
    queryKey: ['reconciliation-history'],
    queryFn: async () => (await api.get('/reconciliation/history')).data as any[],
  })

  const undo = useMutation({
    mutationFn: async (id: string) => api.post(`/reconciliation/${id}/undo`),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['reconciliation-history'] })
      queryClient.invalidateQueries({ queryKey: ['recon-txns'] })
      queryClient.invalidateQueries({ queryKey: ['dashboard'] })
      toast.success('Conciliação desfeita.')
    },
    onError: (e) => toast.error(apiErrorMessage(e)),
  })

  return (
    <Card>
      <h2 className="mb-3 text-sm font-semibold text-gray-700 dark:text-gray-200">Histórico de conciliações</h2>
      <Table headers={['Valor', 'Alvo', 'Modo', 'Data', '']}>
        {history.data?.map((r) => (
          <Tr key={r.id}>
            <td className="px-4 py-2 font-medium">{formatCurrency(r.matchedAmount)}</td>
            <td className="px-4 py-2">{r.targetType}</td>
            <td className="px-4 py-2"><Badge color="blue">{r.mode}</Badge></td>
            <td className="px-4 py-2">{formatDate(r.reconciledAt)}</td>
            <td className="px-4 py-2 text-right">
              {canWrite && <Button variant="ghost" onClick={() => undo.mutate(r.id)}>Desfazer</Button>}
            </td>
          </Tr>
        ))}
        {history.data?.length === 0 && (
          <tr><td colSpan={5} className="px-4 py-6 text-center text-sm text-gray-400">Sem conciliações ainda.</td></tr>
        )}
      </Table>
    </Card>
  )
}
