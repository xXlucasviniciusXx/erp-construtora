import { useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api, apiErrorMessage } from '@/lib/api'
import type { Permission, Role } from '@/lib/types'
import { ActionsMenu } from '@/components/Menu'
import { useToast } from '@/components/Toast'
import { useConfirm } from '@/components/Confirm'
import { Badge, Button, Field, Input, Modal, Table, TableSkeleton, Tr } from '@/components/ui'

const MODULE_LABELS: Record<string, string> = {
  DASHBOARD: 'Dashboard', CLIENTES: 'Clientes', EMPREENDIMENTOS: 'Empreendimentos / Lotes',
  VENDAS: 'Vendas', CONTAS_PAGAR: 'Contas a Pagar', CONTAS_RECEBER: 'Contas a Receber',
  FORNECEDORES: 'Fornecedores', CONCILIACAO: 'Conciliação', DRE: 'DRE',
  RELATORIOS: 'Relatórios', NOTIFICACOES: 'Notificações', SISTEMA: 'Sistema',
}
const MODULE_ORDER = ['DASHBOARD', 'CLIENTES', 'EMPREENDIMENTOS', 'VENDAS', 'CONTAS_PAGAR',
  'CONTAS_RECEBER', 'FORNECEDORES', 'CONCILIACAO', 'DRE', 'RELATORIOS', 'NOTIFICACOES', 'SISTEMA']

interface ModuleGroup {
  module: string
  view?: Permission
  edit?: Permission
  manage: Permission[]
}

