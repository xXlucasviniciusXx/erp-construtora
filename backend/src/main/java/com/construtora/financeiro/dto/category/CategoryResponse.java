package com.construtora.financeiro.dto.category;

import java.util.UUID;

public record CategoryResponse(
        UUID id,
        String grupo,
        String name,
        boolean active
) {}
