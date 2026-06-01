import { useQuery } from '@tanstack/react-query'
import {
  Bar, BarChart, CartesianGrid, Cell, Legend, Line, LineChart, Pie, PieChart,
  ResponsiveContainer, Tooltip, XAxis, YAxis,
} from 'recharts'
import { api } from '@/lib/api'
import type { DashboardAnalytics, Point } from '@/lib/types'
import { Card, PageHeader } from '@/components/ui'
import { formatCurrency } from '@/lib/utils'

const COLORS = ['#1e40af', '#0f766e', '#b45309', '#be123c', '#7c3aed', '#0891b2', '#65a30d', '#db2777']

function Metric({ label, value, accent }: { label: string; value: string; accent?: string }) {
  return (
    <Card>
      <div className="text-xs uppercase tracking-wide text-gray-500">{label}</div>
      <div className={`mt-2 text-xl font-semibold ${accent ?? 'text-gray-800'}`}>{value}</div>
    </Card>
  )
}

function ChartCard({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <Card>
      <h3 className="mb-3 text-sm font-semibold text-gray-700">{title}</h3>
      <div style={{ width: '100%', height: 240 }}>
        <ResponsiveContainer>{children as any}</ResponsiveContainer>
      </div>
    </Card>
  )
}

const brl = (v: number) => formatCurrency(v)
const empty = (d?: Point[]) => !d || d.length === 0

export function DashboardPage() {
  const { data, isLoading } = useQuery({
    queryKey: ['dashboard-analytics'],
    queryFn: async () => (await api.get<DashboardAnalytics>('/dashboard/analytics')).data,
  })

  if (isLoading) return <p className="text-gray-500">Carregando indicadores…</p>
  if (!data) return <p className="text-gray-500">Sem dados.</p>

  const clientPie: Point[] = [
    { label: 'Ativos', value: data.activeClients },
    { label: 'Inativos', value: data.inactiveClients },
    { label: 'Inadimplentes', value: data.delinquentClients },
  ]

  return (
    <div className="space-y-6">
      <PageHeader title="Dashboard" />

      {/* Cards resumidos */}
      <div className="grid grid-cols-2 gap-3 lg:grid-cols-4">
        <Metric label="Total vendido" value={brl(data.totalSold)} accent="text-blue-700" />
        <Metric label="Total recebido" value={brl(data.totalReceived)} accent="text-green-600" />
        <Metric label="Total em aberto" value={brl(data.totalOpen)} accent="text-amber-600" />
        <Metric label="Total em atraso" value={brl(data.totalOverdue)} accent="text-red-600" />
        <Metric label="Clientes inadimplentes" value={String(data.delinquentClients)} accent="text-red-600" />
        <Metric label="Lotes vendidos" value={String(data.lotsSold)} accent="text-blue-700" />
        <Metric label="Lotes disponíveis" value={String(data.lotsAvailable)} accent="text-green-600" />
        <Metric label="Clientes ativos" value={String(data.activeClients)} />
      </div>

      {/* Gráficos */}
      <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
        <ChartCard title="Recebido por mês">
          <BarChart data={data.receivedByMonth}>
            <CartesianGrid strokeDasharray="3 3" /><XAxis dataKey="label" fontSize={11} /><YAxis fontSize={11} />
            <Tooltip formatter={(v: number) => brl(v)} />
            <Bar dataKey="value" fill="#0f766e" name="Recebido" />
          </BarChart>
        </ChartCard>

        <ChartCard title="A receber por mês (próximos meses)">
          <BarChart data={data.toReceiveByMonth}>
            <CartesianGrid strokeDasharray="3 3" /><XAxis dataKey="label" fontSize={11} /><YAxis fontSize={11} />
            <Tooltip formatter={(v: number) => brl(v)} />
            <Bar dataKey="value" fill="#1e40af" name="A receber" />
          </BarChart>
        </ChartCard>

        <ChartCard title="Fluxo de caixa previsto (a receber − a pagar)">
          <LineChart data={data.cashFlowForecast}>
            <CartesianGrid strokeDasharray="3 3" /><XAxis dataKey="label" fontSize={11} /><YAxis fontSize={11} />
            <Tooltip formatter={(v: number) => brl(v)} />
            <Line type="monotone" dataKey="value" stroke="#7c3aed" strokeWidth={2} name="Saldo previsto" />
          </LineChart>
        </ChartCard>

        <ChartCard title="Vendas por mês">
          <BarChart data={data.salesByMonth}>
            <CartesianGrid strokeDasharray="3 3" /><XAxis dataKey="label" fontSize={11} /><YAxis fontSize={11} />
            <Tooltip formatter={(v: number) => brl(v)} />
            <Bar dataKey="value" fill="#0891b2" name="Vendas" />
          </BarChart>
        </ChartCard>

        <ChartCard title="Parcelas vencidas por faixa de atraso">
          <BarChart data={data.overdueByAging}>
            <CartesianGrid strokeDasharray="3 3" /><XAxis dataKey="label" fontSize={10} /><YAxis fontSize={11} />
            <Tooltip formatter={(v: number) => brl(v)} />
            <Bar dataKey="value" fill="#be123c" name="Em atraso" />
          </BarChart>
        </ChartCard>

        <ChartCard title="Inadimplência por empreendimento">
          <BarChart data={data.delinquencyByDevelopment} layout="vertical">
            <CartesianGrid strokeDasharray="3 3" /><XAxis type="number" fontSize={11} /><YAxis type="category" dataKey="label" width={120} fontSize={10} />
            <Tooltip formatter={(v: number) => brl(v)} />
            <Bar dataKey="value" fill="#b45309" name="Inadimplência" />
          </BarChart>
        </ChartCard>

        <ChartCard title="Vendas por forma de pagamento">
          <PieChart>
            <Pie data={data.salesByPaymentMethod} dataKey="value" nameKey="label" outerRadius={80} label>
              {data.salesByPaymentMethod.map((_, i) => <Cell key={i} fill={COLORS[i % COLORS.length]} />)}
            </Pie>
            <Tooltip formatter={(v: number) => brl(v)} /><Legend />
          </PieChart>
        </ChartCard>

        <ChartCard title="Contas a pagar: pagas × em aberto">
          <PieChart>
            <Pie data={data.payablesPaidVsOpen} dataKey="value" nameKey="label" outerRadius={80} label>
              {data.payablesPaidVsOpen.map((_, i) => <Cell key={i} fill={[ '#0f766e', '#b45309'][i % 2]} />)}
            </Pie>
            <Tooltip formatter={(v: number) => brl(v)} /><Legend />
          </PieChart>
        </ChartCard>

        <ChartCard title="Clientes (ativos / inativos / inadimplentes)">
          <PieChart>
            <Pie data={clientPie} dataKey="value" nameKey="label" outerRadius={80} label>
              {clientPie.map((_, i) => <Cell key={i} fill={['#0f766e', '#6b7280', '#be123c'][i]} />)}
            </Pie>
            <Tooltip /><Legend />
          </PieChart>
        </ChartCard>
      </div>
      {empty(data.salesByMonth) && empty(data.receivedByMonth) && (
        <p className="text-center text-xs text-gray-400">Importe/registre dados para ver os gráficos populados.</p>
      )}
    </div>
  )
}
