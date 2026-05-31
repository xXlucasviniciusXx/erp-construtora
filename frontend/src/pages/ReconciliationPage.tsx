import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api, apiErrorMessage } from '@/lib/api'
import type { BankTransaction, Suggestion } from '@/lib/types'
import { useAuth } from '@/auth/AuthContext'
import { Badge, Button, Card, PageHeader, Table } from '@/components/ui'
import { formatCurrency, formatDate } from '@/lib/utils'

export function ReconciliationPage() {
  const { hasPermission } = useAuth()
  const queryClient = useQueryClient()
  const [openTxn, setOpenTxn] = useState<string | null>(null)
  const [suggestions, setSuggestions] = useState<Record<string, Suggestion[]>>({})
  const [error, setError] = useState<string | null>(null)
  const canWrite = hasPermission('RECONCILIATION_WRITE')

  const pending = useQuery({
    queryKey: ['pendencies'],
    queryFn: async () => (await api.get<BankTransaction[]>('/reconciliation/pendencies')).data,
  })

  const loadSuggestions = useMutation({
    mutationFn: async (txnId: string) =>
      (await api.get<Suggestion[]>(`/reconciliation/transactions/${txnId}/suggestions`)).data,
    onSuccess: (data, txnId) => setSuggestions((s) => ({ ...s, [txnId]: data })),
    onError: (e) => setError(apiErrorMessage(e)),
  })

  const reconcile = useMutation({
    mutationFn: async ({ txnId, s }: { txnId: string; s: Suggestion }) =>
      api.post(`/reconciliation/transactions/${txnId}/reconcile`, {
        targetType: s.targetType,
        targetId: s.targetId,
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['pendencies'] })
      queryClient.invalidateQueries({ queryKey: ['reconciliation-history'] })
      setOpenTxn(null)
    },
    onError: (e) => setError(apiErrorMessage(e)),
  })

  const setStatus = useMutation({
    mutationFn: async ({ txnId, status }: { txnId: string; status: string }) =>
      api.patch(`/reconciliation/transactions/${txnId}/status`, null, { params: { status } }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['pendencies'] }),
    onError: (e) => setError(apiErrorMessage(e)),
  })

  function toggle(txnId: string) {
    if (openTxn === txnId) {
      setOpenTxn(null)
      return
    }
    setOpenTxn(txnId)
    if (!suggestions[txnId]) loadSuggestions.mutate(txnId)
  }

  return (
    <div className="space-y-6">
      <PageHeader title="Conciliação bancária" />
      {error && <p className="text-sm text-red-600">{error}</p>}

      <Card>
        <h2 className="mb-3 text-sm font-semibold text-gray-700">
          Transações pendentes ({pending.data?.length ?? 0})
        </h2>
        {pending.isLoading ? (
          <p className="text-gray-500">Carregando…</p>
        ) : (
          <Table headers={['Data', 'Descrição', 'Documento', 'Valor', 'Tipo', 'Ações']}>
            {pending.data?.map((t) => (
              <tr key={t.id} className="align-top hover:bg-gray-50">
                <td className="px-4 py-2">{formatDate(t.transactionDate)}</td>
                <td className="px-4 py-2">
                  {t.description ?? '—'}
                  {openTxn === t.id && (
                    <div className="mt-2 space-y-1">
                      {loadSuggestions.isPending && <p className="text-xs text-gray-400">Buscando sugestões…</p>}
                      {suggestions[t.id]?.length === 0 && (
                        <p className="text-xs text-gray-400">Nenhuma sugestão automática. Use conciliação manual.</p>
                      )}
                      {suggestions[t.id]?.map((s) => (
                        <div key={s.targetId} className="flex items-center justify-between rounded border border-gray-200 bg-white px-2 py-1 text-xs">
                          <div>
                            <span className="font-medium">{s.label}</span>
                            <span className="ml-2 text-gray-400">{s.reason}</span>
                          </div>
                          <div className="flex items-center gap-2">
                            <Badge color={s.score >= 80 ? 'green' : s.score >= 60 ? 'yellow' : 'gray'}>
                              {Math.round(s.score)}%
                            </Badge>
                            {canWrite && (
                              <Button variant="primary" className="px-2 py-1" onClick={() => reconcile.mutate({ txnId: t.id, s })}>
                                Conciliar
                              </Button>
                            )}
                          </div>
                        </div>
                      ))}
                    </div>
                  )}
                </td>
                <td className="px-4 py-2">{t.documentNumber ?? '—'}</td>
                <td className="px-4 py-2 font-medium">{formatCurrency(Math.abs(t.amount))}</td>
                <td className="px-4 py-2">
                  <Badge color={t.type === 'CREDIT' ? 'green' : 'red'}>{t.type === 'CREDIT' ? 'Crédito' : 'Débito'}</Badge>
                </td>
                <td className="px-4 py-2">
                  <div className="flex flex-wrap gap-1">
                    <Button variant="outline" className="px-2 py-1" onClick={() => toggle(t.id)}>
                      {openTxn === t.id ? 'Fechar' : 'Sugestões'}
                    </Button>
                    {canWrite && (
                      <>
                        <Button variant="ghost" className="px-2 py-1" onClick={() => setStatus.mutate({ txnId: t.id, status: 'IGNORED' })}>Ignorar</Button>
                        <Button variant="ghost" className="px-2 py-1" onClick={() => setStatus.mutate({ txnId: t.id, status: 'DIVERGENT' })}>Divergente</Button>
                      </>
                    )}
                  </div>
                </td>
              </tr>
            ))}
            {pending.data?.length === 0 && (
              <tr><td colSpan={6} className="px-4 py-6 text-center text-gray-400">Nenhuma transação pendente. 🎉</td></tr>
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
      queryClient.invalidateQueries({ queryKey: ['pendencies'] })
    },
  })

  return (
    <Card>
      <h2 className="mb-3 text-sm font-semibold text-gray-700">Histórico de conciliações</h2>
      <Table headers={['Valor', 'Alvo', 'Modo', 'Data', '']}>
        {history.data?.map((r) => (
          <tr key={r.id} className="hover:bg-gray-50">
            <td className="px-4 py-2 font-medium">{formatCurrency(r.matchedAmount)}</td>
            <td className="px-4 py-2">{r.targetType}</td>
            <td className="px-4 py-2"><Badge color="blue">{r.mode}</Badge></td>
            <td className="px-4 py-2">{formatDate(r.reconciledAt)}</td>
            <td className="px-4 py-2 text-right">
              {canWrite && <Button variant="ghost" onClick={() => undo.mutate(r.id)}>Desfazer</Button>}
            </td>
          </tr>
        ))}
        {history.data?.length === 0 && (
          <tr><td colSpan={5} className="px-4 py-6 text-center text-gray-400">Sem conciliações ainda.</td></tr>
        )}
      </Table>
    </Card>
  )
}
