package com.construtora.financeiro.dto.dashboard;

import java.math.BigDecimal;
import java.util.List;

/** Indicadores e séries para os gráficos do dashboard. */
public record DashboardAnalyticsResponse(
        // ---- Cards resumidos ----
        BigDecimal totalSold,
        BigDecimal totalReceived,
        BigDecimal totalOpen,
        BigDecimal totalOverdue,
        long delinquentClients,
        long activeClients,
        long inactiveClients,
        long lotsSold,
        long lotsAvailable,
        // ---- Séries (gráficos) ----
        List<Point> receivedByMonth,
        List<Point> toReceiveByMonth,
        List<Point> overdueByMonth,
        List<Point> delinquencyByDevelopment,
        List<Point> salesByMonth,
        List<Point> salesByPurchaseType,
        List<Point> cashFlowForecast,
        List<Point> payablesPaidVsOpen,
        List<Point> receivablesReceivedVsOpen,
        List<Point> overdueByAging
) {}
