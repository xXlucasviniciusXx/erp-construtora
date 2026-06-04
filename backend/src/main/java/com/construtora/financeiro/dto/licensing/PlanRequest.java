package com.construtora.financeiro.dto.licensing;

import jakarta.validation.constraints.NotBlank;

/** Aplica um plano manualmente (liga o pacote de módulos correspondente). */
public record PlanRequest(
        @NotBlank String plan
) {}
