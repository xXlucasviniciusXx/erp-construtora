-- ============================================================
--  V6 — Cadastros de apoio: fornecedores e centros de custo.
--  Usados como listas em outros módulos (ex.: contas a pagar).
-- ============================================================

CREATE TABLE suppliers (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(200) NOT NULL,
    document    VARCHAR(20),                 -- CNPJ/CPF (somente dígitos)
    email       VARCHAR(150),
    phone       VARCHAR(30),
    address     VARCHAR(255),
    city        VARCHAR(120),
    state       VARCHAR(2),
    category    VARCHAR(80),
    notes       TEXT,
    active      BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_suppliers_name ON suppliers(name);

CREATE TABLE cost_centers (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(120) NOT NULL UNIQUE,
    description VARCHAR(255),
    active      BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Seed coerente com os fornecedores/centros já usados nos dados de demo (V3/V5)
INSERT INTO cost_centers (name, description) VALUES
    ('Obra Parque', 'Empreendimento Parque das Águas'),
    ('Obra Villa', 'Empreendimento Villa Toscana'),
    ('Obra Jardins', 'Empreendimento Residencial Jardins'),
    ('Administrativo', 'Despesas administrativas'),
    ('Comercial', 'Despesas comerciais / vendas');

INSERT INTO suppliers (name, document, email, phone, category, city, state) VALUES
    ('Concreteira Forte',     '11222333000181', 'vendas@concreteiraforte.com.br', '1133001001', 'Materiais',   'São Paulo',  'SP'),
    ('Elétrica Luz',          '22333444000172', 'contato@eletricaluz.com.br',     '1133001002', 'Serviços',    'Campinas',   'SP'),
    ('Construdata Materiais', '33444555000163', 'compras@construdata.com.br',     '1133001003', 'Materiais',   'São Paulo',  'SP'),
    ('Energia SA',            '44555666000154', 'atendimento@energiasa.com.br',   '0800001004', 'Utilidades',  'Rio de Janeiro', 'RJ'),
    ('Imobiliária Parceira',  '55666777000145', 'parcerias@imobparceira.com.br',  '1133001005', 'Comissões',   'São Paulo',  'SP');
