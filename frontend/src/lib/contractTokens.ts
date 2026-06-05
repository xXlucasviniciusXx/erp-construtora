/** Campos dinâmicos disponíveis nos modelos de contrato/distrato. */
export interface TokenDef {
  name: string
  label: string
  description: string
  group: 'Empresa' | 'Comprador' | 'Imóvel' | 'Pagamento' | 'Distrato'
}

export const CONTRACT_TOKENS: TokenDef[] = [
  // Empresa / contrato
  { name: 'empresa', label: 'Empresa (vendedora)', description: 'Razão social da construtora/vendedora', group: 'Empresa' },
  { name: 'numero_contrato', label: 'Nº do contrato', description: 'Número sequencial do contrato (ex.: CT-000123)', group: 'Empresa' },
  { name: 'data_hoje', label: 'Data de hoje', description: 'Data atual da geração do documento', group: 'Empresa' },
  // Comprador
  { name: 'cliente_nome', label: 'Nome do comprador', description: 'Nome completo do comprador', group: 'Comprador' },
  { name: 'cliente_documento', label: 'CPF/CNPJ', description: 'CPF ou CNPJ do comprador', group: 'Comprador' },
  { name: 'cliente_rg_ie', label: 'RG / Inscrição', description: 'RG ou Inscrição Estadual do comprador', group: 'Comprador' },
  { name: 'cliente_endereco', label: 'Endereço do comprador', description: 'Endereço completo do comprador', group: 'Comprador' },
  { name: 'cliente_estado_civil', label: 'Estado civil', description: 'Estado civil do comprador', group: 'Comprador' },
  { name: 'cliente_profissao', label: 'Profissão', description: 'Profissão do comprador', group: 'Comprador' },
  { name: 'cliente_email', label: 'E-mail', description: 'E-mail do comprador', group: 'Comprador' },
  { name: 'cliente_telefone', label: 'Telefone', description: 'Telefone do comprador', group: 'Comprador' },
  // Imóvel
  { name: 'empreendimento', label: 'Empreendimento', description: 'Nome do empreendimento', group: 'Imóvel' },
  { name: 'quadra', label: 'Quadra', description: 'Quadra do imóvel', group: 'Imóvel' },
  { name: 'lote', label: 'Lote', description: 'Lote do imóvel', group: 'Imóvel' },
  { name: 'unidade', label: 'Unidade', description: 'Unidade do imóvel (quando houver)', group: 'Imóvel' },
  { name: 'matricula', label: 'Matrícula', description: 'Matrícula do imóvel no cartório', group: 'Imóvel' },
  { name: 'imovel_endereco', label: 'Endereço do imóvel', description: 'Endereço completo do imóvel', group: 'Imóvel' },
  { name: 'area_total', label: 'Área total (m²)', description: 'Área total do imóvel em m²', group: 'Imóvel' },
  { name: 'area_construida', label: 'Área construída (m²)', description: 'Área construída em m²', group: 'Imóvel' },
  // Pagamento
  { name: 'valor_total', label: 'Valor total', description: 'Valor total da venda', group: 'Pagamento' },
  { name: 'entrada', label: 'Entrada', description: 'Valor da entrada', group: 'Pagamento' },
  { name: 'parcelas_qtd', label: 'Qtd. de parcelas', description: 'Quantidade de parcelas', group: 'Pagamento' },
  { name: 'primeiro_vencimento', label: '1º vencimento', description: 'Data do primeiro vencimento', group: 'Pagamento' },
  { name: 'forma_pagamento', label: 'Forma de pagamento', description: 'Forma de pagamento (boleto, PIX...)', group: 'Pagamento' },
  { name: 'indice_correcao', label: 'Índice de correção', description: 'Índice de correção monetária (INCC, IGP-M...)', group: 'Pagamento' },
  { name: 'parcelas_tabela', label: 'Tabela de parcelas', description: 'Tabela completa com nº, valor e vencimento de cada parcela', group: 'Pagamento' },
  { name: 'clausulas_extras', label: 'Cláusulas extras', description: 'Cláusulas adicionais cadastradas no lote', group: 'Pagamento' },
  // Distrato
  { name: 'distrato_data', label: 'Data do distrato', description: 'Data da rescisão (distrato)', group: 'Distrato' },
  { name: 'distrato_motivo', label: 'Motivo do distrato', description: 'Motivo informado na rescisão', group: 'Distrato' },
  { name: 'distrato_devolucao', label: 'Valor devolvido', description: 'Valor a devolver ao comprador', group: 'Distrato' },
  { name: 'distrato_retido', label: 'Valor retido', description: 'Valor retido pela vendedora', group: 'Distrato' },
]

const BY_NAME = new Map(CONTRACT_TOKENS.map((t) => [t.name, t]))

export function tokenLabel(name: string): string {
  return BY_NAME.get(name)?.label ?? name
}
export function tokenDescription(name: string): string {
  return BY_NAME.get(name)?.description ?? name
}
