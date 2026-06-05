# Backlog de evolução (pós-POC)

Itens deixados como `TODO` propositalmente para continuação futura.

---

## ✅ Concluído (histórico)

### Funcionalidades base
- Conciliação manual (qualquer lançamento, valor diferente), abas por status,
  reverter status, motivo de divergência, desfazer e histórico.
- Clientes: menu de ações ⋮ (visualizar/editar/inativar), consulta automática
  de CEP e CNPJ (BrasilAPI) com fallback manual, bloqueio de inativação com débitos.
- Parcelas: dados do cliente (nome, CPF/CNPJ, telefone) + filtros (q, status,
  período de vencimento); menu de ações com pagamento e geração de contrato.
- Contas a pagar: ícones confirmar/cancelar (✅/❌) com tooltip e confirmação;
  associação a fornecedor e centro de custo.
- Dashboard analítico: 8 cards + 10 gráficos Recharts; filtros de período/cliente/lote;
  totalizador de recebíveis.
- Vendas: forma de compra (`purchaseType`) e índice de correção como listas;
  entrada condicional (só "Entrada + parcelas"); edição de venda (PUT /sales/{id}).
- Fornecedores: CRUD completo (migration V6).
- Centros de custo: CRUD completo (migration V6).
- Auditoria (`audit_logs`) gravando ações financeiras.
- Dark mode completo em todas as telas.
- Menu lateral retrátil (collapsed/expanded) + mobile drawer.

### Encargos, dashboard e Contas a Pagar por empreendimento
- Juros/multa por atraso calculados em tempo real (taxas por venda), exibidos
  em parcelas/vendas e usados na conciliação manual (migration V9).
- Drill-down no dashboard (inadimplência por empreendimento, vendas por forma de compra).
- Mapa visual de lotes (grid colorido por status) dentro do empreendimento.
- Contas a Pagar vinculadas a empreendimento (FK opcional, V10): filtro,
  relatório CSV e indicadores de despesas e lucro/prejuízo (caixa) por empreendimento.

### Hierarquia de imóveis (Empreendimento → Quadra → Lote) — migration V8
- Modelo em 3 níveis substituindo a tabela plana `properties`.
- Códigos internos automáticos e hierárquicos (`E001-Q01-L001`).
- Valores derivados calculados (plannedTotal, receivedTotal, actualBlocks, actualLots).
- Limites em cascata (blocksCount, lotsCount) com bloqueio na criação.
- Integração com vendas: `lot.saleValue` e `lot.status` atualizados automaticamente.
- Tela de cadastro em cascata no frontend (DevelopmentsPage).
- Combobox CMDK pesquisável para Cliente e Lote na tela de Vendas.

---

## Conciliação

- [ ] Conciliação parcial / 1 transação ↔ N lançamentos.
- [ ] Auto-conciliar quando score ultrapassa limiar configurável.
- [ ] Tolerância de valor configurável (centavos) além de match exato.
- [ ] Match por similaridade textual da descrição do extrato.
- [ ] Suporte a múltiplas contas no mesmo arquivo OFX (`BANKACCTFROM`).
- [ ] CNAB 240/400 como novos parsers.

## Importação

- [ ] Validação de charset/encoding do arquivo antes do parse.
- [ ] Processamento assíncrono para arquivos grandes (fila + status).
- [ ] Armazenar o arquivo original (Supabase Storage/S3) além dos metadados.

## Contratos

- [x] **Numeração sequencial** de contrato (`CT-NNNNNN`) atribuída na venda; backfill
      das vendas existentes (V19). Exibida na lista/detalhe de Vendas e no PDF.
- [x] **Modelos persistidos e editáveis** (`contract_templates`, V19): CRUD com tipos
      `CONTRACT`/`DISTRATO`, um padrão por tipo (índice único parcial), corpo XHTML com
      tokens `{{...}}` substituídos no `ContractRenderer`.
- [x] **Editor visual (WYSIWYG)** do modelo (Configurações → Contratos, V20): editor
      estilo Word (TipTap) com barra de formatação, paleta de **tokens** com tooltip e
      inserção por clique, "Ver código HTML" (avançado), **pré-visualização** e
      sanitização de HTML inseguro. O `body` virou fragmento; o esqueleto XHTML + CSS e a
      normalização HTML→XHTML (jsoup) ficam no backend (`ContractHtml`).
- [x] **Distrato** (rescisão amigável): cancela a venda (preserva no histórico),
      registra data/motivo/devolução/retido e libera o lote; documento próprio em PDF.
- [x] **Arquivamento/versionamento dos PDFs** (`contract_documents`, V19): cada geração
      de contrato/distrato é arquivada e versionada; histórico baixável na tela de Vendas.
