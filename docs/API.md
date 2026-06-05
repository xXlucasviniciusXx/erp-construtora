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

### Perfis de Acesso (Roles)
| Método | Caminho | Descrição | Permissão |
|--------|---------|-----------|-----------|
| GET | `/roles` | Lista perfis com contagem de permissões e usuários | `USERS_MANAGE` |
| GET | `/roles/permissions` | Catálogo completo de permissões (`code`, `module`, `action`) | `USERS_MANAGE` |
| POST | `/roles` | Cria perfil | `USERS_MANAGE` |
| PUT | `/roles/{id}` | Atualiza perfil (ADMIN protegido) | `USERS_MANAGE` |
| DELETE | `/roles/{id}` | Remove perfil (erro se tiver usuários vinculados) | `USERS_MANAGE` |

> O perfil `ADMIN` é **protegido** — PUT e DELETE retornam 409.

### Clientes
| Método | Caminho | Descrição | Permissão |
|--------|---------|-----------|-----------|
| GET | `/clients?q=&status=&page=&size=` | Lista paginada com busca + filtro de status | `CLIENTES_VIEW` |
| GET | `/clients/{id}` | Detalha cliente | `CLIENTES_VIEW` |
| POST | `/clients` | Cria cliente | `CLIENTES_EDIT` |
| PUT | `/clients/{id}` | Atualiza cliente | `CLIENTES_EDIT` |
| PATCH | `/clients/{id}/inactivate` | Inativa (soft delete; 400 se houver débitos) | `CLIENTES_EDIT` |

### Empreendimentos (nível 1)
| Método | Caminho | Descrição | Permissão |
|--------|---------|-----------|-----------|
| GET | `/developments` | Lista empreendimentos com valores derivados | `EMPREENDIMENTOS_VIEW` |
| GET | `/developments/{id}` | Detalha empreendimento | `EMPREENDIMENTOS_VIEW` |
| POST | `/developments` | Cria empreendimento | `EMPREENDIMENTOS_EDIT` |
| PUT | `/developments/{id}` | Atualiza empreendimento | `EMPREENDIMENTOS_EDIT` |
| DELETE | `/developments/{id}` | Remove empreendimento | `EMPREENDIMENTOS_EDIT` |

> Valores derivados retornados: `plannedTotal` (soma dos `plannedValue` dos lotes),
> `receivedTotal` (soma dos `saleValue` dos lotes vendidos), `actualBlocks` e `actualLots`.

### Quadras (nível 2)
| Método | Caminho | Descrição | Permissão |
|--------|---------|-----------|-----------|
| GET | `/blocks?developmentId=` | Lista quadras do empreendimento | `EMPREENDIMENTOS_VIEW` |
| POST | `/blocks` | Cria quadra (400 se limite atingido) | `EMPREENDIMENTOS_EDIT` |
| PUT | `/blocks/{id}` | Atualiza quadra (nome, matrícula, área) | `EMPREENDIMENTOS_EDIT` |
| DELETE | `/blocks/{id}` | Remove quadra | `EMPREENDIMENTOS_EDIT` |

### Lotes (nível 3)
| Método | Caminho | Descrição | Permissão |
|--------|---------|-----------|-----------|
| GET | `/lots` | Lista todos os lotes | `EMPREENDIMENTOS_VIEW` |
| GET | `/lots?blockId=` | Lista lotes da quadra | `EMPREENDIMENTOS_VIEW` |
| GET | `/lots?developmentId=` | Lista lotes do empreendimento | `EMPREENDIMENTOS_VIEW` |
| POST | `/lots` | Cria lote (400 se limite de lotes atingido) | `EMPREENDIMENTOS_EDIT` |
| PUT | `/lots/{id}` | Atualiza lote | `EMPREENDIMENTOS_EDIT` |
| PATCH | `/lots/{id}/cancel` | Inativa/cancela lote | `EMPREENDIMENTOS_EDIT` |
| DELETE | `/lots/{id}` | Remove lote | `EMPREENDIMENTOS_EDIT` |

> Código interno gerado automaticamente: `<blockCode>-L<nnn>` (ex.: `E001-Q01-L003`).  
> Status possíveis: `AVAILABLE`, `RESERVED`, `SOLD`, `CANCELLED`.

