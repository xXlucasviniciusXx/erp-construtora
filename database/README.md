# Banco de dados

PostgreSQL 16. O schema é **versionado pelo Flyway** em
[`backend/src/main/resources/db/migration`](../backend/src/main/resources/db/migration):

| Migration | Conteúdo |
|-----------|----------|
| `V1__init_schema.sql` | Todas as tabelas, índices e constraints |
| `V2__seed_roles_permissions.sql` | Perfis, permissões e configuração padrão |
| `V3__seed_demo_data.sql` | Dados de demonstração básicos (remova em produção) |
| `V4__bank_transaction_notes.sql` | Coluna `notes` em `bank_transactions` (motivo de divergência) |
| `V5__seed_demo_extended.sql` | Demo ampliada: clientes em várias situações, empreendimentos, lotes, vendas, parcelas, contas (remova em produção) |

O Hibernate roda com `ddl-auto: validate` — o banco **nunca** é alterado pela
aplicação, apenas pelas migrations.

## Modelo (resumo)

```
roles ─┬─< role_permissions >─┬─ permissions
       └──< users

clients ──< property_sales >── properties
                  │
                  └──< installments

accounts_payable        accounts_receivable ──(client/sale/installment)

bank_accounts ──< bank_statement_imports ──< bank_transactions
                                                    │
                                  reconciliation_suggestions
                                                    │
bank_transactions ──< reconciliations >── (receivable | payable | installment)

system_settings    email_notifications    audit_logs
```

### Tabelas

`users, roles, permissions, role_permissions, clients, properties,
property_sales, installments, accounts_payable, accounts_receivable,
bank_accounts, bank_statement_imports, bank_transactions, reconciliations,
reconciliation_suggestions, system_settings, email_notifications, audit_logs`.

Convenções: chaves primárias `UUID` (`gen_random_uuid()`), `snake_case`,
timestamps `TIMESTAMPTZ`, enums modelados como `VARCHAR + CHECK` para portar
entre bancos SQL.

## Arquivos de exemplo

- [`samples/extrato-exemplo.csv`](samples/extrato-exemplo.csv) — extrato CSV.
- [`samples/extrato-exemplo.ofx`](samples/extrato-exemplo.ofx) — extrato OFX (SGML).

Use-os na tela **Importar Extrato** para testar a conciliação.

## Reset local

```bash
docker compose down -v          # apaga o volume do Postgres
docker compose up --build       # recria e reaplica as migrations
```
