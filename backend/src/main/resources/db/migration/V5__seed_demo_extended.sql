-- ============================================================
--  V5 — Dados de demonstração ampliados (POC).
--  Clientes em diferentes situações, empreendimentos, lotes,
--  vendas com parcelas (statuses variados), contas a pagar/receber.
--  Datas relativas a CURRENT_DATE para sempre fazerem sentido.
--  Remova/edite em produção.
-- ============================================================

-- ---------- Clientes ----------
INSERT INTO clients (id, person_type, name, document, state_registration, email, phone, address, city, state, zip_code, marital_status, occupation, status) VALUES
 ('a0000000-0000-0000-0000-000000000001','PF','Ana Souza',          '52998224725', null, 'ana@example.com',     '11991000001','Rua A, 10','São Paulo','SP','01001000','Solteira','Engenheira','ACTIVE'),
 ('a0000000-0000-0000-0000-000000000002','PF','Bruno Lima',         '15350946056', null, 'bruno@example.com',   '11991000002','Rua B, 20','Campinas','SP','13010000','Casado','Comerciante','ACTIVE'),
 ('a0000000-0000-0000-0000-000000000003','PF','Carla Mendes',       '11122233396', null, 'carla@example.com',   '11991000003','Rua C, 30','Santos','SP','11010000','Divorciada','Professora','ACTIVE'),
 ('a0000000-0000-0000-0000-000000000004','PF','Diego Antunes',      '39053344705', null, 'diego@example.com',   '11991000004','Rua D, 40','Sorocaba','SP','18010000','Casado','Médico','ACTIVE'),
 ('a0000000-0000-0000-0000-000000000005','PF','Eva Carvalho',       '90462437012', null, 'eva@example.com',     '11991000005','Rua E, 50','Jundiaí','SP','13201000','Viúva','Aposentada','INACTIVE'),
 ('a0000000-0000-0000-0000-000000000006','PJ','Construtora XPTO Ltda','19131243000197','ISENTO','contato@xpto.com.br','1133000006','Av. Paulista, 1000','São Paulo','SP','01310100',null,null,'ACTIVE'),
 ('a0000000-0000-0000-0000-000000000007','PJ','Imobiliária ABC S/A','45448325000192','123456789','abc@abc.com.br','1133000007','Av. Brasil, 500','Rio de Janeiro','RJ','20040002',null,null,'ACTIVE'),
 ('a0000000-0000-0000-0000-000000000008','PF','Fernanda Rocha',     '49892710000', null, 'fernanda@example.com','11991000008','Rua F, 60','Ribeirão Preto','SP','14010000','Solteira','Advogada','ACTIVE');

-- ---------- Empreendimentos / Lotes ----------
INSERT INTO properties (id, development, block, lot, unit, registration, address, total_area, built_area, sale_value, status) VALUES
 ('b0000000-0000-0000-0000-000000000001','Parque das Águas','A','01',null,'M-2001','Quadra A, Lote 01', 300.00, 0,    180000.00,'SOLD'),
 ('b0000000-0000-0000-0000-000000000002','Parque das Águas','A','02',null,'M-2002','Quadra A, Lote 02', 300.00, 0,    185000.00,'SOLD'),
 ('b0000000-0000-0000-0000-000000000003','Parque das Águas','B','05',null,'M-2005','Quadra B, Lote 05', 320.00, 0,    195000.00,'SOLD'),
 ('b0000000-0000-0000-0000-000000000004','Parque das Águas','B','06',null,'M-2006','Quadra B, Lote 06', 320.00, 0,    195000.00,'AVAILABLE'),
 ('b0000000-0000-0000-0000-000000000005','Villa Toscana','1','10','101','M-3101','Rua das Oliveiras, 100', 200.00, 95.00, 420000.00,'SOLD'),
 ('b0000000-0000-0000-0000-000000000006','Villa Toscana','1','11','102','M-3102','Rua das Oliveiras, 102', 200.00, 95.00, 430000.00,'SOLD'),
 ('b0000000-0000-0000-0000-000000000007','Villa Toscana','2','20','201','M-3201','Rua das Oliveiras, 200', 210.00, 98.00, 450000.00,'AVAILABLE'),
 ('b0000000-0000-0000-0000-000000000008','Residencial Jardins','C','03',null,'M-1003','Rua das Flores, 110', 250.00, 0, 355000.00,'AVAILABLE');

