-- ============================================================
-- Plano de contas estruturado (2 níveis: grupo → item)
--   • Categorias   = natureza do gasto (Terraplanagem, Salários…)
--   • Centros de Custo = área/responsabilidade (Obras, Comercial…)
--   • Empreendimento = dimensão própria (FK já existente em V10)
-- Categoria e Centro de Custo passam a ser FK em Contas a Pagar
-- (antes eram texto livre). Receitas ficam de fora por ora.
-- ============================================================

-- ---------- Categorias (natureza da despesa) ----------
CREATE TABLE categories (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    grupo       VARCHAR(80)  NOT NULL,
    name        VARCHAR(120) NOT NULL,
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    UNIQUE (grupo, name)
);
CREATE INDEX idx_categories_grupo ON categories(grupo);

INSERT INTO categories (grupo, name) VALUES
    ('Infraestrutura e Obras','Terraplanagem'),
    ('Infraestrutura e Obras','Pavimentação'),
    ('Infraestrutura e Obras','Rede de Água'),
    ('Infraestrutura e Obras','Rede de Esgoto'),
    ('Infraestrutura e Obras','Rede Elétrica'),
    ('Infraestrutura e Obras','Iluminação Pública'),
    ('Infraestrutura e Obras','Drenagem Pluvial'),
    ('Infraestrutura e Obras','Meio-fio'),
    ('Infraestrutura e Obras','Paisagismo'),
    ('Infraestrutura e Obras','Sinalização'),
    ('Engenharia e Projetos','Projetos Arquitetônicos'),
    ('Engenharia e Projetos','Projetos de Engenharia'),
    ('Engenharia e Projetos','Topografia'),
    ('Engenharia e Projetos','Consultoria Técnica'),
    ('Engenharia e Projetos','Fiscalização de Obras'),
    ('Documentação e Regularização','Cartório'),
    ('Documentação e Regularização','Registro de Imóveis'),
    ('Documentação e Regularização','Licenciamento Ambiental'),
    ('Documentação e Regularização','Taxas Municipais'),
    ('Documentação e Regularização','Taxas Estaduais'),
    ('Documentação e Regularização','Taxas Federais'),
    ('Documentação e Regularização','Regularização Fundiária'),
    ('Comercial','Comissão de Corretores'),
    ('Comercial','Bonificações'),
    ('Comercial','Premiações'),
    ('Comercial','Material Comercial'),
    ('Comercial','Atendimento ao Cliente'),
    ('Marketing','Redes Sociais'),
    ('Marketing','Google Ads'),
    ('Marketing','Meta Ads'),
    ('Marketing','Produção de Conteúdo'),
    ('Marketing','Impressos'),
    ('Marketing','Eventos'),
    ('Marketing','Outdoors'),
    ('Administrativo','Material de Escritório'),
    ('Administrativo','Correios'),
    ('Administrativo','Assinaturas'),
    ('Administrativo','Consultorias'),
    ('Recursos Humanos','Salários'),
    ('Recursos Humanos','Pró-labore'),
    ('Recursos Humanos','Férias'),
    ('Recursos Humanos','Rescisões'),
    ('Recursos Humanos','Benefícios'),
    ('Recursos Humanos','Treinamentos'),
    ('Tecnologia','Software'),
    ('Tecnologia','Hospedagem'),
    ('Tecnologia','Licenças'),
    ('Tecnologia','Equipamentos'),
    ('Veículos','Combustível'),
    ('Veículos','Manutenção'),
    ('Veículos','Seguro'),
    ('Veículos','Pedágio'),
    ('Veículos','Documentação'),
    ('Utilidades','Energia Elétrica'),
    ('Utilidades','Água'),
    ('Utilidades','Internet'),
    ('Utilidades','Telefonia'),
    ('Financeiro','Juros Pagos'),
    ('Financeiro','Multas Pagas'),
    ('Financeiro','Tarifas Bancárias'),
    ('Financeiro','Emissão de Boletos'),
    ('Financeiro','Taxas de Cartão'),
    ('Financeiro','Taxas de PIX'),
    ('Outros','Despesas Diversas'),
    ('Outros','Ajustes Financeiros'),
    ('Outros','Estornos');

-- ---------- Centros de Custo: grupo (2 níveis) e limpeza ----------
ALTER TABLE cost_centers ADD COLUMN grupo VARCHAR(80);

-- Desativa centros "por empreendimento": o empreendimento já é uma FK própria
UPDATE cost_centers SET active = FALSE
 WHERE name IN ('Obra Parque', 'Obra Villa', 'Obra Jardins');

-- Classifica os centros genéricos existentes
UPDATE cost_centers SET grupo = 'Administrativo' WHERE name = 'Administrativo';
UPDATE cost_centers SET grupo = 'Comercial'      WHERE name = 'Comercial';

-- Novos centros de custo (áreas/departamentos)
INSERT INTO cost_centers (name, description, grupo) VALUES
    ('Administração Geral',    'Gestão e direção',            'Administrativo'),
    ('Recursos Humanos',       'Pessoal e folha',            'Administrativo'),
    ('Departamento Financeiro','Tesouraria e contas',        'Administrativo'),
    ('Contabilidade',          'Contabilidade e fiscal',     'Administrativo'),
    ('Jurídico',               'Assessoria jurídica',        'Administrativo'),
    ('Tecnologia da Informação','TI e sistemas',             'Administrativo'),
    ('Vendas',                 'Equipe comercial',           'Comercial'),
    ('Marketing',              'Marketing e publicidade',    'Comercial'),
    ('Atendimento ao Cliente', 'SAC e pós-venda',            'Comercial'),
    ('Obras',                  'Execução de obras',          'Operacional'),
    ('Engenharia',             'Engenharia e projetos',      'Operacional'),
    ('Manutenção Predial',     'Manutenção de estruturas',   'Operacional'),
    ('Frota',                  'Veículos e equipamentos',    'Operacional')
ON CONFLICT (name) DO NOTHING;

-- ---------- Contas a Pagar: categoria e centro de custo como FK ----------
ALTER TABLE accounts_payable ADD COLUMN category_id    UUID REFERENCES categories(id);
ALTER TABLE accounts_payable ADD COLUMN cost_center_id UUID REFERENCES cost_centers(id);

-- Backfill: garante que todo texto antigo vire um registro mestre (grupo "Outros")
INSERT INTO categories (grupo, name)
    SELECT DISTINCT 'Outros', ap.category
    FROM accounts_payable ap
    WHERE ap.category IS NOT NULL AND ap.category <> ''
      AND ap.category NOT IN (SELECT name FROM categories);

INSERT INTO cost_centers (name, grupo)
    SELECT DISTINCT ap.cost_center, 'Outros'
    FROM accounts_payable ap
    WHERE ap.cost_center IS NOT NULL AND ap.cost_center <> ''
      AND ap.cost_center NOT IN (SELECT name FROM cost_centers)
    ON CONFLICT (name) DO NOTHING;

-- Vincula os registros antigos às novas FKs pelo nome
UPDATE accounts_payable ap SET category_id = c.id
    FROM categories c WHERE c.name = ap.category;
UPDATE accounts_payable ap SET cost_center_id = cc.id
    FROM cost_centers cc WHERE cc.name = ap.cost_center;

-- Remove as colunas de texto livre (substituídas pelas FKs)
ALTER TABLE accounts_payable DROP COLUMN category;
ALTER TABLE accounts_payable DROP COLUMN cost_center;

CREATE INDEX idx_ap_category    ON accounts_payable(category_id);
CREATE INDEX idx_ap_cost_center ON accounts_payable(cost_center_id);
