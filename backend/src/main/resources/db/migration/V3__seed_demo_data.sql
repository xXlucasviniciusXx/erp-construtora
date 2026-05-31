-- ============================================================
--  V3 — Dados de demonstração para o POC.
--  Remova/edite este arquivo em produção. As datas usam CURRENT_DATE
--  para manter o dashboard sempre populado no mês corrente.
-- ============================================================

-- Conta bancária
INSERT INTO bank_accounts (id, name, bank_code, bank_name, agency, account_number, initial_balance)
VALUES ('11111111-1111-1111-1111-111111111111', 'Conta Movimento', '237', 'Bradesco', '1234', '56789-0', 10000.00);

-- Clientes
INSERT INTO clients (id, person_type, name, document, email, phone, city, state, status) VALUES
    ('22222222-2222-2222-2222-222222222221', 'PF', 'João da Silva',   '11144477735', 'joao@example.com',  '11999990001', 'São Paulo', 'SP', 'ACTIVE'),
    ('22222222-2222-2222-2222-222222222222', 'PF', 'Maria Oliveira',  '22255588846', 'maria@example.com', '11999990002', 'Campinas',  'SP', 'ACTIVE');

-- Imóveis
INSERT INTO properties (id, development, block, lot, unit, registration, address, total_area, built_area, sale_value, status) VALUES
    ('33333333-3333-3333-3333-333333333331', 'Residencial Jardins', 'A', '01', '101', 'M-1001', 'Rua das Flores, 100', 250.00, 120.00, 350000.00, 'AVAILABLE'),
    ('33333333-3333-3333-3333-333333333332', 'Residencial Jardins', 'A', '02', '102', 'M-1002', 'Rua das Flores, 102', 250.00, 120.00, 360000.00, 'AVAILABLE');

-- Contas a pagar de exemplo
INSERT INTO accounts_payable (supplier, category, description, amount, due_date, status, cost_center) VALUES
    ('Construdata Materiais', 'Materiais', 'Cimento e areia', 4500.00, CURRENT_DATE + 5, 'OPEN', 'Obra Jardins'),
    ('Energia SA', 'Utilidades', 'Conta de energia', 1200.00, CURRENT_DATE + 10, 'OPEN', 'Administrativo');

-- Contas a receber de exemplo (sem vínculo de venda, para demonstrar conciliação)
INSERT INTO accounts_receivable (client_id, description, amount, due_date, status) VALUES
    ('22222222-2222-2222-2222-222222222221', 'Sinal de reserva', 5000.00, CURRENT_DATE + 3, 'OPEN');
