-- =====================================================================
-- Receitas Financeiras separadas: registra, na baixa da parcela, quanto foi
-- principal x juros x multa. Permite distinguir "Receita de Vendas" (principal)
-- de "Receitas Financeiras" (juros/multa) na DRE com valores efetivamente
-- recebidos, em vez de cálculo aproximado em tempo real.
-- =====================================================================
ALTER TABLE installments ADD COLUMN paid_principal NUMERIC(15,2);
ALTER TABLE installments ADD COLUMN paid_interest  NUMERIC(15,2);
ALTER TABLE installments ADD COLUMN paid_penalty   NUMERIC(15,2);

-- Backfill das parcelas já pagas usando a MESMA fórmula do LateFeeCalculator
-- (o encargo que o cliente efetivamente vê/paga): principal = valor da parcela;
-- multa = valor x taxa fixa; juros de mora = valor x taxa mensal / 30 x dias de
-- atraso (pro rata die). Atraso = payment_date - due_date (dias), quando > 0.
-- Obs.: a DRE antiga calculava os juros SEM o /30 (superestimando ~30x); ao
-- passar a ler estas colunas, a "Receita Financeira" fica igual ao encargo real.
UPDATE installments i SET
    paid_principal = i.amount,
    paid_penalty = case
        when i.payment_date is not null and i.payment_date > i.due_date
            then round(i.amount * coalesce(ps.penalty_rate, 0) / 100.0, 2)
        else 0 end,
    paid_interest = case
        when i.payment_date is not null and i.payment_date > i.due_date
            then round(i.amount * coalesce(ps.interest_rate, 0) / 100.0 / 30.0 * (i.payment_date - i.due_date), 2)
        else 0 end
FROM property_sales ps
WHERE ps.id = i.sale_id AND i.status = 'PAID';
