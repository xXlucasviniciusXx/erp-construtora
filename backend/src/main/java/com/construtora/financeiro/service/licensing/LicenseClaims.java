package com.construtora.financeiro.service.licensing;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDate;

/** Conteúdo (claims) carregado dentro da chave de licenciamento assinada. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record LicenseClaims(
        String plan,
        String customer,
        LocalDate startDate,
        LocalDate endDate,
        Integer periodMonths,
        Integer maxUsers
) {}
