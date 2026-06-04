-- ============================================================
-- Fase 1 do licenciamento por módulos (SEM multitenant ainda).
--   • modules  = catálogo de módulos do sistema com flag "active"
--                (feature flags: liga/desliga áreas do menu)
--   • license  = uma única licença para esta instalação
--                (plano, validade, status) — base para avisos de
--                vencimento. NÃO bloqueia nada por enquanto.
-- Tudo nasce ATIVO, então o sistema atual não muda em nada.
-- ============================================================

-- ---------- Catálogo de módulos ----------
CREATE TABLE modules (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code        VARCHAR(40)  NOT NULL UNIQUE,
    name        VARCHAR(120) NOT NULL,
    description VARCHAR(255),
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    sort_order  INTEGER      NOT NULL DEFAULT 0,
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

INSERT INTO modules (code, name, description, active, sort_order) VALUES
    ('DASHBOARD',       'Dashboard',              'Painel e indicadores',                    TRUE,  10),
    ('CLIENTES',        'Clientes',               'Cadastro de clientes',                    TRUE,  20),
    ('EMPREENDIMENTOS', 'Empreendimentos / Lotes','Empreendimentos, quadras e lotes',        TRUE,  30),
    ('VENDAS',          'Vendas',                 'Vendas, contratos e parcelas',            TRUE,  40),
    ('CONTAS_PAGAR',    'Contas a Pagar',         'Despesas e contas a pagar',               TRUE,  50),
    ('CONTAS_RECEBER',  'Contas a Receber',       'Recebíveis avulsos',                      TRUE,  60),
    ('FORNECEDORES',    'Fornecedores',           'Cadastro de fornecedores',                TRUE,  70),
    ('CONCILIACAO',     'Conciliação Bancária',   'Importação de extratos e conciliação',    TRUE,  80),
    ('DRE',             'DRE',                    'Demonstração do resultado',               TRUE,  90),
    ('RELATORIOS',      'Relatórios',             'Relatórios e exportações',                TRUE, 100),
    ('NOTIFICACOES',    'Notificações',           'E-mails e lembretes de vencimento',       TRUE, 110),
    -- Módulos de roadmap (ainda não implementados): nascem desligados.
    ('PORTAL_CLIENTE',  'Portal do Cliente',      'Acesso do cliente final (futuro)',        FALSE, 200),
    ('APP_MOBILE',      'Aplicativo Android',     'App mobile do cliente (futuro)',          FALSE, 210);

-- ---------- Licença única da instalação ----------
CREATE TABLE license (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    plan          VARCHAR(40)  NOT NULL DEFAULT 'PROFISSIONAL',
    status        VARCHAR(20)  NOT NULL DEFAULT 'ATIVA',
    start_date    DATE         NOT NULL DEFAULT CURRENT_DATE,
    end_date      DATE,
    period_months INTEGER      NOT NULL DEFAULT 12,
    max_users     INTEGER,
    notes         VARCHAR(500),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

INSERT INTO license (plan, status, start_date, end_date, period_months, max_users, notes) VALUES
    ('PROFISSIONAL', 'ATIVA', CURRENT_DATE, CURRENT_DATE + INTERVAL '12 months', 12, 10,
     'Licença inicial do POC (gerada automaticamente).');
