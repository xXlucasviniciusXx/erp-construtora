-- =====================================================================
-- Fluxo completo de Distrato de Lote/Terreno
-- =====================================================================

-- 1) Lote: status EM_DISTRATO + percentual padrão de retenção -----------
-- A tabela "properties" foi renomeada para "lots" (V8); o CHECK inline da
-- V1 mantém o nome legado "properties_status_check". Removemos ambos por
-- segurança e recriamos com o novo valor permitido.
ALTER TABLE lots DROP CONSTRAINT IF EXISTS properties_status_check;
ALTER TABLE lots DROP CONSTRAINT IF EXISTS lots_status_check;
ALTER TABLE lots ADD CONSTRAINT lots_status_check
    CHECK (status IN ('AVAILABLE','RESERVED','SOLD','EM_DISTRATO','CANCELLED'));

ALTER TABLE lots ADD COLUMN IF NOT EXISTS retention_percent NUMERIC(5,2);

-- 2) Venda/Contrato: status DISTRATADO ---------------------------------
ALTER TABLE property_sales DROP CONSTRAINT IF EXISTS property_sales_status_check;
ALTER TABLE property_sales ADD CONSTRAINT property_sales_status_check
    CHECK (status IN ('ACTIVE','COMPLETED','CANCELLED','DISTRATADO'));

-- 3) Configuração da regra financeira de distrato ----------------------
-- development_id NULL = configuração GLOBAL; um registro por empreendimento.
CREATE TABLE distrato_configs (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    development_id  UUID REFERENCES developments(id) ON DELETE CASCADE,
    financial_rule  VARCHAR(60) NOT NULL DEFAULT 'RETENCAO_MAIS_PARCELAS_VENCIDAS'
                      CHECK (financial_rule IN (
                        'APENAS_RETENCAO_SOBRE_VALOR_PAGO',
                        'RETENCAO_MAIS_PARCELAS_VENCIDAS',
                        'RETENCAO_MAIS_PARCELAS_VENCIDAS_E_ENCARGOS',
                        'RETENCAO_MAIS_SALDO_DEVEDOR_TOTAL')),
    active          BOOLEAN NOT NULL DEFAULT true,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
-- Garante no máximo 1 global e 1 por empreendimento.
CREATE UNIQUE INDEX ux_distrato_config_global ON distrato_configs ((development_id IS NULL)) WHERE development_id IS NULL;
CREATE UNIQUE INDEX ux_distrato_config_dev ON distrato_configs (development_id) WHERE development_id IS NOT NULL;

-- Regra global padrão (recomendada): retenção + parcelas vencidas.
INSERT INTO distrato_configs (development_id, financial_rule) VALUES (NULL, 'RETENCAO_MAIS_PARCELAS_VENCIDAS');

-- 4) Distrato ----------------------------------------------------------
CREATE TABLE distratos (
    id                         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sale_id                    UUID NOT NULL REFERENCES property_sales(id),
    client_id                  UUID NOT NULL REFERENCES clients(id),
    lot_id                     UUID NOT NULL REFERENCES lots(id),
    -- Snapshots para o histórico (não dependem do estado futuro do cadastro)
    development_name           VARCHAR(150),
    block_name                 VARCHAR(60),
    lot_name                   VARCHAR(60),

    status                     VARCHAR(40) NOT NULL DEFAULT 'SOLICITADO'
                                 CHECK (status IN ('SOLICITADO','APROVADO','AGUARDANDO_QUITACAO_FINANCEIRA','CONCLUIDO','CANCELADO')),
    financial_rule             VARCHAR(60) NOT NULL
                                 CHECK (financial_rule IN (
                                   'APENAS_RETENCAO_SOBRE_VALOR_PAGO',
                                   'RETENCAO_MAIS_PARCELAS_VENCIDAS',
                                   'RETENCAO_MAIS_PARCELAS_VENCIDAS_E_ENCARGOS',
                                   'RETENCAO_MAIS_SALDO_DEVEDOR_TOTAL')),
    reason                     TEXT,

    contract_total             NUMERIC(15,2) NOT NULL DEFAULT 0,
    paid_amount                NUMERIC(15,2) NOT NULL DEFAULT 0,
    default_retention_percent  NUMERIC(5,2),
    used_retention_percent     NUMERIC(5,2) NOT NULL DEFAULT 0,
    retention_change_reason    TEXT,
    retention_amount           NUMERIC(15,2) NOT NULL DEFAULT 0,
    overdue_amount             NUMERIC(15,2) NOT NULL DEFAULT 0,
    charges_amount             NUMERIC(15,2) NOT NULL DEFAULT 0,
    total_debt_amount          NUMERIC(15,2) NOT NULL DEFAULT 0,
    final_balance              NUMERIC(15,2) NOT NULL DEFAULT 0,   -- pode ser negativo
    financial_outcome          VARCHAR(20) NOT NULL DEFAULT 'ZERO'
                                 CHECK (financial_outcome IN ('PAYABLE','RECEIVABLE','ZERO')),

    payable_id                 UUID REFERENCES accounts_payable(id),
    receivable_id              UUID REFERENCES accounts_receivable(id),

    requested_by               UUID REFERENCES users(id),
    approved_by                UUID REFERENCES users(id),
    settled_by                 UUID REFERENCES users(id),
    requested_at               TIMESTAMPTZ,
    approved_at                TIMESTAMPTZ,
    concluded_at               TIMESTAMPTZ,

    calculation_memory         TEXT,   -- JSON com a memória de cálculo completa

    created_at                 TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at                 TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_distratos_status ON distratos(status);
CREATE INDEX idx_distratos_sale ON distratos(sale_id);
CREATE INDEX idx_distratos_lot ON distratos(lot_id);
CREATE INDEX idx_distratos_payable ON distratos(payable_id);
CREATE INDEX idx_distratos_receivable ON distratos(receivable_id);
