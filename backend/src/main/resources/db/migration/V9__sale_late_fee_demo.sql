-- Encargos por atraso usam as taxas cadastradas em cada venda (interest_rate / penalty_rate).
-- A demo nasceu com 0%, então definimos um padrão de mercado (juros 1% a.m. + multa 2%)
-- nas vendas que ainda estão zeradas, para que o cálculo de encargos fique visível.
-- Vendas com taxas já preenchidas (cadastradas pelo usuário) não são alteradas.

UPDATE property_sales
SET interest_rate = 1.00,   -- 1% ao mês (juros de mora, pro rata die)
    penalty_rate  = 2.00    -- 2% (multa fixa por atraso)
WHERE COALESCE(interest_rate, 0) = 0
  AND COALESCE(penalty_rate, 0) = 0;
