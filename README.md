# ERP Construtora — Controle Financeiro

Aplicação web (POC) de **controle financeiro e conciliação bancária** para uma
contabilidade que atende uma construtora. Cobre conciliação bancária (módulo
principal), contas a pagar/receber, gestão hierárquica de imóveis
(Empreendimento → Quadra → Lote), vendas com geração automática de parcelas,
cadastro de clientes, fornecedores e centros de custo, geração de contratos
PDF/HTML, relatórios CSV, controle de acesso por perfil (RBAC) e personalização
visual com dark mode.

> **Status:** POC funcional — todos os módulos essenciais implementados de ponta
> a ponta. Itens avançados estão no backlog em [`docs/TODO.md`](docs/TODO.md).

URLs de produção (POC):
- **Frontend:** https://erp-construtora-three.vercel.app
- **Backend / Swagger:** https://erp-construtora-backend.onrender.com/swagger-ui.html

---

## 1. Visão geral

| Camada     | Tecnologia |
|------------|------------|
| Frontend   | React 18 + TypeScript + Vite + Tailwind CSS (dark mode) + TanStack Query + Recharts |
| Backend    | Java 21 + Spring Boot 3.3.4 (Web, Security, Data JPA, Validation, Mail) |
| Auth       | JWT (stateless) + RBAC por perfil/permissão |
| Banco      | PostgreSQL 15+ (Supabase no POC) + Flyway V1–V8 |
| Docs API   | Swagger / OpenAPI (springdoc) |
| Contêineres| Docker (Render blueprint) |

A comunicação é 100% via **API REST**, de forma que o frontend pode ser trocado
ou expandido sem alterar a regra de negócio do backend.

```
erp-construtora/
├── backend/      # Spring Boot (Controller/Service/Repository/DTO/Entity/Mapper/...)
├── frontend/     # React + Vite + TS
├── database/     # README de modelagem + arquivos de exemplo (CSV/OFX)
├── docs/         # Documentação técnica (API, arquitetura, deploy, git, backlog)
├── .env.example  # Variáveis de ambiente de referência
├── render.yaml   # Blueprint de deploy no Render
└── README.md
```

---

## 2. Tecnologias usadas

**Backend:** Spring Boot 3.3.4, Spring Security 6, JWT (jjwt), Spring Data JPA +
JdbcTemplate (agregações do dashboard via SQL nativo), Bean Validation, Flyway,
springdoc-openapi, Flying Saucer / OpenPDF (geração de contrato PDF), Lombok 1.18.38.

**Frontend:** React 18, TypeScript, Vite, Tailwind CSS (dark mode via `class`),
React Router 6, TanStack Query, React Hook Form + Zod, Axios, lucide-react,
**Recharts** (10 gráficos analíticos).

**Integrações:** **BrasilAPI** — consulta automática de CEP e CNPJ no cadastro de
clientes (frontend-only, fallback manual se falhar).

**Infra:** PostgreSQL 15+ (testado no 18), Docker (Render), Vercel (SPA), Supabase.

> **JDK 25:** o `pom.xml` já força `lombok.version=1.18.38` para compatibilidade.
> Nenhuma ação extra necessária.

---

## 3. Módulos implementados

| Módulo | Tela | Destaque |
|--------|------|----------|
| **Dashboard** | `/` | 8 cards + 12 gráficos (Recharts) com filtros e drill-down; inclui despesas e lucro/prejuízo por empreendimento |
| **Clientes** | `/clients` | Cadastro PF/PJ, CEP/CNPJ automático (BrasilAPI), inativação com bloqueio por débitos, menu ⋮ |
| **Imóveis / Lotes** | `/properties` | Hierarquia 3 níveis: Empreendimento → Quadra → Lote; códigos automáticos (E001-Q01-L001); valores derivados; limites em cascata |
| **Vendas** | `/sales` | Combobox pesquisável (CMDK) para Cliente e Lote; valor esperado read-only; entrada condicional; edição de venda; contrato PDF/HTML |
| **Contas a Pagar** | `/payable` | Lançamentos com 3 dimensões (FK): **categoria** (plano de contas), **centro de custo** (área) e **empreendimento** (opcional); confirmar pagamento, cancelar, filtros |
| **Contas a Receber** | `/receivable` | Lançamentos manuais, confirmar recebimento, filtros |
| **Parcelas** | (aba em Contas a Receber) | Dados do cliente, baixa de parcela, filtros por status/vencimento/nome |
| **Fornecedores** | `/suppliers` | CRUD com busca textual |
| **Conciliação** | `/reconciliation` | Sugestão por score, conciliação manual/automática, desfazer, histórico |
| **Importar Extrato** | `/import` | Upload CSV/OFX; parsers plugáveis |
| **DRE** | `/dre` | Demonstração do Resultado (base caixa): receitas − despesas = resultado, por período e empreendimento, com margem e export CSV |
| **Relatórios** | `/reports` | Relatórios exportáveis em CSV (incl. despesas por categoria/centro/empreendimento) |
| **Usuários** | `/users` | CRUD de usuários com perfis RBAC (ADMIN only) |
| **Configurações** | `/settings` | Branding, dados da empresa, dark/light; abas de **Categorias**, **Centros de Custo** e contas bancárias |

