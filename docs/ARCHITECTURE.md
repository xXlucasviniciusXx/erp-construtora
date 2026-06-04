# Arquitetura

## Visão geral

```
┌─────────────┐     HTTPS/JSON      ┌──────────────────┐     JDBC      ┌────────────┐
│  Frontend   │ ──────────────────► │     Backend      │ ────────────► │ PostgreSQL │
│ React/Vite  │ ◄────────────────── │  Spring Boot API │ ◄──────────── │  (Supabase)│
└─────────────┘    JWT (Bearer)     └──────────────────┘   Flyway V14  └────────────┘
```

O frontend é um SPA stateless que guarda o JWT no `localStorage` e o envia em
`Authorization: Bearer`. O backend não mantém sessão (stateless), validando o
token a cada requisição. Toda regra de negócio vive no backend; o frontend é
substituível.

---

## Camadas do backend

```
Controller    → expõe REST, valida entrada (@Valid), aplica @PreAuthorize
Service       → regra de negócio e transações (@Transactional)
Repository    → acesso a dados (Spring Data JPA + JdbcTemplate para analytics)
Mapper        → Entity <-> DTO
DTO           → contratos de entrada/saída (records imutáveis)
Entity        → modelo persistente (@Entity)
Security      → JwtService, filtro, UserDetails, SecurityConfig
Interceptor   → LicenseEnforcementInterceptor (módulos + vencimento)
Exception     → GlobalExceptionHandler + exceções de domínio (BusinessException)
Parser        → leitura de extratos (estratégia por formato — CSV/OFX)
```

---

## Segurança e RBAC

- Autenticação: `AuthenticationManager` + `BCryptPasswordEncoder`.
- Autorização: cada `Role` carrega um conjunto de `Permission` com o padrão
  `<MODULO>_VIEW` / `<MODULO>_EDIT`. O `AppUserDetails` publica `ROLE_<NOME>` +
  cada permissão como *authority*. Os controllers usam
  `@PreAuthorize("hasAuthority('...')")`.
- ADMIN recebe todas as permissões (seed V2 + V14). O papel ADMIN é **protegido**:
  não pode ser editado nem deletado via API.
- Perfis de acesso são **gerenciáveis em tempo real** via `GET/POST/PUT/DELETE /api/roles`.
  O catálogo completo de permissões está em `GET /api/roles/permissions`.

### Permissões por módulo (V14)

| Permissão | Módulo | Ação |
|-----------|--------|------|
| `DASHBOARD_VIEW` | Dashboard | Ver |
| `CLIENTES_VIEW` / `CLIENTES_EDIT` | Clientes | Ver / Editar |
| `EMPREENDIMENTOS_VIEW` / `EMPREENDIMENTOS_EDIT` | Empreendimentos | Ver / Editar |
| `VENDAS_VIEW` / `VENDAS_EDIT` | Vendas | Ver / Editar |
| `CONTAS_PAGAR_VIEW` / `CONTAS_PAGAR_EDIT` | Contas a Pagar | Ver / Editar |
| `CONTAS_RECEBER_VIEW` / `CONTAS_RECEBER_EDIT` | Contas a Receber | Ver / Editar |
| `FORNECEDORES_VIEW` / `FORNECEDORES_EDIT` | Fornecedores | Ver / Editar |
| `CONCILIACAO_VIEW` / `CONCILIACAO_EDIT` | Conciliação | Ver / Editar |
| `DRE_VIEW` | DRE | Ver |
| `RELATORIOS_VIEW` | Relatórios | Ver |
| `NOTIFICACOES_VIEW` | Notificações | Ver |
| `USERS_MANAGE` | Sistema | Gerenciar usuários |
| `SETTINGS_MANAGE` | Sistema | Gerenciar configurações |

### Perfis padrão (seed V14)

| Perfil | Permissões principais |
|--------|-----------------------|
| ADMIN | Todas (sistema protegido) |
| FINANCEIRO | `CONTAS_PAGAR_*`, `CONTAS_RECEBER_*`, `CONCILIACAO_*`, `RELATORIOS_VIEW`, + VIEW de todos os módulos |
| CONTABILIDADE | `CONCILIACAO_*`, `DRE_VIEW`, `RELATORIOS_VIEW`, + VIEW de módulos financeiros |
| COMERCIAL | `CLIENTES_*`, `EMPREENDIMENTOS_*`, `VENDAS_*`, + VIEW financeiro |
| VISUALIZADOR | Apenas `*_VIEW` de todos os módulos |

