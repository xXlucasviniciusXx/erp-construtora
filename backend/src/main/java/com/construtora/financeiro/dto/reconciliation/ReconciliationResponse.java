package com.construtora.financeiro.dto.reconciliation;

import com.construtora.financeiro.model.enums.ReconciliationMode;
import com.construtora.financeiro.model.enums.TargetType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ReconciliationResponse(
        UUID id,
        UUID bankTransactionId,
        TargetType targetType,
        UUID targetId,
        BigDecimal matchedAmount,
        ReconciliationMode mode,
        BigDecimal confidence,
        OffsetDateTime reconciledAt,
        boolean undone,
        String notes
) {}
