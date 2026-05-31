-- ============================================================
--  V2 — Seed de papéis, permissões e configuração padrão.
--  O usuário ADMIN inicial é criado pelo DataInitializer
--  (lê app.admin.email / app.admin.password do ambiente),
--  evitando senha em texto puro versionada.
-- ============================================================

INSERT INTO roles (name, description) VALUES
    ('ADMIN',         'Acesso total ao sistema'),
    ('FINANCEIRO',    'Contas a pagar/receber e conciliação bancária'),
    ('CONTABILIDADE', 'Consulta de lançamentos, relatórios e validação de conciliações'),
    ('COMERCIAL',     'Clientes, imóveis, vendas e contratos'),
    ('VISUALIZADOR',  'Acesso somente leitura');

INSERT INTO permissions (code, description) VALUES
    ('USERS_MANAGE',        'Gerenciar usuários e perfis'),
    ('SETTINGS_MANAGE',     'Personalizar tema, logo e dados da empresa'),
    ('CLIENTS_WRITE',       'Cadastrar/editar clientes'),
    ('PROPERTIES_WRITE',    'Cadastrar/editar imóveis'),
    ('SALES_WRITE',         'Registrar vendas e parcelas'),
    ('CONTRACTS_GENERATE',  'Gerar contratos'),
    ('PAYABLE_WRITE',       'Gerenciar contas a pagar'),
    ('RECEIVABLE_WRITE',    'Gerenciar contas a receber'),
    ('RECONCILIATION_WRITE','Importar extratos e conciliar'),
    ('RECONCILIATION_VALIDATE','Validar conciliações'),
    ('REPORTS_EXPORT',      'Exportar relatórios'),
    ('READ',                'Leitura geral');

-- ADMIN: todas as permissões
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p WHERE r.name = 'ADMIN';

-- FINANCEIRO
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p
  ON p.code IN ('PAYABLE_WRITE','RECEIVABLE_WRITE','RECONCILIATION_WRITE','REPORTS_EXPORT','READ')
WHERE r.name = 'FINANCEIRO';

-- CONTABILIDADE
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p
  ON p.code IN ('RECONCILIATION_VALIDATE','REPORTS_EXPORT','READ')
WHERE r.name = 'CONTABILIDADE';

-- COMERCIAL
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p
  ON p.code IN ('CLIENTS_WRITE','PROPERTIES_WRITE','SALES_WRITE','CONTRACTS_GENERATE','READ')
WHERE r.name = 'COMERCIAL';

-- VISUALIZADOR
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p ON p.code = 'READ'
WHERE r.name = 'VISUALIZADOR';

-- Configuração inicial (linha única)
INSERT INTO system_settings (system_name, primary_color, secondary_color, theme, footer_text)
VALUES ('Construtora Financeiro', '#1e40af', '#0f766e', 'light',
        '© Construtora Financeiro — POC');