> Permissões antigas (`READ`, `CLIENTS_WRITE`, `PROPERTIES_WRITE`, `SALES_WRITE`,
> `CONTRACTS_GENERATE`, `PAYABLE_WRITE`, `RECEIVABLE_WRITE`, `RECONCILIATION_WRITE`,
> `RECONCILIATION_VALIDATE`, `REPORTS_EXPORT`) foram removidas na V14.

---

## Licenciamento por módulos

### Modelo de implantação: VM por cliente (silo)

Cada cliente roda em **sua própria VM + seu próprio banco** (PostgreSQL separado).
Isolamento é **físico** — não há `tenant_id`. O sistema single-tenant atual já
serve esse modelo sem reescrita de queries.

### Os três eixos (independentes)

| Eixo | Responde | Onde mora |
|------|----------|-----------|
| **Plano** (tier) | quais módulos a empresa contratou (teto) | tabela `license` |
| **Papel + permissão** | o que cada usuário vê/edita dentro do teto | `roles` / `permissions` |
| **VM** | isolamento entre empresas | banco separado por VM |

### Tabelas (V13 + V14)

- **`modules`** — catálogo de módulos (`code` UNIQUE, `name`, `active`, `sort_order`).
  13 módulos seeded (11 ativos + `PORTAL_CLIENTE` e `APP_MOBILE` como "em breve").
- **`license`** — linha única por VM: `plan`, `status`, `start_date`, `end_date`,
  `period_months`, `max_users`, `customer`, `license_key TEXT`, `grace_days` (padrão 7).

### Distribuição de módulos por plano (cumulativo)

| Plano | Módulos |
|-------|---------|
| **ESSENCIAL** | DASHBOARD, CLIENTES, FORNECEDORES, CONTAS_PAGAR, CONTAS_RECEBER, CONCILIACAO, DRE, RELATORIOS, NOTIFICACOES |
| **PROFISSIONAL** | = Essencial + EMPREENDIMENTOS, VENDAS |
| **PREMIUM** | = Profissional + PORTAL_CLIENTE, APP_MOBILE *(ainda a construir — Fase 4)* |

### Chave de licenciamento (HMAC-SHA256 offline)

`LicenseKeyService` assina/verifica tokens offline sem servidor central:

```
formato: base64url(payload_json).base64url(hmac_sha256(payload, secret))
```

- `generate(LicenseClaims)` → String token.
- `parse(String)` → `LicenseClaims` ou `BusinessException` (assinatura inválida/expirada).
- Comparação com `MessageDigest.isEqual` (constant-time, evita timing attack).
- Segredo configurado em `app.license.secret` (variável `LICENSE_SECRET` na VM).

Endpoints: `POST /api/licensing/license/key` (aplicar) e
`POST /api/licensing/license/key/generate` (gerar nova chave).

### LicenseEnforcementInterceptor

`HandlerInterceptor` registrado em `WebConfig` para `/api/**`. Avalia cada request:

1. **Gate global** (independente do módulo):
   - `CANCELADA` → 403 bloqueio total.
   - `SUSPENSA` ou vencida além do `grace_days` → somente GET passa (modo leitura).
   - Dentro da tolerância → apenas aviso no `LicenseResponse`.

2. **Gate de módulo**: mapeia o path (`/api/clients` → `CLIENTES`, etc.) e verifica
   se o módulo está ativo na tabela `modules`. Módulo inativo → 403.

3. **Paths isentos** (sempre liberados): `/api/auth`, `/api/licensing`,
   `/api/settings`, `/api/users`, `/api/roles`.

### LicensingContext (frontend)

`LicensingContext.tsx` busca `GET /api/licensing/me` após o login e expõe:

- `canAccess(moduleCode?)` — `true` se o módulo está ativo (ou se o código for
  desconhecido ou o contexto ainda estiver carregando).
- `activeByCode: Map<string, boolean>` construído do array `modules`.

`ModuleGuard` envolve cada rota e exibe tela de "Módulo não contratado" (cadeado)
quando `canAccess()` retorna `false`.

### Upsell (plano Essencial)

No Dashboard, consultas de módulos não contratados são desabilitadas via
`enabled: canAccess('VENDAS')` e o espaço do gráfico é substituído por
`<UpsellCard title="..." plan="Profissional" />` — sem erros 403 exibidos ao usuário.

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

## DRE — Demonstração do Resultado (base caixa)

`DreService` monta o demonstrativo em **base caixa** (SQL nativo via
`NamedParameterJdbcTemplate`), opcionalmente por período e empreendimento:

- **Receitas** (linhas fixas): *Receita de Vendas* = parcelas com `status=PAID`
  e `payment_date` no período (join lote→quadra→empreendimento para o filtro);
  *Outras Receitas* = `accounts_receivable` com `status=RECEIVED` no período.
- **Despesas**: `accounts_payable` pagas no período, agrupadas pelo **grupo da
  categoria**.
