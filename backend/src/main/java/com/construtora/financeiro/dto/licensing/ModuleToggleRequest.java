package com.construtora.financeiro.dto.licensing;

import jakarta.validation.constraints.NotNull;

public record ModuleToggleRequest(
        @NotNull Boolean active
) {}
