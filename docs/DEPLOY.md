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

> O Flyway aplica todas as migrations (V1–V14) no primeiro start.  
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
| `LICENSE_SECRET` | Segredo HMAC para chaves de licença (`openssl rand -base64 48`) |
| `MAIL_ENABLED` | `false` no POC (apenas loga; `true` para SMTP real) |

> **SMTP no Render free tier:** a porta 587 é bloqueada. Use **porta 2525** (MailerSend
> suporta ambas). Configure em Configurações → Notificações / E-mail na própria aplicação
> (não por env var — o SMTP é armazenado no banco).
> O remetente (`mailFrom`) deve ser um domínio verificado na conta MailerSend.

> `LICENSE_SECRET` deve ser **único por instalação** e nunca compartilhado. Chaves
> de licença geradas com um segredo são inválidas em outra VM com segredo diferente —
> esse é o mecanismo que evita que um cliente copie a chave de outro.

O `render.yaml` na raiz do repositório já define o blueprint completo, incluindo
o `healthCheckPath: /api/settings/public` (endpoint rápido, público, confirma app + banco).

> **Por que `/api/settings/public`?** O antigo `/v3/api-docs` varreia todos os
> controllers no cold start (10–20 s), estourando o timeout do free tier.
> O novo endpoint lê apenas 1 linha do banco e retorna em < 1 s.

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

## 4. Workflow de deploy com PR (recomendado para migrations)

Migrations que alteram permissões (como V14) precisam de **deploy atômico** — o
novo código e a migration devem subir juntos. O fluxo via PR garante isso:

```bash
# 1. Criar branch de feature
git checkout -b feat/minha-feature

# 2. Desenvolver, commitar, fazer push
git push -u origin feat/minha-feature

# 3. Abrir PR (GitHub CLI)
gh pr create --title "feat: minha feature" --body "Descrição"

# 4. Acompanhar CI
gh pr checks <numero> --watch

# 5. Fazer merge (Render redeploya automaticamente)
gh pr merge <numero> --merge --delete-branch

# 6. Acompanhar deploy
gh run list --limit 5
```

> O Render faz: **build Docker → Flyway → novo container ativo**. O container
> antigo só é desativado após o novo passar no health check. Portanto o código
> novo e a migration sempre sobem juntos — sem janela de inconsistência.

---

## 5. Checklist pós-deploy

- [ ] `POST /api/auth/login` retorna token (teste via Swagger ou curl).
- [ ] `GET /api/settings/public` retorna 200 sem autenticação (health check OK).
- [ ] Frontend carrega branding sem erro de CORS.
- [ ] Flyway aplicou todas as migrations V1–V14 (verifique nos logs do Render).
- [ ] `GET /api/licensing/me` retorna módulos e licença (Fase 1/2 OK).
- [ ] Configurações → Módulos & Licença: aba visível, preset de plano funciona.
- [ ] Configurações → Perfis de Acesso: perfis listados, ADMIN protegido.
- [ ] Upload de extrato CSV → transações aparecendo como pendentes.
- [ ] Conciliação de uma transação funciona de ponta a ponta.
- [ ] `JWT_SECRET` forte e único.
- [ ] `LICENSE_SECRET` forte e único (gerado em `openssl rand -base64 48`).
- [ ] `APP_ADMIN_PASSWORD` trocada por senha forte e única.
- [ ] SMTP configurado e `MAIL_ENABLED=true` se quiser e-mails reais.

---

## 6. Migração para VM própria (modelo VM-por-cliente)

Para cada novo cliente, a implantação segue:

1. Provisionar VM com Docker + PostgreSQL.
2. Clonar o repositório ou usar a imagem Docker publicada.
3. Configurar as variáveis de ambiente (banco, segredos, CORS, admin).
4. Executar `docker compose up --build` — Flyway inicializa o schema automaticamente.
5. Gerar a chave de licença no painel do fornecedor (Fase 5 — por ora use
   `POST /api/licensing/license/key/generate` na VM de demonstração).
6. Aplicar a chave em Configurações → Módulos & Licença da VM do cliente.

A aplicação não depende de recursos específicos do Supabase. Para migrar banco:
- Aponte `SPRING_DATASOURCE_*` para o PostgreSQL da VM.
- Restaure os dados via `pg_dump`/`pg_restore` se necessário.

---

## 7. Deploy em VPS/Hostinger (recomendado para produção por cliente)

