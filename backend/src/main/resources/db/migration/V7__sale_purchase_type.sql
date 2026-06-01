-- ============================================================
--  V7 — Separa "forma de compra" da "forma de pagamento" na venda.
--  Forma de compra: À vista / Entrada + parcelas / Financiamento próprio.
--  Forma de pagamento: Boleto / PIX / Transferência / Cartão / ...
-- ============================================================
ALTER TABLE property_sales ADD COLUMN purchase_type VARCHAR(40);

-- Migra os valores que estavam erroneamente em payment_method
UPDATE property_sales
   SET purchase_type = payment_method
 WHERE payment_method IN ('À vista', 'Entrada + parcelas', 'Financiamento próprio');

-- Limpa o payment_method desses casos (passa a ser preenchido separadamente)
UPDATE property_sales
   SET payment_method = NULL
 WHERE payment_method IN ('À vista', 'Entrada + parcelas', 'Financiamento próprio');
