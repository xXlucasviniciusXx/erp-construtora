-- ============================================================
--  V16: Categorias de receita + Upload de logo no banco
-- ============================================================

-- ---- 1. Categoria de receita em contas a receber ----
-- FK opcional para categories (mesma tabela do plano de contas de despesas).
-- Permite detalhar "Outras Receitas" no DRE por natureza.
ALTER TABLE accounts_receivable
    ADD COLUMN category_id UUID REFERENCES categories(id);

-- ---- 2. Logo e imagens — armazenamento no banco ----
-- Quando o usuário faz upload de um arquivo pelo sistema, o binário é guardado
-- aqui para evitar dependência de storage externo (Supabase/S3).
-- logo_url continua suportando URLs externas; logo_data prevalece quando presente.
ALTER TABLE system_settings
    ADD COLUMN logo_data  BYTEA,
    ADD COLUMN logo_mime  VARCHAR(50);