Esta é a implantação preferida para o modelo **VM-por-cliente**: tudo roda numa
única VPS (banco + backend + frontend + SSL via Caddy).

### Requisitos mínimos na VPS
- Ubuntu 22.04 LTS (ou Debian 12)
- 2 vCPU / 2 GB RAM (Hostinger KVM 2 — ~R$ 30/mês por cliente)
- Docker + Docker Compose Plugin instalados

### Arquitetura

```
Internet (80/443) → Caddy (SSL Let's Encrypt)
                      ├── /api/*  →  backend:8080 (Spring Boot)
                      └── /*      →  frontend:80  (Nginx + React)
                                        banco:5432 (PostgreSQL, só rede interna)
```

### Passo a passo

**1. Instalar Docker na VPS**
```bash
curl -fsSL https://get.docker.com | sh
sudo usermod -aG docker $USER
newgrp docker
```

**2. Apontar o DNS do domínio para o IP da VPS**

No painel do registrador (Hostinger, Registro.br, etc.):
```
Tipo A   erp.minhaempresa.com.br   →   <IP-da-VPS>
```
> Aguarde a propagação (5–60 min) antes de prosseguir com o Caddy.

**3. Clonar o repositório e criar o `.env`**
```bash
git clone https://github.com/xXlucasviniciusXx/erp-construtora.git
cd erp-construtora
cp .env.example .env
nano .env
```

Preencha **pelo menos** estas variáveis:
```env
DOMAIN=erp.minhaempresa.com.br
POSTGRES_PASSWORD=senha_forte_do_banco
JWT_SECRET=$(openssl rand -base64 48)
LICENSE_SECRET=$(openssl rand -base64 48)
APP_ADMIN_EMAIL=admin@minhaempresa.com.br
APP_ADMIN_PASSWORD=SenhaForte@123
APP_CORS_ALLOWED_ORIGINS=https://erp.minhaempresa.com.br
```

**4. Subir o stack de produção**
```bash
docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d --build
```

> O Caddy obtém o certificado SSL automaticamente via Let's Encrypt.
> O Flyway inicializa o banco com todas as migrations V1–V14.
> Os três containers ficam com `restart: unless-stopped` — sobrevivem a reboots.

**5. Verificar os logs**
```bash
# Logs de todos os serviços
docker compose logs -f

# Só o Caddy (ver se o SSL foi emitido)
docker compose logs caddy

# Só o backend (ver migrations do Flyway)
docker compose logs backend
```

**6. Aplicar a chave de licença**

Acesse `https://erp.minhaempresa.com.br` → Configurações → Módulos & Licença,
cole a chave HMAC gerada para esse cliente e clique em **Aplicar**.

**7. Atualizar o sistema (nova versão)**
```bash
git pull
docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d --build
```

> O Flyway aplica apenas as migrations novas (idempotente).
> O container antigo é substituído pelo novo sem downtime visível.

### Abrindo o firewall (se necessário)
```bash
sudo ufw allow 22/tcp    # SSH
sudo ufw allow 80/tcp    # HTTP (Caddy redireciona para HTTPS)
sudo ufw allow 443/tcp   # HTTPS
sudo ufw allow 443/udp   # HTTP/3
sudo ufw enable
```
> Portas 8080 e 5432 **não precisam** ser abertas — ficam apenas na rede interna Docker.

### Diferença local × produção