### Vendas
| Método | Caminho | Descrição | Permissão |
|--------|---------|-----------|-----------|
| GET | `/sales` | Lista vendas | `VENDAS_VIEW` |
| GET | `/sales?clientId=` | Filtra por cliente | `VENDAS_VIEW` |
| GET | `/sales/{id}` | Detalha venda com parcelas | `VENDAS_VIEW` |
| GET | `/sales/{id}/installments` | Lista parcelas da venda | `VENDAS_VIEW` |
| POST | `/sales` | Registra venda e gera parcelas automaticamente | `VENDAS_EDIT` |
| PUT | `/sales/{id}` | Edita venda (regenera parcelas se valor/qtd mudar e nenhuma paga) | `VENDAS_EDIT` |
| POST | `/sales/{id}/distrato` | Distrata a venda (cancela, registra distrato e libera o lote) | `VENDAS_EDIT` |
| DELETE | `/sales/{id}` | Remove venda definitivamente (libera o lote para `AVAILABLE`) | `VENDAS_EDIT` |

> A venda agora carrega `contractNumber` (`CT-NNNNNN`, sequencial) e, quando distratada,
> os campos `distratoDate`, `distratoReason`, `distratoRefundAmount`, `distratoRetainedAmount`.

### Parcelas
| Método | Caminho | Descrição | Permissão |
|--------|---------|-----------|-----------|
| GET | `/installments?q=&status=&dueFrom=&dueTo=&page=&size=` | Lista paginada com dados do cliente e filtros | `VENDAS_VIEW` |
| GET | `/installments/overdue` | Parcelas em atraso | `VENDAS_VIEW` |
| POST | `/installments/{id}/pay` | Confirma pagamento de parcela | `CONTAS_RECEBER_EDIT` |

### Contas a Pagar
| Método | Caminho | Descrição | Permissão |
|--------|---------|-----------|-----------|
| GET | `/accounts-payable?q=&status=&developmentId=&page=&size=` | Lista paginada com filtros (`developmentId=none` = só Geral) | `CONTAS_PAGAR_VIEW` |
| GET | `/accounts-payable/{id}` | Detalha conta | `CONTAS_PAGAR_VIEW` |
| POST | `/accounts-payable` | Cria conta a pagar | `CONTAS_PAGAR_EDIT` |
| PUT | `/accounts-payable/{id}` | Atualiza conta | `CONTAS_PAGAR_EDIT` |
| POST | `/accounts-payable/{id}/pay` | Confirma pagamento | `CONTAS_PAGAR_EDIT` |
| POST | `/accounts-payable/{id}/cancel` | Cancela conta | `CONTAS_PAGAR_EDIT` |
| DELETE | `/accounts-payable/{id}` | Remove conta | `CONTAS_PAGAR_EDIT` |

> Cada conta a pagar tem 3 dimensões (todas FK): **`categoryId`** (natureza do
> gasto — plano de contas), **`costCenterId`** (área/responsabilidade) e
> **`developmentId`** (empreendimento, opcional; nulo = despesa geral/administrativa).
> O `PayableResponse` traz `categoryId/categoryName/categoryGroup`,
> `costCenterId/costCenterName` e `developmentId/developmentName`.

### Contas a Receber
| Método | Caminho | Descrição | Permissão |
|--------|---------|-----------|-----------|
| GET | `/accounts-receivable?q=&status=&page=&size=` | Lista paginada com filtros | `CONTAS_RECEBER_VIEW` |
| GET | `/accounts-receivable/{id}` | Detalha conta | `CONTAS_RECEBER_VIEW` |
| POST | `/accounts-receivable` | Cria conta a receber | `CONTAS_RECEBER_EDIT` |
| PUT | `/accounts-receivable/{id}` | Atualiza conta | `CONTAS_RECEBER_EDIT` |
| POST | `/accounts-receivable/{id}/receive` | Confirma recebimento | `CONTAS_RECEBER_EDIT` |
| DELETE | `/accounts-receivable/{id}` | Remove conta | `CONTAS_RECEBER_EDIT` |

### Fornecedores
| Método | Caminho | Descrição | Permissão |
|--------|---------|-----------|-----------|
| GET | `/suppliers?q=` | Lista fornecedores com busca | `FORNECEDORES_VIEW` |
| POST | `/suppliers` | Cria fornecedor | `FORNECEDORES_EDIT` |
| PUT | `/suppliers/{id}` | Atualiza fornecedor | `FORNECEDORES_EDIT` |
| DELETE | `/suppliers/{id}` | Remove fornecedor | `FORNECEDORES_EDIT` |

### Centros de Custo (área/responsabilidade)
| Método | Caminho | Descrição | Permissão |
|--------|---------|-----------|-----------|
| GET | `/cost-centers` | Lista centros de custo (com `grupo`) | `CONTAS_PAGAR_VIEW` |
| POST | `/cost-centers` | Cria centro de custo | `SETTINGS_MANAGE` |
| PUT | `/cost-centers/{id}` | Atualiza centro de custo | `SETTINGS_MANAGE` |
| DELETE | `/cost-centers/{id}` | Remove centro de custo | `SETTINGS_MANAGE` |