-- ---------- Vendas + Parcelas ----------
-- Diego: contrato QUITADO (12 parcelas, todas pagas)
INSERT INTO property_sales (id, client_id, property_id, total_value, down_payment, installments_count, first_due_date, payment_method, correction_index, status, sale_date)
VALUES ('c0000000-0000-0000-0000-000000000001','a0000000-0000-0000-0000-000000000004','b0000000-0000-0000-0000-000000000001',180000.00,30000.00,12,(current_date - interval '12 months')::date,'Entrada + parcelas','INCC','COMPLETED',(current_date - interval '13 months')::date);
INSERT INTO installments (sale_id, number, amount, due_date, status, payment_date, payment_method)
SELECT 'c0000000-0000-0000-0000-000000000001', g, 12500.00,
       ((current_date - interval '12 months')::date + ((g-1) * interval '1 month'))::date,
       'PAID', ((current_date - interval '12 months')::date + ((g-1) * interval '1 month'))::date, 'Boleto'
FROM generate_series(1,12) g;

-- Bruno: ativo COM PARCELAS EM ATRASO (24 parcelas; 3 pagas, atrasadas no meio, futuras em aberto)
INSERT INTO property_sales (id, client_id, property_id, total_value, down_payment, installments_count, first_due_date, payment_method, correction_index, status, sale_date)
VALUES ('c0000000-0000-0000-0000-000000000002','a0000000-0000-0000-0000-000000000002','b0000000-0000-0000-0000-000000000002',185000.00,25000.00,24,(current_date - interval '6 months')::date,'Boleto','IGP-M','ACTIVE',(current_date - interval '6 months')::date);
INSERT INTO installments (sale_id, number, amount, due_date, status, payment_date, payment_method)
SELECT 'c0000000-0000-0000-0000-000000000002', g, 6666.67,
       ((current_date - interval '6 months')::date + ((g-1) * interval '1 month'))::date,
       case when g <= 3 then 'PAID'
            when ((current_date - interval '6 months')::date + ((g-1) * interval '1 month'))::date < current_date then 'OVERDUE'
            else 'OPEN' end,
       case when g <= 3 then ((current_date - interval '6 months')::date + ((g-1) * interval '1 month'))::date else null end,
       'Boleto'
FROM generate_series(1,24) g;

-- Carla: INADIMPLENTE (36 parcelas; várias atrasadas)
INSERT INTO property_sales (id, client_id, property_id, total_value, down_payment, installments_count, first_due_date, payment_method, correction_index, interest_rate, penalty_rate, status, sale_date)
VALUES ('c0000000-0000-0000-0000-000000000003','a0000000-0000-0000-0000-000000000003','b0000000-0000-0000-0000-000000000003',195000.00,15000.00,36,(current_date - interval '10 months')::date,'Financiamento próprio','IPCA',1.00,2.00,'ACTIVE',(current_date - interval '10 months')::date);
INSERT INTO installments (sale_id, number, amount, due_date, status, payment_date, payment_method)
SELECT 'c0000000-0000-0000-0000-000000000003', g, 5000.00,
       ((current_date - interval '10 months')::date + ((g-1) * interval '1 month'))::date,
       case when g <= 2 then 'PAID'
            when ((current_date - interval '10 months')::date + ((g-1) * interval '1 month'))::date < current_date then 'OVERDUE'
            else 'OPEN' end,
       case when g <= 2 then ((current_date - interval '10 months')::date + ((g-1) * interval '1 month'))::date else null end,
       'Boleto'
FROM generate_series(1,36) g;

-- Fernanda: venda À VISTA (1 parcela paga, sem entrada)
INSERT INTO property_sales (id, client_id, property_id, total_value, down_payment, installments_count, first_due_date, payment_method, correction_index, status, sale_date)
VALUES ('c0000000-0000-0000-0000-000000000004','a0000000-0000-0000-0000-000000000008','b0000000-0000-0000-0000-000000000005',420000.00,0,1,(current_date - interval '2 months')::date,'À vista','Sem correção','COMPLETED',(current_date - interval '2 months')::date);
INSERT INTO installments (sale_id, number, amount, due_date, status, payment_date, payment_method)
VALUES ('c0000000-0000-0000-0000-000000000004',1,420000.00,(current_date - interval '2 months')::date,'PAID',(current_date - interval '2 months')::date,'PIX');

