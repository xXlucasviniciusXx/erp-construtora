# Arquitetura

## Visão geral

```
┌─────────────┐     HTTPS/JSON      ┌──────────────────┐     JDBC      ┌────────────┐
│  Frontend   │ ──────────────────► │     Backend      │ ────────────► │ PostgreSQL │
│ React/Vite  │ ◄────────────────── │  Spring Boot API │ ◄──────────── │  (Supabase)│
└─────────────┘    JWT (Bearer)     └──────────────────┘   Flyway V8   └────────────┘
```

O frontend é um SPA stateless que guarda o JWT no `localStorage` e o envia em
`Authorization: Bearer`. O backend não mantém sessão (stateless), validando o
token a cada requisição. Toda regra de negócio vive no backend; o frontend é
substituível.

---

## Camadas do backend

```
Controller  → expõe REST, valida entrada (@Valid), aplica @PreAuthorize
Service     → regra de negócio e transações (@Transactional)
Repository  → acesso a dados (Spring Data JPA + JdbcTemplate para analytics)
Mapper      → Entity <-> DTO
DTO         → contratos de entrada/saída (records imutáveis)
Entity      → modelo persistente (@Entity)
Security    → JwtService, filtro, UserDetails, SecurityConfig
Exception   → GlobalExceptionHandler + exceções de domínio (BusinessException)
Parser      → leitura de extratos (estratégia por formato — CSV/OFX)
```

---

## Segurança e RBAC

- Autenticação: `AuthenticationManager` + `BCryptPasswordEncoder`.
- Autorização: cada `Role` carrega um conjunto de `Permission` (ex.:
  `RECONCILIATION_WRITE`). O `AppUserDetails` publica `ROLE_<NOME>` + cada
  permissão como *authority*. Os controllers usam
  `@PreAuthorize("hasAuthority('...')")`.
- ADMIN recebe todas as permissões (seed V2).

| Perfil        | Permissões principais |
|---------------|-----------------------|
| ADMIN         | tudo + `USERS_MANAGE`, `SETTINGS_MANAGE` |
| FINANCEIRO    | `PAYABLE_WRITE`, `RECEIVABLE_WRITE`, `RECONCILIATION_WRITE`, `REPORTS_EXPORT` |
| CONTABILIDADE | `RECONCILIATION_VALIDATE`, `REPORTS_EXPORT`, `READ` |
| COMERCIAL     | `CLIENTS_WRITE`, `PROPERTIES_WRITE`, `SALES_WRITE`, `CONTRACTS_GENERATE` |
| VISUALIZADOR  | `READ` |

---

## Hierarquia de imóveis (Empreendimento → Quadra → Lote)

A migration **V8** substituiu a tabela plana `properties` por 3 entidades:

```
developments   (empreendimento)
  └── blocks   (quadra, FK development_id)
        └── lots (lote, FK block_id)   ← antiga tabela properties
```

### Códigos internos automáticos

- `DevelopmentService.nextCode()` → `E` + lpad(count+1, 3) → `E001`, `E002`…
- `BlockService.create()` → `dev.internalCode + "-Q" + lpad(blockCount+1, 2)` → `E001-Q01`
- `LotService.create()` → `block.internalCode + "-L" + lpad(lotInBlock+1, 3)` → `E001-Q01-L001`

### Valores derivados (calculados no banco, não editáveis)

- `plannedTotal` = soma de `lots.planned_value` do empreendimento
- `receivedTotal` = soma de `lots.sale_value` dos lotes com `status = SOLD`
- `actualBlocks` e `actualLots` = contagens reais

Esses valores são calculados via `@Query` (JPQL) nos repositórios e retornados
no `DevelopmentResponse` — o cliente não os envia, apenas lê.

### Limites em cascata

- `blocks_count` define o máximo de quadras; tentar criar além lança `BusinessException`.
- `lots_count` define o total de lotes de todo o empreendimento; verificado ao criar
  lote em qualquer quadra.

### Integração com vendas

- `SaleService.create()` grava `lot.saleValue = totalValue` e `lot.status = SOLD`.
- `SaleService.delete()` reverte `lot.status = AVAILABLE` e `lot.saleValue = null`.
- `SaleService.update()` regenera parcelas **somente** se valor/quantidade mudarem
  **e** nenhuma parcela estiver paga (caso contrário lança `BusinessException`).

---

## Conciliação bancária (módulo principal)

