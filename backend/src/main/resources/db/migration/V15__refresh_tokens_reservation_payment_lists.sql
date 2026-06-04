-- ============================================================
--  V15: Refresh tokens + Reserva de lote + Listas configuráveis
-- ============================================================

-- ---- 1. Refresh tokens (sessão persistente) ----
CREATE TABLE refresh_tokens (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token           TEXT        NOT NULL UNIQUE,
    expires_at      TIMESTAMP   NOT NULL,
    revoked         BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP   NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_rt_token   ON refresh_tokens(token);
CREATE INDEX idx_rt_user_id ON refresh_tokens(user_id);

-- ---- 2. Reserva de lote com expiração automática ----
ALTER TABLE lots ADD COLUMN reservation_expires_at TIMESTAMP;

-- ---- 3. Formas de pagamento configuráveis ----
CREATE TABLE payment_methods (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(100) NOT NULL UNIQUE,
    active      BOOLEAN     NOT NULL DEFAULT TRUE,
    sort_order  INT         NOT NULL DEFAULT 0,
    created_at  TIMESTAMP   NOT NULL DEFAULT NOW()
);
INSERT INTO payment_methods (name, sort_order) VALUES
    ('Boleto',               1),
    ('PIX',                  2),
    ('Transferência bancária',3),
    ('Cartão',               4),
    ('Dinheiro',             5),
    ('Outro',                99);

-- ---- 4. Índices de correção monetária configuráveis ----
CREATE TABLE correction_indexes (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(100) NOT NULL UNIQUE,
    active      BOOLEAN     NOT NULL DEFAULT TRUE,
    sort_order  INT         NOT NULL DEFAULT 0,
    created_at  TIMESTAMP   NOT NULL DEFAULT NOW()
);
INSERT INTO correction_indexes (name, sort_order) VALUES
    ('Sem correção',       1),
    ('INCC',               2),
    ('IPCA',               3),
    ('IGP-M',              4),
    ('Juros fixo mensal',  5),
    ('Outro',              99);
