# Deploy (POC — gratuito/baixo custo)

## 1. Banco — Supabase

1. Crie um projeto em https://supabase.com.
2. Em **Settings → Database**, copie a *connection string* (modo `URI`).
3. Converta para JDBC, por exemplo:
   ```
   jdbc:postgresql://db.<ref>.supabase.co:5432/postgres?sslmode=require
   ```
4. Defina no backend:
   ```
   SPRING_DATASOURCE_URL=jdbc:postgresql://db.<ref>.supabase.co:5432/postgres?sslmode=require
   SPRING_DATASOURCE_USERNAME=postgres
   SPRING_DATASOURCE_PASSWORD=<senha>
   ```
   O Flyway aplica todas as migrations no primeiro start.

> Em produção, considere remover/ajustar a migration `V3` (dados de demonstração).

## 2. Backend — Render (exemplo)

- **New → Web Service** apontando para o repositório, root `backend/`.
- Ambiente **Docker** (usa `backend/Dockerfile`).
- Variáveis de ambiente: `SPRING_DATASOURCE_*`, `JWT_SECRET`, `JWT_EXPIRATION_MS`,
  `APP_CORS_ALLOWED_ORIGINS` (inclua o domínio da Vercel), `APP_ADMIN_*`, `MAIL_*`.
- Porta: `8080`. Health check: `/actuator/health` (ou `/swagger-ui.html`).

`render.yaml` mínimo:
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
        value: https://seu-app.vercel.app
```

> Railway e Fly.io seguem o mesmo princípio (Docker + variáveis de ambiente).

## 3. Frontend — Vercel

- **Import Project**, root directory `frontend/`.
- Framework: **Vite**. Build: `npm run build`. Output: `dist`.
- Variável de ambiente: `VITE_API_BASE_URL=https://<backend-publico>/api`.
- Após o deploy, adicione o domínio gerado em `APP_CORS_ALLOWED_ORIGINS` no backend.

## 4. Checklist pós-deploy

- [ ] `POST /api/auth/login` retorna token.
- [ ] Frontend carrega `/settings/public` (branding) sem erro de CORS.
- [ ] Upload de extrato e conciliação funcionando.
- [ ] `JWT_SECRET` forte e único (nunca o default).
- [ ] SMTP configurado e `MAIL_ENABLED=true` caso queira enviar e-mails de verdade.

## Migração para VM própria

A aplicação não depende de recursos específicos do Supabase. Para migrar:
basta apontar `SPRING_DATASOURCE_*` para o PostgreSQL da VM (mesma versão major)
e subir o `docker-compose.yml` (ou apenas backend+frontend) na máquina. O Flyway
recria o schema; restaure os dados via `pg_dump`/`pg_restore`.
