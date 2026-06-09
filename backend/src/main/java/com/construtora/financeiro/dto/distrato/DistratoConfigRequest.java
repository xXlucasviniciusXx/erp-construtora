package com.construtora.financeiro.dto.distrato;

import com.construtora.financeiro.model.enums.DistratoFinancialRule;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/** Upsert da regra financeira de distrato (global se developmentId == null). */
public record DistratoConfigRequest(
        UUID developmentId,                       // null = configuração global
        @NotNull DistratoFinancialRule financialRule
) {}
