// Consulta automática de CEP e CNPJ via BrasilAPI (pública, sem chave).
// Em caso de falha, retorna null para permitir preenchimento manual.

export interface CepResult {
  street?: string
  neighborhood?: string
  city: string
  state: string
}

export interface CnpjResult {
  razao_social?: string
  nome_fantasia?: string
  cep?: string
  logradouro?: string
  numero?: string
  bairro?: string
  municipio?: string
  uf?: string
  descricao_situacao_cadastral?: string
}

const onlyDigits = (v: string) => (v ?? '').replace(/\D/g, '')

export async function lookupCep(cep: string): Promise<CepResult | null> {
  const d = onlyDigits(cep)
  if (d.length !== 8) return null
  try {
    const r = await fetch(`https://brasilapi.com.br/api/cep/v1/${d}`)
    if (!r.ok) return null
    return (await r.json()) as CepResult
  } catch {
    return null
  }
}

export async function lookupCnpj(cnpj: string): Promise<CnpjResult | null> {
  const d = onlyDigits(cnpj)
  if (d.length !== 14) return null
  try {
    const r = await fetch(`https://brasilapi.com.br/api/cnpj/v1/${d}`)
    if (!r.ok) return null
    return (await r.json()) as CnpjResult
  } catch {
    return null
  }
}
