package com.construtora.financeiro.dto.role;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record RoleRequest(
        @NotBlank String name,
        String description,
        List<String> permissions
) {}