---

## 4. Hierarquia de imóveis (Empreendimento → Quadra → Lote)

A migration **V8** transformou a tabela plana `properties` em uma estrutura em
3 níveis com códigos internos gerados automaticamente:

```
Empreendimento  E001  (ex.: Parque das Águas)
└── Quadra      E001-Q01  (ex.: Quadra A)
    ├── Lote    E001-Q01-L001  (ex.: Lote 01)
    └── Lote    E001-Q01-L002
```

**Regras de negócio:**
- O empreendimento define os limites máximos de quadras e lotes.
- Tentar criar além dos limites retorna HTTP 400 (BusinessException).
- Os campos **previsto total** e **recebido** são calculados automaticamente dos
  lotes (não é possível editá-los diretamente).
- Ao registrar uma venda, `lot.saleValue` e `lot.status = SOLD` são atualizados
  automaticamente; ao excluir a venda, o lote volta para `AVAILABLE`.

---

## 5. Perfis de acesso (RBAC)

| Perfil | Permissões |
|--------|-----------|
| `ADMIN` | Todas as permissões + `USERS_MANAGE` + `SETTINGS_MANAGE` |
| `FINANCEIRO` | `PAYABLE_WRITE`, `RECEIVABLE_WRITE`, `RECONCILIATION_WRITE`, `REPORTS_EXPORT` |
| `CONTABILIDADE` | `RECONCILIATION_VALIDATE`, `REPORTS_EXPORT`, `READ` |
| `COMERCIAL` | `CLIENTS_WRITE`, `PROPERTIES_WRITE`, `SALES_WRITE`, `CONTRACTS_GENERATE` |
| `VISUALIZADOR` | `READ` |

---

## 6. Como rodar localmente

### Sem Docker

**Pré-requisitos:** JDK 21+ (testado com OpenJDK 25), Maven 3.9+, Node 20+,
PostgreSQL 15+ (testado no 18).

```bash
# 1) Banco local (rode como superusuário postgres)
psql -U postgres -h localhost -f database/init-local.sql
# Cria role construtora / banco construtora / extension pgcrypto

# 2) Backend
cd backend
# Windows PowerShell: $env:JWT_SECRET = (openssl rand -base64 48)
export JWT_SECRET=$(openssl rand -base64 48)
mvn spring-boot:run
# Flyway aplica V1–V8 automaticamente.
# API: http://localhost:8080 | Swagger: http://localhost:8080/swagger-ui.html

# 3) Frontend (outro terminal)
cd frontend
cp .env.example .env.local    # VITE_API_BASE_URL=http://localhost:8080/api
npm install && npm run dev
# App: http://localhost:5173
```

### Login inicial

| Campo | Valor padrão |
|-------|-------------|
| E-mail | `admin@construtora.com.br` |
| Senha | `Admin@123` |

O usuário ADMIN é criado idempotente no primeiro start pelas variáveis
`APP_ADMIN_EMAIL` / `APP_ADMIN_PASSWORD`. A migration V3/V5 popula dados de
demonstração (clientes, empreendimentos, vendas, contas, extrato bancário).

---

## 7. Variáveis de ambiente

| Variável | Descrição |
|----------|-----------|
| `SPRING_DATASOURCE_URL` | JDBC URL do PostgreSQL (Supabase: Session pooler porta 5432) |
| `SPRING_DATASOURCE_USERNAME` | Usuário do banco |
| `SPRING_DATASOURCE_PASSWORD` | Senha do banco |
| `JWT_SECRET` | Segredo HMAC ≥ 256 bits (`openssl rand -base64 48`) |
| `JWT_EXPIRATION_MS` | Validade do token em ms (default 86400000 = 24h) |
| `APP_CORS_ALLOWED_ORIGINS` | Origens permitidas (frontend local + Vercel, separadas por vírgula) |
| `APP_ADMIN_EMAIL` | E-mail do admin inicial |
| `APP_ADMIN_PASSWORD` | Senha do admin inicial |
| `MAIL_HOST/PORT/USERNAME/PASSWORD` | Configuração SMTP |
| `MAIL_ENABLED` | `true` para enviar e-mails; `false` apenas loga (default no POC) |
| `VITE_API_BASE_URL` | URL base da API consumida pelo frontend |

---

## 8. Deploy (POC gratuito)

Detalhes em [`docs/DEPLOY.md`](docs/DEPLOY.md). Resumo:

- **Banco → Supabase:** use a connection string do **Session pooler** (IPv4 compatível,
  porta 5432). O Flyway cria todo o schema no primeiro start.
- **Backend → Render:** existe um [`render.yaml`](render.yaml) na raiz (Blueprint).
  *New → Blueprint* apontando para o repo; preencha as variáveis `sync: false`.
