import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api, apiErrorMessage } from '@/lib/api'
import type { BankAccount } from '@/lib/types'
import { useToast } from '@/components/Toast'
import { Badge, Button, Card, Field, PageHeader, Table, TableSkeleton, Select, Tr } from '@/components/ui'
import { formatDate } from '@/lib/utils'

export function ImportPage() {
  const queryClient = useQueryClient()
  const toast = useToast()
  const [bankAccountId, setBankAccountId] = useState('')
  const [file, setFile] = useState<File | null>(null)
  const [error, setError] = useState<string | null>(null)

  const accounts = useQuery({
    queryKey: ['bank-accounts'],
    queryFn: async () => (await api.get<BankAccount[]>('/bank-accounts')).data,
  })

  const history = useQuery({
    queryKey: ['imports', bankAccountId],
    queryFn: async () => (await api.get(`/bank-accounts/${bankAccountId}/imports`)).data as any[],
    enabled: !!bankAccountId,
  })

  const upload = useMutation({
    mutationFn: async () => {
      const fd = new FormData()
      fd.append('file', file!)
      return api.post(`/bank-accounts/${bankAccountId}/imports`, fd, {
        headers: { 'Content-Type': 'multipart/form-data' },
      })
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['imports', bankAccountId] })
      queryClient.invalidateQueries({ queryKey: ['pendencies'] })
      setFile(null)
      toast.success('Extrato importado com sucesso.')
    },
    onError: (e) => { setError(apiErrorMessage(e)); toast.error(apiErrorMessage(e)) },
  })

  return (
    <div className="space-y-6">
      <PageHeader title="Importação de extrato" subtitle="Envie arquivos CSV ou OFX para conciliar transações" />

      <Card className="max-w-xl space-y-4">
        <Field label="Conta bancária">
          <Select value={bankAccountId} onChange={(e) => setBankAccountId(e.target.value)}>
            <option value="">Selecione…</option>
            {accounts.data?.map((a) => <option key={a.id} value={a.id}>{a.name} — {a.bankName}</option>)}
          </Select>
        </Field>

        <Field label="Arquivo (.csv ou .ofx)">
          <input
            type="file"
            accept=".csv,.ofx"
            onChange={(e) => setFile(e.target.files?.[0] ?? null)}
            className="block w-full text-sm text-gray-600 file:mr-3 file:rounded-md file:border-0 file:bg-gray-100 file:px-3 file:py-2 file:text-sm"
          />
        </Field>

        {error && <p className="text-sm text-red-600">{error}</p>}

        <Button disabled={!bankAccountId || !file || upload.isPending} onClick={() => { setError(null); upload.mutate() }}>
          {upload.isPending ? 'Importando…' : 'Importar'}
        </Button>

        <p className="text-xs text-gray-400">
          CSV esperado com cabeçalho contendo ao menos as colunas <code>data</code> e <code>valor</code>.
          Veja exemplos em <code>database/samples/</code>.
        </p>
      </Card>

      {bankAccountId && (
        <Card>
          <h2 className="mb-3 text-sm font-semibold text-gray-700 dark:text-gray-200">Histórico de importações</h2>
          {history.isLoading ? <TableSkeleton rows={3} cols={6} /> : (
          <Table headers={['Arquivo', 'Formato', 'Status', 'Linhas', 'Importadas', 'Data']}>
            {history.data?.map((h) => (
              <Tr key={h.id}>
                <td className="px-4 py-2">{h.fileName}</td>
                <td className="px-4 py-2">{h.fileFormat}</td>
                <td className="px-4 py-2">
                  <Badge dot color={h.status === 'COMPLETED' ? 'green' : h.status === 'FAILED' ? 'red' : 'yellow'}>
                    {h.status}
                  </Badge>
                  {h.errorMessage && <span className="ml-2 text-xs text-red-500">{h.errorMessage}</span>}
                </td>
                <td className="px-4 py-2">{h.totalRows}</td>
                <td className="px-4 py-2">{h.importedRows}</td>
                <td className="px-4 py-2">{formatDate(h.createdAt)}</td>
              </Tr>
            ))}
            {history.data?.length === 0 && (
              <tr><td colSpan={6} className="px-4 py-6 text-center text-sm text-gray-400">Nenhuma importação ainda.</td></tr>
            )}
          </Table>
          )}
        </Card>
      )}
    </div>
  )
}
