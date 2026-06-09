-- ============================================================
--  V24 (Fase A): categoria de empreendimento, regra de distrato e 3 perfis
-- ============================================================

-- ---- 1. Categoria do empreendimento (Corretora / Terrenista) ----
ALTER TABLE developments ADD COLUMN category VARCHAR(20) NOT NULL DEFAULT 'CORRETORA'
    CHECK (category IN ('CORRETORA', 'TERRENISTA'));
ALTER TABLE developments ADD COLUMN terrenista_count INTEGER;        -- opcional (só Terrenista)
ALTER TABLE developments ADD COLUMN division_percent NUMERIC(5,2);   -- opcional (só Terrenista)

-- ---- 2. Regra aplicada no distrato (rastreabilidade/auditoria) ----
ALTER TABLE property_sales ADD COLUMN distrato_rule        VARCHAR(120);
ALTER TABLE property_sales ADD COLUMN distrato_rule_detail TEXT;

-- ---- 3. Perfis novos ----
INSERT INTO roles (name, description) VALUES
    ('CONTADOR',               'Consulta financeira: DRE, relatórios, recebíveis/pagáveis e conciliação'),
    ('EMPRESA_EMPREENDIMENTO', 'Acompanha vendas e recebíveis dos seus empreendimentos'),
    ('DONO_LOTE',              'Visualiza (somente leitura) os empreendimentos vinculados a ele')
ON CONFLICT (name) DO NOTHING;

-- CONTADOR: somente leitura financeira
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p ON p.code IN
    ('DASHBOARD_VIEW', 'DRE_VIEW', 'RELATORIOS_VIEW',
     'CONTAS_PAGAR_VIEW', 'CONTAS_RECEBER_VIEW', 'CONCILIACAO_VIEW')
WHERE r.name = 'CONTADOR';

-- EMPRESA_EMPREENDIMENTO: vê vendas/recebíveis dos seus empreendimentos
-- (o escopo por empreendimento é aplicado na Fase E)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p ON p.code IN
    ('EMPREENDIMENTOS_VIEW', 'VENDAS_VIEW', 'CONTAS_RECEBER_VIEW',
     'DASHBOARD_VIEW', 'RELATORIOS_VIEW')
WHERE r.name = 'EMPRESA_EMPREENDIMENTO';

-- DONO_LOTE: visualizador dos empreendimentos vinculados (escopo por empreendimento na Fase E)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p ON p.code IN
    ('EMPREENDIMENTOS_VIEW', 'VENDAS_VIEW')
WHERE r.name = 'DONO_LOTE';