1. **Importação** (`StatementImportService`) recebe o arquivo, detecta o formato
   pela extensão e delega a um `BankStatementParser` (CSV/OFX). Cada transação
   vira um `BankTransaction` com status `PENDING`. Transações com identificador
   bancário já existente são ignoradas (idempotência).

2. **Sugestão** (`SuggestionEngine`) compara a transação a contas a receber,
   parcelas (créditos) ou contas a pagar (débitos) e calcula um **score 0–100**:
   valor exato (+60) + proximidade de data (até +25) + documento CPF/CNPJ (+15).

3. **Conciliação** (`ReconciliationService`) registra um `Reconciliation`,
   marca a transação como `RECONCILED` e dá baixa no lançamento-alvo
   (`PAID`/`RECEIVED`). Suporta **desfazer**, revertendo ambos os lados.

4. **Pendências e histórico** ficam disponíveis para relatório/auditoria.

### Estendendo formatos de extrato

Implemente `BankStatementParser`, declare o `FileFormat` suportado e anote com
`@Component`. A `StatementParserFactory` registra automaticamente. Nenhuma outra
camada precisa mudar.

---

## Geração automática de parcelas

`SaleService.create()` divide `(totalValue − downPayment)` pela quantidade de
parcelas, arredondando para baixo e somando o resíduo à última (fecha o total
exato). Vencimentos mensais a partir do `firstDueDate` informado.

- **"Entrada + parcelas":** `downPayment` entra como parcela 0 (ou separada); as
  demais são numeradas 1…N.
- **"À vista":** `downPayment` é zerado pelo service mesmo que enviado pelo cliente.

---

## Conciliação manual

Além das sugestões automáticas, o endpoint
`/reconciliation/transactions/{id}/targets` lista **todos** os lançamentos em
aberto compatíveis com o tipo da transação (crédito → contas a receber/parcelas;
débito → contas a pagar). A conciliação aceita alvo de **valor diferente**,
registrando `matched_amount = valor do extrato`.

Status manuais (`IGNORED`, `DIVERGENT`, voltar para `PENDING`) gravam um motivo
opcional em `bank_transactions.notes`.

---

## Dashboard analítico

As agregações dos gráficos (`/dashboard/analytics`) usam **SQL nativo (PostgreSQL)**
via `NamedParameterJdbcTemplate` em `DashboardAnalyticsService`. Suporta filtros
opcionais de período (`from`/`to`), cliente (`clientId`) e lote (`propertyId`).

Séries disponíveis:

| Série | SQL |
|-------|-----|
| `receivedByMonth` | Parcelas pagas agrupadas por `to_char(payment_date,'Mon/YY')` |
| `toReceiveByMonth` | Parcelas abertas por `due_date` |
| `overdueByMonth` | Parcelas vencidas por mês de vencimento |
| `salesByMonth` | Valor total de vendas por `sale_date` |
| `salesByPurchaseType` | Soma por `purchase_type` |
| `delinquencyByDevelopment` | Inadimplência (parcelas OVERDUE) agrupada por empreendimento (via lots→blocks→developments) |
| `overdueByAging` | Faixas: 1-30, 31-60, 61-90, 90+ dias |
| `cashFlowForecast` | A receber − a pagar por mês |
| `payablesPaidVsOpen` | Contas a pagar: pagas × em aberto |
| `receivablesReceivedVsOpen` | Contas a receber: recebidas × em aberto |

A complexidade analítica fica isolada nessa camada; entidades e repositórios JPA
permanecem simples.

---

## Plano de contas: 3 dimensões de Contas a Pagar

A migration **V11** estrutura o lançamento de despesas em três dimensões, todas
**FK** (antes `category`/`cost_center` eram texto livre):

| Dimensão | Pergunta | Entidade | Exemplos |
|----------|----------|----------|----------|
| **Categoria** | *o quê* (natureza) | `categories` (2 níveis: `grupo` → `name`) | Terraplanagem, Salários, Tarifas Bancárias |
| **Centro de Custo** | *onde/quem* (área) | `cost_centers` (+ coluna `grupo`) | Obras, Comercial, Administração Geral |
| **Empreendimento** | *qual obra* (opcional) | `developments` (FK em V10) | Parque das Águas |

