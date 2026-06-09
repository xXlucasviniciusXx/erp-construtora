package com.construtora.financeiro.dto.lot;

import com.construtora.financeiro.model.enums.PropertyStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record LotRequest(
        @NotNull java.util.UUID blockId,
        @NotBlank String name,
        String registration,        // matrícula (manual, opcional)
        String unit,
        String address,
        BigDecimal totalArea,
        BigDecimal builtArea,
        BigDecimal plannedValue,    // valor previsto
        BigDecimal retentionPercent, // % padrão de retenção no distrato
        PropertyStatus status,
        String contractExtra,
        String notes
) {}
