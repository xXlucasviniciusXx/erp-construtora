-- Vínculo de Contas a Pagar com Empreendimento.
-- O campo é OPCIONAL (nullable): despesas administrativas gerais ficam sem
-- empreendimento (tratadas como "Geral / Administrativo"); despesas de obra
-- são vinculadas ao empreendimento correspondente.
-- Registros antigos permanecem com development_id = NULL (compatível).

ALTER TABLE accounts_payable
    ADD COLUMN development_id UUID REFERENCES developments(id);

CREATE INDEX idx_ap_development ON accounts_payable(development_id);

-- ---- Seeds demo: despesas de obra vinculadas a empreendimentos ----
-- (usam subselect pelo código interno; só inserem se o empreendimento existir)

INSERT INTO accounts_payable (supplier, category, description, amount, due_date, payment_date, status, payment_method, cost_center, development_id)
SELECT 'Terraplanagem Brasil Ltda', 'Obra', 'Terraplanagem e drenagem', 120000.00, DATE '2026-02-10', DATE '2026-02-10', 'PAID', 'Transferência bancária', 'Obras', d.id
FROM developments d WHERE d.internal_code = 'E001';

INSERT INTO accounts_payable (supplier, category, description, amount, due_date, payment_date, status, payment_method, cost_center, development_id)
SELECT 'Depósito Materiais Forte', 'Material', 'Material de construção - 1ª etapa', 80000.00, DATE '2026-03-05', DATE '2026-03-05', 'PAID', 'Boleto', 'Obras', d.id
FROM developments d WHERE d.internal_code = 'E001';

INSERT INTO accounts_payable (supplier, category, description, amount, due_date, payment_date, status, payment_method, cost_center, development_id)
SELECT 'Pavimentação Sul', 'Obra', 'Pavimentação asfáltica das vias', 150000.00, DATE '2026-04-12', DATE '2026-04-12', 'PAID', 'Transferência bancária', 'Obras', d.id
FROM developments d WHERE d.internal_code = 'E002';

INSERT INTO accounts_payable (supplier, category, description, amount, due_date, payment_date, status, payment_method, cost_center, development_id)
SELECT 'Engenharia & Projetos', 'Serviços', 'Projeto arquitetônico e aprovação', 90000.00, DATE '2026-01-20', DATE '2026-01-20', 'PAID', 'PIX', 'Projetos', d.id
FROM developments d WHERE d.internal_code = 'E003';

INSERT INTO accounts_payable (supplier, category, description, amount, due_date, status, payment_method, cost_center, development_id)
SELECT 'Construtora Toscana Obras', 'Obra', 'Infraestrutura - rede elétrica', 50000.00, DATE '2026-07-15', 'OPEN', 'Boleto', 'Obras', d.id
FROM developments d WHERE d.internal_code = 'E003';