A V11 **cria** `categories` (seed com ~70 itens), adiciona `grupo` a
`cost_centers`, e converte `accounts_payable.category`/`cost_center` em
`category_id`/`cost_center_id`. Os registros antigos (texto livre) são
preservados via **backfill**: valores não mapeados viram categorias do grupo
"Outros" e os centros por empreendimento ("Obra X") são desativados (a dimensão
empreendimento já é FK própria). `supplier` permanece como texto (autocomplete);
migrá-lo para FK fica no backlog.

Esse modelo alimenta filtros, relatórios CSV
(`/reports/expenses-by-category`, `/expenses-by-cost-center`,
`/expenses-by-development`) e os indicadores do dashboard
(`expensesByCategory`, `expensesByCostCenter`, `expensesByDevelopment`,
`profitByDevelopment`).

## Fornecedores

Entidade `suppliers` (V6): nome, CNPJ/CPF, e-mail, telefone, endereço, categoria, ativo.

## Contas a Pagar × Empreendimento

A migration **V10** adiciona `accounts_payable.development_id` (FK **nullable**
para `developments`). O vínculo é **opcional**:
- Despesa administrativa geral → `development_id` nulo (tratada como
  "Geral / Administrativo").
- Despesa de obra/loteamento → vinculada ao empreendimento.

Registros antigos permanecem com `development_id` nulo (compatível). Esse vínculo
alimenta o filtro por empreendimento na listagem, o relatório
`/reports/expenses-by-development` e dois indicadores no dashboard:
- **`expensesByDevelopment`**: despesas pagas agrupadas por empreendimento.
- **`profitByDevelopment`**: lucro/prejuízo **em caixa** = parcelas recebidas
  (PAID) − despesas pagas (PAID), por empreendimento. Reflete o caixa real:
  uma obra pode ficar negativa enquanto os custos saem antes dos recebimentos.

---

## Geração de contrato PDF

`ContractTemplateService` monta um documento HTML interpolando os dados da
venda/cliente/lote. **Flying Saucer** (`flying-saucer-pdf-openpdf:9.1.22`) converte
o HTML para PDF tratando o conteúdo como XML bem-formado.

Atenção: entidades HTML inválidas em XML (como `&nbsp;`) devem ser substituídas
por referências numéricas (`&#160;`). Valores interpolados são escapados via
método `esc()` para evitar quebra de XML.

---

## Consulta CEP/CNPJ

O cadastro de clientes consulta a **BrasilAPI** diretamente do frontend
(`lib/brasilapi.ts`) ao sair dos campos CEP/CNPJ, preenchendo endereço e razão
social. Em qualquer falha, retorna `null` e o preenchimento manual segue normalmente.

---

## Frontend — estrutura relevante

```
src/
├── pages/
│   ├── DashboardPage.tsx      # 8 cards + 10 gráficos (Recharts) + filtros
│   ├── DevelopmentsPage.tsx   # Cadastro em cascata: lista empreendimentos
│   │                          #   → DevelopmentManager (quadras + lotes)
│   ├── SalesPage.tsx          # Combobox CMDK para Cliente/Lote, edição de venda
│   ├── ClientsPage.tsx        # Menu ⋮, inativação, CEP/CNPJ automático
│   ├── PayablePage.tsx        # Filtros, ícones confirmar/cancelar
│   ├── ReceivablePage.tsx     # Filtros, abas Contas/Parcelas
│   ├── SuppliersPage.tsx      # CRUD fornecedores
│   ├── ReconciliationPage.tsx # Sugestões, conciliação manual, histórico
│   └── ...
├── components/
│   ├── ui/                    # Button, Card, Field, Input, Select, PageHeader…
│   ├── Combobox.tsx           # Combobox pesquisável estilo CMDK (sem dependências)
│   └── Layout.tsx             # Sidebar retrátil, dark mode, nav protegida por permissão
└── lib/
    ├── api.ts                 # Instância Axios com interceptor de token
    ├── types.ts               # Tipos espelhando os DTOs do backend
    ├── utils.ts               # formatCurrency, cn (classnames), …
    └── brasilapi.ts           # lookupCep / lookupCnpj
```

### Combobox pesquisável (CMDK-style)

`components/Combobox.tsx` — leve, sem dependências externas. Filtra em tempo real
(até 50 resultados), dark-mode aware, suporta hint por opção. Usado nos selects de
Cliente e Lote na tela de Vendas.

### Dark mode

Configurado em `tailwind.config.js` com `darkMode: 'class'`. O `Layout` alterna
a classe `dark` no `<html>`. Todas as telas usam variantes `dark:` do Tailwind.
