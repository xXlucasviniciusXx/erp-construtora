package com.construtora.financeiro.dto.development;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

public record DevelopmentRequest(
        @NotBlank String name,
        Integer blocksCount,
        Integer lotsCount,
        BigDecimal expectedValue,
        String address,
        String status,
        String dimensions
) {}
