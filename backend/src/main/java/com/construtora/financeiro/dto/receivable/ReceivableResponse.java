package com.construtora.financeiro.dto.receivable;

import com.construtora.financeiro.model.enums.ReceivableStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ReceivableResponse(
        UUID id,
        UUID clientId,
        String clientName,
        UUID saleId,
        UUID installmentId,
        String description,
        BigDecimal amount,
        LocalDate dueDate,
        LocalDate receiveDate,
        ReceivableStatus status,
        String paymentMethod,
        String notes
) {}
