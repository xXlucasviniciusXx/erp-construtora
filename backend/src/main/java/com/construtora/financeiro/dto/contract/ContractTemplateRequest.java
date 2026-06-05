package com.construtora.financeiro.dto.contract;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

/** Criação/edição de um modelo de contrato/distrato. */
public record ContractTemplateRequest(
        String kind,                 // CONTRACT | DISTRATO (default CONTRACT)
        @NotBlank String name,
        @NotBlank String body,
        UUID developmentId,          // null = global (qualquer empreendimento)
        Boolean active,
        Boolean isDefault
) {}
