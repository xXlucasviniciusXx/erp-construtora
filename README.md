# Construtora Financeiro

Aplicação web (POC) de **controle financeiro e conciliação bancária** para uma
contabilidade que atende uma construtora. Cobre conciliação bancária (módulo
principal), contas a pagar/receber, vendas de imóveis com geração de parcelas,
cadastro de clientes e imóveis, geração de contratos, notificações por e-mail,
controle de acesso por perfil e personalização visual.

> **Status:** POC funcional. Os módulos essenciais estão implementados de ponta a
> ponta; itens avançados estão marcados como `TODO` neste README e no código.

---

## 1. Visão geral

| Camada     | Tecnologia |
|------------|------------|
| Frontend   | React + TypeScript + Vite + Tailwind + TanStack Query |
| Backend    | Java 21 + Spring Boot 3 (Web, Security, Data JPA, Validation, Mail) |
| Auth       | JWT (stateless) + RBAC por perfil/permissão |
| Banco      | PostgreSQL (Supabase no POC) + Flyway (migrations) |
| Docs API   | Swagger / OpenAPI (springdoc) |
| Contêineres| Docker Compose (db + backend + frontend) |

A comunicação é 100% via **API REST**, de forma que o frontend pode ser trocado
ou expandido sem alterar a regra de negócio do backend.

```
construtora-financeiro/
├── backend/      # Spring Boot (Controller/Service/Repository/DTO/Entity/Mapper/...)
├── frontend/     # React + Vite + TS
├── database/     # README de modelagem + arquivos de exemplo (CSV/OFX)
├── docs/         # Documentação técnica (arquitetura, API, deploy)
├── .env.example  # Variáveis de ambiente de referência
├── docker-compose.yml
└── README.md
```

---

## 2. Tecnologias usadas

**Backend:** Spring Boot, Spring Security, JWT (jjwt), Spring Data JPA, Bean
Validation, Flyway, springdoc-openapi, Flying Saucer (PDF), Lombok.

**Frontend:** React 18, TypeScript, Vite, Tailwind CSS, React Router, TanStack
Query, React Hook Form, Zod, Axios, lucide-react.

**Infra:** PostgreSQL 16, Docker, Nginx (serve o build do frontend).

---

## 3. Como rodar localmente

### Opção A — Docker Compose (recomendado)

```bash
cp .env.example .env          # ajuste o que quiser; os defaults já funcionam
docker compose up --build
```

- Frontend: http://localhost:5173
- Backend:  http://localhost:8080
- Swagger:  http://localhost:8080/swagger-ui.html
- Banco:    localhost:5432 (user/senha do `.env`)

### Opção B — Sem Docker

**Pré-requisitos:** JDK 21, Maven 3.9+, Node 20+, PostgreSQL 16.

```bash
# 1) Banco
createdb construtora    # ou crie via psql/Supabase

# 2) Backend
cd backend
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/construtora
export SPRING_DATASOURCE_USERNAME=construtora
export SPRING_DATASOURCE_PASSWORD=construtora
export JWT_SECRET=$(openssl rand -base64 48)
mvn spring-boot:run        # roda Flyway e cria o admin inicial

# 3) Frontend (outro terminal)
cd frontend
cp .env.example .env        # confira VITE_API_BASE_URL
npm install
npm run dev
```

> No Windows (PowerShell), use `$env:VAR = "valor"` em vez de `export`.

### Login inicial

O usuário ADMIN é criado no primeiro start a partir das variáveis
`APP_ADMIN_EMAIL` / `APP_ADMIN_PASSWORD` (default `admin@construtora.com.br` /
`Admin@123`). A migration `V3` ainda popula dados de demonstração (clientes,
imóveis, contas) para o dashboard e a conciliação já nascerem com conteúdo.

---

## 4. Variáveis de ambiente

Veja [`.env.example`](.env.example) (raiz) e
[`frontend/.env.example`](frontend/.env.example). Principais:

| Variável | Descrição |
|----------|-----------|
| `SPRING_DATASOURCE_URL/USERNAME/PASSWORD` | Conexão PostgreSQL/Supabase |
| `JWT_SECRET` | Segredo HMAC ≥ 256 bits (gere com `openssl rand -base64 48`) |
| `JWT_EXPIRATION_MS` | Validade do token (default 24h) |
| `APP_CORS_ALLOWED_ORIGINS` | Origens liberadas (frontend local + Vercel) |
| `APP_ADMIN_EMAIL/PASSWORD` | Admin inicial (seed idempotente) |
| `MAIL_*` / `MAIL_ENABLED` | SMTP das notificações (desligado no POC: apenas loga) |
| `VITE_API_BASE_URL` | URL da API consumida pelo frontend |

