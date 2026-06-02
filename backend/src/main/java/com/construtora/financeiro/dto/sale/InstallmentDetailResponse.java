package com.construtora.financeiro.dto.sale;

import com.construtora.financeiro.model.enums.InstallmentStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Parcela com identificação do cliente, para a listagem com filtros. */
public record InstallmentDetailResponse(
        UUID id,
        UUID saleId,
        Integer number,
        BigDecimal amount,
        LocalDate dueDate,
        LocalDate paymentDate,
        InstallmentStatus status,
        UUID clientId,
        String clientName,
        String clientDocument,
        String clientPhone,
        String development,
        String propertyLabel,
        long daysLate,
        BigDecimal penaltyAmount,
        BigDecimal interestAmount,
        BigDecimal updatedAmount
) {}
