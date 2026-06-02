# API REST — Referência

Base URL: `http://localhost:8080/api`  
Swagger UI: `http://localhost:8080/swagger-ui.html` (produção: `/swagger-ui.html` no domínio do Render)

Todos os endpoints, exceto `POST /auth/login` e `GET /settings/public`, exigem
`Authorization: Bearer <token>`.

---

## Autenticação

```http
POST /api/auth/login
Content-Type: application/json

{ "email": "admin@construtora.com.br", "password": "Admin@123" }
```

Resposta: `{ token, tokenType, expiresInMs, userId, name, email, role, permissions[] }`

```http
GET /api/auth/me   → dados do usuário autenticado
```

---

## Endpoints por módulo

### Usuários
| Método | Caminho | Descrição | Permissão |
|--------|---------|-----------|-----------|
| GET | `/users` | Lista usuários | `USERS_MANAGE` |
| POST | `/users` | Cria usuário | `USERS_MANAGE` |
| PUT | `/users/{id}` | Atualiza usuário | `USERS_MANAGE` |
| DELETE | `/users/{id}` | Remove usuário | `USERS_MANAGE` |

### Clientes
| Método | Caminho | Descrição | Permissão |
|--------|---------|-----------|-----------|
| GET | `/clients?q=&page=&size=` | Lista paginada com busca | `READ` |
| GET | `/clients/{id}` | Detalha cliente | `READ` |
| POST | `/clients` | Cria cliente | `CLIENTS_WRITE` |
| PUT | `/clients/{id}` | Atualiza cliente | `CLIENTS_WRITE` |
| PATCH | `/clients/{id}/inactivate` | Inativa (soft delete; 400 se houver débitos) | `CLIENTS_WRITE` |

### Empreendimentos (nível 1)
| Método | Caminho | Descrição | Permissão |
|--------|---------|-----------|-----------|
| GET | `/developments` | Lista empreendimentos com valores derivados | `READ` |
| GET | `/developments/{id}` | Detalha empreendimento | `READ` |
| POST | `/developments` | Cria empreendimento | `PROPERTIES_WRITE` |
| PUT | `/developments/{id}` | Atualiza empreendimento | `PROPERTIES_WRITE` |
| DELETE | `/developments/{id}` | Remove empreendimento | `PROPERTIES_WRITE` |

> Valores derivados retornados: `plannedTotal` (soma dos `plannedValue` dos lotes),
> `receivedTotal` (soma dos `saleValue` dos lotes vendidos), `actualBlocks` e `actualLots`.

### Quadras (nível 2)
| Método | Caminho | Descrição | Permissão |
|--------|---------|-----------|-----------|
| GET | `/blocks?developmentId=` | Lista quadras do empreendimento | `READ` |
| POST | `/blocks` | Cria quadra (400 se limite atingido) | `PROPERTIES_WRITE` |
| PUT | `/blocks/{id}` | Atualiza quadra (nome, matrícula, área) | `PROPERTIES_WRITE` |
| DELETE | `/blocks/{id}` | Remove quadra | `PROPERTIES_WRITE` |

### Lotes (nível 3)
| Método | Caminho | Descrição | Permissão |
|--------|---------|-----------|-----------|
| GET | `/lots` | Lista todos os lotes | `READ` |
| GET | `/lots?blockId=` | Lista lotes da quadra | `READ` |
| GET | `/lots?developmentId=` | Lista lotes do empreendimento | `READ` |
| POST | `/lots` | Cria lote (400 se limite de lotes atingido) | `PROPERTIES_WRITE` |
| PUT | `/lots/{id}` | Atualiza lote | `PROPERTIES_WRITE` |
| PATCH | `/lots/{id}/cancel` | Inativa/cancela lote | `PROPERTIES_WRITE` |
| DELETE | `/lots/{id}` | Remove lote | `PROPERTIES_WRITE` |

> Código interno gerado automaticamente: `<blockCode>-L<nnn>` (ex.: `E001-Q01-L003`).  
> Status possíveis: `AVAILABLE`, `RESERVED`, `SOLD`, `CANCELLED`.

