package com.construtora.financeiro.dto.distrato;

import com.construtora.financeiro.model.enums.DistratoFinancialOutcome;
import com.construtora.financeiro.model.enums.DistratoFinancialRule;
import com.construtora.financeiro.model.enums.DistratoStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Distrato completo (listagem e detalhe). */
public record DistratoResponse(
        UUID id,
        UUID saleId,
        String contractNumber,
        UUID clientId,
        String clientName,
        UUID lotId,
        String developmentName,
        String blockName,
        String lotName,
        DistratoStatus status,
        DistratoFinancialRule financialRule,
        String reason,
        BigDecimal contractTotal,
        BigDecimal paidAmount,
        BigDecimal defaultRetentionPercent,
        BigDecimal usedRetentionPercent,
        String retentionChangeReason,
        BigDecimal retentionAmount,
        BigDecimal overdueAmount,
        BigDecimal chargesAmount,
        BigDecimal totalDebtAmount,
        BigDecimal finalBalance,
        DistratoFinancialOutcome financialOutcome,
        BigDecimal financialEntryAmount,
        UUID payableId,
        UUID receivableId,
        UUID requestedBy,
        UUID approvedBy,
        UUID settledBy,
        OffsetDateTime requestedAt,
        OffsetDateTime approvedAt,
        OffsetDateTime concludedAt,
        String calculationMemory,
        OffsetDateTime createdAt
) {}
