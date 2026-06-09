-- ============================================================
--  V23: description do extrato vira TEXT
-- ============================================================
-- Descrições de extratos variam muito entre bancos (e o PDF pode trazer
-- históricos longos). VARCHAR(255) era apertado — usa TEXT.

ALTER TABLE bank_transactions ALTER COLUMN description TYPE TEXT;
