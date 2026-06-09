package com.construtora.financeiro.dto.distrato;

import java.time.LocalDate;

/** Registro da quitação financeira (baixa do AP/AR) que conclui o distrato. */
public record DistratoSettleRequest(
        LocalDate settleDate   // null = data de hoje
) {}
