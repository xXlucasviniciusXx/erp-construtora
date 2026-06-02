package com.construtora.financeiro.dto.payable;

import com.construtora.financeiro.model.enums.PayableStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record PayableRequest(
        @NotBlank String supplier,
        UUID categoryId,
        String description,
        @NotNull @Positive BigDecimal amount,
        @NotNull LocalDate dueDate,
        LocalDate paymentDate,
        PayableStatus status,
        String paymentMethod,
        UUID costCenterId,
        UUID developmentId,
        String attachmentUrl
) {}
