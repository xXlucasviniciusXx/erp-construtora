package com.construtora.financeiro.dto.licensing;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

/** Geração de uma chave assinada (uso interno/teste; na Fase 5 vira o painel). */
public record LicenseKeyGenerateRequest(
        @NotBlank String plan,
        String customer,
        LocalDate startDate,
        Integer periodMonths,
        Integer maxUsers
) {}
