package com.construtora.financeiro.dto.role;

import java.util.List;
import java.util.UUID;

public record RoleResponse(
        UUID id,
        String name,
        String description,
        List<String> permissions,
        /** ADMIN é protegido (não pode ser editado/removido). */
        boolean system,
        /** Quantos usuários usam este papel (impede remoção se > 0). */
        long userCount
) {}