export function AccessProfilesTab() {
  const queryClient = useQueryClient()
  const toast = useToast()
  const confirm = useConfirm()
  const [editing, setEditing] = useState<Partial<Role> | null>(null)
  const [error, setError] = useState<string | null>(null)

  const { data: roles, isLoading } = useQuery({
    queryKey: ['roles'],
    queryFn: async () => (await api.get<Role[]>('/roles')).data,
  })
  const { data: perms } = useQuery({
    queryKey: ['permissions'],
    queryFn: async () => (await api.get<Permission[]>('/roles/permissions')).data,
  })

  const groups = useMemo<ModuleGroup[]>(() => {
    const map = new Map<string, ModuleGroup>()
    for (const p of perms ?? []) {
      const g = map.get(p.module) ?? { module: p.module, manage: [] }
      if (p.action === 'VIEW') g.view = p
      else if (p.action === 'EDIT') g.edit = p
      else g.manage.push(p)
      map.set(p.module, g)
    }
    return [...map.values()].sort((a, b) => {
      const ia = MODULE_ORDER.indexOf(a.module), ib = MODULE_ORDER.indexOf(b.module)
      return (ia < 0 ? 99 : ia) - (ib < 0 ? 99 : ib)
    })
  }, [perms])

  const save = useMutation({
    mutationFn: async (r: Partial<Role>) =>
      r.id ? api.put(`/roles/${r.id}`, r) : api.post('/roles', r),
    onSuccess: (_d, r) => {
      queryClient.invalidateQueries({ queryKey: ['roles'] })
      setEditing(null)
      toast.success(r.id ? 'Perfil atualizado.' : 'Perfil criado.')
    },
    onError: (e) => setError(apiErrorMessage(e)),
  })
  const remove = useMutation({
    mutationFn: async (id: string) => api.delete(`/roles/${id}`),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['roles'] }); toast.success('Perfil removido.') },
    onError: (e) => toast.error(apiErrorMessage(e)),
  })

  async function confirmRemove(r: Role) {
    if (await confirm({ title: 'Remover perfil', message: `Remover o perfil "${r.name}"?`, confirmLabel: 'Remover', danger: true }))
      remove.mutate(r.id)
  }

  function openNew() { setError(null); setEditing({ name: '', description: '', permissions: [] }) }
  function openEdit(r: Role) { setError(null); setEditing({ ...r, permissions: [...r.permissions] }) }

  function toggle(code: string, on: boolean, linkedView?: string, isView?: boolean, dependentEdit?: string) {
    setEditing((prev) => {
      if (!prev) return prev
      const set = new Set(prev.permissions ?? [])
      if (on) {
        set.add(code)
        if (linkedView) set.add(linkedView) // marcar EDITAR liga VER
      } else {
        set.delete(code)
        if (isView && dependentEdit) set.delete(dependentEdit) // desmarcar VER tira EDITAR
      }
      return { ...prev, permissions: [...set] }
    })
  }

  return (
    <div>
      <div className="mb-3 flex items-center justify-between">
        <p className="text-sm text-gray-500 dark:text-gray-400">
          Defina o que cada perfil <span className="font-medium">vê</span> e <span className="font-medium">edita</span> por módulo.
          O perfil ADMIN é protegido.
        </p>
        <Button onClick={openNew}>Novo perfil</Button>
      </div>

      {isLoading ? <TableSkeleton rows={5} cols={4} /> : (
        <Table headers={['Perfil', 'Descrição', 'Permissões', 'Usuários', 'Ações']}>
          {(roles ?? []).map((r) => (
            <Tr key={r.id}>
              <td className="px-4 py-2 font-medium">
                {r.name} {r.system && <Badge color="blue">protegido</Badge>}
              </td>
              <td className="px-4 py-2 text-gray-500">{r.description}</td>
              <td className="px-4 py-2 text-gray-500">{r.system ? 'Todas' : `${r.permissions.length}`}</td>
              <td className="px-4 py-2 text-gray-500">{r.userCount}</td>
              <td className="px-4 py-2 text-right">
                {r.system ? (
                  <span className="text-xs text-gray-400">—</span>
                ) : (
                  <ActionsMenu items={[
                    { label: 'Editar', onClick: () => openEdit(r) },
                    { label: 'Remover', danger: true, onClick: () => confirmRemove(r) },
                  ]} />
                )}
              </td>
            </Tr>
          ))}
        </Table>
      )}

      <Modal open={!!editing} onClose={() => setEditing(null)} title={editing?.id ? 'Editar perfil' : 'Novo perfil'} size="xl">
        {editing && (
          <form className="space-y-4" onSubmit={(e) => { e.preventDefault(); save.mutate({ ...editing, name: editing.name?.trim() }) }}>
            <div className="grid grid-cols-2 gap-3">
              <Field label="Nome do perfil">
                <Input value={editing.name ?? ''} onChange={(e) => setEditing({ ...editing, name: e.target.value })} placeholder="Ex.: CORRETOR" required />
              </Field>
              <Field label="Descrição">
                <Input value={editing.description ?? ''} onChange={(e) => setEditing({ ...editing, description: e.target.value })} placeholder="Para que serve" />
              </Field>
            </div>

            <div className="overflow-hidden rounded-lg border border-gray-200 dark:border-gray-700">
              <table className="w-full text-sm">
                <thead className="bg-gray-50 text-xs text-gray-500 dark:bg-gray-800 dark:text-gray-400">
                  <tr>
                    <th className="px-3 py-2 text-left font-medium">Módulo</th>
                    <th className="w-20 px-3 py-2 text-center font-medium">Ver</th>
                    <th className="w-20 px-3 py-2 text-center font-medium">Editar</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-100 dark:divide-gray-700">
                  {groups.map((g) => {
                    const sel = new Set(editing.permissions ?? [])
                    if (g.module === 'SISTEMA') {
                      return g.manage.map((p) => (
                        <tr key={p.code}>
                          <td className="px-3 py-2">{p.description ?? p.code}</td>
                          <td colSpan={2} className="px-3 py-2 text-center">
                            <input type="checkbox" checked={sel.has(p.code)} onChange={(e) => toggle(p.code, e.target.checked)} />
                          </td>
                        </tr>
                      ))
                    }
                    return (
                      <tr key={g.module}>
                        <td className="px-3 py-2 text-gray-800 dark:text-gray-100">{MODULE_LABELS[g.module] ?? g.module}</td>
                        <td className="px-3 py-2 text-center">
                          {g.view ? (
                            <input
                              type="checkbox"
                              checked={sel.has(g.view.code)}
                              onChange={(e) => toggle(g.view!.code, e.target.checked, undefined, true, g.edit?.code)}
                            />
                          ) : <span className="text-gray-300">—</span>}
                        </td>
                        <td className="px-3 py-2 text-center">
                          {g.edit ? (
                            <input
                              type="checkbox"
                              checked={sel.has(g.edit.code)}
                              onChange={(e) => toggle(g.edit!.code, e.target.checked, g.view?.code)}
                            />
                          ) : <span className="text-gray-300">—</span>}
                        </td>
                      </tr>
                    )
                  })}
                </tbody>
              </table>
            </div>

            {error && <p className="text-sm text-red-600">{error}</p>}
            <div className="flex justify-end gap-2">
              <Button type="button" variant="outline" onClick={() => setEditing(null)}>Cancelar</Button>
              <Button type="submit" loading={save.isPending}>Salvar perfil</Button>
            </div>
          </form>
        )}
      </Modal>
    </div>
  )
}
