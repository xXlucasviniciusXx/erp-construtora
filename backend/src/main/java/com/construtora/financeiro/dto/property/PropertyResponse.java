package com.construtora.financeiro.dto.property;

import com.construtora.financeiro.model.enums.PropertyStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record PropertyResponse(
        UUID id,
        String development,
        String block,
        String lot,
        String unit,
        String registration,
        String address,
        BigDecimal totalArea,
        BigDecimal builtArea,
        BigDecimal saleValue,
        PropertyStatus status,
        String contractExtra,
        String notes,
        OffsetDateTime createdAt
) {}
