package com.construtora.financeiro.dto.block;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record BlockRequest(
        @NotNull UUID developmentId,
        @NotBlank String name,
        String registration,   // matrícula (manual, opcional)
        BigDecimal area
) {}
