package com.construtora.financeiro.dto.bank;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.util.UUID;

public record BankAccountRequest(
        @NotBlank String name,
        String bankCode,
        String bankName,
        String agency,
        String accountNumber,
        BigDecimal initialBalance,
        Boolean active,
        UUID developmentId   // null = conta geral
) {}
