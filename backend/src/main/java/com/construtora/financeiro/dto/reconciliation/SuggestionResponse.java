package com.construtora.financeiro.dto.reconciliation;

import com.construtora.financeiro.model.enums.TargetType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record SuggestionResponse(
        TargetType targetType,
        UUID targetId,
        String label,
        BigDecimal amount,
        LocalDate dueDate,
        BigDecimal score,
        String reason
) {}
