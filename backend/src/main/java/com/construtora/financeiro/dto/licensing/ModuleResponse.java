package com.construtora.financeiro.dto.licensing;

import java.util.UUID;

public record ModuleResponse(
        UUID id,
        String code,
        String name,
        String description,
        boolean active,
        int sortOrder
) {}