- [x] **Modelos por empreendimento + duplicar** (V21): `development_id` em
      `contract_templates` (null = global); na geração, o modelo do empreendimento da
      venda tem prioridade sobre o global. Ação **Duplicar** cria cópia no mesmo escopo.
- [ ] Assinatura eletrônica e aditivo contratual (Premium / Fase 4).

## Notificações

- [x] Envio real por SMTP **configurável pela aplicação** (Configurações →
      Notificações / E-mail); sender construído a partir do banco.
- [x] Templates **HTML** com a identidade do sistema.
- [x] **Lembrete de vencimento** (N dias antes, job diário) além do aviso de atraso.
- [x] **Histórico** de notificações (tela) com status, visualização e **reenvio**.
- [x] **E-mail de teste** nas Configurações para validar o SMTP.
- [ ] Fila de envio assíncrona com retry e dead-letter (hoje o envio é síncrono).
- [ ] Preferências de notificação por cliente (opt-out).

## Financeiro

- [x] Aplicar juros/multa automaticamente no atraso (taxas por venda, cálculo em tempo real).
- [ ] Correção monetária por índice (INCC/IGPM) nas parcelas — hoje é apenas informativo.
- [ ] Relatório de DRE simplificado (receitas − despesas por centro de custo).
- [x] Plano de contas estruturado: entidade **Categoria** (grupo → item) e
      **Centro de Custo** com grupo, ambos FK em Contas a Pagar (V11) — com
      seeds editáveis pelo admin, filtros, relatórios CSV e indicadores no dashboard.
- [ ] Migrar `supplier` de texto livre para FK (Supplier).
- [x] **DRE (Demonstração do Resultado)** em base caixa: tela própria com
      receitas − despesas = resultado, filtros de período/empreendimento, margem
      e export CSV (`/dre`, `/dre/export`).
- [ ] DRE em **competência** (faturado − incorrido) como visão alternativa.
- [ ] Categorias de **receita** (FK em Contas a Receber) para detalhar "Outras Receitas".
- [ ] Separar **Receitas Financeiras** (juros/multas recebidos) — requer gravar
      principal/juros/multa na baixa da parcela.
- [ ] Mapa no Dashboard (cards empreendimento → quadra → lotes) — em avaliação pelo cliente.

## Cadastros de referência

- [ ] Tornar formas de pagamento e índices de correção configuráveis via tabela
      em vez de listas fixas no frontend.
- [ ] Categorias de fornecedor configuráveis.

## Dashboard

- [ ] Drill-down: clicar no gráfico → lista detalhada filtrada.
- [ ] Cache leve das agregações pesadas (Redis ou Caffeine) para bases grandes.
- [ ] Exportar gráfico como imagem (PNG).

## Imóveis / Lotes

- [ ] Upload de planta/foto do lote (Supabase Storage).
- [ ] Mapa interativo dos lotes por quadra (grid visual com status por cor).
- [ ] Reserva temporária de lote com expiração automática.

## Plataforma

- [ ] Refresh token + revogação (blacklist de JTI).
- [ ] Auditoria automática via AOP em vez de chamadas manuais.
- [ ] Upload de logo/comprovantes via storage em vez de URL externa.
- [ ] CEP/CNPJ via proxy no backend (cache + resiliência) em vez de chamada direta.
- [x] Paginação e filtros **server-side** nas listagens de maior volume:
      Clientes, Contas a Pagar, Contas a Receber e Parcelas (componente
      `Pagination` reutilizável; filtros q/status/empreendimento no backend).
- [x] Estendida a paginação server-side a Vendas, Fornecedores e Transações
      bancárias (consumidores de drill-down/datalist ajustados para `.content`).
- [ ] Combobox server-side (busca remota) nos selects de Cliente/Lote em Vendas
      (hoje carregam até 500 itens de uma vez).
- [ ] Code-splitting do bundle (Recharts e React Router lazy loading).

## Estratégia comercial / Licenciamento por módulos (ESPECIFICAÇÃO — implementar por fases)

> Especificação oficial do licenciamento. Objetivo: comercializar o sistema em
> planos, liberando só os módulos contratados por cliente. A **Fase 1 já está
> pronta**; as demais não devem ser iniciadas sem decisão explícita.

### ✅ DECISÃO — Modelo de implantação: VM por cliente (silo)
Cada cliente roda **sua própria VM + seu próprio banco**. Isolamento é **físico**
(bancos separados), não lógico. Consequências:
- **Multitenant (`tenant_id`) NÃO será feito** — a Fase 3 fica descartada (mantida
  abaixo só como alternativa caso o modelo mude para "muitos clientes / infra única").
- O sistema atual (single-tenant) **já atende** esse modelo sem reescrita de queries.
- Trade-off aceito: custo de infra por cliente + atualizar N VMs (mitigado por deploy
  automatizado via imagem Docker) + backup/monitoramento por VM.
