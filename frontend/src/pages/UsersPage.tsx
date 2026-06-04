import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ShieldCheck } from 'lucide-react'
import { api, apiErrorMessage } from '@/lib/api'
import type { AppUser, Role } from '@/lib/types'
import { useToast } from '@/components/Toast'
import { useConfirm } from '@/components/Confirm'
import { Badge, Button, EmptyState, Field, Input, Modal, PageHeader, Select, Table, TableSkeleton, Tr } from '@/components/ui'

const FALLBACK_ROLES = ['ADMIN', 'FINANCEIRO', 'CONTABILIDADE', 'COMERCIAL', 'VISUALIZADOR']
interface UserForm { id?: string; name: string; email: string; password?: string; role: string; active: boolean }
const EMPTY: UserForm = { name: '', email: '', password: '', role: 'VISUALIZADOR', active: true }

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
                <Button variant="ghost" onClick={() => { setForm({ id: u.id, name: u.name, email: u.email, role: u.role, active: u.active }); setError(null); setModalOpen(true) }}>Editar</Button>
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
