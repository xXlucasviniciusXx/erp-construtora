# Backlog de evolução (pós-POC)

Itens deixados como `TODO` propositalmente, para outro desenvolvedor continuar.

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

## Plataforma
- [ ] Auditoria automática (preencher `audit_logs` via AOP/listener).
- [ ] Refresh token + revogação.
- [ ] Upload de logo/comprovantes (storage) em vez de URL.
- [ ] Testes: cobertura de serviços (reconciliação, geração de parcelas, parsers).
- [ ] Geração de tipos do frontend a partir do OpenAPI.
- [ ] Paginação e filtros server-side em todas as listagens.
- [ ] Dark mode completo nas telas (infra de tema já existe).
