-- ============================================================
--  V22: Importação de extratos em XLSX e PDF (além de CSV/OFX)
-- ============================================================
-- Libera os novos formatos no CHECK de file_format.

ALTER TABLE bank_statement_imports DROP CONSTRAINT IF EXISTS bank_statement_imports_file_format_check;
ALTER TABLE bank_statement_imports
    ADD CONSTRAINT bank_statement_imports_file_format_check
    CHECK (file_format IN ('CSV', 'OFX', 'XLSX', 'PDF'));
