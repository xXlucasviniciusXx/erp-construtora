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
        BigDecimal retentionPercent,
        PropertyStatus status,
        String contractExtra,
        String notes,
        String label,                          // "Empreendimento / Quadra / Lote"
        LocalDateTime reservationExpiresAt,    // não-nulo apenas quando RESERVED
        UUID clientId,                         // comprador da venda ATIVA (null se não vendido)
        String clientName,                     // nome do comprador da venda ATIVA
        String saleStatus                      // status da venda ATIVA (ex.: ACTIVE)
) {}
