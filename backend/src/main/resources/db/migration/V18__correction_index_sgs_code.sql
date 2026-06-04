-- ============================================================
--  V18: Código SGS (Banco Central) nos índices de correção
--  Permite buscar o valor oficial acumulado de cada índice na
--  API pública do BCB (api.bcb.gov.br/dados/serie/bcdata.sgs.{codigo}).
-- ============================================================
ALTER TABLE correction_indexes ADD COLUMN sgs_code INTEGER;

-- Códigos das séries temporais do BCB (variação mensal %)
UPDATE correction_indexes SET sgs_code = 192 WHERE name = 'INCC';   -- INCC (FGV)
UPDATE correction_indexes SET sgs_code = 189 WHERE name = 'IGP-M';  -- IGP-M (FGV)
UPDATE correction_indexes SET sgs_code = 433 WHERE name = 'IPCA';   -- IPCA (IBGE)
