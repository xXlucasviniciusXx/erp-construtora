package com.construtora.financeiro.dto.user;

import java.time.OffsetDateTime;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String name,
        String email,
        String role,
        boolean active,
        OffsetDateTime createdAt
) {}
