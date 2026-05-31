import { useQuery } from '@tanstack/react-query'
import { api } from '@/lib/api'
import type { DashboardData } from '@/lib/types'
import { Card, PageHeader } from '@/components/ui'
import { formatCurrency } from '@/lib/utils'

function Metric({ label, value, accent }: { label: string; value: string; accent?: string }) {
  return (
    <Card>
      <div className="text-xs uppercase tracking-wide text-gray-500">{label}</div>
      <div className={`mt-2 text-2xl font-semibold ${accent ?? 'text-gray-800'}`}>{value}</div>
    </Card>
  )
}

export function DashboardPage() {
  const { data, isLoading } = useQuery({
    queryKey: ['dashboard'],
    queryFn: async () => (await api.get<DashboardData>('/dashboard')).data,
  })

  if (isLoading) return <p className="text-gray-500">Carregando indicadores…</p>
  if (!data) return <p className="text-gray-500">Sem dados.</p>

  return (
    <div>
      <PageHeader title="Dashboard" />
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <Metric label="A receber no mês" value={formatCurrency(data.totalReceivableMonth)} accent="text-green-600" />
        <Metric label="A pagar no mês" value={formatCurrency(data.totalPayableMonth)} accent="text-red-600" />
        <Metric label="Saldo previsto" value={formatCurrency(data.expectedBalance)} accent="text-blue-600" />
        <Metric label="Transações pendentes" value={String(data.pendingBankTransactions)} />
        <Metric label="Parcelas vencidas" value={String(data.overdueInstallments)} accent="text-red-600" />
        <Metric label="Parcelas a vencer" value={String(data.upcomingInstallments)} />
        <Metric label="Imóveis disponíveis" value={String(data.availableProperties)} />
        <Metric label="Imóveis vendidos" value={String(data.soldProperties)} />
      </div>
    </div>
  )
}
