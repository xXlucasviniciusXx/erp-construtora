package com.construtora.financeiro.dto.distrato;

import com.construtora.financeiro.model.enums.DistratoFinancialRule;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

/** Entrada da simulação de distrato (não persiste nada). */
public record DistratoSimulationRequest(
        @NotNull UUID saleId,
        BigDecimal usedRetentionPercent,   // null = usa o padrão do lote
        DistratoFinancialRule financialRule // null = usa a regra configurada (empreendimento/global)
) {}
