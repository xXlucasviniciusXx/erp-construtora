-- =====================================================================
-- Controle de acesso por empreendimento (Fase E)
-- =====================================================================

-- Vínculo usuário ↔ empreendimentos. Usuários SEM a permissão de acesso
-- global ficam restritos aos empreendimentos aqui vinculados (sem vínculo =
-- não enxerga nada — padrão seguro).
CREATE TABLE user_developments (
    user_id        UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    development_id UUID NOT NULL REFERENCES developments(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, development_id)
);
CREATE INDEX idx_user_developments_user ON user_developments(user_id);
CREATE INDEX idx_user_developments_dev ON user_developments(development_id);

-- Permissão que dispensa o escopo (enxerga TODOS os empreendimentos).
INSERT INTO permissions (code, description) VALUES
    ('ACESSO_GLOBAL_EMPREENDIMENTOS', 'Acessa dados de todos os empreendimentos (sem restrição de escopo)')
ON CONFLICT (code) DO NOTHING;

-- Concede o acesso global às roles que hoje enxergam tudo (preserva o
-- comportamento atual). DONO_LOTE e EMPRESA_EMPREENDIMENTO ficam restritos
-- aos empreendimentos vinculados a cada usuário.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code = 'ACESSO_GLOBAL_EMPREENDIMENTOS'
WHERE r.name IN ('ADMIN', 'FINANCEIRO', 'CONTABILIDADE', 'COMERCIAL', 'VISUALIZADOR', 'CONTADOR');

-- Endpoints AGREGADOS (dashboard, DRE, relatórios CSV) ainda não respeitam o
-- escopo por empreendimento. Para não vazar totais globais, removemos
-- DASHBOARD_VIEW e RELATORIOS_VIEW dos perfis restritos por ora (os endpoints
-- de DADOS — empreendimentos, lotes, vendas, contas a receber — já são
-- filtrados por escopo). Reconceder após escopar os agregados.
DELETE FROM role_permissions rp
USING roles r, permissions p
WHERE rp.role_id = r.id AND rp.permission_id = p.id
  AND p.code IN ('RELATORIOS_VIEW', 'DASHBOARD_VIEW')
  AND r.name IN ('DONO_LOTE', 'EMPRESA_EMPREENDIMENTO');
