package com.construtora.financeiro.dto.dre;

import com.construtora.financeiro.dto.dashboard.Point;

import java.util.List;

/**
 * Demonstrativo de Resultado (DRE) em base caixa:
 * Receitas (recebidas) − Despesas (pagas) = Resultado.
 */
public record DreResponse(
        List<Point> revenues,      // linhas de receita
        double totalRevenue,
        List<Point> expenses,      // despesas por grupo de categoria
        double totalExpense,
        double result              // totalRevenue − totalExpense
) {}