-- Ana: ativa SEM DÉBITO (parcelas futuras em dia, nenhuma vencida)
INSERT INTO property_sales (id, client_id, property_id, total_value, down_payment, installments_count, first_due_date, payment_method, correction_index, status, sale_date)
VALUES ('c0000000-0000-0000-0000-000000000005','a0000000-0000-0000-0000-000000000001','b0000000-0000-0000-0000-000000000006',430000.00,80000.00,10,(current_date + interval '1 month')::date,'Entrada + parcelas','INCC','ACTIVE',current_date);
INSERT INTO installments (sale_id, number, amount, due_date, status, payment_method)
SELECT 'c0000000-0000-0000-0000-000000000005', g, 35000.00,
       ((current_date + interval '1 month')::date + ((g-1) * interval '1 month'))::date,
       'OPEN','Boleto'
FROM generate_series(1,10) g;

-- Imobiliária ABC: MAIS DE UM LOTE (2 vendas, à vista e parcelada)
INSERT INTO property_sales (id, client_id, property_id, total_value, down_payment, installments_count, first_due_date, payment_method, correction_index, status, sale_date)
VALUES ('c0000000-0000-0000-0000-000000000006','a0000000-0000-0000-0000-000000000007','b0000000-0000-0000-0000-000000000007',450000.00,450000.00,0,current_date,'Transferência bancária','Sem correção','COMPLETED',(current_date - interval '3 months')::date);
INSERT INTO property_sales (id, client_id, property_id, total_value, down_payment, installments_count, first_due_date, payment_method, correction_index, status, sale_date)
VALUES ('c0000000-0000-0000-0000-000000000007','a0000000-0000-0000-0000-000000000007','b0000000-0000-0000-0000-000000000008',355000.00,55000.00,6,(current_date + interval '1 month')::date,'Entrada + parcelas','Juros fixo mensal','ACTIVE',(current_date - interval '1 month')::date);
INSERT INTO installments (sale_id, number, amount, due_date, status, payment_method)
SELECT 'c0000000-0000-0000-0000-000000000007', g, 50000.00,
       ((current_date + interval '1 month')::date + ((g-1) * interval '1 month'))::date,
       'OPEN','Boleto'
FROM generate_series(1,6) g;
-- Lotes da ABC passam a vendidos
UPDATE properties SET status='SOLD' WHERE id IN ('b0000000-0000-0000-0000-000000000007','b0000000-0000-0000-0000-000000000008');

-- ---------- Contas a pagar (pagas, em aberto, vencidas) ----------
INSERT INTO accounts_payable (supplier, category, description, amount, due_date, payment_date, status, payment_method, cost_center) VALUES
 ('Concreteira Forte','Materiais','Concreto usinado', 18500.00,(current_date - interval '20 days')::date,(current_date - interval '18 days')::date,'PAID','Transferência bancária','Obra Parque'),
 ('Elétrica Luz','Serviços','Instalação elétrica', 9200.00,(current_date + interval '7 days')::date, null,'OPEN','Boleto','Obra Villa'),
 ('Prefeitura','Impostos','ISS', 3400.00,(current_date - interval '5 days')::date, null,'OVERDUE','Boleto','Administrativo'),
 ('Imobiliária Parceira','Comissões','Comissão de vendas', 12000.00,(current_date + interval '15 days')::date, null,'OPEN','PIX','Comercial');

-- ---------- Contas a receber (recebidas e em aberto) ----------
INSERT INTO accounts_receivable (client_id, description, amount, due_date, receive_date, status, payment_method) VALUES
 ('a0000000-0000-0000-0000-000000000001','Taxa de reserva', 2000.00,(current_date - interval '10 days')::date,(current_date - interval '9 days')::date,'RECEIVED','PIX'),
 ('a0000000-0000-0000-0000-000000000002','Acordo de quitação parcial', 3500.00,(current_date + interval '5 days')::date,null,'OPEN','Boleto');
