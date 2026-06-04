-- ============================================================
-- Fase 2 do licenciamento:
--   (A) Permissões por MÓDULO no formato <MODULO>_VIEW / <MODULO>_EDIT
--       (substitui o antigo READ global + *_WRITE avulsos), permitindo
--       perfis de acesso sob medida ("vê só alguns módulos" / "vê sem editar").
--   (B) Colunas na licença para a CHAVE de licenciamento (token HMAC),
--       cliente e período de tolerância (bloqueio escalonado).
-- ADMIN continua com todas as permissões; o bypass de ADMIN no código
-- garante que o admin nunca se tranca para fora.
-- ============================================================

-- ---------- (B) Licença: chave + cliente + tolerância ----------
ALTER TABLE license ADD COLUMN IF NOT EXISTS license_key TEXT;
ALTER TABLE license ADD COLUMN IF NOT EXISTS customer    VARCHAR(200);
ALTER TABLE license ADD COLUMN IF NOT EXISTS grace_days  INTEGER NOT NULL DEFAULT 7;

-- ---------- (A) Novas permissões por módulo ----------
INSERT INTO permissions (code, description) VALUES
    ('DASHBOARD_VIEW',       'Ver o dashboard'),
    ('CLIENTES_VIEW',        'Ver clientes'),
    ('CLIENTES_EDIT',        'Cadastrar/editar clientes'),
    ('EMPREENDIMENTOS_VIEW', 'Ver empreendimentos, quadras e lotes'),
    ('EMPREENDIMENTOS_EDIT', 'Cadastrar/editar empreendimentos, quadras e lotes'),
    ('VENDAS_VIEW',          'Ver vendas, parcelas e contratos'),
    ('VENDAS_EDIT',          'Registrar vendas, baixar parcelas e gerar contratos'),
    ('CONTAS_PAGAR_VIEW',    'Ver contas a pagar'),
    ('CONTAS_PAGAR_EDIT',    'Gerenciar contas a pagar'),
    ('CONTAS_RECEBER_VIEW',  'Ver contas a receber'),
    ('CONTAS_RECEBER_EDIT',  'Gerenciar contas a receber'),
    ('FORNECEDORES_VIEW',    'Ver fornecedores'),
    ('FORNECEDORES_EDIT',    'Cadastrar/editar fornecedores'),
    ('CONCILIACAO_VIEW',     'Ver conciliação e extratos'),
    ('CONCILIACAO_EDIT',     'Importar extratos, conciliar e validar'),
    ('DRE_VIEW',             'Ver a DRE'),
    ('RELATORIOS_VIEW',      'Ver e exportar relatórios'),
    ('NOTIFICACOES_VIEW',    'Ver o histórico de notificações')
ON CONFLICT (code) DO NOTHING;
-- USERS_MANAGE e SETTINGS_MANAGE já existem (V2) e são mantidos.

-- ---------- Reconstrói os vínculos papel↔permissão ----------
DELETE FROM role_permissions;

-- ADMIN: todas as permissões
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p WHERE r.name = 'ADMIN';

-- VISUALIZADOR: tudo que é *_VIEW (somente leitura)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p ON p.code LIKE '%\_VIEW'
WHERE r.name = 'VISUALIZADOR';

-- Demais papéis: mapa explícito (papel → permissão)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM (VALUES
    -- FINANCEIRO: financeiro completo + leitura geral de apoio
    ('FINANCEIRO','CONTAS_PAGAR_VIEW'),   ('FINANCEIRO','CONTAS_PAGAR_EDIT'),
    ('FINANCEIRO','CONTAS_RECEBER_VIEW'), ('FINANCEIRO','CONTAS_RECEBER_EDIT'),
    ('FINANCEIRO','FORNECEDORES_VIEW'),   ('FINANCEIRO','FORNECEDORES_EDIT'),
    ('FINANCEIRO','CONCILIACAO_VIEW'),    ('FINANCEIRO','CONCILIACAO_EDIT'),
    ('FINANCEIRO','DASHBOARD_VIEW'),      ('FINANCEIRO','DRE_VIEW'),
    ('FINANCEIRO','RELATORIOS_VIEW'),     ('FINANCEIRO','NOTIFICACOES_VIEW'),
    -- CONTABILIDADE: consulta financeira + validação de conciliação
    ('CONTABILIDADE','DASHBOARD_VIEW'),     ('CONTABILIDADE','DRE_VIEW'),
    ('CONTABILIDADE','RELATORIOS_VIEW'),    ('CONTABILIDADE','CONTAS_PAGAR_VIEW'),
    ('CONTABILIDADE','CONTAS_RECEBER_VIEW'),('CONTABILIDADE','CONCILIACAO_VIEW'),
    ('CONTABILIDADE','CONCILIACAO_EDIT'),
    -- COMERCIAL: clientes, imóveis e vendas
    ('COMERCIAL','CLIENTES_VIEW'),        ('COMERCIAL','CLIENTES_EDIT'),
    ('COMERCIAL','EMPREENDIMENTOS_VIEW'), ('COMERCIAL','EMPREENDIMENTOS_EDIT'),
    ('COMERCIAL','VENDAS_VIEW'),          ('COMERCIAL','VENDAS_EDIT'),
    ('COMERCIAL','DASHBOARD_VIEW')
) AS m(role_name, perm_code)
JOIN roles r       ON r.name = m.role_name
JOIN permissions p ON p.code = m.perm_code;

-- ---------- Remove as permissões antigas (substituídas) ----------
DELETE FROM permissions WHERE code IN (
    'READ','CLIENTS_WRITE','PROPERTIES_WRITE','SALES_WRITE','CONTRACTS_GENERATE',
    'PAYABLE_WRITE','RECEIVABLE_WRITE','RECONCILIATION_WRITE','RECONCILIATION_VALIDATE',
    'REPORTS_EXPORT'
);