- **Frontend → Vercel:** root `frontend/`. O [`frontend/vercel.json`](frontend/vercel.json)
  define build/output e o rewrite de SPA. Defina `VITE_API_BASE_URL` e adicione o
  domínio Vercel em `APP_CORS_ALLOWED_ORIGINS` no backend.

---

## 9. Estrutura de pastas (backend)

```
backend/src/main/java/com/construtora/financeiro/
├── config/        # OpenAPI, scheduling
├── security/      # JWT, filtro, SecurityConfig, UserDetails
├── exception/     # GlobalExceptionHandler + exceções de domínio
├── model/         # @Entity + enums (Development, Block, Lot, PropertySale…)
├── repository/    # Spring Data JPA
├── dto/           # Records de entrada/saída por módulo
├── mapper/        # Conversão entity <-> DTO
├── service/       # Regra de negócio (reconciliation/, contract/, report/, dashboard/)
├── parser/        # Parsers de extrato (CSV, OFX) — extensível
├── scheduler/     # Job de atualização de parcelas em atraso
├── controller/    # 20 REST controllers
└── bootstrap/     # Seed do admin inicial
```

---

## 10. Migrações de banco (Flyway)

| Versão | Conteúdo |
|--------|----------|
| V1 | Schema inicial completo |
| V2 | Seed: perfis, permissões, usuário admin |
| V3 | Seed: dados de demonstração básicos |
| V4 | Coluna `notes` em `bank_transactions` |
| V5 | Seed: dados de demonstração ampliados (vendas, parcelas, contas) |
| V6 | Tabelas `suppliers` e `cost_centers` |
| V7 | Coluna `purchase_type` em `property_sales` |
| V8 | Hierarquia de imóveis: cria `developments` e `blocks`; renomeia `properties` → `lots`; gera códigos internos hierárquicos (E001-Q01-L001); vincula vendas ao novo campo `lot_id` |
| V9 | Define taxas de encargos (juros 1% a.m. + multa 2%) nas vendas demo zeradas (base para o cálculo de juros/multa por atraso) |
| V10 | Vínculo `accounts_payable.development_id` (FK nullable) + seeds de despesas de obra por empreendimento |
| V11 | Plano de contas: cria `categories` (grupo → item, ~70 seeds), adiciona `grupo` a `cost_centers`, converte `accounts_payable.category`/`cost_center` em FK com backfill dos dados antigos |

---

## 11. Testes e CI

- **Testes unitários** do núcleo financeiro (`backend/src/test`): `LateFeeCalculator`
  (juros/multa) e geração de parcelas/entrada — rode com `mvn -f backend/pom.xml test`.
- **CI** (`.github/workflows/ci.yml`): a cada push/PR roda os testes do backend e o
  build do frontend.
- **Smoke test** end-to-end via API (`scripts/smoke.ps1`): com o backend no ar,
  valida cadastros, vínculos FK, encargos, parcelas, dashboard, DRE, conciliação e CSV.

## 12. Boas práticas

- Camadas separadas (Controller → Service → Repository) com DTOs isolando entidades.
- Senhas com **BCrypt**; **JWT** com expiração configurável; CORS restrito.
- **Bean Validation** nas entradas; `GlobalExceptionHandler` com payload padrão.
- Schema versionado por **Flyway** (`ddl-auto: validate`, nunca `update`).
- Segredos fora do código (`.env` / variáveis de ambiente no Render).
- RBAC no backend (`@PreAuthorize`) **e** proteção de rotas no frontend.
- Agregações analíticas em **SQL nativo** (PostgreSQL) isoladas em `DashboardAnalyticsService`.
- Camada de **parser plugável** para novos formatos de extrato (`@Component` + `BankStatementParser`).
- Auditoria financeira em `audit_logs`.

---

## 13. Fluxo de demonstração sugerido

1. Login como admin em https://erp-construtora-three.vercel.app.
2. **Imóveis / Lotes** → veja os 3 empreendimentos demo com quadras e lotes;
   crie um novo empreendimento e adicione quadras/lotes respeitando os limites.
3. **Vendas** → clique em "Nova venda", use o combobox para buscar um lote disponível;
   confira o "Valor esperado" preenchido automaticamente; gere o contrato PDF.
4. **Importar Extrato** → conta "Conta Movimento" → suba `database/samples/extrato-exemplo.csv`.
5. **Conciliação** → veja sugestões com score → **Conciliar** o crédito de R$ 5.000.
6. **Dashboard** → aplique filtro de período e veja os gráficos atualizarem.
7. **Configurações** → troque a cor primária e alterne dark/light mode.

---

## 14. Versionamento / GitHub

- Branches sugeridas: `main` (estável), `develop` (integração), `feature/*`.
- Estratégia de commits em [`docs/GIT.md`](docs/GIT.md).
- API REST completa em [`docs/API.md`](docs/API.md).
- Arquitetura detalhada em [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md).
