-- ============================================================
--  V21: Modelos de contrato vinculáveis por empreendimento
-- ============================================================
-- development_id NULL = modelo GLOBAL (vale para qualquer empreendimento).
-- development_id preenchido = modelo específico daquele empreendimento.

ALTER TABLE contract_templates
    ADD COLUMN development_id UUID REFERENCES developments(id) ON DELETE CASCADE;

CREATE INDEX idx_contract_templates_dev ON contract_templates(development_id);

-- O "padrão" agora é por escopo (kind + empreendimento). Substitui o índice
-- único antigo (que era só por kind). coalesce colapsa o NULL (global) em um
-- único valor sentinela para a unicidade.
DROP INDEX IF EXISTS idx_contract_templates_default;
CREATE UNIQUE INDEX idx_contract_templates_default
    ON contract_templates (kind, coalesce(development_id, '00000000-0000-0000-0000-000000000000'::uuid))
    WHERE is_default;
