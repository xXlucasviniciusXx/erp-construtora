package com.construtora.financeiro.dto.bank;

import java.math.BigDecimal;
import java.util.UUID;

public record BankAccountResponse(
        UUID id,
        String name,
        String bankCode,
        String bankName,
        String agency,
        String accountNumber,
        BigDecimal initialBalance,
        boolean active
) {}
