package com.construtora.financeiro.dto.category;

import jakarta.validation.constraints.NotBlank;

public record CategoryRequest(
        @NotBlank String grupo,
        @NotBlank String name,
        Boolean active
) {}
