# Estratégia de versionamento

## Branches

- `main` — sempre estável/deployável (CD direto para Render/Vercel).
- `develop` — integração das features.
- `feature/<nome>` — uma feature por branch, parte de `develop` ou `main`.

Fluxo recomendado para features com migrations: `feature/*` → PR → CI verde → merge → Render redeploya automaticamente.

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
feat(backend): juros/multa por atraso + drill-down dashboard + mapa de lotes (V9)
feat(backend): Contas a Pagar vinculadas a empreendimento (V10)
feat(backend): plano de contas estruturado — Categorias + Centros de Custo FK (V11)
feat(backend): paginação e filtros server-side nas listagens de maior volume
feat(backend): paginação server-side — Vendas, Fornecedores e Transações bancárias
ci: GitHub Actions — mvn test (backend) + npm run build (frontend)
test: testes do núcleo financeiro e smoke E2E
feat(backend): DRE em base caixa + export CSV (V12)
feat(backend): notificações por e-mail + SMTP configurável pela aplicação
feat: licenciamento Fase 1 — módulos + licença + LicensingContext + ModuleGuard (V13)
feat: licenciamento Fase 2 — permissões por módulo, perfis de acesso, chave HMAC,
      enforcement backend (V14) — PR #1
fix: health check do Render trocado para /api/settings/public
docs: atualização completa pós-Fase 2 (API, ARCHITECTURE, DEPLOY, GIT)
```

## Próximos commits sugeridos

```
# Fase 3 — Provisionamento de VM por cliente
feat(ops): script de provisionamento de nova VM (Docker + banco + chave)
feat(ci): deploy automatizado nas N VMs (image push + pull trigger)

# Fase 4 — Módulos Premium
feat(backend): Portal do Cliente — autenticação separada (jwt comprador)
feat(backend): Portal do Cliente — tela de parcelas, contrato e 2ª via
feat(backend): integração Asaas — boleto/PIX + webhook de baixa automática
feat(backend): correção monetária (INCC/IGPM) nas parcelas
feat(backend): assinatura eletrônica de contratos

# Fase 5 — App de gestão de licenças (2º sistema)
feat(license-app): painel do fornecedor — geração e gestão de chaves
feat(license-app): inventário de VMs/clientes + controle de vencimento

# Qualidade
test: integração com Testcontainers (Postgres) — migrations + reconciliação
feat(backend): health check dedicado /actuator/health com detalhe de banco
feat(frontend): geração de tipos TypeScript a partir do OpenAPI
feat(frontend): combobox server-side para Cliente/Lote em Vendas
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

# 4. Validar funcionalidades críticas (login, cascade, vendas, conciliação, licença)

# 5. Smoke test E2E
pwsh scripts/smoke.ps1

# 6. Para features com migrations: usar PR em vez de push direto em main
git checkout -b feat/minha-feature
git push -u origin feat/minha-feature
gh pr create --title "feat: minha feature" --body "..."
gh pr checks <numero> --watch
gh pr merge <numero> --merge --delete-branch
```

> **Migrations que apagam dados de permissão (como V14)** exigem deploy atômico —
> o novo código e a migration devem subir juntos via PR/merge → Render redeploya
> como uma unidade (build Docker → Flyway → novo container).
