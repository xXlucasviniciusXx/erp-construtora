import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import {
  Bar, BarChart, CartesianGrid, Cell, Legend, Line, LineChart, Pie, PieChart,
  ResponsiveContainer, Tooltip, XAxis, YAxis,
} from 'recharts'
import { api } from '@/lib/api'
import type { Client, DashboardAnalytics, InstallmentDetail, Lot, Page, Point, Sale } from '@/lib/types'
import { Badge, Button, Card, Field, Input, Modal, PageHeader, Select, Skeleton, Table, Tr } from '@/components/ui'
import { formatCurrency, formatDate } from '@/lib/utils'

const COLORS = ['#1e40af', '#0f766e', '#b45309', '#be123c', '#7c3aed', '#0891b2', '#65a30d', '#db2777']
const brl = (v: number) => formatCurrency(v)

function Metric({ label, value, accent }: { label: string; value: string; accent?: string }) {
  return (
    <Card className="cursor-default transition-all duration-150 hover:-translate-y-0.5 hover:shadow-md hover:ring-1 hover:ring-primary/30">
      <div className="text-xs uppercase tracking-wide text-gray-500 dark:text-gray-400">{label}</div>
      <div className={`mt-2 text-xl font-semibold ${accent ?? 'text-gray-800 dark:text-gray-100'}`}>{value}</div>
    </Card>
  )
}

function ChartCard({ title, hint, footer, children }: { title: string; hint?: string; footer?: React.ReactNode; children: React.ReactNode }) {
  return (
    <Card className="transition-all duration-150 hover:shadow-md">
      <div className="mb-3 flex items-baseline justify-between gap-2">
        <h3 className="text-sm font-semibold text-gray-700 dark:text-gray-200">{title}</h3>
        {hint && <span className="text-[11px] text-gray-400">{hint}</span>}
      </div>
      <div style={{ width: '100%', height: 220 }}>
        <ResponsiveContainer>{children as any}</ResponsiveContainer>
      </div>
      {footer}
    </Card>
  )
}

type DrillState = { kind: 'overdue' | 'sales'; key: string; title: string }

