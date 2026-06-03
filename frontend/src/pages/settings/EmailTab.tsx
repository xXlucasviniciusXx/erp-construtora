import { useEffect, useState } from 'react'
import { useMutation, useQuery } from '@tanstack/react-query'
import { Send } from 'lucide-react'
import { api, apiErrorMessage } from '@/lib/api'
import type { SystemSettings } from '@/lib/types'
import { useToast } from '@/components/Toast'
import { Button, Card, Field, Input, Select } from '@/components/ui'

export function EmailTab() {
  const toast = useToast()
  const [form, setForm] = useState<SystemSettings | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [testTo, setTestTo] = useState('')

  const { data } = useQuery({
    queryKey: ['settings'],
    queryFn: async () => (await api.get<SystemSettings>('/settings')).data,
  })
  useEffect(() => { if (data) setForm({ ...data, mailPassword: '' }) }, [data])

  const save = useMutation({
    mutationFn: async (p: SystemSettings) => api.put('/settings', p),
    onSuccess: () => toast.success('Configuração de e-mail salva.'),
    onError: (e) => setError(apiErrorMessage(e)),
  })

  const sendTest = useMutation({
    mutationFn: async (to: string) => api.post('/notifications/test', null, { params: { to } }),
    onSuccess: (r) => {
      const status = (r.data as { status?: string })?.status
      if (status === 'SENT') toast.success('E-mail de teste enviado com sucesso!')
      else if (status === 'PENDING') toast.info('E-mail registrado, mas envio está desligado/simulado. Ative e preencha o SMTP.')
      else toast.error('Falha no envio — verifique o host, usuário e senha do SMTP.')
    },
    onError: (e) => toast.error(apiErrorMessage(e)),
  })

  if (!form) return <p className="text-gray-500">Carregando…</p>

  return (
    <div className="max-w-2xl space-y-4">
      <Card className="space-y-4">
        <form className="space-y-4" onSubmit={(e) => { e.preventDefault(); save.mutate(form) }}>
          <div className="flex items-center justify-between">
            <div>
              <h3 className="text-sm font-semibold text-gray-700 dark:text-gray-200">Envio de e-mails</h3>
              <p className="text-xs text-gray-500 dark:text-gray-400">
                Quando desligado, as notificações são apenas registradas (modo simulado), sem envio real.
              </p>
            </div>
            <Field label="Habilitar envio">
              <Select value={form.mailEnabled ? '1' : '0'} onChange={(e) => setForm({ ...form, mailEnabled: e.target.value === '1' })}>
                <option value="0">Desligado (simulado)</option>
                <option value="1">Ligado (SMTP)</option>
              </Select>
            </Field>
          </div>

          <div className="grid grid-cols-1 gap-3 sm:grid-cols-3">
            <div className="sm:col-span-2">
              <Field label="Servidor SMTP (host)">
                <Input value={form.mailHost ?? ''} onChange={(e) => setForm({ ...form, mailHost: e.target.value })} placeholder="smtp.gmail.com" />
              </Field>
            </div>
            <Field label="Porta">
              <Input type="number" value={form.mailPort ?? 587} onChange={(e) => setForm({ ...form, mailPort: Number(e.target.value) })} placeholder="587" />
            </Field>
          </div>

          <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
            <Field label="Usuário">
              <Input value={form.mailUsername ?? ''} onChange={(e) => setForm({ ...form, mailUsername: e.target.value })} placeholder="usuario@dominio.com" />
            </Field>
            <Field label={form.mailPasswordSet ? 'Senha (deixe em branco para manter)' : 'Senha'}>
              <Input type="password" value={form.mailPassword ?? ''} onChange={(e) => setForm({ ...form, mailPassword: e.target.value })}
                placeholder={form.mailPasswordSet ? '•••••••• (já definida)' : 'senha ou app password'} />
            </Field>
          </div>

          <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
            <Field label="Remetente (From)">
              <Input value={form.mailFrom ?? ''} onChange={(e) => setForm({ ...form, mailFrom: e.target.value })} placeholder="nao-responder@suaempresa.com" />
            </Field>
            <Field label="Lembrete de vencimento (dias antes)">
              <Input type="number" min={1} value={form.mailReminderDays ?? 3} onChange={(e) => setForm({ ...form, mailReminderDays: Number(e.target.value) })} />
            </Field>
          </div>

          {error && <p className="text-sm text-red-600">{error}</p>}
          <Button type="submit" loading={save.isPending}>Salvar configuração de e-mail</Button>
        </form>
      </Card>

      {/* Teste de envio */}
      <Card className="space-y-3">
        <h3 className="text-sm font-semibold text-gray-700 dark:text-gray-200">Enviar e-mail de teste</h3>
        <p className="text-xs text-gray-500 dark:text-gray-400">
          Salve a configuração acima primeiro. Use um e-mail seu para validar o SMTP.
        </p>
        <div className="flex flex-wrap items-end gap-3">
          <div className="flex-1 sm:max-w-xs">
            <Field label="Destinatário">
              <Input type="email" value={testTo} onChange={(e) => setTestTo(e.target.value)} placeholder="voce@email.com" />
            </Field>
          </div>
          <Button variant="outline" disabled={!testTo || sendTest.isPending} loading={sendTest.isPending} onClick={() => sendTest.mutate(testTo)}>
            <Send className="h-4 w-4" /> Enviar teste
          </Button>
        </div>
      </Card>
    </div>
  )
}
