package com.construtora.financeiro.dto.distrato;

import com.construtora.financeiro.model.enums.DistratoFinancialRule;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

/** Solicitação de distrato (status inicial SOLICITADO). */
public record DistratoCreateRequest(
        @NotNull UUID saleId,
        String reason,
        BigDecimal usedRetentionPercent,    // null = usa o padrão do lote
        DistratoFinancialRule financialRule, // null = usa a regra configurada
        String retentionChangeReason         // obrigatório se percentual != padrão do lote
) {}
