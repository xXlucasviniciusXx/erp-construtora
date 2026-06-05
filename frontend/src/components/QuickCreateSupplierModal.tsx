import { useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { api, apiErrorMessage } from '@/lib/api'
import type { Page, Supplier } from '@/lib/types'
import { useToast } from '@/components/Toast'
import { Button, Field, Input, Modal } from '@/components/ui'

interface Props {
  onClose: () => void
  /** Chamado com o fornecedor criado (ou o existente, em caso de documento duplicado). */
  onCreated: (supplier: Supplier) => void
  defaultName?: string
}

/**
 * Cadastro rápido de fornecedor (campos mínimos) reaproveitável — usado no
 * lançamento de despesa. Após salvar, devolve o fornecedor para seleção
 * automática. Monte condicionalmente no pai para resetar o estado a cada abertura.
 */
export function QuickCreateSupplierModal({ onClose, onCreated, defaultName }: Props) {
  const toast = useToast()
  const queryClient = useQueryClient()
  const [form, setForm] = useState<Partial<Supplier>>({ active: true, name: defaultName ?? '' })
  const [error, setError] = useState<string | null>(null)

  const save = useMutation({
    mutationFn: async (payload: Partial<Supplier>): Promise<{ dup: boolean; supplier: Supplier }> => {
      // Evita duplicado por CNPJ/CPF quando informado.
      if (payload.document) {
        const found = (await api.get<Page<Supplier>>('/suppliers', { params: { q: payload.document, size: 10 } })).data.content
        const dup = found.find((s) => s.document === payload.document)
        if (dup) return { dup: true, supplier: dup }
      }
      const created = (await api.post<Supplier>('/suppliers', payload)).data
      return { dup: false, supplier: created }
    },
    onSuccess: ({ dup, supplier }) => {
      queryClient.invalidateQueries({ queryKey: ['suppliers-all'] })
      queryClient.invalidateQueries({ queryKey: ['suppliers'] })
      if (dup) toast.info('Já existe um fornecedor com este documento — selecionado o existente.')
      else toast.success('Fornecedor cadastrado.')
      onCreated(supplier)
      onClose()
    },
    onError: (e) => setError(apiErrorMessage(e)),
  })

  return (
    <Modal open onClose={onClose} title="Novo fornecedor (cadastro rápido)">
      <form className="space-y-3" onSubmit={(e) => { e.preventDefault(); setError(null); save.mutate(form) }}>
        <Field label="Nome / Razão social">
          <Input value={form.name ?? ''} onChange={(e) => setForm({ ...form, name: e.target.value })} required autoFocus />
        </Field>
        <div className="grid grid-cols-2 gap-3">
          <Field label="CNPJ / CPF (opcional)">
            <Input value={form.document ?? ''} onChange={(e) => setForm({ ...form, document: e.target.value })} />
          </Field>
          <Field label="Telefone">
            <Input value={form.phone ?? ''} onChange={(e) => setForm({ ...form, phone: e.target.value })} />
          </Field>
        </div>
        <Field label="E-mail">
          <Input type="email" value={form.email ?? ''} onChange={(e) => setForm({ ...form, email: e.target.value })} />
        </Field>
        <Field label="Observações">
          <Input value={form.notes ?? ''} onChange={(e) => setForm({ ...form, notes: e.target.value })} />
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
