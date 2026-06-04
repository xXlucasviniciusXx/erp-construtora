package com.construtora.financeiro.dto.receivable;

import com.construtora.financeiro.model.enums.ReceivableStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ReceivableRequest(
        UUID clientId,
        UUID saleId,
        UUID installmentId,
        UUID categoryId,           // categoria de receita (opcional)
        String description,
        @NotNull @Positive BigDecimal amount,
        @NotNull LocalDate dueDate,
        LocalDate receiveDate,
        ReceivableStatus status,
        String paymentMethod,
        String notes
) {}
