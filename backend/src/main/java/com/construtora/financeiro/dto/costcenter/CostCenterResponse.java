package com.construtora.financeiro.dto.costcenter;

import java.util.UUID;

public record CostCenterResponse(
        UUID id,
        String name,
        String description,
        boolean active
) {}