- A **chave de licenciamento** (Fase 2) é o que configura o plano de cada VM.
- A **app de gestão (Fase 5)** vira o painel central de todas as VMs/clientes.

### Os eixos (independentes — não confundir)
| Eixo | Responde | Exemplos | Onde mora |
|------|----------|----------|-----------|
| **Plano** (tier comercial) | quais módulos a EMPRESA contratou (teto) | Essencial / Profissional / Premium | tabela `license` |
| **Papel + permissão por módulo** | o que CADA usuário vê/edita dentro do teto | Admin, Financeiro, Corretor (só leitura) | papéis/permissões |
| ~~Tenant (multitenant)~~ | ~~de QUEM são os dados~~ | **resolvido por VM separada** (não por `tenant_id`) | — |

Regra de ouro: o **Plano** define o teto da empresa; o **Papel** recorta por
usuário DENTRO do teto; o isolamento entre empresas é **por VM** (não por `tenant_id`).
"Perfil de negócio" (Contabilidade/Construtora/Comercial) NÃO é um eixo separado — é só
um *preset* que aplica um Plano + ajusta módulos.

### Distribuição de módulos por plano (cumulativo)
**🟢 ESSENCIAL** (foco contábil/financeiro) — todos já construídos:
`DASHBOARD`, `CLIENTES`, `FORNECEDORES`, `CONTAS_PAGAR`, `CONTAS_RECEBER`,
`CONCILIACAO`, `DRE`, `RELATORIOS`, `NOTIFICACOES`.

**🔵 PROFISSIONAL** (= Essencial + construtora/imobiliária) — já construídos:
`EMPREENDIMENTOS`, `MAPA` (lotes), `VENDAS`/contratos/parcelas, encargos (juros/multa).

**🟣 PREMIUM** (= Profissional + avançado) — **ainda a construir** (Fase 4):
`PORTAL_CLIENTE` (web do comprador), `COBRANCA` (boleto/PIX via **Asaas** + baixa
automática), `CORRECAO_MONETARIA` (INCC/IGPM real nas parcelas), `ASSINATURA`
(assinatura eletrônica de contratos), `APP_MOBILE` (app do cliente — futuro).
> Ligar a flag Premium NÃO cria estas funcionalidades — elas precisam ser
> desenvolvidas (Fase 4). Hoje existem só como flags "Em breve" no catálogo.

### Licença
- Campos: plano, status, data início/validade, período (12/24/36 meses),
  máx. usuários, limites (clientes/empreendimentos), observações.
- Descontos por período (2 anos ~10%, 3 anos ~15–20%).
- Bloqueio escalonado (Fase 2): aviso → tolerância (7/15/30 dias) → só leitura → bloqueio total.

---

### Fases de implementação

- [x] **Fase 1 — Fundação (FEITA)** (migration V13): tabelas `modules` (catálogo +
      flag `active`) e `license` (plano/validade/status, linha única). Backend
      `LicensingService`/`LicensingController` (`/api/licensing/me`, `/modules/{code}`,
      `/license`). Frontend `LicensingContext.canAccess()`, `ModuleGuard` nas rotas,
      `NAV` filtra por módulo, banner de vencimento e aba **Configurações → Módulos &
      Licença**. **Gating só no frontend**; plano ainda é rótulo solto. Tudo nasce ATIVO.

- [x] **Fase 2 — Planos amarrados + permissões por módulo + enforcement + chave (FEITA)**
      (migration V14). Detalhes:
  - [x] **Plano → pacote de módulos** (`PlanModules`): `POST /licensing/plan` e a chave
        ligam automaticamente o conjunto do plano (Essencial/Profissional/Premium).
  - [x] **Permissões por módulo** `<MODULO>_VIEW` / `<MODULO>_EDIT` (substituem o `READ`
        global e os `*_WRITE` avulsos). CRUD de **perfis de acesso** com grade VER/EDITAR
        em **Configurações → Perfis de Acesso** (`/api/roles`, `/api/roles/permissions`);
        ADMIN protegido. Todos os controllers e o frontend migrados para os novos códigos.
  - [x] **Chave de licenciamento** (token **HMAC-SHA256 offline**, `LicenseKeyService`):
        aplicar (`POST /licensing/license/key`) e gerar (`.../key/generate`) em
        Configurações → Módulos & Licença. Aplica plano + validade + cliente + limites.
  - [x] **Enforcement no backend** (`LicenseEnforcementInterceptor`): recusa endpoint de
        módulo desligado; núcleo (auth/licensing/settings/users/roles) sempre liberado.
  - [x] **Bloqueio escalonado** por vencimento: dentro da tolerância → só aviso; após a
        tolerância ou SUSPENSA → modo **somente-leitura**; CANCELADA → **bloqueio total**.
  - ⚠️ Requer **deploy atômico** (V14 + código novo juntos): a migração apaga os códigos
        antigos (`READ`, `*_WRITE`), então não pode rodar sozinha contra um app antigo.
  - [ ] *Follow-up*: telas que cruzam módulos (Dashboard chama `/sales` e `/installments`;
        Relatórios) podem retornar 403 quando o plano NÃO inclui o módulo de origem
        (ex.: Essencial sem VENDAS). Tratar erro por-consulta no front ou condicionar a
        consulta a `canAccess()`. Não afeta o cliente atual (Profissional, com Vendas).

