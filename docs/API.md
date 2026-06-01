# API REST

Base URL: `http://localhost:8080/api` (Swagger UI em `/swagger-ui.html`).
Todos os endpoints, exceto `POST /auth/login` e `GET /settings/public`, exigem
`Authorization: Bearer <token>`.

## Autenticação

```http
POST /api/auth/login
Content-Type: application/json

{ "email": "admin@construtora.com.br", "password": "Admin@123" }
```
Resposta: `{ token, tokenType, expiresInMs, userId, name, email, role, permissions }`

`GET /api/auth/me` → dados do usuário autenticado.

## Endpoints por módulo

| Módulo | Método | Caminho | Permissão |
|--------|--------|---------|-----------|
| Users | GET/POST/PUT/DELETE | `/users` | `USERS_MANAGE` |
| Clients | GET | `/clients?q=&page=&size=` | `READ` |
| Clients | POST/PUT/DELETE | `/clients/{id}` | `CLIENTS_WRITE` |
| Clients | PATCH | `/clients/{id}/inactivate` (soft delete; 400 se houver débitos) | `CLIENTS_WRITE` |
| Properties | GET / POST/PUT/DELETE | `/properties` | `READ` / `PROPERTIES_WRITE` |
| Sales | GET | `/sales?clientId=`, `/sales/{id}`, `/sales/{id}/installments` | `READ` |
| Sales | POST/DELETE | `/sales` | `SALES_WRITE` |
| Installments | GET | `/installments?q=&status=&dueFrom=&dueTo=` (com dados do cliente), `/installments/overdue` | `READ` |
| Installments | POST | `/installments/{id}/pay` | `RECEIVABLE_WRITE`/`SALES_WRITE` |
| Accounts Payable | GET / POST/PUT/DELETE / POST `/{id}/pay` / POST `/{id}/cancel` | `/accounts-payable` | `READ` / `PAYABLE_WRITE` |
| Accounts Receivable | GET / POST/PUT/DELETE / POST `/{id}/receive` | `/accounts-receivable` | `READ` / `RECEIVABLE_WRITE` |
| Bank Accounts | GET / POST/PUT/DELETE | `/bank-accounts` | `READ` / `RECONCILIATION_WRITE` |
| Statement Import | GET / POST(multipart) | `/bank-accounts/{id}/imports` | `READ` / `RECONCILIATION_WRITE` |
| Bank Transactions | GET | `/bank-transactions?bankAccountId=&status=` | `READ` |
| Reconciliation | GET | `/reconciliation/pendencies`, `/reconciliation/history` | `READ` |
| Reconciliation | GET | `/reconciliation/transactions/{id}/suggestions` | `RECONCILIATION_WRITE/VALIDATE` |
| Reconciliation | GET | `/reconciliation/transactions/{id}/targets` (alvos p/ conciliação manual) | `RECONCILIATION_WRITE/VALIDATE` |
| Reconciliation | POST | `/reconciliation/transactions/{id}/reconcile` | `RECONCILIATION_WRITE` |
| Reconciliation | PATCH | `/reconciliation/transactions/{id}/status?status=&notes=` | `RECONCILIATION_WRITE` |
| Reconciliation | POST | `/reconciliation/{id}/undo` | `RECONCILIATION_WRITE` |
| Contracts | GET | `/contracts/sales/{saleId}/html` · `/pdf` | `CONTRACTS_GENERATE`/`READ` |
| Settings | GET `/settings/public` (público) · GET/PUT `/settings` | `READ` / `SETTINGS_MANAGE` |
| Dashboard | GET | `/dashboard` · `/dashboard/analytics` (cards + séries dos gráficos) | `READ` |
| Reports | GET | `/reports/*` (CSV) | `REPORTS_EXPORT`/`READ` |

> **Consulta CEP/CNPJ** no cadastro de clientes é feita no **frontend** via BrasilAPI
> (`https://brasilapi.com.br/api/cep/v1/{cep}` e `/api/cnpj/v1/{cnpj}`), sem passar
> pelo backend. Em caso de falha, o preenchimento manual continua disponível.

## Exemplos

**Criar venda (gera parcelas):**
```http
POST /api/sales
{
  "clientId": "…", "propertyId": "…",
  "totalValue": 350000.00, "downPayment": 50000.00,
  "installmentsCount": 60, "firstDueDate": "2026-07-10",
  "paymentMethod": "Boleto", "correctionIndex": "INCC"
}
```

**Conciliar transação (manual ou aceitando sugestão):**
```http
GET  /api/reconciliation/transactions/{id}/targets   → lança­mentos em aberto p/ escolher
POST /api/reconciliation/transactions/{id}/reconcile
{ "targetType": "RECEIVABLE", "targetId": "…", "notes": "Conferido" }
```
> A conciliação manual aceita alvo de **valor diferente** do extrato — a diferença
> fica registrada (`matched_amount` = valor do extrato).

**Parcelas com filtros (dados do cliente):**
```http
GET /api/installments?q=Carla&status=OVERDUE&dueFrom=2026-01-01&dueTo=2026-12-31
```

**Dashboard (gráficos):**
```http
GET /api/dashboard/analytics
→ { totalSold, totalReceived, totalOpen, totalOverdue, lotsSold, lotsAvailable,
    receivedByMonth[], overdueByAging[], cashFlowForecast[], ... }
```

**Importar extrato (multipart):**
```bash
curl -H "Authorization: Bearer $TOKEN" \
  -F "file=@database/samples/extrato-exemplo.csv" \
  http://localhost:8080/api/bank-accounts/11111111-1111-1111-1111-111111111111/imports
```
