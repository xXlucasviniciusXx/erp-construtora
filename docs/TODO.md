# Backlog de evolução (pós-POC)

Itens deixados como `TODO` propositalmente, para outro desenvolvedor continuar.

## ✅ Concluído após o scaffold inicial
- Conciliação manual (qualquer lançamento, valor diferente), abas por status,
  reverter status, motivo de divergência.
- Clientes: menu de ações, inativação (soft delete + bloqueio por débitos),
  visualizar/lotes, consulta automática de CEP/CNPJ (BrasilAPI).
- Parcelas: dados do cliente + filtros; geração de contrato pelo menu.
- Contas a pagar: ícones confirmar/cancelar; auditoria em `audit_logs`.
- Dashboard analítico com 9 gráficos (Recharts) + cards.
- Vendas: forma de pagamento e índice de correção como listas.
- Migration V5 com dados de demonstração ampliados.

## Conciliação
- [ ] Conciliação parcial / 1 transação ↔ N lançamentos.
- [ ] Persistir `mode=AUTO` quando o score ultrapassar um limiar configurável.
- [ ] Tolerância de valor configurável (centavos) além de match exato.
- [ ] Match por intervalo de datas e por similaridade textual da descrição.
- [ ] Suporte a múltiplas contas no mesmo arquivo OFX (`BANKACCTFROM`).

## Importação
- [ ] Validação do cabeçalho/charset do OFX e CNAB (240/400) como novos parsers.
- [ ] Processamento assíncrono para arquivos grandes (fila + status).
- [ ] Armazenar o arquivo original (S3/Supabase Storage) além dos metadados.

## Contratos
- [ ] Modelos personalizáveis por empreendimento (tabela `contract_templates`).
- [ ] Editor de template no frontend (ADMIN/COMERCIAL).
- [ ] Versionamento e assinatura eletrônica.

## Notificações
- [ ] Templates HTML (Thymeleaf) e fila de envio com retry.
- [ ] Preferências de notificação por cliente.

## Financeiro
- [ ] Aplicar juros/multa automáticos no atraso (já há campos na venda).
- [ ] Correção monetária por índice (INCC/IGPM) nas parcelas.
- [ ] Centros de custo como entidade.

## Cadastros de referência
- [ ] Tornar **forma de pagamento** e **índice de correção** configuráveis
      (tabela de referência) em vez de listas fixas no frontend.

## Dashboard
- [ ] Filtro de período (intervalo de datas) nos gráficos.
- [ ] Drill-down (clicar no gráfico → lista detalhada).
- [ ] Cache leve das agregações (consultas pesadas em bases grandes).

## Plataforma
- [ ] Auditoria automática (preencher `audit_logs` via AOP/listener) — hoje é manual
      nos pontos financeiros principais.
- [ ] Refresh token + revogação.
- [ ] Upload de logo/comprovantes (storage) em vez de URL.
- [ ] CEP/CNPJ via proxy no backend (cache + resiliência) em vez de chamada direta.
- [ ] Testes: cobertura de serviços (reconciliação, geração de parcelas, parsers, dashboard).
- [ ] Geração de tipos do frontend a partir do OpenAPI.
- [ ] Paginação e filtros server-side em todas as listagens.
- [ ] Code-splitting do bundle (Recharts deixou o JS grande).
- [ ] Dark mode completo nas telas (infra de tema já existe).
