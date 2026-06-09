import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ShieldCheck } from 'lucide-react'
import { api, apiErrorMessage } from '@/lib/api'
import type { AppUser, Development, Role } from '@/lib/types'
import { useToast } from '@/components/Toast'
import { useConfirm } from '@/components/Confirm'
import { Badge, Button, EmptyState, Field, Input, Modal, PageHeader, Select, Table, TableSkeleton, Tr } from '@/components/ui'

const GLOBAL_ACCESS = 'ACESSO_GLOBAL_EMPREENDIMENTOS'
const FALLBACK_ROLES = ['ADMIN', 'FINANCEIRO', 'CONTABILIDADE', 'COMERCIAL', 'VISUALIZADOR']
interface UserForm { id?: string; name: string; email: string; password?: string; role: string; active: boolean; developmentIds: string[] }
const EMPTY: UserForm = { name: '', email: '', password: '', role: 'VISUALIZADOR', active: true, developmentIds: [] }

export function UsersPage() {
  const queryClient = useQueryClient()
  const toast = useToast()
  const confirm = useConfirm()
  const [modalOpen, setModalOpen] = useState(false)
  const [form, setForm] = useState<UserForm>(EMPTY)
  const [error, setError] = useState<string | null>(null)

  const { data, isLoading } = useQuery({
    queryKey: ['users'],
    queryFn: async () => (await api.get<AppUser[]>('/users')).data,
  })

  const { data: roles } = useQuery({
    queryKey: ['roles'],
    queryFn: async () => (await api.get<Role[]>('/roles')).data,
  })
  const roleNames = roles?.map((r) => r.name) ?? FALLBACK_ROLES

  const { data: developments } = useQuery({
    queryKey: ['developments'],
    queryFn: async () => (await api.get<Development[]>('/developments')).data,
  })
  // O perfil selecionado tem acesso global? (então o escopo por empreendimento não restringe)
  const selectedRole = roles?.find((r) => r.name === form.role)
  const roleHasGlobalAccess = selectedRole ? selectedRole.permissions.includes(GLOBAL_ACCESS) : true
  function toggleDev(id: string) {
    setForm((f) => ({
      ...f,
      developmentIds: f.developmentIds.includes(id)
        ? f.developmentIds.filter((d) => d !== id)
        : [...f.developmentIds, id],
    }))
  }

  const save = useMutation({
    mutationFn: async (p: UserForm) => (p.id ? api.put(`/users/${p.id}`, p) : api.post('/users', p)),
    onSuccess: (_d, p) => { queryClient.invalidateQueries({ queryKey: ['users'] }); setModalOpen(false); setForm(EMPTY); toast.success(p.id ? 'Usuário atualizado.' : 'Usuário criado.') },
    onError: (e) => setError(apiErrorMessage(e)),
  })

  const remove = useMutation({
    mutationFn: async (id: string) => api.delete(`/users/${id}`),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['users'] }); toast.success('Usuário desativado.') },
    onError: (e) => toast.error(apiErrorMessage(e)),
  })

  async function confirmRemove(u: AppUser) {
    if (await confirm({ title: 'Desativar usuário', message: `Desativar o acesso de "${u.name}"?`, confirmLabel: 'Desativar', danger: true })) remove.mutate(u.id)
  }

  return (
    <div>
      <PageHeader title="Usuários e perfis" subtitle="Controle de acesso por perfil (RBAC)" action={<Button onClick={() => { setForm(EMPTY); setError(null); setModalOpen(true) }}>Novo usuário</Button>} />
      {isLoading ? <TableSkeleton rows={5} cols={5} /> : (
        <Table headers={['Nome', 'E-mail', 'Perfil', 'Status', '']}>
          {data?.map((u) => (
            <Tr key={u.id}>
              <td className="px-4 py-2 font-medium">{u.name}</td>
              <td className="px-4 py-2">{u.email}</td>
              <td className="px-4 py-2"><Badge color="blue">{u.role}</Badge></td>
              <td className="px-4 py-2"><Badge dot color={u.active ? 'green' : 'gray'}>{u.active ? 'Ativo' : 'Inativo'}</Badge></td>
              <td className="px-4 py-2 text-right">
                <Button variant="ghost" onClick={() => { setForm({ id: u.id, name: u.name, email: u.email, role: u.role, active: u.active, developmentIds: u.developmentIds ?? [] }); setError(null); setModalOpen(true) }}>Editar</Button>
                <Button variant="ghost" onClick={() => confirmRemove(u)}>Desativar</Button>
              </td>
            </Tr>
          ))}
          {data?.length === 0 && (
            <tr><td colSpan={5} className="p-0">
              <EmptyState icon={ShieldCheck} title="Nenhum usuário" description="Cadastre o primeiro usuário do sistema." />
            </td></tr>
          )}
        </Table>
      )}

      <Modal open={modalOpen} onClose={() => setModalOpen(false)} title={form.id ? 'Editar usuário' : 'Novo usuário'}>
        <form className="space-y-3" onSubmit={(e) => { e.preventDefault(); save.mutate(form) }}>
          <Field label="Nome"><Input value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} required /></Field>
          <Field label="E-mail"><Input type="email" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} required /></Field>
          <Field label={form.id ? 'Senha (deixe em branco para manter)' : 'Senha'}>
            <Input type="password" value={form.password ?? ''} onChange={(e) => setForm({ ...form, password: e.target.value })} />
          </Field>
          <div className="grid grid-cols-2 gap-3">
            <Field label="Perfil">
              <Select value={form.role} onChange={(e) => setForm({ ...form, role: e.target.value })}>
                {roleNames.map((r) => <option key={r} value={r}>{r}</option>)}
              </Select>
            </Field>
            <Field label="Ativo">
              <Select value={form.active ? '1' : '0'} onChange={(e) => setForm({ ...form, active: e.target.value === '1' })}>
                <option value="1">Sim</option>
                <option value="0">Não</option>
              </Select>
            </Field>
          </div>

          <Field label="Empreendimentos com acesso (escopo)">
            {roleHasGlobalAccess ? (
              <p className="rounded-md bg-blue-50 px-3 py-2 text-xs text-blue-700 dark:bg-blue-900/20 dark:text-blue-300">
                Este perfil tem <strong>acesso global</strong> — enxerga todos os empreendimentos. O escopo abaixo não restringe.
              </p>
            ) : (
              <p className="mb-1.5 text-xs text-amber-600 dark:text-amber-400">
                Perfil restrito: o usuário verá <strong>apenas</strong> os empreendimentos marcados (sem nenhum = não vê nada).
              </p>
            )}
            <div className="max-h-40 space-y-1 overflow-auto rounded-md border border-gray-200 p-2 dark:border-gray-700">
              {(developments ?? []).map((d) => (
                <label key={d.id} className="flex items-center gap-2 text-sm">
                  <input type="checkbox" checked={form.developmentIds.includes(d.id)} onChange={() => toggleDev(d.id)} />
                  {d.name}
                </label>
              ))}
              {(developments ?? []).length === 0 && <p className="text-xs text-gray-400">Nenhum empreendimento cadastrado.</p>}
            </div>
          </Field>

          {error && <p className="text-sm text-red-600">{error}</p>}
          <div className="flex justify-end gap-2 pt-2">
            <Button type="button" variant="outline" onClick={() => setModalOpen(false)}>Cancelar</Button>
            <Button type="submit" loading={save.isPending}>Salvar</Button>
          </div>
        </form>
      </Modal>
    </div>
  )
}
