-- ============================================================
--  V4 — Observacao/motivo na transacao bancaria
--  Usado para registrar o motivo ao marcar como DIVERGENTE
--  e anotacoes da conciliacao manual.
-- ============================================================
ALTER TABLE bank_transactions ADD COLUMN notes VARCHAR(255);
