# Backlog de evolução (pós-POC)

Itens deixados como `TODO` propositalmente para continuação futura.

---

## ✅ Concluído (histórico)

### Funcionalidades base
- Conciliação manual (qualquer lançamento, valor diferente), abas por status,
  reverter status, motivo de divergência, desfazer e histórico.
- Clientes: menu de ações ⋮ (visualizar/editar/inativar), consulta automática
  de CEP e CNPJ (BrasilAPI) com fallback manual, bloqueio de inativação com débitos.
- Parcelas: dados do cliente (nome, CPF/CNPJ, telefone) + filtros (q, status,
  período de vencimento); menu de ações com pagamento e geração de contrato.
- Contas a pagar: ícones confirmar/cancelar (✅/❌) com tooltip e confirmação;
  associação a fornecedor e centro de custo.
- Dashboard analítico: 8 cards + 10 gráficos Recharts; filtros de período/cliente/lote;
  totalizador de recebíveis.
- Vendas: forma de compra (`purchaseType`) e índice de correção como listas;
  entrada condicional (só "Entrada + parcelas"); edição de venda (PUT /sales/{id}).
- Fornecedores: CRUD completo (migration V6).
- Centros de custo: CRUD completo (migration V6).
- Auditoria (`audit_logs`) gravando ações financeiras.
- Dark mode completo em todas as telas.
- Menu lateral retrátil (collapsed/expanded) + mobile drawer.

### Encargos, dashboard e Contas a Pagar por empreendimento
- Juros/multa por atraso calculados em tempo real (taxas por venda), exibidos
  em parcelas/vendas e usados na conciliação manual (migration V9).
- Drill-down no dashboard (inadimplência por empreendimento, vendas por forma de compra).
- Mapa visual de lotes (grid colorido por status) dentro do empreendimento.
- Contas a Pagar vinculadas a empreendimento (FK opcional, V10): filtro,
  relatório CSV e indicadores de despesas e lucro/prejuízo (caixa) por empreendimento.

### Hierarquia de imóveis (Empreendimento → Quadra → Lote) — migration V8
- Modelo em 3 níveis substituindo a tabela plana `properties`.
- Códigos internos automáticos e hierárquicos (`E001-Q01-L001`).
- Valores derivados calculados (plannedTotal, receivedTotal, actualBlocks, actualLots).
- Limites em cascata (blocksCount, lotsCount) com bloqueio na criação.
- Integração com vendas: `lot.saleValue` e `lot.status` atualizados automaticamente.
- Tela de cadastro em cascata no frontend (DevelopmentsPage).
- Combobox CMDK pesquisável para Cliente e Lote na tela de Vendas.

---

## Conciliação

- [ ] Conciliação parcial / 1 transação ↔ N lançamentos.
- [ ] Auto-conciliar quando score ultrapassa limiar configurável.
- [ ] Tolerância de valor configurável (centavos) além de match exato.
- [ ] Match por similaridade textual da descrição do extrato.
- [ ] Suporte a múltiplas contas no mesmo arquivo OFX (`BANKACCTFROM`).
- [ ] CNAB 240/400 como novos parsers.

## Importação

- [ ] Validação de charset/encoding do arquivo antes do parse.
- [ ] Processamento assíncrono para arquivos grandes (fila + status).
- [ ] Armazenar o arquivo original (Supabase Storage/S3) além dos metadados.

## Contratos

- [ ] Modelos personalizáveis por empreendimento (`contract_templates`).
- [ ] Editor de template no frontend (ADMIN/COMERCIAL).
- [ ] Versionamento de templates e assinatura eletrônica.

## Notificações

- [ ] Templates HTML (Thymeleaf) para os e-mails.
- [ ] Fila de envio com retry e dead-letter.
- [ ] Preferências de notificação por cliente.

## Financeiro

- [x] Aplicar juros/multa automaticamente no atraso (taxas por venda, cálculo em tempo real).
- [ ] Correção monetária por índice (INCC/IGPM) nas parcelas — hoje é apenas informativo.
- [ ] Relatório de DRE simplificado (receitas − despesas por centro de custo).
- [x] Plano de contas estruturado: entidade **Categoria** (grupo → item) e
      **Centro de Custo** com grupo, ambos FK em Contas a Pagar (V11) — com
      seeds editáveis pelo admin, filtros, relatórios CSV e indicadores no dashboard.
- [ ] Migrar `supplier` de texto livre para FK (Supplier).
- [x] **DRE (Demonstração do Resultado)** em base caixa: tela própria com
      receitas − despesas = resultado, filtros de período/empreendimento, margem
      e export CSV (`/dre`, `/dre/export`).
- [ ] DRE em **competência** (faturado − incorrido) como visão alternativa.
- [ ] Categorias de **receita** (FK em Contas a Receber) para detalhar "Outras Receitas".
- [ ] Separar **Receitas Financeiras** (juros/multas recebidos) — requer gravar
      principal/juros/multa na baixa da parcela.
- [ ] Mapa no Dashboard (cards empreendimento → quadra → lotes) — em avaliação pelo cliente.

## Cadastros de referência

- [ ] Tornar formas de pagamento e índices de correção configuráveis via tabela
      em vez de listas fixas no frontend.
- [ ] Categorias de fornecedor configuráveis.

## Dashboard

- [ ] Drill-down: clicar no gráfico → lista detalhada filtrada.
- [ ] Cache leve das agregações pesadas (Redis ou Caffeine) para bases grandes.
- [ ] Exportar gráfico como imagem (PNG).

## Imóveis / Lotes

- [ ] Upload de planta/foto do lote (Supabase Storage).
- [ ] Mapa interativo dos lotes por quadra (grid visual com status por cor).
- [ ] Reserva temporária de lote com expiração automática.

## Plataforma

- [ ] Refresh token + revogação (blacklist de JTI).
- [ ] Auditoria automática via AOP em vez de chamadas manuais.
- [ ] Upload de logo/comprovantes via storage em vez de URL externa.
- [ ] CEP/CNPJ via proxy no backend (cache + resiliência) em vez de chamada direta.
- [x] Paginação e filtros **server-side** nas listagens de maior volume:
      Clientes, Contas a Pagar, Contas a Receber e Parcelas (componente
      `Pagination` reutilizável; filtros q/status/empreendimento no backend).
- [ ] Estender paginação server-side a Vendas, Fornecedores e Transações bancárias.
- [ ] Combobox server-side (busca remota) nos selects de Cliente/Lote em Vendas
      (hoje carregam até 500 itens de uma vez).
- [ ] Code-splitting do bundle (Recharts e React Router lazy loading).

## Qualidade / DevOps

- [x] Testes unitários do núcleo financeiro: `LateFeeCalculator` (juros/multa) e
      geração de parcelas/entrada (`SaleService`) — 14 testes.
- [x] **CI (GitHub Actions)**: roda `mvn test` (backend) e `npm run build`
      (frontend) a cada push/PR em `main`/`develop`.
- [x] Smoke test end-to-end via API (`scripts/smoke.ps1`) — 13 verificações
      cobrindo cadastros, FKs, encargos, parcelas, dashboard, DRE, conciliação e CSV.
- [ ] Testes de integração com Testcontainers (Postgres) p/ migrations, dashboard
      nativo, reconciliação e cascade hierarchy.
- [ ] Geração de tipos TypeScript do frontend a partir do OpenAPI
      (`openapi-typescript` + `npm run gen-types`).
- [ ] Deploy automático no push a `main` (estender o CI).
- [ ] Health check dedicado em `/actuator/health` com detalhe de banco.
