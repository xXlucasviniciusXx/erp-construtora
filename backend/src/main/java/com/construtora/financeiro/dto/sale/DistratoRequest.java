package com.construtora.financeiro.dto.sale;

import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Dados para distratar (rescindir amigavelmente) uma venda. */
public record DistratoRequest(
        LocalDate distratoDate,
        String reason,
        @PositiveOrZero BigDecimal refundAmount,
        @PositiveOrZero BigDecimal retainedAmount,
        String rule,         // regra aplicada (ex.: "Retenção de 20% sobre o pago")
        String ruleDetail    // memória de cálculo / detalhe da regra
) {}
