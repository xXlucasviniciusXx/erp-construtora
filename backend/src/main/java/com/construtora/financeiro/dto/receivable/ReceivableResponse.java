package com.construtora.financeiro.dto.receivable;

import com.construtora.financeiro.model.enums.ReceivableApprovalStatus;
import com.construtora.financeiro.model.enums.ReceivableStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ReceivableResponse(
        UUID id,
        UUID clientId,
        String clientName,
        UUID saleId,
        UUID installmentId,
        UUID categoryId,
        String categoryName,
        String categoryGroup,
        String description,
        BigDecimal amount,
        LocalDate dueDate,
        LocalDate receiveDate,
        ReceivableStatus status,
        String paymentMethod,
        String notes,
        ReceivableApprovalStatus approvalStatus,
        UUID approvedBy,
        OffsetDateTime approvedAt,
        String rejectionReason,
        // Data do crédito no extrato bancário, capturada na conciliação (informativo, uso futuro).
        LocalDate bankCreditDate
) {}
