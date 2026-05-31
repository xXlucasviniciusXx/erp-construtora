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
| Properties | GET / POST/PUT/DELETE | `/properties` | `READ` / `PROPERTIES_WRITE` |
| Sales | GET | `/sales`, `/sales/{id}`, `/sales/{id}/installments` | `READ` |
| Sales | POST/DELETE | `/sales` | `SALES_WRITE` |
| Installments | GET | `/installments/overdue` | `READ` |
| Installments | POST | `/installments/{id}/pay` | `RECEIVABLE_WRITE`/`SALES_WRITE` |
| Accounts Payable | GET / POST/PUT/DELETE / POST `/{id}/pay` | `/accounts-payable` | `READ` / `PAYABLE_WRITE` |
| Accounts Receivable | GET / POST/PUT/DELETE / POST `/{id}/receive` | `/accounts-receivable` | `READ` / `RECEIVABLE_WRITE` |
| Bank Accounts | GET / POST/PUT/DELETE | `/bank-accounts` | `READ` / `RECONCILIATION_WRITE` |
| Statement Import | GET / POST(multipart) | `/bank-accounts/{id}/imports` | `READ` / `RECONCILIATION_WRITE` |
| Bank Transactions | GET | `/bank-transactions?bankAccountId=&status=` | `READ` |
| Reconciliation | GET | `/reconciliation/pendencies`, `/reconciliation/history` | `READ` |
| Reconciliation | GET | `/reconciliation/transactions/{id}/suggestions` | `RECONCILIATION_WRITE/VALIDATE` |
| Reconciliation | POST | `/reconciliation/transactions/{id}/reconcile` | `RECONCILIATION_WRITE` |
| Reconciliation | PATCH | `/reconciliation/transactions/{id}/status?status=` | `RECONCILIATION_WRITE` |
| Reconciliation | POST | `/reconciliation/{id}/undo` | `RECONCILIATION_WRITE` |
| Contracts | GET | `/contracts/sales/{saleId}/html` · `/pdf` | `CONTRACTS_GENERATE`/`READ` |
| Settings | GET `/settings/public` (público) · GET/PUT `/settings` | `READ` / `SETTINGS_MANAGE` |
| Dashboard | GET | `/dashboard` | `READ` |
| Reports | GET | `/reports/*` (CSV) | `REPORTS_EXPORT`/`READ` |

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

**Conciliar transação:**
```http
POST /api/reconciliation/transactions/{transactionId}/reconcile
{ "targetType": "RECEIVABLE", "targetId": "…", "notes": "Conferido" }
```

**Importar extrato (multipart):**
```bash
curl -H "Authorization: Bearer $TOKEN" \
  -F "file=@database/samples/extrato-exemplo.csv" \
  http://localhost:8080/api/bank-accounts/11111111-1111-1111-1111-111111111111/imports
```
