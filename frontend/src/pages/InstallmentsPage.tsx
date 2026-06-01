import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api, getToken } from '@/lib/api'
import type { InstallmentDetail } from '@/lib/types'
import { useAuth } from '@/auth/AuthContext'
import { ActionsMenu } from '@/components/Menu'
import { Badge, Card, Field, Input, PageHeader, Select, Table } from '@/components/ui'
import { formatCurrency, formatDate } from '@/lib/utils'

async function openContract(saleId: string) {
  const res = await fetch(`${api.defaults.baseURL}/contracts/sales/${saleId}/pdf`, {
    headers: { Authorization: `Bearer ${getToken()}` },
  })
  if (!res.ok) { alert('Falha ao gerar o contrato'); return }
  window.open(URL.createObjectURL(await res.blob()))
}

const STATUS_COLOR: Record<string, string> = { OPEN: 'gray', PAID: 'green', OVERDUE: 'red', CANCELLED: 'gray' }

export function InstallmentsPage() {
  const { hasPermission } = useAuth()
  const queryClient = useQueryClient()
  const canPay = hasPermission('RECEIVABLE_WRITE') || hasPermission('SALES_WRITE')
  const canContract = hasPermission('CONTRACTS_GENERATE')

  const [q, setQ] = useState('')
  const [status, setStatus] = useState('OVERDUE')
  const [dueFrom, setDueFrom] = useState('')
  const [dueTo, setDueTo] = useState('')

  const { data, isLoading } = useQuery({
    queryKey: ['installments-search', q, status, dueFrom, dueTo],
    queryFn: async () =>
      (await api.get<InstallmentDetail[]>('/installments', {
        params: {
          q: q || undefined,
          status: status || undefined,
          dueFrom: dueFrom || undefined,
          dueTo: dueTo || undefined,
        },
      })).data,
  })

  const pay = useMutation({
    mutationFn: async (id: string) =>
      api.post(`/installments/${id}/pay`, { paymentDate: new Date().toISOString().slice(0, 10) }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['installments-search'] })
      queryClient.invalidateQueries({ queryKey: ['dashboard-analytics'] })
    },
  })

  return (
    <div>
      <PageHeader title="Parcelas" />

      <Card className="mb-4">
        <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-4">
          <Field label="Cliente (nome / CPF / CNPJ)">
            <Input placeholder="Buscar…" value={q} onChange={(e) => setQ(e.target.value)} />
          </Field>
          <Field label="Status">
            <Select value={status} onChange={(e) => setStatus(e.target.value)}>
              <option value="">Todos</option>
              <option value="OVERDUE">Atrasada</option>
              <option value="OPEN">Em aberto</option>
              <option value="PAID">Paga</option>
              <option value="CANCELLED">Cancelada</option>
            </Select>
          </Field>
          <Field label="Vencimento de">
            <Input type="date" value={dueFrom} onChange={(e) => setDueFrom(e.target.value)} />
          </Field>
          <Field label="Vencimento até">
            <Input type="date" value={dueTo} onChange={(e) => setDueTo(e.target.value)} />
          </Field>
        </div>
      </Card>

      {isLoading ? <p className="text-gray-500">Carregando…</p> : (
        <Table headers={['Cliente', 'Documento', 'Telefone', 'Parcela', 'Valor', 'Vencimento', 'Status', 'Ações']}>
          {data?.map((i) => (
            <tr key={i.id} className="hover:bg-gray-50">
              <td className="px-4 py-2 font-medium">{i.clientName}</td>
              <td className="px-4 py-2">{i.clientDocument}</td>
              <td className="px-4 py-2">{i.clientPhone ?? '—'}</td>
              <td className="px-4 py-2">#{i.number}</td>
              <td className="px-4 py-2">{formatCurrency(i.amount)}</td>
              <td className="px-4 py-2">{formatDate(i.dueDate)}</td>
              <td className="px-4 py-2"><Badge color={STATUS_COLOR[i.status]}>{i.status}</Badge></td>
              <td className="px-4 py-2 text-right">
                <ActionsMenu
                  items={[
                    ...(canPay && i.status !== 'PAID'
                      ? [{ label: 'Registrar pagamento', onClick: () => pay.mutate(i.id) }]
                      : []),
                    { label: 'Gerar contrato', onClick: () => openContract(i.saleId), disabled: !canContract },
                  ]}
                />
              </td>
            </tr>
          ))}
          {data?.length === 0 && (
            <tr><td colSpan={8} className="px-4 py-6 text-center text-gray-400">Nenhuma parcela encontrada.</td></tr>
          )}
        </Table>
      )}
    </div>
  )
}
