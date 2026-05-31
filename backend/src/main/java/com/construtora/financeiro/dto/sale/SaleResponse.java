package com.construtora.financeiro.dto.sale;

import com.construtora.financeiro.model.enums.SaleStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record SaleResponse(
        UUID id,
        UUID clientId,
        String clientName,
        UUID propertyId,
        String propertyLabel,
        BigDecimal totalValue,
        BigDecimal downPayment,
        Integer installmentsCount,
        LocalDate firstDueDate,
        String paymentMethod,
        String correctionIndex,
        BigDecimal interestRate,
        BigDecimal penaltyRate,
        SaleStatus status,
        LocalDate saleDate,
        String notes,
        List<InstallmentResponse> installments
) {}
