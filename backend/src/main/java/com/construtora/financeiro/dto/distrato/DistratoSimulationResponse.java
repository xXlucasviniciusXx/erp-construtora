package com.construtora.financeiro.dto.distrato;

import com.construtora.financeiro.model.enums.DistratoFinancialOutcome;
import com.construtora.financeiro.model.enums.DistratoFinancialRule;

import java.math.BigDecimal;
import java.util.UUID;

/** Resultado da simulação do distrato exibido antes da aprovação. */
public record DistratoSimulationResponse(
        UUID saleId,
        String contractNumber,
        String clientName,
        String developmentName,
        String blockName,
        String lotName,
        BigDecimal contractTotal,
        BigDecimal paidAmount,
        BigDecimal defaultRetentionPercent,
        BigDecimal usedRetentionPercent,
        BigDecimal retentionAmount,
        DistratoFinancialRule financialRule,
        BigDecimal overdueAmount,
        BigDecimal chargesAmount,
        BigDecimal totalDebtAmount,
        BigDecimal deductions,
        BigDecimal finalBalance,
        DistratoFinancialOutcome financialOutcome,
        BigDecimal financialEntryAmount,   // valor do lançamento (AP/AR); 0 se ZERO
        String reason
) {}
