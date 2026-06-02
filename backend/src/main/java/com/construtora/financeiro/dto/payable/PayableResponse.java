package com.construtora.financeiro.dto.payable;

import com.construtora.financeiro.model.enums.PayableStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record PayableResponse(
        UUID id,
        String supplier,
        String category,
        String description,
        BigDecimal amount,
        LocalDate dueDate,
        LocalDate paymentDate,
        PayableStatus status,
        String paymentMethod,
        String costCenter,
        UUID developmentId,
        String developmentName,
        String attachmentUrl
) {}
