import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Download } from 'lucide-react'
import { api, getToken } from '@/lib/api'
import type { Development, Dre } from '@/lib/types'
import { Button, Card, Field, Input, PageHeader, Select, Skeleton } from '@/components/ui'
import { formatCurrency } from '@/lib/utils'

export function DrePage() {
  const [from, setFrom] = useState('')
  const [to, setTo] = useState('')
  const [developmentId, setDevelopmentId] = useState('')

  const developments = useQuery({
    queryKey: ['developments'],
    queryFn: async () => (await api.get<Development[]>('/developments')).data,
  })

  const params = {
    from: from || undefined,
    to: to || undefined,
    developmentId: developmentId || undefined,
  }
  const { data, isLoading } = useQuery({
    queryKey: ['dre', from, to, developmentId],
    queryFn: async () => (await api.get<Dre>('/dre', { params })).data,
  })

  function clear() { setFrom(''); setTo(''); setDevelopmentId('') }

  function exportCsv() {
    const qs = new URLSearchParams()
    if (from) qs.set('from', from)
    if (to) qs.set('to', to)
    if (developmentId) qs.set('developmentId', developmentId)
    fetch(`${api.defaults.baseURL}/dre/export?${qs.toString()}`, { headers: { Authorization: `Bearer ${getToken()}` } })
      .then((r) => r.blob())
      .then((blob) => {
        const url = URL.createObjectURL(blob)
        const a = document.createElement('a')
        a.href = url; a.download = 'dre.csv'; a.click()
        URL.revokeObjectURL(url)
      })
  }

  const devName = developments.data?.find((d) => d.id === developmentId)?.name

  return (
    <div className="space-y-6">
      <PageHeader
        title="DRE — Demonstração do Resultado"
        subtitle="Base caixa: receitas recebidas − despesas pagas no período"
        action={<Button variant="outline" onClick={exportCsv}><Download className="h-4 w-4" /> Exportar CSV</Button>}
      />

      {/* Filtros */}
      <Card>
        <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-4">
          <Field label="Período de"><Input type="date" value={from} onChange={(e) => setFrom(e.target.value)} /></Field>
          <Field label="até"><Input type="date" value={to} onChange={(e) => setTo(e.target.value)} /></Field>
          <Field label="Empreendimento">
            <Select value={developmentId} onChange={(e) => setDevelopmentId(e.target.value)}>
              <option value="">Todos (consolidado)</option>
              {developments.data?.map((d) => <option key={d.id} value={d.id}>{d.name}</option>)}
            </Select>
          </Field>
          <div className="flex items-end">
            <Button variant="outline" onClick={clear}>Limpar filtros</Button>
          </div>
        </div>
      </Card>

      {isLoading || !data ? (
        <Card><Skeleton className="h-3 w-40" /><Skeleton className="mt-3 h-64 w-full" /></Card>
      ) : (
        <Card className="mx-auto max-w-2xl">
          <div className="mb-4 text-center">
            <div className="text-sm font-semibold text-gray-700 dark:text-gray-200">Demonstrativo de Resultado</div>
            <div className="text-xs text-gray-400">
              {devName ?? 'Consolidado (todos os empreendimentos)'}
              {(from || to) && ` · ${from || '…'} a ${to || '…'}`}
            </div>
          </div>

          {/* Receitas */}
          <Section title="Receitas">
            {data.revenues.map((r) => <Line key={r.label} label={r.label} value={r.value} />)}
            <Line label="Total de Receitas" value={data.totalRevenue} bold color="text-green-600" />
          </Section>

          {/* Despesas */}
          <Section title="(−) Despesas">
            {data.expenses.length === 0 && <p className="px-1 py-2 text-xs text-gray-400">Sem despesas pagas no período.</p>}
            {data.expenses.map((e) => <Line key={e.label} label={e.label} value={-e.value} />)}
            <Line label="Total de Despesas" value={-data.totalExpense} bold color="text-red-600" />
          </Section>

          {/* Resultado */}
          <div className={`mt-4 flex items-center justify-between rounded-lg px-4 py-3 ${data.result >= 0 ? 'bg-green-50 dark:bg-green-900/20' : 'bg-red-50 dark:bg-red-900/20'}`}>
            <span className="font-semibold text-gray-800 dark:text-gray-100">Resultado (Lucro / Prejuízo)</span>
            <span className={`text-lg font-bold ${data.result >= 0 ? 'text-green-600' : 'text-red-600'}`}>
              {formatCurrency(data.result)}
            </span>
          </div>
          {data.totalRevenue > 0 && (
            <p className="mt-2 text-center text-xs text-gray-400">
              Margem: {((data.result / data.totalRevenue) * 100).toFixed(1)}% das receitas
            </p>
          )}
        </Card>
      )}
    </div>
  )
}

function Section({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <div className="mb-3">
      <div className="mb-1 border-b border-gray-200 pb-1 text-xs font-semibold uppercase tracking-wide text-gray-500 dark:border-gray-700 dark:text-gray-400">
        {title}
      </div>
      <div className="divide-y divide-gray-50 dark:divide-gray-700/40">{children}</div>
    </div>
  )
}

function Line({ label, value, bold, color }: { label: string; value: number; bold?: boolean; color?: string }) {
  return (
    <div className={`flex items-center justify-between px-1 py-1.5 text-sm ${bold ? 'font-semibold' : ''}`}>
      <span className={bold ? 'text-gray-800 dark:text-gray-100' : 'text-gray-600 dark:text-gray-300'}>{label}</span>
      <span className={color ?? (value < 0 ? 'text-gray-600 dark:text-gray-300' : 'text-gray-800 dark:text-gray-100')}>
        {formatCurrency(value)}
      </span>
    </div>
  )
}
