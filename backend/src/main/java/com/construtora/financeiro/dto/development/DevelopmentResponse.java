package com.construtora.financeiro.dto.development;

import java.math.BigDecimal;
import java.util.UUID;

public record DevelopmentResponse(
        UUID id,
        String name,
        String internalCode,
        Integer blocksCount,
        Integer lotsCount,
        BigDecimal expectedValue,     // manual
        BigDecimal plannedTotal,      // derivado: soma dos valores previstos dos lotes
        BigDecimal receivedTotal,     // derivado: soma dos valores realmente vendidos
        long actualBlocks,            // quadras já cadastradas
        long actualLots,              // lotes já cadastrados
        String address,
        String status,
        String dimensions
) {}
