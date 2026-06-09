import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api, apiErrorMessage } from '@/lib/api'
import type { Development, DistratoConfig, DistratoFinancialRule } from '@/lib/types'
import { useToast } from '@/components/Toast'
import { useConfirm } from '@/components/Confirm'
import { Badge, Button, Field, Select, Table, TableSkeleton, Tr } from '@/components/ui'

const RULE_LABEL: Record<DistratoFinancialRule, string> = {
  APENAS_RETENCAO_SOBRE_VALOR_PAGO: 'Apenas retenção sobre o valor pago',
  RETENCAO_MAIS_PARCELAS_VENCIDAS: 'Retenção + parcelas vencidas (recomendado)',
  RETENCAO_MAIS_PARCELAS_VENCIDAS_E_ENCARGOS: 'Retenção + parcelas vencidas + encargos',
  RETENCAO_MAIS_SALDO_DEVEDOR_TOTAL: 'Retenção + saldo devedor total',
}
const RULES = Object.keys(RULE_LABEL) as DistratoFinancialRule[]

export function DistratoConfigTab() {
  const queryClient = useQueryClient()
  const toast = useToast()
  const confirm = useConfirm()
  const [error, setError] = useState<string | null>(null)
  const [newDev, setNewDev] = useState('')
  const [newRule, setNewRule] = useState<DistratoFinancialRule>('RETENCAO_MAIS_PARCELAS_VENCIDAS')

  const configs = useQuery({
    queryKey: ['distrato-config'],
    queryFn: async () => (await api.get<DistratoConfig[]>('/distrato-config')).data,
  })
  const developments = useQuery({
    queryKey: ['developments'],
    queryFn: async () => (await api.get<Development[]>('/developments')).data,
  })

  const refresh = () => queryClient.invalidateQueries({ queryKey: ['distrato-config'] })
  const upsert = useMutation({
    mutationFn: async (body: { developmentId: string | null; financialRule: DistratoFinancialRule }) =>
      (await api.put('/distrato-config', body)).data,
    onSuccess: () => { refresh(); toast.success('Regra salva.'); setNewDev('') },
    onError: (e) => setError(apiErrorMessage(e)),
  })
  const remove = useMutation({
    mutationFn: async (id: string) => api.delete(`/distrato-config/${id}`),
    onSuccess: () => { refresh(); toast.success('Regra removida.') },
    onError: (e) => toast.error(apiErrorMessage(e)),
  })

  const global = configs.data?.find((c) => !c.developmentId)
  const perDev = configs.data?.filter((c) => c.developmentId) ?? []
  const usedDevIds = new Set(perDev.map((c) => c.developmentId))
  const availableDevs = (developments.data ?? []).filter((d) => !usedDevIds.has(d.id))

  return (
    <div className="space-y-6">
      <div>
        <h3 className="mb-1 text-sm font-semibold text-gray-700 dark:text-gray-200">Regra Financeira de Distrato</h3>
        <p className="mb-3 text-xs text-gray-500">
          Define como o saldo do distrato é calculado. A regra <strong>global</strong> é usada quando o empreendimento não tem regra própria.
        </p>
        <Field label="Regra global (padrão para todos os empreendimentos)">
          <div className="flex gap-2">
            <Select
              value={global?.financialRule ?? 'RETENCAO_MAIS_PARCELAS_VENCIDAS'}
              onChange={(e) => upsert.mutate({ developmentId: null, financialRule: e.target.value as DistratoFinancialRule })}
            >
              {RULES.map((r) => <option key={r} value={r}>{RULE_LABEL[r]}</option>)}
            </Select>
          </div>
        </Field>
      </div>

      <div>
        <h3 className="mb-2 text-sm font-semibold text-gray-700 dark:text-gray-200">Regras por empreendimento (opcional)</h3>
        {configs.isLoading ? <TableSkeleton rows={3} cols={3} /> : (
          <Table headers={['Empreendimento', 'Regra', 'Ações']}>
            {perDev.map((c) => (
              <Tr key={c.id}>
                <td className="px-4 py-2 font-medium">{c.developmentName}</td>
                <td className="px-4 py-2">
                  <Select value={c.financialRule}
                    onChange={(e) => upsert.mutate({ developmentId: c.developmentId!, financialRule: e.target.value as DistratoFinancialRule })}>
                    {RULES.map((r) => <option key={r} value={r}>{RULE_LABEL[r]}</option>)}
                  </Select>
                </td>
                <td className="px-4 py-2 text-right">
                  <Button variant="outline" className="px-2 py-1 text-xs" onClick={async () => {
                    if (await confirm({ title: 'Remover regra', message: `Remover a regra específica de "${c.developmentName}"? Passará a usar a global.`, confirmLabel: 'Remover', danger: true }))
                      remove.mutate(c.id)
                  }}>Remover</Button>
                </td>
              </Tr>
            ))}
            {perDev.length === 0 && <tr><td colSpan={3} className="px-4 py-3 text-center text-sm text-gray-400">Nenhuma regra específica — todos usam a global.</td></tr>}
          </Table>
        )}

        <div className="mt-3 flex items-end gap-2">
          <Field label="Adicionar regra para empreendimento">
            <Select value={newDev} onChange={(e) => setNewDev(e.target.value)}>
              <option value="">— selecione —</option>
              {availableDevs.map((d) => <option key={d.id} value={d.id}>{d.name}</option>)}
            </Select>
          </Field>
          <Field label="Regra">
            <Select value={newRule} onChange={(e) => setNewRule(e.target.value as DistratoFinancialRule)}>
              {RULES.map((r) => <option key={r} value={r}>{RULE_LABEL[r]}</option>)}
            </Select>
          </Field>
          <Button disabled={!newDev || upsert.isPending}
            onClick={() => upsert.mutate({ developmentId: newDev, financialRule: newRule })}>Adicionar</Button>
        </div>
      </div>

      <p className="text-xs text-gray-400">
        <Badge color="blue">Dica</Badge> O percentual padrão de retenção é configurado por <strong>lote</strong> (cadastro do lote), e pode ser ajustado no momento do distrato.
      </p>
      {error && <p className="text-sm text-red-600">{error}</p>}
    </div>
  )
}
