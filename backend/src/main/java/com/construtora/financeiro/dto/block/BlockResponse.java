package com.construtora.financeiro.dto.block;

import java.math.BigDecimal;
import java.util.UUID;

public record BlockResponse(
        UUID id,
        UUID developmentId,
        String developmentName,
        String name,
        String internalCode,
        String registration,
        BigDecimal area,
        long lotsCount
) {}
