-- Fase F: Data do crédito no extrato bancário.
-- Capturada no momento da conciliação (data da transação bancária = crédito no extrato).
-- Campo informativo, para uso futuro. NÃO substitui a data da baixa
-- (payment_date / receive_date), que continua sendo a referência financeira principal.

ALTER TABLE installments ADD COLUMN bank_credit_date DATE;
ALTER TABLE accounts_receivable ADD COLUMN bank_credit_date DATE;

COMMENT ON COLUMN installments.bank_credit_date IS
    'Data do crédito no extrato bancário, capturada na conciliação (uso futuro).';
COMMENT ON COLUMN accounts_receivable.bank_credit_date IS
    'Data do crédito no extrato bancário, capturada na conciliação (uso futuro).';
