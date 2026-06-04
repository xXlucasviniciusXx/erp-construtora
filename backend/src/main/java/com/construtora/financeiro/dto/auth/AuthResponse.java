package com.construtora.financeiro.dto.auth;

import java.util.List;
import java.util.UUID;

public record AuthResponse(
        String token,
        String tokenType,
        long expiresInMs,
        UUID userId,
        String name,
        String email,
        String role,
        List<String> permissions,
        String refreshToken      // null em endpoints que não emitem refresh token
) {}
