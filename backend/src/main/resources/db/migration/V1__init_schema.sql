-- ============================================================
--  V1 — Esquema inicial
--  Convenções: snake_case, PK uuid, timestamps em UTC,
--  enums modelados como varchar + CHECK para portabilidade.
-- ============================================================

CREATE EXTENSION IF NOT EXISTS "pgcrypto";   -- gen_random_uuid()

-- ---------- Controle de acesso ----------
CREATE TABLE roles (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(40) NOT NULL UNIQUE,        -- ADMIN, FINANCEIRO, ...
    description VARCHAR(255)
);

CREATE TABLE permissions (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code        VARCHAR(80) NOT NULL UNIQUE,        -- ex: ACCOUNTS_PAYABLE_WRITE
    description VARCHAR(255)
);

CREATE TABLE role_permissions (
    role_id       UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    permission_id UUID NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, permission_id)
);

CREATE TABLE users (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name          VARCHAR(150) NOT NULL,
    email         VARCHAR(150) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role_id       UUID NOT NULL REFERENCES roles(id),
    active        BOOLEAN NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_users_role ON users(role_id);

-- ---------- Clientes ----------
CREATE TABLE clients (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    person_type        VARCHAR(10) NOT NULL DEFAULT 'PF' CHECK (person_type IN ('PF','PJ')),
    name               VARCHAR(200) NOT NULL,        -- nome completo / razão social
    document           VARCHAR(20)  NOT NULL,        -- CPF / CNPJ (somente dígitos)
    state_registration VARCHAR(30),                  -- RG / inscrição estadual
    email              VARCHAR(150),
    phone              VARCHAR(30),
    address            VARCHAR(255),
    city               VARCHAR(120),
    state              VARCHAR(2),
    zip_code           VARCHAR(10),
    marital_status     VARCHAR(30),
    occupation         VARCHAR(120),
    notes              TEXT,
    status             VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE','INACTIVE')),
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX uq_clients_document ON clients(document);

-- ---------- Imóveis / lotes / unidades ----------
CREATE TABLE properties (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    development       VARCHAR(150) NOT NULL,          -- empreendimento
    block             VARCHAR(30),                    -- quadra
    lot               VARCHAR(30),                    -- lote
    unit              VARCHAR(30),                    -- unidade
    registration      VARCHAR(60),                    -- matrícula
    address           VARCHAR(255),
    total_area        NUMERIC(12,2),
    built_area        NUMERIC(12,2),
    sale_value        NUMERIC(15,2),
    status            VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE'
                        CHECK (status IN ('AVAILABLE','RESERVED','SOLD','CANCELLED')),
    contract_extra    TEXT,                           -- dados adicionais para contrato
    notes             TEXT,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_properties_status ON properties(status);
CREATE INDEX idx_properties_development ON properties(development);

-- ---------- Vendas ----------
CREATE TABLE property_sales (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id            UUID NOT NULL REFERENCES clients(id),
    property_id          UUID NOT NULL REFERENCES properties(id),
    total_value          NUMERIC(15,2) NOT NULL,
    down_payment         NUMERIC(15,2) NOT NULL DEFAULT 0,
    installments_count   INTEGER NOT NULL CHECK (installments_count >= 0),
    first_due_date       DATE NOT NULL,
    payment_method       VARCHAR(40),
    correction_index     VARCHAR(20),                 -- IGPM, IPCA, INCC...
    interest_rate        NUMERIC(6,4) DEFAULT 0,      -- % juros mensal por atraso
    penalty_rate         NUMERIC(6,4) DEFAULT 0,      -- % multa por atraso
    status               VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
                          CHECK (status IN ('ACTIVE','COMPLETED','CANCELLED')),
    sale_date            DATE NOT NULL DEFAULT CURRENT_DATE,
    notes                TEXT,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_sales_client ON property_sales(client_id);
CREATE INDEX idx_sales_property ON property_sales(property_id);

-- ---------- Parcelas ----------
CREATE TABLE installments (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sale_id         UUID NOT NULL REFERENCES property_sales(id) ON DELETE CASCADE,
    number          INTEGER NOT NULL,
    amount          NUMERIC(15,2) NOT NULL,
    due_date        DATE NOT NULL,
    payment_date    DATE,
    status          VARCHAR(20) NOT NULL DEFAULT 'OPEN'
                     CHECK (status IN ('OPEN','PAID','OVERDUE','CANCELLED')),
    payment_method  VARCHAR(40),
    receipt_url     VARCHAR(255),
    notes           TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_installments_sale ON installments(sale_id);
CREATE INDEX idx_installments_status ON installments(status);
CREATE INDEX idx_installments_due ON installments(due_date);

-- ---------- Contas a pagar ----------
CREATE TABLE accounts_payable (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    supplier       VARCHAR(200) NOT NULL,
    category       VARCHAR(80),
    description    VARCHAR(255),
    amount         NUMERIC(15,2) NOT NULL,
    due_date       DATE NOT NULL,
    payment_date   DATE,
    status         VARCHAR(20) NOT NULL DEFAULT 'OPEN'
                    CHECK (status IN ('OPEN','PAID','OVERDUE','CANCELLED')),
    payment_method VARCHAR(40),
    cost_center    VARCHAR(80),
    attachment_url VARCHAR(255),
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_ap_status ON accounts_payable(status);
CREATE INDEX idx_ap_due ON accounts_payable(due_date);

-- ---------- Contas a receber ----------
CREATE TABLE accounts_receivable (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id       UUID REFERENCES clients(id),
    sale_id         UUID REFERENCES property_sales(id),
    installment_id  UUID REFERENCES installments(id),
    description     VARCHAR(255),
    amount          NUMERIC(15,2) NOT NULL,
    due_date        DATE NOT NULL,
    receive_date    DATE,
    status          VARCHAR(20) NOT NULL DEFAULT 'OPEN'
                     CHECK (status IN ('OPEN','RECEIVED','OVERDUE','CANCELLED')),
    payment_method  VARCHAR(40),
    notes           TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_ar_status ON accounts_receivable(status);
CREATE INDEX idx_ar_due ON accounts_receivable(due_date);

-- ---------- Contas bancárias ----------
CREATE TABLE bank_accounts (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name          VARCHAR(120) NOT NULL,
    bank_code     VARCHAR(10),
    bank_name     VARCHAR(120),
    agency        VARCHAR(20),
    account_number VARCHAR(30),
    initial_balance NUMERIC(15,2) NOT NULL DEFAULT 0,
    active        BOOLEAN NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ---------- Importação de extratos ----------
CREATE TABLE bank_statement_imports (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    bank_account_id  UUID NOT NULL REFERENCES bank_accounts(id),
    file_name        VARCHAR(255) NOT NULL,
    file_format      VARCHAR(10) NOT NULL CHECK (file_format IN ('CSV','OFX')),
    file_size        BIGINT,
    status           VARCHAR(20) NOT NULL DEFAULT 'PROCESSING'
                      CHECK (status IN ('PROCESSING','COMPLETED','FAILED')),
    total_rows       INTEGER DEFAULT 0,
    imported_rows    INTEGER DEFAULT 0,
    error_message    TEXT,
    imported_by      UUID REFERENCES users(id),
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_imports_account ON bank_statement_imports(bank_account_id);

-- ---------- Transações bancárias ----------
CREATE TABLE bank_transactions (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    import_id        UUID REFERENCES bank_statement_imports(id) ON DELETE SET NULL,
    bank_account_id  UUID NOT NULL REFERENCES bank_accounts(id),
    transaction_date DATE NOT NULL,
    description      VARCHAR(255),
    amount           NUMERIC(15,2) NOT NULL,         -- positivo=crédito, negativo=débito
    type             VARCHAR(10) NOT NULL CHECK (type IN ('CREDIT','DEBIT')),
    document_number  VARCHAR(60),                    -- CPF/CNPJ encontrado, se houver
    bank_identifier  VARCHAR(120),                   -- FITID/identificador único do banco
    status           VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                      CHECK (status IN ('PENDING','RECONCILED','IGNORED','DIVERGENT')),
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_txn_account ON bank_transactions(bank_account_id);
CREATE INDEX idx_txn_status ON bank_transactions(status);
CREATE INDEX idx_txn_date ON bank_transactions(transaction_date);
-- Evita reimportar a mesma transação (quando o banco fornece identificador)
CREATE UNIQUE INDEX uq_txn_bank_identifier
    ON bank_transactions(bank_account_id, bank_identifier)
    WHERE bank_identifier IS NOT NULL;

-- ---------- Conciliações ----------
CREATE TABLE reconciliations (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    bank_transaction_id   UUID NOT NULL REFERENCES bank_transactions(id),
    target_type           VARCHAR(20) NOT NULL
                           CHECK (target_type IN ('RECEIVABLE','PAYABLE','INSTALLMENT')),
    target_id             UUID NOT NULL,             -- id polimórfico do alvo conciliado
    matched_amount        NUMERIC(15,2) NOT NULL,
    mode                  VARCHAR(10) NOT NULL DEFAULT 'MANUAL'
                           CHECK (mode IN ('AUTO','MANUAL')),
    confidence            NUMERIC(5,2),              -- score 0-100 quando sugerida
    reconciled_by         UUID REFERENCES users(id),
    reconciled_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    undone                BOOLEAN NOT NULL DEFAULT FALSE,
    undone_at             TIMESTAMPTZ,
    notes                 TEXT
);
CREATE INDEX idx_recon_txn ON reconciliations(bank_transaction_id);
CREATE INDEX idx_recon_target ON reconciliations(target_type, target_id);

-- Sugestões de conciliação (geradas pelo motor automático, ainda não confirmadas)
CREATE TABLE reconciliation_suggestions (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    bank_transaction_id UUID NOT NULL REFERENCES bank_transactions(id) ON DELETE CASCADE,
    target_type         VARCHAR(20) NOT NULL,
    target_id           UUID NOT NULL,
    score               NUMERIC(5,2) NOT NULL,       -- 0-100
    reason              VARCHAR(255),                -- explica o match (valor/data/doc)
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_suggest_txn ON reconciliation_suggestions(bank_transaction_id);

-- ---------- Configurações do sistema (personalização) ----------
CREATE TABLE system_settings (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    system_name      VARCHAR(120) NOT NULL DEFAULT 'Construtora Financeiro',
    logo_url         VARCHAR(255),
    primary_color    VARCHAR(20) NOT NULL DEFAULT '#1e40af',
    secondary_color  VARCHAR(20) NOT NULL DEFAULT '#0f766e',
    theme            VARCHAR(10) NOT NULL DEFAULT 'light' CHECK (theme IN ('light','dark')),
    company_name     VARCHAR(200),
    company_document VARCHAR(20),
    company_address  VARCHAR(255),
    company_phone    VARCHAR(30),
    company_email    VARCHAR(150),
    footer_text      VARCHAR(255),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ---------- Notificações por e-mail ----------
CREATE TABLE email_notifications (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    recipient   VARCHAR(150) NOT NULL,
    subject     VARCHAR(255) NOT NULL,
    body        TEXT,
    event_type  VARCHAR(40) NOT NULL,                -- PAYMENT_OVERDUE, PAYMENT_CONFIRMED...
    status      VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                 CHECK (status IN ('PENDING','SENT','FAILED')),
    error       TEXT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    sent_at     TIMESTAMPTZ
);
CREATE INDEX idx_email_status ON email_notifications(status);

-- ---------- Auditoria ----------
CREATE TABLE audit_logs (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID REFERENCES users(id),
    action      VARCHAR(80) NOT NULL,
    entity      VARCHAR(80),
    entity_id   VARCHAR(80),
    detail      TEXT,
    ip_address  VARCHAR(45),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_audit_user ON audit_logs(user_id);
CREATE INDEX idx_audit_entity ON audit_logs(entity, entity_id);
