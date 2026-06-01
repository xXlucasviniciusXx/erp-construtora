import { useState } from 'react'
import { api, getToken } from '@/lib/api'
import { Button, Card, Field, Input, PageHeader } from '@/components/ui'

async function download(path: string, filename: string) {
  const res = await fetch(`${api.defaults.baseURL}${path}`, {
    headers: { Authorization: `Bearer ${getToken()}` },
  })
  if (!res.ok) {
    alert('Falha ao gerar relatório')
    return
  }
  const blob = await res.blob()
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  a.click()
  URL.revokeObjectURL(url)
}

export function ReportsPage() {
  const today = new Date().toISOString().slice(0, 10)
  const monthStart = today.slice(0, 8) + '01'
  const [start, setStart] = useState(monthStart)
  const [end, setEnd] = useState(today)

  return (
    <div className="space-y-6">
      <PageHeader title="Relatórios" />

      <Card className="space-y-4">
        <h2 className="text-sm font-semibold text-gray-700 dark:text-gray-200">Relatórios por período</h2>
        <div className="flex flex-wrap items-end gap-3">
          <Field label="Início"><Input type="date" value={start} onChange={(e) => setStart(e.target.value)} /></Field>
          <Field label="Fim"><Input type="date" value={end} onChange={(e) => setEnd(e.target.value)} /></Field>
          <Button variant="outline" onClick={() => download(`/reports/accounts-payable?start=${start}&end=${end}`, 'contas-a-pagar.csv')}>Contas a pagar</Button>
          <Button variant="outline" onClick={() => download(`/reports/accounts-receivable?start=${start}&end=${end}`, 'contas-a-receber.csv')}>Contas a receber</Button>
        </div>
      </Card>

      <Card className="space-y-3">
        <h2 className="text-sm font-semibold text-gray-700 dark:text-gray-200">Outros relatórios (CSV)</h2>
        <div className="flex flex-wrap gap-2">
          <Button variant="outline" onClick={() => download('/reports/overdue-installments', 'parcelas-em-atraso.csv')}>Parcelas em atraso</Button>
          <Button variant="outline" onClick={() => download('/reports/reconciliations', 'conciliacoes.csv')}>Conciliações realizadas</Button>
          <Button variant="outline" onClick={() => download('/reports/pending-transactions', 'transacoes-pendentes.csv')}>Transações pendentes</Button>
          <Button variant="outline" onClick={() => download('/reports/sales-by-development', 'vendas-por-empreendimento.csv')}>Vendas por empreendimento</Button>
          <Button variant="outline" onClick={() => download('/reports/delinquent-clients', 'clientes-inadimplentes.csv')}>Clientes inadimplentes</Button>
        </div>
      </Card>
    </div>
  )
}
