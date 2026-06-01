package com.construtora.financeiro.dto.sale;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record SaleRequest(
        @NotNull UUID clientId,
        @NotNull UUID propertyId,
        @NotNull @Positive BigDecimal totalValue,
        @PositiveOrZero BigDecimal downPayment,
        @NotNull @PositiveOrZero Integer installmentsCount,
        @NotNull LocalDate firstDueDate,
        String purchaseType,
        String paymentMethod,
        String correctionIndex,
        BigDecimal interestRate,
        BigDecimal penaltyRate,
        String notes
) {}