### Categorias (plano de contas / natureza do gasto)
| Método | Caminho | Descrição | Permissão |
|--------|---------|-----------|-----------|
| GET | `/categories` | Lista categorias (2 níveis: `grupo` → `name`) | `CONTAS_PAGAR_VIEW` |
| POST | `/categories` | Cria categoria | `SETTINGS_MANAGE` |
| PUT | `/categories/{id}` | Atualiza categoria | `SETTINGS_MANAGE` |
| DELETE | `/categories/{id}` | Remove categoria | `SETTINGS_MANAGE` |

### Contas Bancárias
| Método | Caminho | Descrição | Permissão |
|--------|---------|-----------|-----------|
| GET | `/bank-accounts` | Lista contas | `CONCILIACAO_VIEW` |
| POST | `/bank-accounts` | Cria conta | `CONCILIACAO_EDIT` |
| PUT | `/bank-accounts/{id}` | Atualiza conta | `CONCILIACAO_EDIT` |
| DELETE | `/bank-accounts/{id}` | Remove conta | `CONCILIACAO_EDIT` |

### Importação de Extrato
| Método | Caminho | Descrição | Permissão |
|--------|---------|-----------|-----------|
| GET | `/bank-accounts/{id}/imports` | Lista importações da conta | `CONCILIACAO_VIEW` |
| POST | `/bank-accounts/{id}/imports` | Importa extrato (multipart/form-data, campo `file`) | `CONCILIACAO_EDIT` |

### Transações Bancárias
| Método | Caminho | Descrição | Permissão |
|--------|---------|-----------|-----------|
| GET | `/bank-transactions?bankAccountId=&status=` | Lista transações | `CONCILIACAO_VIEW` |

### Conciliação Bancária
| Método | Caminho | Descrição | Permissão |
|--------|---------|-----------|-----------|
| GET | `/reconciliation/pendencies` | Transações pendentes de conciliação | `CONCILIACAO_VIEW` |
| GET | `/reconciliation/history` | Histórico de conciliações | `CONCILIACAO_VIEW` |
| GET | `/reconciliation/transactions/{id}/suggestions` | Sugestões automáticas (score 0–100) | `CONCILIACAO_EDIT` |
| GET | `/reconciliation/transactions/{id}/targets` | Todos os lançamentos em aberto compatíveis | `CONCILIACAO_EDIT` |
| POST | `/reconciliation/transactions/{id}/reconcile` | Concilia (manual ou aceitando sugestão) | `CONCILIACAO_EDIT` |
| PATCH | `/reconciliation/transactions/{id}/status?status=&notes=` | Marca como IGNORED/DIVERGENT/PENDING | `CONCILIACAO_EDIT` |
| POST | `/reconciliation/{id}/undo` | Desfaz conciliação | `CONCILIACAO_EDIT` |

### Contratos
| Método | Caminho | Descrição | Permissão |
|--------|---------|-----------|-----------|
| GET | `/contracts/sales/{saleId}/html` | Contrato em HTML (pré-visualização) | `VENDAS_VIEW` |
| GET | `/contracts/sales/{saleId}/pdf` | Contrato em PDF (arquiva a versão) | `VENDAS_EDIT` |
| GET | `/contracts/sales/{saleId}/distrato/pdf` | Distrato em PDF (só vendas distratadas; arquiva) | `VENDAS_EDIT` |
| GET | `/contracts/sales/{saleId}/documents` | Lista documentos arquivados da venda | `VENDAS_VIEW` |
| GET | `/contracts/documents/{documentId}` | Baixa um documento arquivado | `VENDAS_VIEW` |

O contrato/distrato em PDF é gerado a partir de um **modelo** persistido (XHTML com tokens
`{{...}}`) e cada geração é **arquivada e versionada** em `contract_documents`.

### Modelos de contrato/distrato
| Método | Caminho | Descrição | Permissão |
|--------|---------|-----------|-----------|
| GET | `/contract-templates?kind=` | Lista modelos (`CONTRACT`/`DISTRATO`) | `SETTINGS_MANAGE` |
| GET | `/contract-templates/{id}` | Detalha um modelo | `SETTINGS_MANAGE` |
| POST | `/contract-templates` | Cria modelo (um padrão por tipo) | `SETTINGS_MANAGE` |
| PUT | `/contract-templates/{id}` | Edita modelo | `SETTINGS_MANAGE` |
| POST | `/contract-templates/{id}/copy` | Duplica um modelo (cópia não-padrão, mesmo escopo) | `SETTINGS_MANAGE` |
| DELETE | `/contract-templates/{id}` | Remove modelo (exceto o padrão) | `SETTINGS_MANAGE` |
| POST | `/contract-templates/preview` | Pré-visualiza um corpo com dados de exemplo (HTML) | `SETTINGS_MANAGE` |

