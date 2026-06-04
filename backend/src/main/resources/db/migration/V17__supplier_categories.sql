-- ============================================================
--  V17: Categorias de fornecedor configuráveis
-- ============================================================
CREATE TABLE supplier_categories (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(100) NOT NULL UNIQUE,
    active      BOOLEAN     NOT NULL DEFAULT TRUE,
    sort_order  INT         NOT NULL DEFAULT 0,
    created_at  TIMESTAMP   NOT NULL DEFAULT NOW()
);

INSERT INTO supplier_categories (name, sort_order) VALUES
    ('Construção Civil',     1),
    ('Material de Obra',     2),
    ('Serviços Gerais',      3),
    ('Mão de Obra',          4),
    ('Tecnologia',           5),
    ('Financeiro / Banco',   6),
    ('Energia / Utilities',  7),
    ('Jurídico / Contábil',  8),
    ('Marketing',            9),
    ('Outro',               99);