---

## 5. Como fazer deploy (POC gratuito)

Detalhes em [`docs/DEPLOY.md`](docs/DEPLOY.md). Resumo:

- **Banco → Supabase:** crie o projeto, pegue a connection string e aponte
  `SPRING_DATASOURCE_*`. O Flyway cria todo o schema no primeiro start.
- **Backend → Render/Railway/Fly.io:** build via `backend/Dockerfile`, configure
  as variáveis de ambiente. Exemplo de `render.yaml` em `docs/`.
- **Frontend → Vercel:** root `frontend/`, build `npm run build`, output `dist`,
  variável `VITE_API_BASE_URL` apontando para o backend público. Lembre de
  incluir o domínio da Vercel em `APP_CORS_ALLOWED_ORIGINS`.

---

## 6. Estrutura de pastas (backend)

```
backend/src/main/java/com/construtora/financeiro/
├── config/        # OpenAPI, scheduling
├── security/      # JWT, filtro, SecurityConfig, UserDetails
├── exception/     # GlobalExceptionHandler + exceções de domínio
├── model/         # @Entity + enums
├── repository/    # Spring Data JPA
├── dto/           # Records de entrada/saída por módulo
├── mapper/        # Conversão entity <-> DTO
├── service/       # Regra de negócio (inclui reconciliation/, contract/, report/)
├── parser/        # Parsers de extrato (CSV, OFX) — extensível
├── scheduler/     # Job de parcelas em atraso
├── controller/    # Endpoints REST
└── bootstrap/     # Seed do admin inicial
```

---

## 7. Módulos e prioridade de implementação

Implementados de ponta a ponta (prioridade do POC):

1. ✅ Login e perfis (JWT, 5 perfis, RBAC por permissão)
2. ✅ Clientes (CRUD + busca)
3. ✅ Imóveis / lotes (CRUD + status)
4. ✅ Vendas e parcelas (gera parcelas automaticamente)
5. ✅ Contas a pagar / receber (CRUD + baixa)
6. ✅ Importação CSV (+ **OFX** já funcional)
7. ✅ Conciliação bancária (sugestão automática com score, manual, desfazer, histórico)
8. ✅ Dashboard (indicadores do mês)
9. ✅ Configurações de tema/logo/empresa
10. ✅ Contratos (HTML + PDF), Notificações (registro + SMTP), Relatórios (CSV)

Veja o backlog de evolução em [`docs/TODO.md`](docs/TODO.md).

---

## 8. Documentação da API (Swagger)

Com o backend rodando: **http://localhost:8080/swagger-ui.html**
(JSON em `/v3/api-docs`). Autentique em `POST /api/auth/login`, copie o `token` e
use o botão **Authorize** (Bearer).

Lista de endpoints e exemplos em [`docs/API.md`](docs/API.md).

---

## 9. Boas práticas utilizadas

- Camadas separadas (Controller → Service → Repository) e DTOs isolando entidades.
- Senhas com **BCrypt**; **JWT** com expiração; CORS restrito por configuração.
- **Bean Validation** nas entradas; tratamento global de exceções com payload padrão.
- Schema versionado por **Flyway** (`ddl-auto: validate`, nunca `update`).
- Segredos fora do código (`.env` / variáveis de ambiente).
- RBAC no backend (`@PreAuthorize`) **e** proteção de rotas no frontend.
- Camada de **parser plugável** para novos formatos de extrato.
- Logs estruturados e auditoria preparada (`audit_logs`).

---

## 10. Fluxo de demonstração sugerido

1. Login como admin.
2. **Importar Extrato** → conta "Conta Movimento" → suba `database/samples/extrato-exemplo.csv`.
3. **Conciliação** → veja as transações pendentes → clique **Sugestões** → **Conciliar**
   o crédito de R$ 5.000 com a conta a receber "Sinal de reserva".
4. **Vendas** → crie uma venda usando um imóvel disponível → veja as parcelas geradas → baixe o contrato.
5. **Configurações** → troque a cor primária e veja o tema mudar.

---

## 11. Versionamento / GitHub

- Branches sugeridas: `main` (estável), `develop` (integração), `feature/*`.
- Sugestão de commits iniciais em [`docs/GIT.md`](docs/GIT.md).
