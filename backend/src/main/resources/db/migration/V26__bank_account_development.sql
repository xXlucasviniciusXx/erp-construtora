-- =====================================================================
-- Associação Conta Bancária ↔ Empreendimento
-- development_id NULL = conta geral (não vinculada a um empreendimento).
-- Direciona o escopo das sugestões de conciliação para o empreendimento.
-- =====================================================================
ALTER TABLE bank_accounts
    ADD COLUMN development_id UUID REFERENCES developments(id) ON DELETE SET NULL;

CREATE INDEX idx_bank_accounts_development ON bank_accounts(development_id);
