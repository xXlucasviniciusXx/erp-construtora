# Arquitetura

## Visão geral

```
┌─────────────┐     HTTPS/JSON      ┌──────────────────┐     JDBC      ┌────────────┐
│  Frontend   │ ──────────────────► │     Backend      │ ────────────► │ PostgreSQL │
│ React/Vite  │ ◄────────────────── │  Spring Boot API │ ◄──────────── │  (Supabase)│
└─────────────┘    JWT (Bearer)     └──────────────────┘    Flyway     └────────────┘
```

O frontend é um SPA stateless que guarda o JWT no `localStorage` e o envia em
`Authorization: Bearer`. O backend não mantém sessão (stateless), validando o
token a cada requisição. Toda regra de negócio vive no backend; o frontend é
substituível.

## Camadas do backend

```
Controller  → expõe REST, valida entrada (@Valid), aplica @PreAuthorize
Service     → regra de negócio e transações (@Transactional)
Repository  → acesso a dados (Spring Data JPA)
Mapper      → Entity <-> DTO
DTO         → contratos de entrada/saída (records imutáveis)
Entity      → modelo persistente (@Entity)
Security    → JwtService, filtro, UserDetails, SecurityConfig
Exception   → GlobalExceptionHandler + exceções de domínio
Parser      → leitura de extratos (estratégia por formato)
```

## Segurança e RBAC

- Autenticação: `AuthenticationManager` + `BCryptPasswordEncoder`.
- Autorização: cada `Role` carrega um conjunto de `Permission` (ex.:
  `RECONCILIATION_WRITE`). O `AppUserDetails` publica `ROLE_<NOME>` + cada
  permissão como *authority*. Os controllers usam
  `@PreAuthorize("hasAuthority('...')")`.
- ADMIN recebe todas as permissões (seed `V2`).

| Perfil        | Permissões principais |
|---------------|-----------------------|
| ADMIN         | tudo + `USERS_MANAGE`, `SETTINGS_MANAGE` |
| FINANCEIRO    | `PAYABLE_WRITE`, `RECEIVABLE_WRITE`, `RECONCILIATION_WRITE`, `REPORTS_EXPORT` |
| CONTABILIDADE | `RECONCILIATION_VALIDATE`, `REPORTS_EXPORT`, `READ` |
| COMERCIAL     | `CLIENTS_WRITE`, `PROPERTIES_WRITE`, `SALES_WRITE`, `CONTRACTS_GENERATE` |
| VISUALIZADOR  | `READ` |

## Conciliação bancária (módulo principal)

1. **Importação** (`StatementImportService`) recebe o arquivo, detecta o formato
   pela extensão e delega a um `BankStatementParser` (CSV/OFX). Cada transação
   vira um `BankTransaction` com status `PENDING`. Transações com identificador
   bancário já existente são ignoradas (idempotência).
2. **Sugestão** (`SuggestionEngine`) compara a transação a contas a receber,
   parcelas (créditos) ou contas a pagar (débitos) e calcula um **score 0–100**:
   valor exato (60) + proximidade de data (até 25) + documento CPF/CNPJ (15).
3. **Conciliação** (`ReconciliationService`) registra um `Reconciliation`,
   marca a transação como `RECONCILED` e dá baixa no lançamento-alvo
   (`PAID`/`RECEIVED`). Permite **desfazer**, revertendo ambos os lados.
4. **Pendências e histórico** ficam disponíveis para relatório/auditoria.

### Estendendo formatos de extrato

Implemente `BankStatementParser`, declare o `FileFormat` suportado e anote com
`@Component`. A `StatementParserFactory` registra automaticamente. Nenhuma outra
camada precisa mudar.

## Geração automática de parcelas

`SaleService` divide `(valor total − entrada)` pela quantidade de parcelas,
arredondando para baixo e somando o resíduo à última parcela (fecha o total
exato). Vencimentos mensais a partir do primeiro vencimento informado.

## Conciliação manual

Além das sugestões automáticas (match por valor exato), o endpoint
`/reconciliation/transactions/{id}/targets` lista **todos** os lançamentos em
aberto compatíveis com o tipo da transação (crédito → contas a receber/parcelas;
débito → contas a pagar). A conciliação aceita alvo de **valor diferente**,
registrando `matched_amount` = valor do extrato. Status manuais (`IGNORED`,
`DIVERGENT`, voltar para `PENDING`) gravam um motivo opcional em `bank_transactions.notes`.

## Dashboard analítico

As agregações dos gráficos (`/dashboard/analytics`) usam **SQL nativo (PostgreSQL)**
via `JdbcTemplate` em `DashboardAnalyticsService` — agrupamentos por mês
(`to_char`/`date_trunc`), faixas de atraso (`CURRENT_DATE - due_date`) e fluxo de
caixa previsto (a receber − a pagar). A complexidade analítica fica isolada nessa
camada, mantendo entidades e repositórios JPA simples.

## Consulta CEP/CNPJ

O cadastro de clientes consulta a **BrasilAPI** diretamente do frontend
(`lib/brasilapi.ts`) ao sair dos campos CEP/CNPJ, preenchendo endereço e razão
social. Em qualquer falha, retorna `null` e o preenchimento manual segue normal.
```
