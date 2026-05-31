package com.construtora.financeiro.dto.payable;

import com.construtora.financeiro.model.enums.PayableStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PayableRequest(
        @NotBlank String supplier,
        String category,
        String description,
        @NotNull @Positive BigDecimal amount,
        @NotNull LocalDate dueDate,
        LocalDate paymentDate,
        PayableStatus status,
        String paymentMethod,
        String costCenter,
        String attachmentUrl
) {}
