-- ============================================================
--  V8 — Cadastro em cascata: Empreendimento -> Quadra -> Lote
--  Substitui a tabela plana "properties" pela hierarquia
--  developments / blocks / lots, migrando os dados existentes.
--  Identificadores: codigo interno (auto, hierarquico) + matricula (manual).
-- ============================================================

CREATE TABLE developments (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name           VARCHAR(150) NOT NULL,
    internal_code  VARCHAR(20) NOT NULL UNIQUE,
    blocks_count   INTEGER,            -- qtd de quadras prevista (limite)
    lots_count     INTEGER,            -- qtd de lotes prevista no empreendimento (limite total)
    expected_value NUMERIC(15,2) NOT NULL DEFAULT 0,   -- valor expectativa (manual)
    address        VARCHAR(255),
    status         VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    dimensions     VARCHAR(120),
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE blocks (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    development_id UUID NOT NULL REFERENCES developments(id) ON DELETE CASCADE,
    name           VARCHAR(60) NOT NULL,
    internal_code  VARCHAR(40) NOT NULL UNIQUE,
    registration   VARCHAR(60),        -- matricula do cartorio (editavel, pode ser nula)
    area           NUMERIC(12,2),
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_blocks_development ON blocks(development_id);

-- ---- Transforma "properties" em "lots" ----
ALTER TABLE properties RENAME TO lots;
ALTER TABLE lots RENAME COLUMN lot TO name;
ALTER TABLE lots ADD COLUMN block_id UUID REFERENCES blocks(id);
ALTER TABLE lots ADD COLUMN internal_code VARCHAR(60);
ALTER TABLE lots ADD COLUMN planned_value NUMERIC(15,2);

-- planned_value herda o antigo "valor de venda" (previsto); name nunca nulo
UPDATE lots SET planned_value = sale_value;
UPDATE lots SET name = COALESCE(NULLIF(name, ''), 'Lote') WHERE name IS NULL OR name = '';

-- 1) Empreendimentos a partir dos "development" distintos
INSERT INTO developments (id, name, internal_code, expected_value)
SELECT gen_random_uuid(), d.development,
       'E' || lpad((row_number() OVER (ORDER BY d.development))::text, 3, '0'),
       COALESCE(d.total, 0)
FROM (SELECT development, SUM(planned_value) AS total FROM lots GROUP BY development) d;

-- 2) Quadras a partir dos pares (development, block) distintos
INSERT INTO blocks (id, development_id, name, internal_code)
SELECT gen_random_uuid(), dev.id, b.bname,
       dev.internal_code || '-Q' ||
         lpad((row_number() OVER (PARTITION BY b.development ORDER BY b.bname))::text, 2, '0')
FROM (SELECT DISTINCT development, COALESCE(NULLIF(block, ''), 'Única') AS bname FROM lots) b
JOIN developments dev ON dev.name = b.development;

-- 3) Vincula cada lote à sua quadra
UPDATE lots l SET block_id = bk.id
FROM blocks bk JOIN developments dv ON dv.id = bk.development_id
WHERE dv.name = l.development AND bk.name = COALESCE(NULLIF(l.block, ''), 'Única');

-- 4) Codigo interno do lote (sequencial dentro da quadra)
WITH numbered AS (
  SELECT l.id,
         bk.internal_code || '-L' ||
           lpad((row_number() OVER (PARTITION BY l.block_id ORDER BY l.name, l.created_at))::text, 3, '0') AS code
  FROM lots l JOIN blocks bk ON bk.id = l.block_id
)
UPDATE lots l SET internal_code = n.code FROM numbered n WHERE n.id = l.id;

-- 5) Preenche os limites (qtd) do empreendimento com o que existe hoje
UPDATE developments d SET
  blocks_count = (SELECT count(*) FROM blocks b WHERE b.development_id = d.id),
  lots_count   = (SELECT count(*) FROM lots l JOIN blocks b ON b.id = l.block_id WHERE b.development_id = d.id);

-- 6) sale_value passa a significar "valor REAL vendido": preenche dos lotes vendidos
UPDATE lots SET sale_value = NULL;
UPDATE lots l SET sale_value = s.total_value
FROM property_sales s
WHERE s.property_id = l.id AND l.status = 'SOLD';

-- 7) Limpeza: campos agora relacionais e obrigatoriedades
ALTER TABLE lots ALTER COLUMN block_id SET NOT NULL;
ALTER TABLE lots ALTER COLUMN internal_code SET NOT NULL;
ALTER TABLE lots ADD CONSTRAINT uq_lots_internal_code UNIQUE (internal_code);
ALTER TABLE lots DROP COLUMN development;
ALTER TABLE lots DROP COLUMN block;

-- 8) Renomeia a FK da venda para refletir o novo nome
ALTER TABLE property_sales RENAME COLUMN property_id TO lot_id;