### Vendas
| Método | Caminho | Descrição | Permissão |
|--------|---------|-----------|-----------|
| GET | `/sales` | Lista vendas | `READ` |
| GET | `/sales?clientId=` | Filtra por cliente | `READ` |
| GET | `/sales/{id}` | Detalha venda com parcelas | `READ` |
| GET | `/sales/{id}/installments` | Lista parcelas da venda | `READ` |
| POST | `/sales` | Registra venda e gera parcelas automaticamente | `SALES_WRITE` |
| PUT | `/sales/{id}` | Edita venda (regenera parcelas se valor/qtd mudar e nenhuma paga) | `SALES_WRITE` |
| DELETE | `/sales/{id}` | Remove venda (libera o lote para `AVAILABLE`) | `SALES_WRITE` |

### Parcelas
| Método | Caminho | Descrição | Permissão |
|--------|---------|-----------|-----------|
| GET | `/installments?q=&status=&dueFrom=&dueTo=` | Lista com dados do cliente e filtros | `READ` |
| GET | `/installments/overdue` | Parcelas em atraso | `READ` |
| POST | `/installments/{id}/pay` | Confirma pagamento de parcela | `RECEIVABLE_WRITE` ou `SALES_WRITE` |

### Contas a Pagar
| Método | Caminho | Descrição | Permissão |
|--------|---------|-----------|-----------|
| GET | `/accounts-payable` | Lista contas a pagar | `READ` |
| GET | `/accounts-payable/{id}` | Detalha conta | `READ` |
| POST | `/accounts-payable` | Cria conta a pagar | `PAYABLE_WRITE` |
| PUT | `/accounts-payable/{id}` | Atualiza conta | `PAYABLE_WRITE` |
| POST | `/accounts-payable/{id}/pay` | Confirma pagamento | `PAYABLE_WRITE` |
| POST | `/accounts-payable/{id}/cancel` | Cancela conta | `PAYABLE_WRITE` |
| DELETE | `/accounts-payable/{id}` | Remove conta | `PAYABLE_WRITE` |

> Cada conta a pagar tem 3 dimensões (todas FK): **`categoryId`** (natureza do
> gasto — plano de contas), **`costCenterId`** (área/responsabilidade) e
> **`developmentId`** (empreendimento, opcional; nulo = despesa geral/administrativa).
> O `PayableResponse` traz `categoryId/categoryName/categoryGroup`,
> `costCenterId/costCenterName` e `developmentId/developmentName`.

### Contas a Receber
| Método | Caminho | Descrição | Permissão |
|--------|---------|-----------|-----------|
| GET | `/accounts-receivable` | Lista contas a receber | `READ` |
| GET | `/accounts-receivable/{id}` | Detalha conta | `READ` |
| POST | `/accounts-receivable` | Cria conta a receber | `RECEIVABLE_WRITE` |
| PUT | `/accounts-receivable/{id}` | Atualiza conta | `RECEIVABLE_WRITE` |
| POST | `/accounts-receivable/{id}/receive` | Confirma recebimento | `RECEIVABLE_WRITE` |
| DELETE | `/accounts-receivable/{id}` | Remove conta | `RECEIVABLE_WRITE` |

### Fornecedores
| Método | Caminho | Descrição | Permissão |
|--------|---------|-----------|-----------|
| GET | `/suppliers?q=` | Lista fornecedores com busca | `READ` |
| POST | `/suppliers` | Cria fornecedor | `PAYABLE_WRITE` |
| PUT | `/suppliers/{id}` | Atualiza fornecedor | `PAYABLE_WRITE` |
| DELETE | `/suppliers/{id}` | Remove fornecedor | `PAYABLE_WRITE` |

### Centros de Custo (área/responsabilidade)
| Método | Caminho | Descrição | Permissão |
|--------|---------|-----------|-----------|
| GET | `/cost-centers` | Lista centros de custo (com `grupo`) | `READ` |
| POST | `/cost-centers` | Cria centro de custo | `SETTINGS_MANAGE` |
| PUT | `/cost-centers/{id}` | Atualiza centro de custo | `SETTINGS_MANAGE` |
| DELETE | `/cost-centers/{id}` | Remove centro de custo | `SETTINGS_MANAGE` |

### Categorias (plano de contas / natureza do gasto)
| Método | Caminho | Descrição | Permissão |
|--------|---------|-----------|-----------|
| GET | `/categories` | Lista categorias (2 níveis: `grupo` → `name`) | `READ` |
| POST | `/categories` | Cria categoria | `SETTINGS_MANAGE` |
| PUT | `/categories/{id}` | Atualiza categoria | `SETTINGS_MANAGE` |
| DELETE | `/categories/{id}` | Remove categoria | `SETTINGS_MANAGE` |

