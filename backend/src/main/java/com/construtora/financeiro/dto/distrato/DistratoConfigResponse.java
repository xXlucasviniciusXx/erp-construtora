package com.construtora.financeiro.dto.distrato;

import com.construtora.financeiro.model.enums.DistratoFinancialRule;

import java.util.UUID;

public record DistratoConfigResponse(
        UUID id,
        UUID developmentId,        // null = global
        String developmentName,    // null = global
        DistratoFinancialRule financialRule,
        boolean active
) {}
