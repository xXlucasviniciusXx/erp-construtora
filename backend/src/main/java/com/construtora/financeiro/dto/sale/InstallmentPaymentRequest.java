package com.construtora.financeiro.dto.sale;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record InstallmentPaymentRequest(
        @NotNull LocalDate paymentDate,
        String paymentMethod,
        String receiptUrl,
        String notes
) {}
