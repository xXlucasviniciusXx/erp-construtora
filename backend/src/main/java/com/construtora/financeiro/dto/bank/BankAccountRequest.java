package com.construtora.financeiro.dto.bank;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

public record BankAccountRequest(
        @NotBlank String name,
        String bankCode,
        String bankName,
        String agency,
        String accountNumber,
        BigDecimal initialBalance,
        Boolean active
) {}
