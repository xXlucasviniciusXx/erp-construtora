import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Mail } from 'lucide-react'
import { api, apiErrorMessage } from '@/lib/api'
import type { EmailNotification, Page } from '@/lib/types'
import { ActionsMenu } from '@/components/Menu'
import { useToast } from '@/components/Toast'
import { Badge, EmptyState, Modal, PageHeader, Pagination, Select, Table, TableSkeleton, Tr } from '@/components/ui'
import { formatDateTime } from '@/lib/utils'

const EVENT_LABEL: Record<string, string> = {
  PAYMENT_OVERDUE: 'Atraso', PAYMENT_DUE_SOON: 'Lembrete', PAYMENT_CONFIRMED: 'Pagamento',
  SALE_CREATED: 'Venda', CONTRACT_GENERATED: 'Contrato', TEST: 'Teste',
}
const STATUS: Record<string, { label: string; color: string }> = {
  SENT: { label: 'Enviado', color: 'green' }, PENDING: { label: 'Pendente', color: 'gray' }, FAILED: { label: 'Falhou', color: 'red' },
}

export function NotificationsPage() {
  const queryClient = useQueryClient()
  const toast = useToast()
  const [status, setStatus] = useState('')
  const [eventType, setEventType] = useState('')
  const [page, setPage] = useState(0)
  const [view, setView] = useState<EmailNotification | null>(null)

  const { data, isLoading } = useQuery({
    queryKey: ['notifications', status, eventType, page],
    queryFn: async () => (await api.get<Page<EmailNotification>>('/notifications', {
      params: { status: status || undefined, eventType: eventType || undefined, page, size: 20 },
    })).data,
  })

  const resend = useMutation({
    mutationFn: async (id: string) => api.post(`/notifications/${id}/resend`),
    onSuccess: (r) => {
      queryClient.invalidateQueries({ queryKey: ['notifications'] })
      const st = (r.data as { status?: string })?.status
      if (st === 'SENT') toast.success('Reenviado com sucesso.')
      else if (st === 'PENDING') toast.info('Registrado, mas o envio está desligado/simulado.')
      else toast.error('Falha no reenvio — verifique o SMTP nas Configurações.')
    },
    onError: (e) => toast.error(apiErrorMessage(e)),
  })

  return (
    <div>
      <PageHeader title="Notificações" subtitle="Histórico de e-mails (enviados, pendentes e com falha)" />

      <div className="mb-4 grid grid-cols-1 gap-3 sm:grid-cols-3">
        <Select value={status} onChange={(e) => { setStatus(e.target.value); setPage(0) }}>
          <option value="">Todos os status</option>
          <option value="SENT">Enviado</option>
          <option value="PENDING">Pendente (simulado)</option>
          <option value="FAILED">Falhou</option>
        </Select>
        <Select value={eventType} onChange={(e) => { setEventType(e.target.value); setPage(0) }}>
          <option value="">Todos os eventos</option>
          {Object.entries(EVENT_LABEL).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
        </Select>
      </div>

      {isLoading ? <TableSkeleton rows={6} cols={6} /> : (
        <Table headers={['Data', 'Destinatário', 'Assunto', 'Evento', 'Status', 'Ações']}>
          {data?.content.map((n) => (
            <Tr key={n.id}>
              <td className="px-4 py-2 whitespace-nowrap">{formatDateTime(n.sentAt ?? n.createdAt)}</td>
              <td className="px-4 py-2">{n.recipient}</td>
              <td className="px-4 py-2">{n.subject}</td>
              <td className="px-4 py-2"><Badge color="blue">{EVENT_LABEL[n.eventType] ?? n.eventType}</Badge></td>
              <td className="px-4 py-2">
                <Badge dot color={STATUS[n.status]?.color ?? 'gray'}>{STATUS[n.status]?.label ?? n.status}</Badge>
                {n.status === 'FAILED' && n.error && <span className="ml-2 text-xs text-red-500" title={n.error}>⚠</span>}
              </td>
              <td className="px-4 py-2 text-right">
                <ActionsMenu items={[
                  { label: 'Visualizar', onClick: () => setView(n) },
                  ...(n.status !== 'SENT' ? [{ label: 'Reenviar', onClick: () => resend.mutate(n.id) }] : []),
                ]} />
              </td>
            </Tr>
          ))}
          {data?.content.length === 0 && (
            <tr><td colSpan={6} className="p-0">
              <EmptyState icon={Mail} title="Nenhuma notificação" description="Os e-mails enviados pelo sistema aparecem aqui." />
            </td></tr>
          )}
        </Table>
      )}
      {data && <Pagination page={data.number} totalPages={data.totalPages} totalElements={data.totalElements} onChange={setPage} />}

      {view && (
        <Modal open onClose={() => setView(null)} title={view.subject} size="xl">
          <div className="mb-3 grid grid-cols-2 gap-2 text-sm">
            <div><div className="text-xs text-gray-400">Destinatário</div><div>{view.recipient}</div></div>
            <div><div className="text-xs text-gray-400">Status</div><Badge dot color={STATUS[view.status]?.color ?? 'gray'}>{STATUS[view.status]?.label ?? view.status}</Badge></div>
            <div><div className="text-xs text-gray-400">Evento</div><div>{EVENT_LABEL[view.eventType] ?? view.eventType}</div></div>
            <div><div className="text-xs text-gray-400">Enviado em</div><div>{formatDateTime(view.sentAt ?? view.createdAt)}</div></div>
          </div>
          {view.error && <p className="mb-3 rounded-md bg-red-50 px-3 py-2 text-xs text-red-600 dark:bg-red-900/20 dark:text-red-300">{view.error}</p>}
          <div className="text-xs text-gray-400">Prévia do e-mail</div>
          <div className="mt-1 overflow-auto rounded-lg border border-gray-200 bg-gray-50 p-3 dark:border-gray-700 dark:bg-gray-900/40"
               dangerouslySetInnerHTML={{ __html: view.body }} />
        </Modal>
      )}
    </div>
  )
}
