package com.construtora.financeiro.dto.lot;

import com.construtora.financeiro.model.enums.PropertyStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record LotResponse(
        UUID id,
        UUID blockId,
        String blockName,
        UUID developmentId,
        String developmentName,
        String name,
        String internalCode,
        String registration,
        String unit,
        String address,
        BigDecimal totalArea,
        BigDecimal builtArea,
        BigDecimal plannedValue,
        BigDecimal saleValue,
        PropertyStatus status,
        String contractExtra,
        String notes,
        String label,                          // "Empreendimento / Quadra / Lote"
        LocalDateTime reservationExpiresAt     // não-nulo apenas quando RESERVED
) {}
