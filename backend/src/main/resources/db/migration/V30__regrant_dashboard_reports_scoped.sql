-- =====================================================================
-- Agregados (dashboard + relatórios) agora respeitam o escopo por
-- empreendimento. Reconcede DASHBOARD_VIEW e RELATORIOS_VIEW ao perfil
-- EMPRESA_EMPREENDIMENTO (DONO_LOTE permanece sem — é visualizador de
-- empreendimentos/vendas apenas).
-- =====================================================================
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN ('DASHBOARD_VIEW', 'RELATORIOS_VIEW')
WHERE r.name = 'EMPRESA_EMPREENDIMENTO'
  AND NOT EXISTS (
        SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id);