O modelo guarda um **fragmento HTML** (editor visual WYSIWYG); o backend embrulha no
esqueleto XHTML + CSS e normaliza para XML (jsoup) antes de gerar o PDF. Cada modelo tem
`developmentId` (null = global); na geração, o modelo do empreendimento da venda tem
prioridade sobre o global. Um modelo padrão por escopo (tipo + empreendimento).

### Dashboard
| Método | Caminho | Descrição | Permissão |
|--------|---------|-----------|-----------|
| GET | `/dashboard` | Cards de resumo | `DASHBOARD_VIEW` |
| GET | `/dashboard/analytics?from=&to=&clientId=&propertyId=` | Séries dos gráficos analíticos | `DASHBOARD_VIEW` |

### DRE — Demonstração do Resultado (base caixa)
| Método | Caminho | Descrição | Permissão |
|--------|---------|-----------|-----------|
| GET | `/dre?from=&to=&developmentId=` | Receitas recebidas − despesas pagas = resultado | `DRE_VIEW` |
| GET | `/dre/export?from=&to=&developmentId=` | Exporta o DRE em CSV | `DRE_VIEW` |

> Receitas em linhas fixas: **Receita de Vendas** (parcelas recebidas) + **Outras
> Receitas** (recebíveis avulsos). Despesas agrupadas por **grupo de categoria**.
> Resposta: `{ revenues[], totalRevenue, expenses[], totalExpense, result }`.

### Relatórios (CSV)
| Método | Caminho | Descrição | Permissão |
|--------|---------|-----------|-----------|
| GET | `/reports/accounts-payable?from=&to=` | Contas a pagar por período | `RELATORIOS_VIEW` |
| GET | `/reports/accounts-receivable?from=&to=` | Contas a receber por período | `RELATORIOS_VIEW` |
| GET | `/reports/overdue-installments` | Parcelas em atraso | `RELATORIOS_VIEW` |
| GET | `/reports/reconciliations?from=&to=` | Conciliações realizadas | `RELATORIOS_VIEW` |
| GET | `/reports/pending-transactions` | Transações bancárias pendentes | `RELATORIOS_VIEW` |
| GET | `/reports/sales-by-development` | Vendas agrupadas por empreendimento | `RELATORIOS_VIEW` |
| GET | `/reports/expenses-by-development` | Despesas pagas por empreendimento | `RELATORIOS_VIEW` |
| GET | `/reports/expenses-by-category` | Despesas pagas por categoria (grupo/item) | `RELATORIOS_VIEW` |
| GET | `/reports/expenses-by-cost-center` | Despesas pagas por centro de custo | `RELATORIOS_VIEW` |
| GET | `/reports/delinquent-clients` | Clientes com parcelas em atraso | `RELATORIOS_VIEW` |

### Configurações
| Método | Caminho | Descrição | Permissão |
|--------|---------|-----------|-----------|
| GET | `/settings/public` | **Apenas branding** (sem auth; sem dados sensíveis; usado pelo health check do Render) | — |
| GET | `/settings` | Configuração completa, **inclui SMTP** (sem a senha) | `SETTINGS_MANAGE` |
| PUT | `/settings` | Atualiza configurações (branding, empresa e **SMTP**) | `SETTINGS_MANAGE` |

> O SMTP é configurado **pela aplicação** (não por variáveis de ambiente):
> `mailEnabled`, `mailHost`, `mailPort`, `mailUsername`, `mailPassword`
> (write-only — nunca retorna; `mailPasswordSet` indica se está definida),
> `mailFrom`, `mailReminderDays`. Com `mailEnabled=false` ou host vazio, os
> e-mails ficam em **modo simulado** (registrados como `PENDING` e logados).

### Notificações (e-mail)
| Método | Caminho | Descrição | Permissão |
|--------|---------|-----------|-----------|
| GET | `/notifications?status=&eventType=&page=&size=` | Histórico paginado de e-mails | `SETTINGS_MANAGE` |
| POST | `/notifications/{id}/resend` | Reenvia uma notificação | `SETTINGS_MANAGE` |
| POST | `/notifications/test?to=` | Envia um e-mail de teste (valida o SMTP) | `SETTINGS_MANAGE` |