- [ ] **Fase 3 — Provisionamento de VM por cliente** (substitui o multitenant):
  - [x] **Atualização da frota (build 1x → puxa em N VMs)**: CI publica imagens
        versionadas no GHCR (`release-images.yml`), `docker-compose.registry.yml`
        (pull-only, frontend com API relativa serve qualquer domínio),
        `scripts/update-fleet.sh` (atualiza todas por SSH, com canário) e Watchtower
        opcional. Flyway migra o banco de cada cliente no boot. (docs/DEPLOY.md §8)
  - [ ] Provisionar uma **VM nova a cada venda** (script de bootstrap: VM + banco +
        `.env` + chave de licença + DNS) — hoje é manual (DEPLOY.md §7).
  - [ ] **Backup e monitoramento por VM** (dump agendado do Postgres, healthcheck/alertas).
      > ~~Alternativa multitenant (DESCARTADA): `tenant_id` nas tabelas + escopo nas
      > queries + login por tenant. Só reabrir se o modelo mudar para infra única com
      > muitos clientes.~~

- [ ] **Fase 4 — Construir os módulos Premium** (independente das Fases 2/3; já
      beneficia o cliente atual): **Portal do Cliente (web)** (login do comprador:
      contrato, parcelas, 2ª via boleto/PIX, comunicados, dados cadastrais) → base
      reutilizável para o **App Android** (React Native/Flutter + FCM); **Cobrança
      Asaas** (geração de boleto/PIX e baixa automática); **Correção monetária**
      (INCC/IGPM); **Assinatura eletrônica** de contratos.

- [ ] **Fase 5 — Aplicação web de gestão de VPS, licenças e releases** (2º sistema,
      separado; o "painel de controle" do modelo VM-por-cliente). Painel do FORNECEDOR
      (você), com duas frentes:
  - **Licenciamento e clientes:** gerar/renovar chaves de licença por plano, controle
    de vencimento, cobrança/faturamento, dashboard de clientes ativos/inadimplentes e
    **inventário das VMs** (qual cliente, qual plano, qual versão rodando). A Fase 2
    cria a "chave" que esta app passa a gerar automaticamente.
  - **Gestão de versões e atualizações (control plane):** **registro/catálogo das
    versões** publicadas (releases do CI no GHCR), **liberação controlada de updates**
    por cliente/grupo (canário → frota; agendamento; janela de manutenção), e
    **histórico de releases disponibilizadas** (changelog + qual cliente está em qual
    versão). Orquestra o mecanismo já pronto (CI → GHCR → `update-fleet`/Watchtower,
    DEPLOY.md §8), substituindo o disparo manual por um fluxo central de distribuição.

### Nota de alinhamento (estado atual)
Base aproveitável: papéis/permissões (`hasPermission`), Configurações por sistema,
notificações por e-mail, API REST coesa e a Fundação (Fase 1) de módulos/licença.
Como o isolamento é **por VM** (decisão acima), o sistema single-tenant atual já serve —
**não há multitenant a fazer**. Faltam: amarrar plano↔módulos + permissões por módulo +
chave de licenciamento + enforcement (Fase 2), provisionamento/deploy das VMs (Fase 3),
os módulos Premium (Fase 4) e a app de gestão de licenças (Fase 5).

---

## Qualidade / DevOps

- [x] Testes unitários do núcleo financeiro: `LateFeeCalculator` (juros/multa) e
      geração de parcelas/entrada (`SaleService`) — 14 testes.
- [x] **CI (GitHub Actions)**: roda `mvn test` (backend) e `npm run build`
      (frontend) a cada push/PR em `main`/`develop`.
- [x] Smoke test end-to-end via API (`scripts/smoke.ps1`) — 13 verificações
      cobrindo cadastros, FKs, encargos, parcelas, dashboard, DRE, conciliação e CSV.
- [ ] Testes de integração com Testcontainers (Postgres) p/ migrations, dashboard
      nativo, reconciliação e cascade hierarchy.
- [ ] Geração de tipos TypeScript do frontend a partir do OpenAPI
      (`openapi-typescript` + `npm run gen-types`).
- [ ] Deploy automático no push a `main` (estender o CI).
- [ ] Health check dedicado em `/actuator/health` com detalhe de banco.
