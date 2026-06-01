# Deploy (POC — gratuito/baixo custo)

URLs de produção atuais:
- **Frontend:** https://erp-construtora-three.vercel.app
- **Backend:** https://erp-construtora-backend.onrender.com
- **Swagger:** https://erp-construtora-backend.onrender.com/swagger-ui.html

---

## 1. Banco — Supabase

1. Crie um projeto em https://supabase.com.
2. Em **Settings → Database**, copie a connection string do **Session pooler**
   (não use a conexão direta — ela é IPv6-only e pode falhar em ambientes como o Render).
3. O host do Session pooler tem o formato:
   ```
   aws-0-<region>.pooler.supabase.com:5432
   ```
4. Converta para JDBC:
   ```
   jdbc:postgresql://aws-0-<region>.pooler.supabase.com:5432/postgres?sslmode=require
   ```
5. O usuário do Session pooler tem o formato `postgres.<ref>` (ex.: `postgres.aqyqpxcuaovukdkacxxm`).
6. Defina no backend:
   ```
   SPRING_DATASOURCE_URL=jdbc:postgresql://aws-0-<region>.pooler.supabase.com:5432/postgres?sslmode=require
   SPRING_DATASOURCE_USERNAME=postgres.<ref>
   SPRING_DATASOURCE_PASSWORD=<senha-do-projeto>
   ```

> O Flyway aplica todas as migrations (V1–V8) no primeiro start.  
> Em produção, considere remover ou adaptar as migrations V3 e V5 (dados de demonstração).

---

## 2. Backend — Render

- **New → Web Service** apontando para o repositório, root `backend/`.
- Ambiente **Docker** (usa `backend/Dockerfile`).
- Variáveis de ambiente obrigatórias:

| Variável | Valor |
|----------|-------|
| `SPRING_DATASOURCE_URL` | JDBC URL do Supabase Session pooler |
| `SPRING_DATASOURCE_USERNAME` | `postgres.<ref>` |
| `SPRING_DATASOURCE_PASSWORD` | Senha do projeto Supabase |
| `JWT_SECRET` | Segredo forte (`openssl rand -base64 48`) |
| `JWT_EXPIRATION_MS` | `86400000` (24h) ou conforme necessidade |
| `APP_CORS_ALLOWED_ORIGINS` | URL do Vercel + `http://localhost:5173` (desenvolvimento) |
| `APP_ADMIN_EMAIL` | E-mail do admin inicial |
| `APP_ADMIN_PASSWORD` | Senha forte do admin |
| `MAIL_ENABLED` | `false` no POC (apenas loga; `true` para SMTP real) |

`render.yaml` mínimo (já existe na raiz do repositório):
```yaml
services:
  - type: web
    name: construtora-backend
    env: docker
    dockerfilePath: backend/Dockerfile
    dockerContext: backend
    envVars:
      - key: SPRING_DATASOURCE_URL
        sync: false
      - key: SPRING_DATASOURCE_USERNAME
        sync: false
      - key: SPRING_DATASOURCE_PASSWORD
        sync: false
      - key: JWT_SECRET
        generateValue: true
      - key: APP_CORS_ALLOWED_ORIGINS
        value: https://erp-construtora-three.vercel.app
      - key: APP_ADMIN_EMAIL
        sync: false
      - key: APP_ADMIN_PASSWORD
        sync: false
```

> Railway e Fly.io seguem o mesmo princípio (Docker + variáveis de ambiente).

---

## 3. Frontend — Vercel

- **Import Project**, root directory `frontend/`.
- Framework detectado automaticamente como **Vite**.
- Build command: `npm run build`. Output directory: `dist`.
- Variável de ambiente:
  ```
  VITE_API_BASE_URL=https://erp-construtora-backend.onrender.com/api
  ```
- O arquivo [`frontend/vercel.json`](../frontend/vercel.json) já define o rewrite
  de SPA (todas as rotas → `index.html`) para que o React Router funcione corretamente.
- Após o deploy, **adicione o domínio Vercel gerado** em `APP_CORS_ALLOWED_ORIGINS`
  no backend (Render) e faça um redeploy ou restart do serviço.

---

## 4. Checklist pós-deploy

- [ ] `POST /api/auth/login` retorna token (teste via Swagger ou curl).
- [ ] Frontend carrega `GET /api/settings/public` (branding) sem erro de CORS.
- [ ] Flyway aplicou todas as migrations V1–V8 (verifique nos logs do Render).
- [ ] Upload de extrato CSV → transações aparecendo como pendentes.
- [ ] Conciliação de uma transação funciona de ponta a ponta.
- [ ] `JWT_SECRET` forte e único (nunca o default/generateValue default).
- [ ] `APP_ADMIN_PASSWORD` trocada por senha forte e única.
- [ ] SMTP configurado e `MAIL_ENABLED=true` se quiser e-mails reais.

---

## 5. Migração para VM própria

A aplicação não depende de recursos específicos do Supabase. Para migrar:
- Aponte `SPRING_DATASOURCE_*` para o PostgreSQL da VM.
- O Flyway recria o schema; restaure os dados via `pg_dump`/`pg_restore`.
- Use `docker compose up --build` com o `docker-compose.yml` da raiz para subir
  tudo em uma única VM (banco + backend + frontend via Nginx).

---

## 6. Desenvolvimento local com banco local

```bash
# Cria role, banco e extensão pgcrypto (superusuário postgres)
psql -U postgres -h localhost -f database/init-local.sql

# Backend aponta para localhost por padrão (application.properties)
cd backend && mvn spring-boot:run

# Frontend
cd frontend && cp .env.example .env.local
# Edite: VITE_API_BASE_URL=http://localhost:8080/api
npm run dev
```

> **Dica de validação antes de push:** compile (`mvn compile`), build do frontend
> (`npm run build`) e teste as funcionalidades críticas no stack local antes de
> fazer push — evita descobrir erros após o redeploy do Render (que pode levar
> vários minutos).
