package com.construtora.financeiro.dto.costcenter;

import jakarta.validation.constraints.NotBlank;

public record CostCenterRequest(
        @NotBlank String name,
        String description,
        Boolean active
) {}
