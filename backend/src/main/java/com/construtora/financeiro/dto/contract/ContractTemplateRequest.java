package com.construtora.financeiro.dto.contract;

import jakarta.validation.constraints.NotBlank;

/** Criação/edição de um modelo de contrato/distrato. */
public record ContractTemplateRequest(
        String kind,                 // CONTRACT | DISTRATO (default CONTRACT)
        @NotBlank String name,
        @NotBlank String body,
        Boolean active,
        Boolean isDefault
) {}
