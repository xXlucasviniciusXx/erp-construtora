package com.construtora.financeiro.dto.reconciliation;

import com.construtora.financeiro.model.enums.TargetType;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ReconcileRequest(
        @NotNull TargetType targetType,
        @NotNull UUID targetId,
        String notes
) {}