### Contas Bancárias
| Método | Caminho | Descrição | Permissão |
|--------|---------|-----------|-----------|
| GET | `/bank-accounts` | Lista contas | `READ` |
| POST | `/bank-accounts` | Cria conta | `RECONCILIATION_WRITE` |
| PUT | `/bank-accounts/{id}` | Atualiza conta | `RECONCILIATION_WRITE` |
| DELETE | `/bank-accounts/{id}` | Remove conta | `RECONCILIATION_WRITE` |

### Importação de Extrato
| Método | Caminho | Descrição | Permissão |
|--------|---------|-----------|-----------|
| GET | `/bank-accounts/{id}/imports` | Lista importações da conta | `READ` |
| POST | `/bank-accounts/{id}/imports` | Importa extrato (multipart/form-data, campo `file`) | `RECONCILIATION_WRITE` |

### Transações Bancárias
| Método | Caminho | Descrição | Permissão |
|--------|---------|-----------|-----------|
| GET | `/bank-transactions?bankAccountId=&status=` | Lista transações | `READ` |

### Conciliação Bancária
| Método | Caminho | Descrição | Permissão |
|--------|---------|-----------|-----------|
| GET | `/reconciliation/pendencies` | Transações pendentes de conciliação | `READ` |
| GET | `/reconciliation/history` | Histórico de conciliações | `READ` |
| GET | `/reconciliation/transactions/{id}/suggestions` | Sugestões automáticas (score 0–100) | `RECONCILIATION_WRITE` |
| GET | `/reconciliation/transactions/{id}/targets` | Todos os lançamentos em aberto compatíveis | `RECONCILIATION_WRITE` |
| POST | `/reconciliation/transactions/{id}/reconcile` | Concilia (manual ou aceitando sugestão) | `RECONCILIATION_WRITE` |
| PATCH | `/reconciliation/transactions/{id}/status?status=&notes=` | Marca como IGNORED/DIVERGENT/PENDING | `RECONCILIATION_WRITE` |
| POST | `/reconciliation/{id}/undo` | Desfaz conciliação | `RECONCILIATION_WRITE` |

### Contratos
| Método | Caminho | Descrição | Permissão |
|--------|---------|-----------|-----------|
| GET | `/contracts/sales/{saleId}/html` | Contrato em HTML | `READ` |
| GET | `/contracts/sales/{saleId}/pdf` | Contrato em PDF (Flying Saucer) | `CONTRACTS_GENERATE` |

### Dashboard
| Método | Caminho | Descrição | Permissão |
|--------|---------|-----------|-----------|
| GET | `/dashboard` | Cards de resumo | `READ` |
| GET | `/dashboard/analytics?from=&to=&clientId=&propertyId=` | Séries dos gráficos analíticos | `READ` |

### DRE — Demonstração do Resultado (base caixa)
| Método | Caminho | Descrição | Permissão |
|--------|---------|-----------|-----------|
| GET | `/dre?from=&to=&developmentId=` | Receitas recebidas − despesas pagas = resultado | `READ` |
| GET | `/dre/export?from=&to=&developmentId=` | Exporta o DRE em CSV | `READ` |

> Receitas em linhas fixas: **Receita de Vendas** (parcelas recebidas) + **Outras
> Receitas** (recebíveis avulsos). Despesas agrupadas por **grupo de categoria**.
> Resposta: `{ revenues[], totalRevenue, expenses[], totalExpense, result }`.

### Relatórios (CSV)
| Método | Caminho | Descrição | Permissão |
|--------|---------|-----------|-----------|
| GET | `/reports/accounts-payable?from=&to=` | Contas a pagar por período | `REPORTS_EXPORT` |
| GET | `/reports/accounts-receivable?from=&to=` | Contas a receber por período | `REPORTS_EXPORT` |
| GET | `/reports/overdue-installments` | Parcelas em atraso | `REPORTS_EXPORT` |
| GET | `/reports/reconciliations?from=&to=` | Conciliações realizadas | `REPORTS_EXPORT` |
| GET | `/reports/pending-transactions` | Transações bancárias pendentes | `REPORTS_EXPORT` |
| GET | `/reports/sales-by-development` | Vendas agrupadas por empreendimento | `REPORTS_EXPORT` |
| GET | `/reports/expenses-by-development` | Despesas pagas por empreendimento | `REPORTS_EXPORT` |
| GET | `/reports/expenses-by-category` | Despesas pagas por categoria (grupo/item) | `REPORTS_EXPORT` |
| GET | `/reports/expenses-by-cost-center` | Despesas pagas por centro de custo | `REPORTS_EXPORT` |
| GET | `/reports/delinquent-clients` | Clientes com parcelas em atraso | `REPORTS_EXPORT` |