- **Resultado** = total de receitas − total de despesas.

O resultado por empreendimento é coerente com o `profitByDevelopment` do
dashboard (mesma base caixa). Exposto em JSON (`/dre`) e CSV (`/dre/export`),
e renderizado na tela **DRE** com filtros e margem sobre a receita.

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

## Notificações por e-mail

`NotificationService` centraliza os e-mails transacionais (HTML, com a
identidade do sistema). A configuração de **SMTP é lida do banco**
(`system_settings`), editável na tela **Configurações → Notificações / E-mail** —
não depende mais de variáveis de ambiente. O `JavaMailSender` é construído sob
demanda a partir dessas configurações.

- **Modo simulado**: se `mailEnabled=false` ou sem host, a notificação é
  registrada como `PENDING` e logada (sem envio real).
- **Eventos**: atraso, **lembrete N dias antes do vencimento** (job diário
  `OverdueScheduler.sendDueSoonReminders`), pagamento confirmado, venda e contrato.
- **Histórico**: tudo é persistido em `email_notifications` (status SENT/PENDING/
  FAILED + erro) e exposto na tela **Notificações**, com reenvio e visualização.
- **Segurança**: `/settings/public` expõe só branding; a config de SMTP fica no
  `/settings` (SETTINGS_MANAGE) e a **senha nunca é retornada** (write-only).

## Consulta CEP/CNPJ

O cadastro de clientes consulta a **BrasilAPI** diretamente do frontend
(`lib/brasilapi.ts`) ao sair dos campos CEP/CNPJ, preenchendo endereço e razão
social. Em qualquer falha, retorna `null` e o preenchimento manual segue normalmente.

---

## Frontend — estrutura relevante

```
src/
├── licensing/
│   └── LicensingContext.tsx   # canAccess(code?), activeByCode, busca /licensing/me
├── components/
│   ├── ModuleGuard.tsx         # Envolve rotas; tela de cadeado se módulo inativo
│   ├── ui/                     # Button, Card, Field, Input, Select, PageHeader…
│   ├── Combobox.tsx            # Combobox pesquisável estilo CMDK (sem dependências)
│   └── Layout.tsx              # Sidebar retrátil, dark mode, nav filtrada por
│                               #   permissão E módulo; banner de vencimento
├── pages/
│   ├── DashboardPage.tsx       # 8 cards + 10 gráficos (Recharts) + filtros;
│   │                           #   consultas desabilitadas por canAccess(); UpsellCard
│   ├── DevelopmentsPage.tsx    # Cadastro em cascata: lista empreendimentos
│   │                           #   → DevelopmentManager (quadras + lotes)
│   ├── SalesPage.tsx           # Combobox CMDK para Cliente/Lote, edição de venda
│   ├── ClientsPage.tsx         # Menu ⋮, inativação, CEP/CNPJ automático
│   ├── PayablePage.tsx         # Filtros, ícones confirmar/cancelar
│   ├── ReceivablePage.tsx      # Filtros, abas Contas/Parcelas
│   ├── SuppliersPage.tsx       # CRUD fornecedores
│   ├── ReconciliationPage.tsx  # Sugestões, conciliação manual, histórico
│   ├── UsersPage.tsx           # CRUD usuários; roles carregadas dinamicamente
│   └── settings/
│       ├── ModulesTab.tsx      # Planos preset, toggle de módulos, chave de licença
│       └── AccessProfilesTab.tsx # CRUD perfis de acesso; grade VER/EDITAR por módulo
└── lib/
    ├── api.ts                  # Instância Axios com interceptor de token
    ├── types.ts                # Tipos espelhando os DTOs (inclui License, Module, Role, Permission)
    ├── utils.ts                # formatCurrency, cn (classnames), …
    └── brasilapi.ts            # lookupCep / lookupCnpj
```

### Combobox pesquisável (CMDK-style)

`components/Combobox.tsx` — leve, sem dependências externas. Filtra em tempo real
(até 50 resultados), dark-mode aware, suporta hint por opção. Usado nos selects de
Cliente e Lote na tela de Vendas.

### Dark mode

Configurado em `tailwind.config.js` com `darkMode: 'class'`. O `Layout` alterna
a classe `dark` no `<html>`. Todas as telas usam variantes `dark:` do Tailwind.

### Proteção de rota por módulo

`App.tsx` envolve cada rota com `<ModuleGuard code="NOME_MODULO">`. O guard lê
`canAccess()` do `LicensingContext` e, se o módulo estiver inativo, exibe uma tela
de cadeado em vez de redirecionar para 404 ou 403. O `LicensingProvider` fica no
`main.tsx` dentro do `AuthProvider`.
