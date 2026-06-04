package com.construtora.financeiro.dto.licensing;

import java.time.LocalDate;

public record LicenseResponse(
        String plan,
        /** Status efetivo: ATIVA, EXPIRADA, SUSPENSA, CANCELADA. */
        String status,
        LocalDate startDate,
        LocalDate endDate,
        int periodMonths,
        Integer maxUsers,
        String notes,
        String customer,
        int graceDays,
        /** Dias até o vencimento (negativo se já venceu); null se sem data de fim. */
        Long daysToExpire,
        boolean expired,
        /** Vencida além da tolerância ou suspensa → sistema em modo só-leitura. */
        boolean readOnly,
        /** Cancelada → acesso bloqueado. */
        boolean blocked,
        /** Se a licença atual veio de uma chave assinada. */
        boolean hasKey
) {}