### Configurações
| Método | Caminho | Descrição | Permissão |
|--------|---------|-----------|-----------|
| GET | `/settings/public` | Branding público (sem auth) | — |
| GET | `/settings` | Todas as configurações | `READ` |
| PUT | `/settings` | Atualiza configurações | `SETTINGS_MANAGE` |

---

## Exemplos

### Criar venda (gera parcelas automaticamente)
```http
POST /api/sales
Content-Type: application/json
Authorization: Bearer <token>

{
  "clientId": "uuid-do-cliente",
  "lotId": "uuid-do-lote",
  "totalValue": 350000.00,
  "downPayment": 50000.00,
  "installmentsCount": 60,
  "firstDueDate": "2026-07-10",
  "purchaseType": "Entrada + parcelas",
  "paymentMethod": "Boleto",
  "correctionIndex": "INCC"
}
```

> - `purchaseType` aceita: `"À vista"` ou `"Entrada + parcelas"`.  
> - `downPayment` é ignorado quando `purchaseType` for `"À vista"`.  
> - Ao criar a venda: `lot.saleValue` e `lot.status = SOLD` são atualizados automaticamente.  
> - Ao excluir: lote volta para `AVAILABLE` e `saleValue` é zerado.

### Criar empreendimento com hierarquia
```http
POST /api/developments
{ "name": "Residencial Mirante", "blocksCount": 4, "lotsCount": 80,
  "expectedValue": 12000000.00, "address": "Rua das Flores, 100" }

POST /api/blocks
{ "developmentId": "uuid-do-empreendimento", "name": "Quadra A", "area": 5000.0 }

POST /api/lots
{ "blockId": "uuid-da-quadra", "name": "Lote 01",
  "totalArea": 250.0, "plannedValue": 150000.00 }
```

> Códigos internos são gerados automaticamente: `E002`, `E002-Q01`, `E002-Q01-L001`.

### Conciliar transação
```http
# 1. Ver alvos disponíveis (lançamentos em aberto compatíveis)
GET /api/reconciliation/transactions/{txId}/targets

# 2. Conciliar (manual ou aceitando sugestão)
POST /api/reconciliation/transactions/{txId}/reconcile
{ "targetType": "RECEIVABLE", "targetId": "uuid-do-lancamento", "notes": "Conferido OK" }
```

> A conciliação aceita alvo de **valor diferente** — a diferença fica em `matched_amount`.

### Parcelas com filtros
```http
GET /api/installments?q=Carla&status=OVERDUE&dueFrom=2026-01-01&dueTo=2026-12-31
```

### Dashboard analítico
```http
GET /api/dashboard/analytics?from=2026-01-01&to=2026-12-31
→ {
    totalSold, totalReceived, totalOpen, totalOverdue,
    delinquentClients, activeClients, inactiveClients,
    lotsSold, lotsAvailable,
    receivedByMonth[], toReceiveByMonth[], overdueByMonth[],
    delinquencyByDevelopment[], salesByMonth[], salesByPurchaseType[],
    cashFlowForecast[], payablesPaidVsOpen[], receivablesReceivedVsOpen[],
    overdueByAging[], expensesByDevelopment[], profitByDevelopment[],
    expensesByCategory[], expensesByCostCenter[]
  }
```

> `expensesByDevelopment` = despesas **pagas** por empreendimento (nulo →
> "Geral / Administrativo"). `profitByDevelopment` = **lucro/prejuízo em caixa**
> por empreendimento (parcelas recebidas − despesas pagas).

### Importar extrato (cURL)
```bash
curl -H "Authorization: Bearer $TOKEN" \
  -F "file=@database/samples/extrato-exemplo.csv" \
  http://localhost:8080/api/bank-accounts/11111111-1111-1111-1111-111111111111/imports
```

---

## Consulta CEP/CNPJ

Feita no **frontend** via BrasilAPI (`https://brasilapi.com.br/api/cep/v1/{cep}`
e `/api/cnpj/v1/{cnpj}`) — não passa pelo backend. Em falha, o preenchimento
manual continua disponível normalmente.
