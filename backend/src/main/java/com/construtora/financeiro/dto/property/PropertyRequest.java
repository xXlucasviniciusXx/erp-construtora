package com.construtora.financeiro.dto.property;

import com.construtora.financeiro.model.enums.PropertyStatus;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

public record PropertyRequest(
        @NotBlank String development,
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
        String notes
) {}
