package com.construtora.financeiro.dto.reconciliation;

import com.construtora.financeiro.model.enums.TargetType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Lançamento em aberto candidato à conciliação manual (qualquer valor). */
public record ManualTargetResponse(
        TargetType targetType,
        UUID targetId,
        String label,
        BigDecimal amount,
        LocalDate dueDate
) {}
