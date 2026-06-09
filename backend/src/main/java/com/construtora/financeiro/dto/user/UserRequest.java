package com.construtora.financeiro.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.util.List;
import java.util.UUID;

public record UserRequest(
        @NotBlank String name,
        @NotBlank @Email String email,
        String password,          // obrigatório na criação; opcional na edição
        @NotBlank String role,    // ADMIN, FINANCEIRO, ...
        Boolean active,
        List<UUID> developmentIds // empreendimentos vinculados (escopo de acesso); null = não altera
) {}
