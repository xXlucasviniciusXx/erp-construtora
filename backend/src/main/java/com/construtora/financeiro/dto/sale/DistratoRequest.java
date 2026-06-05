package com.construtora.financeiro.dto.sale;

import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Dados para distratar (rescindir amigavelmente) uma venda. */
public record DistratoRequest(
        LocalDate distratoDate,
        String reason,
        @PositiveOrZero BigDecimal refundAmount,
        @PositiveOrZero BigDecimal retainedAmount
) {}
