import { useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { api, apiErrorMessage } from '@/lib/api'
import type { Client, Page } from '@/lib/types'
import { useToast } from '@/components/Toast'
import { Button, Field, Input, Modal, Select } from '@/components/ui'

interface Props {
  onClose: () => void
  /** Chamado com o cliente criado (ou o existente, em caso de documento duplicado). */
  onCreated: (client: Client) => void
  defaultName?: string
}

/**
 * Cadastro rápido de cliente (campos mínimos) reaproveitável em qualquer tela —
 * usado na Nova venda. Após salvar, devolve o cliente para seleção automática.
 * Monte condicionalmente no pai (`{open && <QuickCreateClientModal .../>}`) para
 * resetar o estado a cada abertura.
 */
export function QuickCreateClientModal({ onClose, onCreated, defaultName }: Props) {
  const toast = useToast()
  const queryClient = useQueryClient()
  const [form, setForm] = useState<Partial<Client>>({ personType: 'PF', status: 'ACTIVE', name: defaultName ?? '' })
  const [error, setError] = useState<string | null>(null)

  const save = useMutation({
    mutationFn: async (payload: Partial<Client>): Promise<{ dup: boolean; client: Client }> => {
      // Evita duplicado por CPF/CNPJ quando informado.
      if (payload.document) {
        const found = (await api.get<Page<Client>>('/clients', { params: { q: payload.document, size: 10 } })).data.content
        const dup = found.find((c) => c.document === payload.document)
        if (dup) return { dup: true, client: dup }
      }
      const created = (await api.post<Client>('/clients', payload)).data
      return { dup: false, client: created }
    },
    onSuccess: ({ dup, client }) => {
      queryClient.invalidateQueries({ queryKey: ['clients-search'] })
      queryClient.invalidateQueries({ queryKey: ['clients'] })
      if (dup) toast.info('Já existe um cliente com este CPF/CNPJ — selecionado o existente.')
      else toast.success('Cliente cadastrado.')
      onCreated(client)
      onClose()
    },
    onError: (e) => setError(apiErrorMessage(e)),
  })

  return (
    <Modal open onClose={onClose} title="Novo cliente (cadastro rápido)">
      <form className="space-y-3" onSubmit={(e) => { e.preventDefault(); setError(null); save.mutate(form) }}>
        <div className="grid grid-cols-3 gap-3">
          <Field label="Tipo">
            <Select value={form.personType} onChange={(e) => setForm({ ...form, personType: e.target.value as Client['personType'] })}>
              <option value="PF">Pessoa Física</option>
              <option value="PJ">Pessoa Jurídica</option>
            </Select>
          </Field>
          <div className="col-span-2">
            <Field label="Nome / Razão social">
              <Input value={form.name ?? ''} onChange={(e) => setForm({ ...form, name: e.target.value })} required autoFocus />
            </Field>
          </div>
        </div>
        <Field label="CPF / CNPJ">
          <Input value={form.document ?? ''} onChange={(e) => setForm({ ...form, document: e.target.value })} required />
        </Field>
        <div className="grid grid-cols-2 gap-3">
          <Field label="Telefone">
            <Input value={form.phone ?? ''} onChange={(e) => setForm({ ...form, phone: e.target.value })} />
          </Field>
          <Field label="E-mail">
            <Input type="email" value={form.email ?? ''} onChange={(e) => setForm({ ...form, email: e.target.value })} />
          </Field>
        </div>
        <Field label="Endereço">
          <Input value={form.address ?? ''} onChange={(e) => setForm({ ...form, address: e.target.value })} />
        </Field>
        {error && <p className="text-sm text-red-600">{error}</p>}
        <div className="flex justify-end gap-2 pt-2">
          <Button type="button" variant="outline" onClick={onClose}>Cancelar</Button>
          <Button type="submit" loading={save.isPending}>Salvar e selecionar</Button>
        </div>
      </form>
    </Modal>
  )
}
