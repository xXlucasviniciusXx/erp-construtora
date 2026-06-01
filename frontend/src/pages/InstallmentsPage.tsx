import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api, getToken } from '@/lib/api'
import type { Installment } from '@/lib/types'
import { useAuth } from '@/auth/AuthContext'
import { ActionsMenu } from '@/components/Menu'
import { Badge, Card, PageHeader, Table } from '@/components/ui'
import { formatCurrency, formatDate } from '@/lib/utils'

/** Abre o contrato (PDF) da venda à qual a parcela pertence. */
async function openContract(saleId: string) {
  const res = await fetch(`${api.defaults.baseURL}/contracts/sales/${saleId}/pdf`, {
    headers: { Authorization: `Bearer ${getToken()}` },
  })
  if (!res.ok) { alert('Falha ao gerar o contrato'); return }
  const blob = await res.blob()
  window.open(URL.createObjectURL(blob))
}

export function InstallmentsPage() {
  const { hasPermission } = useAuth()
  const queryClient = useQueryClient()
  const canPay = hasPermission('RECEIVABLE_WRITE') || hasPermission('SALES_WRITE')
  const canContract = hasPermission('CONTRACTS_GENERATE')

  const overdue = useQuery({
    queryKey: ['installments-overdue'],
    queryFn: async () => (await api.get<Installment[]>('/installments/overdue')).data,
  })

  const pay = useMutation({
    mutationFn: async (id: string) =>
      api.post(`/installments/${id}/pay`, { paymentDate: new Date().toISOString().slice(0, 10) }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['installments-overdue'] })
      queryClient.invalidateQueries({ queryKey: ['dashboard'] })
    },
  })

  return (
    <div>
      <PageHeader title="Parcelas em atraso" />
      <Card>
        {overdue.isLoading ? <p className="text-gray-500">Carregando…</p> : (
          <Table headers={['Parcela', 'Valor', 'Vencimento', 'Status', 'Ações']}>
            {overdue.data?.map((i) => (
              <tr key={i.id} className="hover:bg-gray-50">
                <td className="px-4 py-2 font-medium">#{i.number}</td>
                <td className="px-4 py-2">{formatCurrency(i.amount)}</td>
                <td className="px-4 py-2">{formatDate(i.dueDate)}</td>
                <td className="px-4 py-2"><Badge color="red">{i.status}</Badge></td>
                <td className="px-4 py-2 text-right">
                  <ActionsMenu
                    items={[
                      ...(canPay ? [{ label: 'Registrar pagamento', onClick: () => pay.mutate(i.id) }] : []),
                      {
                        label: 'Gerar contrato',
                        onClick: () => openContract(i.saleId),
                        disabled: !canContract,
                      },
                    ]}
                  />
                </td>
              </tr>
            ))}
            {overdue.data?.length === 0 && (
              <tr><td colSpan={5} className="px-4 py-6 text-center text-gray-400">Nenhuma parcela em atraso. 🎉</td></tr>
            )}
          </Table>
        )}
      </Card>
    </div>
  )
}