| | Desenvolvimento local | Produção VPS |
|---|---|---|
| Comando | `docker compose up` | `docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d` |
| SSL | Não | Sim (Caddy + Let's Encrypt) |
| Portas expostas | 5173 (front), 8080 (back), 5432 (db) | Apenas 80 e 443 (Caddy) |
| `VITE_API_BASE_URL` | `http://localhost:8080/api` | `https://<DOMAIN>/api` (injetado pelo override) |

---

## 8. Atualização da frota (imagens versionadas — modelo da frota)

> **Pergunta que isto resolve:** "tenho 5 clientes, preciso atualizar os 5?"
> As VMs **não** se atualizam sozinhas (cada uma é isolada), mas a imagem é a
> **mesma**: você builda **1 vez** (CI) e cada VM só **puxa** a imagem pronta. Dá
> pra atualizar todas com **um comando**.

### Como funciona
1. **Build 1x:** ao dar merge na `main` (ou empurrar uma tag `vX.Y.Z`), o workflow
   [`release-images.yml`](../.github/workflows/release-images.yml) builda e publica
   no **GHCR**: `ghcr.io/<owner>/erp-construtora-backend` e `...-frontend`, com as
   tags `latest`, `sha-<curto>` e `vX.Y.Z`.
2. **Deploy por VM:** cada cliente roda com [`docker-compose.registry.yml`](../docker-compose.registry.yml),
   que **puxa** a imagem em vez de compilar. Não há build na VM.
3. **Banco migra sozinho:** o Flyway aplica as migrations novas no boot do container
   — o banco de **cada** cliente é migrado automaticamente, sem SQL manual.

> O **frontend** é buildado no CI com `VITE_API_BASE_URL=/api` (relativo). Como o
> Caddy serve frontend e backend no **mesmo domínio**, a **mesma** imagem de
> frontend serve **todos** os clientes — não precisa rebuildar por domínio.

### Pré-requisito: acesso às imagens na VM
As imagens GHCR nascem **privadas**. Escolha:
- **Tornar públicas** (Packages → Package settings → Change visibility → Public), ou
- **Login na VM** com um PAT read-only: `docker login ghcr.io -u <user> -p <token>`.

### Primeira subida numa VM (registry)
```bash
git clone https://github.com/xXlucasviniciusXx/erp-construtora.git && cd erp-construtora
cp .env.example .env && nano .env          # DOMAIN, segredos, IMAGE_REGISTRY, APP_VERSION
docker compose -f docker-compose.registry.yml up -d
```

### Atualizar 1 VM
```bash
cd erp-construtora
docker compose -f docker-compose.registry.yml pull
docker compose -f docker-compose.registry.yml up -d
```

### Atualizar a frota TODA de uma vez
Crie o inventário `scripts/fleet.txt` (modelo em `scripts/fleet.example.txt`):
```
deploy@203.0.113.10
deploy@203.0.113.11:/opt/erp-construtora
root@erp.gama.com.br
```
Depois:
```bash
scripts/update-fleet.sh --canary        # atualiza só a 1ª VM; valide o cliente
scripts/update-fleet.sh                  # libera para o restante (tag 'latest')
scripts/update-fleet.sh --version v1.4.0 # ou pin numa release específica
```
O script faz `pull` + `up -d` em cada VM por SSH, confirma o backend de pé e
resume sucessos/falhas. (`scripts/fleet.txt` é git-ignored — contém IPs.)

### Alternativa "push e esquece": Watchtower
Para clientes que acompanham a tag móvel `latest`, suba o
[`docker-compose.watchtower.yml`](../docker-compose.watchtower.yml) junto:
```bash
docker compose -f docker-compose.registry.yml -f docker-compose.watchtower.yml up -d
```
O Watchtower checa o registry a cada 5 min e atualiza **só** backend/frontend
(marcados por label) quando sai imagem nova. Publicou → as VMs se atualizam sozinhas.

### Releases e rollback
- **Release pinada:** `git tag v1.4.0 && git push origin v1.4.0` → o CI publica
  `...:v1.4.0`. Cada VM com `APP_VERSION=v1.4.0` roda exatamente essa versão.
- **Rollback:** troque `APP_VERSION` para a tag anterior no `.env` e
  `docker compose -f docker-compose.registry.yml up -d`. (Atenção: migrations de
  banco não revertem sozinhas — rollback é seguro quando a versão anterior é
  compatível com o schema atual.)

### Build-from-source × registry
| | `docker-compose.prod.yml` (build) | `docker-compose.registry.yml` (pull) |
|---|---|---|
| Onde compila | na própria VM (`--build`) | 1x no CI; VM só baixa |
| Bom para | 1–2 clientes / sem registry | frota (vários clientes) |
| Atualizar | `git pull && up -d --build` | `update-fleet.sh` / Watchtower |

> **Gestão centralizada (futuro):** o controle de **quais versões existem**, a
> **liberação** de updates por cliente, o **licenciamento** e o **histórico de
> releases** ficarão no app separado de gestão de VPS/licenças (roadmap Fase 5).
> Este mecanismo (CI → GHCR → frota) é a base que aquele painel vai orquestrar.

---

## 9. Desenvolvimento local com banco local

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
> (`npm run build`) e rode o smoke test (`pwsh scripts/smoke.ps1`) antes de fazer
> push — evita descobrir erros após o redeploy do Render.
