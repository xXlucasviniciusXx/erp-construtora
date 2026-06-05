import { useEffect, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api, apiErrorMessage } from '@/lib/api'
import type { ContractTemplate } from '@/lib/types'
import { ActionsMenu } from '@/components/Menu'
import { useToast } from '@/components/Toast'
import { useConfirm } from '@/components/Confirm'
import { Badge, Button, Field, Input, Modal, Select, Table, TableSkeleton, Tr } from '@/components/ui'
import { cn } from '@/lib/utils'

const KINDS = [
  { key: 'CONTRACT', label: 'Contrato' },
  { key: 'DISTRATO', label: 'Distrato' },
] as const
type Kind = (typeof KINDS)[number]['key']

/** Tokens disponíveis para uso no corpo do modelo. */
const TOKENS = [
  'empresa', 'numero_contrato', 'data_hoje',
  'cliente_nome', 'cliente_documento', 'cliente_rg_ie', 'cliente_endereco',
  'cliente_estado_civil', 'cliente_profissao', 'cliente_email', 'cliente_telefone',
  'empreendimento', 'quadra', 'lote', 'unidade', 'matricula', 'imovel_endereco',
  'area_total', 'area_construida',
  'valor_total', 'entrada', 'parcelas_qtd', 'primeiro_vencimento',
  'forma_pagamento', 'indice_correcao', 'parcelas_tabela', 'clausulas_extras',
  'distrato_data', 'distrato_motivo', 'distrato_devolucao', 'distrato_retido',
]

interface TemplateForm { id?: string; kind: Kind; name: string; body: string; isDefault: boolean; active: boolean }

