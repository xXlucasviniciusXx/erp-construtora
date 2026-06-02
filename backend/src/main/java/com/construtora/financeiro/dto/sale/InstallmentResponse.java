package com.construtora.financeiro.dto.sale;

import com.construtora.financeiro.model.enums.InstallmentStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record InstallmentResponse(
        UUID id,
        UUID saleId,
        Integer number,
        BigDecimal amount,
        LocalDate dueDate,
        LocalDate paymentDate,
        InstallmentStatus status,
        String paymentMethod,
        String receiptUrl,
        String notes,
        long daysLate,
        BigDecimal penaltyAmount,
        BigDecimal interestAmount,
        BigDecimal updatedAmount
) {}