export function DashboardPage() {
  const [from, setFrom] = useState('')
  const [to, setTo] = useState('')
  const [clientId, setClientId] = useState('')
  const [propertyId, setPropertyId] = useState('')

  const clients = useQuery({
    queryKey: ['clients-all'],
    queryFn: async () => (await api.get<Page<Client>>('/clients', { params: { size: 200 } })).data.content,
  })
  const properties = useQuery({
    queryKey: ['lots-all'],
    queryFn: async () => (await api.get<Lot[]>('/lots')).data,
  })

  // Fontes para o drill-down (clicar no gráfico → lista detalhada)
  const allSales = useQuery({
    queryKey: ['sales-all'],
    queryFn: async () => (await api.get<Sale[]>('/sales')).data,
  })
  const overdueInst = useQuery({
    queryKey: ['installments-overdue'],
    queryFn: async () => (await api.get<InstallmentDetail[]>('/installments', { params: { status: 'OVERDUE' } })).data,
  })
  const [drill, setDrill] = useState<DrillState | null>(null)

  const { data, isLoading } = useQuery({
    queryKey: ['dashboard-analytics', from, to, clientId, propertyId],
    queryFn: async () =>
      (await api.get<DashboardAnalytics>('/dashboard/analytics', {
        params: {
          from: from || undefined, to: to || undefined,
          clientId: clientId || undefined, propertyId: propertyId || undefined,
        },
      })).data,
  })

  function clearFilters() { setFrom(''); setTo(''); setClientId(''); setPropertyId('') }

  const clientPie: Point[] = data ? [
    { label: 'Ativos', value: data.activeClients },
    { label: 'Inativos', value: data.inactiveClients },
    { label: 'Inadimplentes', value: data.delinquentClients },
  ] : []

  return (
    <div className="space-y-6">
      <PageHeader title="Dashboard" subtitle="Indicadores financeiros e comerciais" />

      {/* Filtros */}
      <Card>
        <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-5">
          <Field label="Período de"><Input type="date" value={from} onChange={(e) => setFrom(e.target.value)} /></Field>
          <Field label="até"><Input type="date" value={to} onChange={(e) => setTo(e.target.value)} /></Field>
          <Field label="Cliente">
            <Select value={clientId} onChange={(e) => setClientId(e.target.value)}>
              <option value="">Todos</option>
              {clients.data?.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
            </Select>
          </Field>
          <Field label="Lote">
            <Select value={propertyId} onChange={(e) => setPropertyId(e.target.value)}>
              <option value="">Todos</option>
              {properties.data?.map((p) => <option key={p.id} value={p.id}>{p.label}</option>)}
            </Select>
          </Field>
          <div className="flex items-end">
            <Button variant="outline" onClick={clearFilters}>Limpar filtros</Button>
          </div>
        </div>
      </Card>

      {isLoading || !data ? (
        <>
          <div className="grid grid-cols-2 gap-3 lg:grid-cols-4">
            {Array.from({ length: 8 }).map((_, i) => (
              <Card key={i}><Skeleton className="h-3 w-20" /><Skeleton className="mt-3 h-6 w-28" /></Card>
            ))}
          </div>
          <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
            {Array.from({ length: 4 }).map((_, i) => (
              <Card key={i}><Skeleton className="mb-3 h-3 w-40" /><Skeleton className="h-[220px] w-full" /></Card>
            ))}
          </div>
        </>
      ) : (
        <>
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

          <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
            <ChartCard title="Recebido por mês">
              <BarChart data={data.receivedByMonth}>
                <CartesianGrid strokeDasharray="3 3" /><XAxis dataKey="label" fontSize={11} /><YAxis fontSize={11} />
                <Tooltip formatter={(v: number) => brl(v)} /><Bar dataKey="value" fill="#0f766e" name="Recebido" />
              </BarChart>
            </ChartCard>

            <ChartCard title="A receber por mês (próximos meses)">
              <BarChart data={data.toReceiveByMonth}>
                <CartesianGrid strokeDasharray="3 3" /><XAxis dataKey="label" fontSize={11} /><YAxis fontSize={11} />
                <Tooltip formatter={(v: number) => brl(v)} /><Bar dataKey="value" fill="#1e40af" name="A receber" />
              </BarChart>
            </ChartCard>

            <ChartCard title="Fluxo de caixa previsto (a receber − a pagar)">
              <LineChart data={data.cashFlowForecast}>
                <CartesianGrid strokeDasharray="3 3" /><XAxis dataKey="label" fontSize={11} /><YAxis fontSize={11} />
                <Tooltip formatter={(v: number) => brl(v)} /><Line type="monotone" dataKey="value" stroke="#7c3aed" strokeWidth={2} name="Saldo previsto" />
              </LineChart>
            </ChartCard>

            <ChartCard title="Vendas por mês">
              <BarChart data={data.salesByMonth}>
                <CartesianGrid strokeDasharray="3 3" /><XAxis dataKey="label" fontSize={11} /><YAxis fontSize={11} />
                <Tooltip formatter={(v: number) => brl(v)} /><Bar dataKey="value" fill="#0891b2" name="Vendas" />
              </BarChart>
            </ChartCard>

            <ChartCard title="Parcelas vencidas por faixa de atraso">
              <BarChart data={data.overdueByAging}>
                <CartesianGrid strokeDasharray="3 3" /><XAxis dataKey="label" fontSize={10} /><YAxis fontSize={11} />
                <Tooltip formatter={(v: number) => brl(v)} /><Bar dataKey="value" fill="#be123c" name="Em atraso" />
              </BarChart>
            </ChartCard>

            <ChartCard title="Inadimplência por empreendimento" hint="Clique numa barra para ver as parcelas">
              <BarChart data={data.delinquencyByDevelopment} layout="vertical">
                <CartesianGrid strokeDasharray="3 3" /><XAxis type="number" fontSize={11} /><YAxis type="category" dataKey="label" width={120} fontSize={10} />
                <Tooltip formatter={(v: number) => brl(v)} />
                <Bar dataKey="value" fill="#b45309" name="Inadimplência" cursor="pointer"
                  onClick={(d: any) => setDrill({ kind: 'overdue', key: d?.payload?.label, title: `Parcelas em atraso — ${d?.payload?.label}` })} />
              </BarChart>
            </ChartCard>

            <ChartCard title="Vendas por forma de compra" hint="Clique numa fatia para ver as vendas">
              <PieChart>
                <Pie data={data.salesByPurchaseType} dataKey="value" nameKey="label" outerRadius={80} label cursor="pointer"
                  onClick={(d: any) => setDrill({ kind: 'sales', key: d?.label ?? d?.payload?.label, title: `Vendas — ${d?.label ?? d?.payload?.label}` })}>
                  {data.salesByPurchaseType.map((_, i) => <Cell key={i} fill={COLORS[i % COLORS.length]} />)}
                </Pie>
                <Tooltip formatter={(v: number) => brl(v)} /><Legend />
              </PieChart>
            </ChartCard>

            <ChartCard
              title="Contas a Receber — recebido × a receber"
              footer={<ReceivablesTotalizer data={data.receivablesReceivedVsOpen} />}
            >
              <PieChart>
                <Pie data={data.receivablesReceivedVsOpen} dataKey="value" nameKey="label" outerRadius={80} label>
                  {data.receivablesReceivedVsOpen.map((_, i) => <Cell key={i} fill={['#0f766e', '#b45309'][i % 2]} />)}
                </Pie>
                <Tooltip formatter={(v: number) => brl(v)} /><Legend />
              </PieChart>
            </ChartCard>

            <ChartCard title="Contas a Pagar — pagas × em aberto">
              <PieChart>
                <Pie data={data.payablesPaidVsOpen} dataKey="value" nameKey="label" outerRadius={80} label>
                  {data.payablesPaidVsOpen.map((_, i) => <Cell key={i} fill={['#0f766e', '#be123c'][i % 2]} />)}
                </Pie>
                <Tooltip formatter={(v: number) => brl(v)} /><Legend />
              </PieChart>
            </ChartCard>

            <ChartCard title="Despesas por empreendimento">
              <BarChart data={data.expensesByDevelopment} layout="vertical">
                <CartesianGrid strokeDasharray="3 3" /><XAxis type="number" fontSize={11} /><YAxis type="category" dataKey="label" width={120} fontSize={10} />
                <Tooltip formatter={(v: number) => brl(v)} /><Bar dataKey="value" fill="#be123c" name="Despesas pagas" />
              </BarChart>
            </ChartCard>

            <ChartCard title="Lucro/prejuízo por empreendimento (caixa)" hint="Recebido − despesas pagas">
              <BarChart data={data.profitByDevelopment} layout="vertical">
                <CartesianGrid strokeDasharray="3 3" /><XAxis type="number" fontSize={11} /><YAxis type="category" dataKey="label" width={120} fontSize={10} />
                <Tooltip formatter={(v: number) => brl(v)} />
                <Bar dataKey="value" name="Lucro/prejuízo">
                  {data.profitByDevelopment.map((pt, i) => <Cell key={i} fill={pt.value >= 0 ? '#0f766e' : '#be123c'} />)}
                </Bar>
              </BarChart>
            </ChartCard>

            <ChartCard title="Despesas por categoria (pagas)" hint="Plano de contas">
              <BarChart data={data.expensesByCategory.slice(0, 8)} layout="vertical">
                <CartesianGrid strokeDasharray="3 3" /><XAxis type="number" fontSize={11} /><YAxis type="category" dataKey="label" width={140} fontSize={9} />
                <Tooltip formatter={(v: number) => brl(v)} /><Bar dataKey="value" fill="#7c3aed" name="Despesas" />
              </BarChart>
            </ChartCard>

            <ChartCard title="Despesas por centro de custo (pagas)">
              <BarChart data={data.expensesByCostCenter.slice(0, 8)} layout="vertical">
                <CartesianGrid strokeDasharray="3 3" /><XAxis type="number" fontSize={11} /><YAxis type="category" dataKey="label" width={120} fontSize={10} />
                <Tooltip formatter={(v: number) => brl(v)} /><Bar dataKey="value" fill="#0891b2" name="Despesas" />
              </BarChart>
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
        </>
      )}

      {/* Drill-down: lista detalhada do segmento clicado */}
      {drill && (
        <DrillModal
          drill={drill}
          sales={allSales.data ?? []}
          overdue={overdueInst.data ?? []}
          onClose={() => setDrill(null)}
        />
      )}
    </div>
  )
}

function DrillModal({ drill, sales, overdue, onClose }: {
  drill: DrillState; sales: Sale[]; overdue: InstallmentDetail[]; onClose: () => void
}) {
  if (drill.kind === 'overdue') {
    const rows = overdue.filter((i) => i.development === drill.key)
    const total = rows.reduce((s, i) => s + (i.updatedAmount || i.amount), 0)
    return (
      <Modal open onClose={onClose} title={drill.title} size="xl">
        <p className="mb-3 text-sm text-gray-500 dark:text-gray-400">
          {rows.length} parcela(s) · total atualizado <span className="font-semibold text-amber-600">{formatCurrency(total)}</span>
        </p>
        <Table headers={['Cliente', 'Lote', 'Parcela', 'Vencimento', 'Atraso', 'Total atualizado']}>
          {rows.map((i) => (
            <Tr key={i.id}>
              <td className="px-4 py-2 font-medium">{i.clientName}</td>
              <td className="px-4 py-2">{i.propertyLabel ?? '—'}</td>
              <td className="px-4 py-2">#{i.number}</td>
              <td className="px-4 py-2">{formatDate(i.dueDate)}</td>
              <td className="px-4 py-2 text-red-600">{i.daysLate}d</td>
              <td className="px-4 py-2 font-medium">{formatCurrency(i.updatedAmount || i.amount)}</td>
            </Tr>
          ))}
          {rows.length === 0 && <tr><td colSpan={6} className="px-4 py-6 text-center text-sm text-gray-400">Sem parcelas neste empreendimento.</td></tr>}
        </Table>
      </Modal>
    )
  }
  // kind === 'sales'
  const rows = sales.filter((s) => (s.purchaseType ?? '—') === drill.key)
  const total = rows.reduce((s, v) => s + v.totalValue, 0)
  const STATUS: Record<string, string> = { ACTIVE: 'Ativa', COMPLETED: 'Quitada', CANCELLED: 'Cancelada' }
  return (
    <Modal open onClose={onClose} title={drill.title} size="xl">
      <p className="mb-3 text-sm text-gray-500 dark:text-gray-400">
        {rows.length} venda(s) · total <span className="font-semibold text-blue-700">{formatCurrency(total)}</span>
      </p>
      <Table headers={['Cliente', 'Lote', 'Valor', 'Data', 'Status']}>
        {rows.map((s) => (
          <Tr key={s.id}>
            <td className="px-4 py-2 font-medium">{s.clientName}</td>
            <td className="px-4 py-2">{s.propertyLabel}</td>
            <td className="px-4 py-2">{formatCurrency(s.totalValue)}</td>
            <td className="px-4 py-2">{formatDate(s.saleDate)}</td>
            <td className="px-4 py-2"><Badge dot color={s.status === 'ACTIVE' ? 'blue' : s.status === 'COMPLETED' ? 'green' : 'gray'}>{STATUS[s.status] ?? s.status}</Badge></td>
          </Tr>
        ))}
        {rows.length === 0 && <tr><td colSpan={5} className="px-4 py-6 text-center text-sm text-gray-400">Sem vendas nesta forma de compra.</td></tr>}
      </Table>
    </Modal>
  )
}

function ReceivablesTotalizer({ data }: { data: Point[] }) {
  const received = data.find((d) => d.label === 'Recebido')?.value ?? 0
  const toReceive = data.find((d) => d.label === 'A receber')?.value ?? 0
  const total = received + toReceive
  const pct = (v: number) => (total > 0 ? Math.round((v / total) * 100) : 0)
  return (
    <div className="mt-2 grid grid-cols-3 gap-2 border-t border-gray-100 pt-2 text-center text-xs dark:border-gray-700">
      <div><div className="text-gray-400">Total</div><div className="font-semibold">{brl(total)}</div></div>
      <div><div className="text-gray-400">Recebido</div><div className="font-semibold text-teal-600">{brl(received)} ({pct(received)}%)</div></div>
      <div><div className="text-gray-400">A receber</div><div className="font-semibold text-amber-600">{brl(toReceive)} ({pct(toReceive)}%)</div></div>
    </div>
  )
}
