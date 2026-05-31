package com.construtora.financeiro.dto.dashboard;

import java.math.BigDecimal;

public record DashboardResponse(
        BigDecimal totalReceivableMonth,
        BigDecimal totalPayableMonth,
        BigDecimal expectedBalance,
        long overdueInstallments,
        long upcomingInstallments,
        long pendingBankTransactions,
        long availableProperties,
        long soldProperties
) {}