> Eventos automáticos: `PAYMENT_OVERDUE` (atraso), `PAYMENT_DUE_SOON` (lembrete
> N dias antes do vencimento — job diário), `PAYMENT_CONFIRMED`, `SALE_CREATED`,
> `CONTRACT_GENERATED`. E-mails em **HTML** com a identidade do sistema.

### Licenciamento
| Método | Caminho | Descrição | Permissão |
|--------|---------|-----------|-----------|
| GET | `/licensing/me` | Retorna `{ modules[], license }` completo (módulos + licença) | qualquer usuário autenticado |
| GET | `/licensing/modules/{code}` | Retorna módulo por código | qualquer usuário autenticado |
| GET | `/licensing/license` | Detalha a licença da VM | `SETTINGS_MANAGE` |
| PUT | `/licensing/license` | Edita licença (plano, status, datas, limites, notas) | `SETTINGS_MANAGE` |
| POST | `/licensing/plan` | Aplica preset de plano (ESSENCIAL / PROFISSIONAL / PREMIUM) | `SETTINGS_MANAGE` |
| PATCH | `/licensing/modules/{code}` | Liga/desliga módulo individual | `SETTINGS_MANAGE` |
| POST | `/licensing/license/key` | Aplica chave de licença HMAC (atualiza plano + validade) | `SETTINGS_MANAGE` |
| POST | `/licensing/license/key/generate` | Gera nova chave assinada (uso pelo fornecedor) | `SETTINGS_MANAGE` |

> **`GET /licensing/me`** — resposta completa:
> ```json
> {
>   "modules": [{ "code": "CLIENTES", "name": "Clientes", "active": true, ... }],
>   "license": {
>     "plan": "PROFISSIONAL",
>     "status": "ATIVA",
>     "startDate": "2025-01-01",
>     "endDate": "2026-01-01",
>     "daysToExpire": 210,
>     "expired": false,
>     "readOnly": false,
>     "blocked": false,
>     "hasKey": true,
>     "customer": "Construtora XYZ",
>     "graceDays": 7
>   }
> }
> ```

> **Chave de licença** — formato `base64url(payload).base64url(hmac)`.
> Payload JSON: `{ plan, customer, startDate, endDate, periodMonths, maxUsers }`.
> Verificada com HMAC-SHA256 usando `LICENSE_SECRET` (env var da VM). Expiração
> é validada no parse — chaves vencidas são rejeitadas.

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

### Aplicar chave de licença
```http
POST /api/licensing/license/key
Content-Type: application/json
Authorization: Bearer <token>

{ "key": "eyJwbGFuIjoiUFJPRklTU0lPTkFMIn0.xXlRm..." }
```

> A chave é verificada com HMAC-SHA256. Se válida, o plano, datas e módulos
> são atualizados automaticamente. Se expirada ou com assinatura inválida → 400.

### Gerar chave de licença (uso pelo fornecedor)
```http
POST /api/licensing/license/key/generate
Content-Type: application/json
Authorization: Bearer <token>

{ "plan": "PROFISSIONAL", "customer": "Construtora XYZ",
  "startDate": "2026-01-01", "periodMonths": 12, "maxUsers": 10 }
```

> Retorna `{ "key": "eyJ....<hmac>" }`. A chave pode ser copiada no painel e
> enviada ao cliente para ser aplicada em Configurações → Módulos & Licença.

### Criar/editar perfil de acesso
```http
POST /api/roles
Content-Type: application/json
Authorization: Bearer <token>

{
  "name": "CORRETOR",
  "description": "Acesso comercial com leitura financeira",
  "permissions": ["CLIENTES_VIEW", "CLIENTES_EDIT", "EMPREENDIMENTOS_VIEW",
                  "VENDAS_VIEW", "VENDAS_EDIT", "CONTAS_RECEBER_VIEW"]
}
```

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

---

## Enforcement de licença no backend

O `LicenseEnforcementInterceptor` avalia **cada request** antes do controller:

| Situação | Resultado |
|----------|-----------|
| Licença `CANCELADA` | 403 bloqueio total |
| Licença `SUSPENSA` ou vencida além do `grace_days` | Apenas GETs passam (modo somente-leitura) |
| Dentro da tolerância de vencimento | Request passa; `license.readOnly=false` no `GET /licensing/me` |
| Módulo da rota está inativo | 403 para qualquer método |
| Path isento (`/auth`, `/licensing`, `/settings`, `/users`, `/roles`) | Sempre liberado |
