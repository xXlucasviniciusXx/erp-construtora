import { lazy, Suspense, useRef, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api, apiErrorMessage } from '@/lib/api'
import type { ContractTemplate, Development } from '@/lib/types'
import type { RichEditorHandle } from '@/components/editor/ContractTemplateRichEditor'
import { ActionsMenu } from '@/components/Menu'
import { useToast } from '@/components/Toast'
import { useConfirm } from '@/components/Confirm'
import { Badge, Button, Field, Input, Modal, Select, Table, TableSkeleton, Tr } from '@/components/ui'
import { cn } from '@/lib/utils'

// O editor visual (TipTap) é pesado: carrega só quando o modal de edição abre.
const ContractTemplateRichEditor = lazy(() =>
  import('@/components/editor/ContractTemplateRichEditor').then((m) => ({ default: m.ContractTemplateRichEditor })),
)

const KINDS = [
  { key: 'CONTRACT', label: 'Contrato' },
  { key: 'DISTRATO', label: 'Distrato' },
] as const
type Kind = (typeof KINDS)[number]['key']

interface TemplateForm { id?: string; kind: Kind; name: string; body: string; developmentId: string; isDefault: boolean; active: boolean }

export function ContractTemplatesTab() {
  const queryClient = useQueryClient()
  const toast = useToast()
  const confirm = useConfirm()
  const [kind, setKind] = useState<Kind>('CONTRACT')
  const [editing, setEditing] = useState<TemplateForm | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [preview, setPreview] = useState<string | null>(null)
  const [previewLoading, setPreviewLoading] = useState(false)
  const editorRef = useRef<RichEditorHandle>(null)

  /** Conteúdo atual do editor (fonte da verdade), com fallback para o valor inicial. */
  const currentBody = () => editorRef.current?.getContent() || editing?.body || ''

  const { data, isLoading } = useQuery({
    queryKey: ['contract-templates'],
    queryFn: async () => (await api.get<ContractTemplate[]>('/contract-templates')).data,
  })
  const items = (data ?? []).filter((t) => t.kind === kind)

  const developments = useQuery({
    queryKey: ['developments'],
    queryFn: async () => (await api.get<Development[]>('/developments')).data,
  })
  const devName = (id?: string | null) =>
    id ? (developments.data?.find((d) => d.id === id)?.name ?? 'Empreendimento') : 'Global'

  const save = useMutation({
    mutationFn: async (f: Omit<TemplateForm, 'developmentId'> & { developmentId: string | null }) =>
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
  const duplicate = useMutation({
    mutationFn: async (id: string) => api.post(`/contract-templates/${id}/copy`),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['contract-templates'] }); toast.success('Modelo duplicado.') },
    onError: (e) => toast.error(apiErrorMessage(e)),
  })

  async function confirmRemove(t: ContractTemplate) {
    if (await confirm({ title: 'Remover modelo', message: `Remover "${t.name}"?`, confirmLabel: 'Remover', danger: true }))
      remove.mutate(t.id)
  }

  function openNew() {
    setError(null)
    setEditing({ kind, name: '', body: DEFAULT_FRAGMENT, developmentId: '', isDefault: items.length === 0, active: true })
  }
  function openEdit(t: ContractTemplate) {
    setError(null)
    setEditing({ id: t.id, kind: t.kind, name: t.name, body: t.body, developmentId: t.developmentId ?? '', isDefault: t.isDefault, active: t.active })
  }

  async function runPreview() {
    if (!editing) return
    setPreviewLoading(true)
    try {
      const res = await api.post<string>('/contract-templates/preview', { body: currentBody() }, { responseType: 'text' })
      setPreview(res.data)
    } catch (e) {
      toast.error(apiErrorMessage(e))
    } finally {
      setPreviewLoading(false)
    }
  }

  function submitForm(e: React.FormEvent) {
    e.preventDefault()
    if (!editing) return
    // developmentId vazio = global (envia null)
    save.mutate({ ...editing, body: currentBody(), developmentId: editing.developmentId || null })
  }

  return (
    <div>
      <p className="mb-3 text-sm text-gray-500 dark:text-gray-400">
        Modelos de contrato e distrato gerados em PDF. Edite o texto como em um editor comum e insira os
        <strong> campos dinâmicos</strong> pela paleta lateral. O modelo marcado como <strong>padrão</strong> é o usado ao gerar o documento.
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
        <Table headers={['Nome', 'Escopo', 'Status', 'Ações']}>
          {items.map((t) => (
            <Tr key={t.id}>
              <td className="px-4 py-2 font-medium">
                {t.name}
                {t.isDefault && <span className="ml-2"><Badge color="green">padrão</Badge></span>}
              </td>
              <td className="px-4 py-2">
                <Badge color={t.developmentId ? 'blue' : 'gray'}>{devName(t.developmentId)}</Badge>
              </td>
              <td className="px-4 py-2"><Badge dot color={t.active ? 'green' : 'gray'}>{t.active ? 'Ativo' : 'Inativo'}</Badge></td>
              <td className="px-4 py-2 text-right">
                <ActionsMenu items={[
                  { label: 'Editar', onClick: () => openEdit(t) },
                  { label: 'Duplicar', onClick: () => duplicate.mutate(t.id) },
                  { label: 'Remover', danger: true, onClick: () => confirmRemove(t) },
                ]} />
              </td>
            </Tr>
          ))}
          {items.length === 0 && <tr><td colSpan={4} className="px-4 py-6 text-center text-sm text-gray-400">Nenhum modelo.</td></tr>}
        </Table>
      )}

      {/* Modal de edição */}
      {editing && (
        <Modal open onClose={() => setEditing(null)} title={editing.id ? 'Editar modelo' : 'Novo modelo'} size="xl">
          <form className="space-y-3" onSubmit={submitForm}>
            <Field label="Nome do modelo">
              <Input value={editing.name} onChange={(e) => setEditing({ ...editing, name: e.target.value })} required autoFocus />
            </Field>
            <div className="grid grid-cols-2 gap-3">
              <Field label="Tipo">
                <Select value={editing.kind} onChange={(e) => setEditing({ ...editing, kind: e.target.value as Kind })}>
                  {KINDS.map((k) => <option key={k.key} value={k.key}>{k.label}</option>)}
                </Select>
              </Field>
              <Field label="Empreendimento">
                <Select value={editing.developmentId} onChange={(e) => setEditing({ ...editing, developmentId: e.target.value })}>
                  <option value="">Global (todos)</option>
                  {(developments.data ?? []).map((d) => <option key={d.id} value={d.id}>{d.name}</option>)}
                </Select>
                <p className="mt-1 text-[11px] text-gray-400">
                  Modelo específico de um empreendimento tem prioridade sobre o global na geração.
                </p>
              </Field>
            </div>

            <Suspense fallback={<div className="h-72 animate-pulse rounded-md bg-gray-100 dark:bg-gray-800" />}>
              <ContractTemplateRichEditor
                key={editing.id ?? 'new'}
                ref={editorRef}
                value={editing.body}
                onPreview={runPreview}
              />
            </Suspense>

            <div className="flex items-center gap-4">
              <label className="flex items-center gap-2 text-sm">
                <input type="checkbox" checked={editing.isDefault} onChange={(e) => setEditing({ ...editing, isDefault: e.target.checked })} />
                Usar como modelo padrão
              </label>
              <label className="flex items-center gap-2 text-sm">
                <input type="checkbox" checked={editing.active} onChange={(e) => setEditing({ ...editing, active: e.target.checked })} />
                Ativo
              </label>
              {previewLoading && <span className="text-xs text-gray-400">gerando pré-visualização…</span>}
            </div>

            {error && <p className="text-sm text-red-600">{error}</p>}
            <div className="flex justify-end gap-2 pt-2">
              <Button type="button" variant="outline" onClick={() => setEditing(null)}>Cancelar</Button>
              <Button type="submit" loading={save.isPending}>Salvar modelo</Button>
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

/** Fragmento inicial para um modelo novo (o usuário edita visualmente). */
const DEFAULT_FRAGMENT = `<h1>NOVO DOCUMENTO</h1>
<p>Contrato nº <span data-token="numero_contrato"></span></p>
<p>Comprador(a): <span data-token="cliente_nome"></span> — CPF/CNPJ <span data-token="cliente_documento"></span></p>
<p>Imóvel: <span data-token="empreendimento"></span>, Quadra <span data-token="quadra"></span>, Lote <span data-token="lote"></span></p>
<p>Valor total: R$ <span data-token="valor_total"></span></p>
<p>Local e data: ____________________, <span data-token="data_hoje"></span>.</p>`
