# Estratégia de versionamento

## Branches

- `main` — sempre estável/deployável (CD direto para Render/Vercel).
- `develop` — integração das features.
- `feature/<nome>` — uma feature por branch, parte de `develop`.

Fluxo: `feature/*` → PR para `develop` → quando estável, `develop` → `main`.

## Commits realizados (histórico do POC)

```
chore: estrutura inicial do monorepo (backend, frontend, database, docs)
chore(ci): docker-compose, .gitignore e .env.example
feat(db): schema inicial e seeds (Flyway V1–V3)
feat(backend): segurança JWT, RBAC e tratamento global de exceções
feat(backend): módulos clientes, imóveis, vendas e parcelas
feat(backend): contas a pagar/receber
feat(backend): importação de extratos (CSV/OFX) e parsers extensíveis
feat(backend): motor de conciliação bancária (sugestão, manual, desfazer)
feat(backend): dashboard, relatórios CSV, contratos (HTML/PDF), notificações
feat(backend): configurações do sistema e Swagger/OpenAPI
feat(frontend): shell (auth, layout, rotas protegidas, branding dinâmico)
feat(frontend): telas de clientes, imóveis, vendas, financeiro
feat(frontend): conciliação bancária e importação de extrato
feat(frontend): relatórios, usuários e configurações
feat(backend): conciliação manual com lançamentos de valor diferente
feat(frontend): menu de ações nos clientes, inativação e visualização de lotes
feat(frontend): parcelas com dados do cliente e filtros avançados
feat(frontend): ícones de ação em contas a pagar
feat(frontend): dashboard analítico com 9 gráficos Recharts e filtros
feat(db): Flyway V4 — notes em bank_transactions
feat(db): Flyway V5 — dados de demonstração ampliados
feat(backend): fornecedores e centros de custo (Flyway V6)
feat(frontend): tela de fornecedores e associação em contas a pagar
feat(db): Flyway V7 — purchase_type em property_sales
feat(backend): forma de compra e entrada condicional nas vendas
feat(frontend): combobox CMDK pesquisável em vendas
feat(backend): hierarquia Empreendimento→Quadra→Lote (Flyway V8)
feat(frontend): cadastro em cascata de imóveis (DevelopmentsPage)
feat(frontend): tela de vendas com lote esperado, edição e contrato
fix: dark mode em parcelas, sugestões e cards de clientes
feat(frontend): sidebar retrátil e drawer mobile
docs: atualização completa (README, API, ARCHITECTURE, DEPLOY, TODO)
```

## Próximos commits sugeridos

```
feat(backend): conciliação parcial (1 transação → N lançamentos)
feat(frontend): mapa visual de lotes por quadra (grid com status)
feat(backend): aplicação automática de juros/multa no atraso
feat(backend): refresh token e revogação por JTI
feat(frontend): drill-down nos gráficos do dashboard
test: cobertura dos serviços críticos (reconciliação, parcelas, cascade)
chore: code-splitting do bundle (Recharts lazy loading)
```

## Dica de workflow antes de push

```bash
# 1. Compilar backend
cd backend && mvn compile

# 2. Build do frontend
cd ../frontend && npm run build

# 3. Testar stack local (backend + PG local + frontend dev)
cd ../backend && mvn spring-boot:run &
cd ../frontend && npm run dev

# 4. Validar funcionalidades críticas (login, cascade, vendas, conciliação)

# 5. Fazer push — Render redeploya automaticamente em ~5 min
git add -p && git commit -m "feat: ..." && git push
```
