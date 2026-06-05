import { api, getToken } from './api'

/**
 * Abre um PDF servido pela API (com autenticação) numa nova aba.
 * Fluxo único reutilizado pela tela de Vendas e pelo cadastro do cliente —
 * evita duplicar a regra de geração (que vive no backend).
 */
export async function openApiPdf(path: string): Promise<void> {
  const res = await fetch(`${api.defaults.baseURL}${path}`, {
    headers: { Authorization: `Bearer ${getToken()}` },
  })
  if (!res.ok) throw new Error('Falha ao gerar o documento.')
  const blob = await res.blob()
  window.open(URL.createObjectURL(blob))
}

/** Gera/visualiza o contrato (PDF) de uma venda. */
export function openContractPdf(saleId: string): Promise<void> {
  return openApiPdf(`/contracts/sales/${saleId}/pdf`)
}
