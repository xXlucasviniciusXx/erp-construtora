# Estratégia de versionamento

## Branches

- `main` — sempre estável/deployável.
- `develop` — integração das features.
- `feature/<nome>` — uma feature por branch, parte de `develop`.

Fluxo: `feature/*` → PR para `develop` → quando estável, `develop` → `main`.

## Sugestão de commits iniciais

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
docs: README, arquitetura, API, deploy e backlog
```

## Inicializando

```bash
git init
git add .
git commit -m "chore: estrutura inicial do monorepo (backend, frontend, database, docs)"
git branch -M main
git checkout -b develop
# git remote add origin <url> && git push -u origin main develop
```
