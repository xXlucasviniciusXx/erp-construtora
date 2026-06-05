import { useQuery } from '@tanstack/react-query'
import { api } from '@/lib/api'
import type { Client, Page, Sale } from '@/lib/types'
import { openContractPdf } from '@/lib/contracts'
import { useToast } from '@/components/Toast'
import { Badge, Button, Modal, Table, Tr } from '@/components/ui'
import { formatCurrency, formatDate } from '@/lib/utils'

const SALE_STATUS: Record<string, { label: string; color: string }> = {
  ACTIVE: { label: 'Ativa', color: 'blue' },
  COMPLETED: { label: 'Quitada', color: 'green' },
  CANCELLED: { label: 'Cancelada', color: 'gray' },
}

interface Props {
  client: Client
  canGenerate: boolean
  onClose: () => void
}

/**
 * Lista os contratos/vendas do cliente e permite gerar/visualizar o PDF de cada
 * um — reaproveita o mesmo fluxo de geração da tela de Vendas (openContractPdf).
 */
export function ClientContractsModal({ client, canGenerate, onClose }: Props) {
  const toast = useToast()
  const sales = useQuery({
    queryKey: ['client-contracts', client.id],
    queryFn: async () => (await api.get<Page<Sale>>('/sales', { params: { clientId: client.id, size: 500 } })).data.content,
  })

  async function generate(saleId: string) {
    try { await openContractPdf(saleId) } catch { toast.error('Não foi possível gerar o contrato.') }
  }

  const rows = sales.data ?? []
  return (
    <Modal open onClose={onClose} title={`Contratos — ${client.name}`} size="2xl">
      {sales.isLoading ? (
        <p className="py-6 text-center text-sm text-gray-400">Carregando…</p>
      ) : rows.length === 0 ? (
        <p className="py-6 text-center text-sm text-gray-400">Este cliente ainda não possui contratos/vendas.</p>
      ) : (
        <>
          <p className="mb-3 text-sm text-gray-500 dark:text-gray-400">
            {rows.length} contrato(s) vinculado(s){rows.length === 1 ? ' — gere/visualize abaixo.' : ' — selecione qual gerar/visualizar.'}
          </p>
          <Table headers={['Nº do contrato', 'Empreendimento / Quadra / Lote', 'Data da venda', 'Valor total', 'Status', 'Contrato']}>
            {rows.map((s) => (
              <Tr key={s.id}>
                <td className="px-4 py-2 font-medium">{s.contractNumber ?? '—'}</td>
                <td className="px-4 py-2">{s.propertyLabel}</td>
                <td className="px-4 py-2">{formatDate(s.saleDate)}</td>
                <td className="px-4 py-2">{formatCurrency(s.totalValue)}</td>
                <td className="px-4 py-2">
                  <Badge dot color={SALE_STATUS[s.status]?.color ?? 'gray'}>{SALE_STATUS[s.status]?.label ?? s.status}</Badge>
                </td>
                <td className="px-4 py-2 text-right">
                  <Button variant="outline" disabled={!canGenerate} onClick={() => generate(s.id)}>Gerar / visualizar</Button>
                </td>
              </Tr>
            ))}
          </Table>
        </>
      )}
    </Modal>
  )
}
