package com.construtora.financeiro.dto.role;

public record PermissionResponse(
        String code,
        String description,
        /** Módulo derivado do código (ex.: CLIENTES, SISTEMA). */
        String module,
        /** Ação derivada: VIEW, EDIT ou MANAGE. */
        String action
) {}
