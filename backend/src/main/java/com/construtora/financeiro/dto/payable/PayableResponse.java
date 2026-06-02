package com.construtora.financeiro.dto.payable;

import com.construtora.financeiro.model.enums.PayableStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record PayableResponse(
        UUID id,
        String supplier,
        UUID categoryId,
        String categoryName,
        String categoryGroup,
        String description,
        BigDecimal amount,
        LocalDate dueDate,
        LocalDate paymentDate,
        PayableStatus status,
        String paymentMethod,
        UUID costCenterId,
        String costCenterName,
        UUID developmentId,
        String developmentName,
        String attachmentUrl
) {}