export function ContractTemplatesTab() {
  const queryClient = useQueryClient()
  const toast = useToast()
  const confirm = useConfirm()
  const [kind, setKind] = useState<Kind>('CONTRACT')
  const [editing, setEditing] = useState<TemplateForm | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [preview, setPreview] = useState<string | null>(null)
  const [previewLoading, setPreviewLoading] = useState(false)

  const { data, isLoading } = useQuery({
    queryKey: ['contract-templates'],
    queryFn: async () => (await api.get<ContractTemplate[]>('/contract-templates')).data,
  })
  const items = (data ?? []).filter((t) => t.kind === kind)

  const save = useMutation({
    mutationFn: async (f: TemplateForm) =>
      f.id ? api.put(`/contract-templates/${f.id}`, f) : api.post('/contract-templates', f),
    onSuccess: (_d, f) => {
      queryClient.invalidateQueries({ queryKey: ['contract-templates'] })
      setEditing(null)
      toast.success(f.id ? 'Modelo atualizado.' : 'Modelo criado.')
    },
    onError: (e) => setError(apiErrorMessage(e)),
  })
  const remove = useMutation({
    mutationFn: async (id: string) => api.delete(`/contract-templates/${id}`),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['contract-templates'] }); toast.success('Modelo removido.') },
    onError: (e) => toast.error(apiErrorMessage(e)),
  })

  async function confirmRemove(t: ContractTemplate) {
    if (await confirm({ title: 'Remover modelo', message: `Remover "${t.name}"?`, confirmLabel: 'Remover', danger: true }))
      remove.mutate(t.id)
  }

  function openNew() {
    setError(null)
    setEditing({ kind, name: '', body: DEFAULT_SKELETON, isDefault: items.length === 0, active: true })
  }
  function openEdit(t: ContractTemplate) {
    setError(null)
    setEditing({ id: t.id, kind: t.kind, name: t.name, body: t.body, isDefault: t.isDefault, active: t.active })
  }

  async function runPreview() {
    if (!editing) return
    setPreviewLoading(true)
    try {
      const res = await api.post<string>('/contract-templates/preview', { body: editing.body }, { responseType: 'text' })
      setPreview(res.data)
    } catch (e) {
      toast.error(apiErrorMessage(e))
    } finally {
      setPreviewLoading(false)
    }
  }

  return (
    <div>
      <p className="mb-3 text-sm text-gray-500 dark:text-gray-400">
        Modelos de contrato e distrato gerados em PDF. Edite o texto livremente usando os <strong>tokens</strong> entre chaves duplas
        (ex.: <code className="rounded bg-gray-100 px-1 dark:bg-gray-800">{'{{cliente_nome}}'}</code>). O modelo marcado como
        <strong> padrão</strong> é o usado ao gerar o documento.
      </p>

      {/* Sub-abas por tipo */}
      <div className="mb-4 inline-flex rounded-lg border border-gray-200 bg-gray-50 p-0.5 dark:border-gray-700 dark:bg-gray-800">
        {KINDS.map((k) => (
          <button
            key={k.key}
            onClick={() => setKind(k.key)}
            className={cn(
              'rounded-md px-3 py-1.5 text-sm font-medium transition',
              kind === k.key
                ? 'bg-white text-gray-900 shadow-sm dark:bg-gray-700 dark:text-gray-100'
                : 'text-gray-500 hover:text-gray-700 dark:hover:text-gray-300',
            )}
          >
            {k.label}
          </button>
        ))}
      </div>

      <div className="mb-3 flex justify-end">
        <Button onClick={openNew}>Novo modelo</Button>
      </div>

      {isLoading ? <TableSkeleton rows={3} cols={3} /> : (
        <Table headers={['Nome', 'Status', 'Ações']}>
          {items.map((t) => (
            <Tr key={t.id}>
              <td className="px-4 py-2 font-medium">
                {t.name}
                {t.isDefault && <span className="ml-2"><Badge color="green">padrão</Badge></span>}
              </td>
              <td className="px-4 py-2"><Badge dot color={t.active ? 'green' : 'gray'}>{t.active ? 'Ativo' : 'Inativo'}</Badge></td>
              <td className="px-4 py-2 text-right">
                <ActionsMenu items={[
                  { label: 'Editar', onClick: () => openEdit(t) },
                  { label: 'Remover', danger: true, onClick: () => confirmRemove(t) },
                ]} />
              </td>
            </Tr>
          ))}
          {items.length === 0 && <tr><td colSpan={3} className="px-4 py-6 text-center text-sm text-gray-400">Nenhum modelo.</td></tr>}
        </Table>
      )}

      {/* Modal de edição */}
      {editing && (
        <Modal open onClose={() => setEditing(null)} title={editing.id ? 'Editar modelo' : 'Novo modelo'} size="xl">
          <form className="space-y-3" onSubmit={(e) => { e.preventDefault(); save.mutate(editing) }}>
            <div className="grid grid-cols-2 gap-3">
              <Field label="Nome do modelo">
                <Input value={editing.name} onChange={(e) => setEditing({ ...editing, name: e.target.value })} required autoFocus />
              </Field>
              <Field label="Tipo">
                <Select value={editing.kind} onChange={(e) => setEditing({ ...editing, kind: e.target.value as Kind })}>
                  {KINDS.map((k) => <option key={k.key} value={k.key}>{k.label}</option>)}
                </Select>
              </Field>
            </div>

            <Field label="Corpo do modelo (XHTML com tokens)">
              <textarea
                className="min-h-[280px] w-full rounded-md border border-gray-300 bg-white px-3 py-2 font-mono text-xs dark:border-gray-600 dark:bg-gray-800"
                value={editing.body}
                onChange={(e) => setEditing({ ...editing, body: e.target.value })}
                spellCheck={false}
                required
              />
            </Field>

            <details className="rounded-md border border-gray-200 bg-gray-50 p-2 text-xs dark:border-gray-700 dark:bg-gray-800/50">
              <summary className="cursor-pointer font-medium text-gray-600 dark:text-gray-300">Tokens disponíveis ({TOKENS.length})</summary>
              <div className="mt-2 flex flex-wrap gap-1">
                {TOKENS.map((tk) => (
                  <button key={tk} type="button"
                    onClick={() => setEditing((cur) => cur ? { ...cur, body: cur.body + `{{${tk}}}` } : cur)}
                    className="rounded bg-gray-200 px-1.5 py-0.5 font-mono text-[11px] text-gray-700 hover:bg-primary hover:text-white dark:bg-gray-700 dark:text-gray-200">
                    {`{{${tk}}}`}
                  </button>
                ))}
              </div>
            </details>

            <div className="flex items-center gap-4">
              <label className="flex items-center gap-2 text-sm">
                <input type="checkbox" checked={editing.isDefault} onChange={(e) => setEditing({ ...editing, isDefault: e.target.checked })} />
                Usar como modelo padrão
              </label>
              <label className="flex items-center gap-2 text-sm">
                <input type="checkbox" checked={editing.active} onChange={(e) => setEditing({ ...editing, active: e.target.checked })} />
                Ativo
              </label>
            </div>

            {error && <p className="text-sm text-red-600">{error}</p>}
            <div className="flex justify-between gap-2 pt-2">
              <Button type="button" variant="outline" onClick={runPreview} loading={previewLoading}>Pré-visualizar</Button>
              <div className="flex gap-2">
                <Button type="button" variant="outline" onClick={() => setEditing(null)}>Cancelar</Button>
                <Button type="submit" loading={save.isPending}>Salvar modelo</Button>
              </div>
            </div>
          </form>
        </Modal>
      )}

      {/* Modal de pré-visualização */}
      {preview !== null && (
        <Modal open onClose={() => setPreview(null)} title="Pré-visualização (dados de exemplo)" size="xl">
          <iframe title="preview" srcDoc={preview} className="h-[60vh] w-full rounded border border-gray-200 bg-white dark:border-gray-700" />
        </Modal>
      )}
    </div>
  )
}

const DEFAULT_SKELETON = `<?xml version="1.0" encoding="UTF-8"?>
<html xmlns="http://www.w3.org/1999/xhtml">
<head><style>
  body { font-family: sans-serif; font-size: 12px; color: #111; }
  h1 { font-size: 18px; text-align: center; }
</style></head>
<body>
  <h1>NOVO DOCUMENTO</h1>
  <p>Contrato nº {{numero_contrato}}</p>
  <p>Comprador(a): {{cliente_nome}} — CPF/CNPJ {{cliente_documento}}</p>
  <p>Imóvel: {{empreendimento}}, Quadra {{quadra}}, Lote {{lote}}</p>
  <p>Valor total: R$ {{valor_total}}</p>
  <p>Local e data: ____________________, {{data_hoje}}.</p>
</body></html>`
