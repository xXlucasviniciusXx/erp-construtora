package com.construtora.financeiro.dto.bank;

import com.construtora.financeiro.model.enums.TransactionStatus;
import com.construtora.financeiro.model.enums.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record BankTransactionResponse(
        UUID id,
        UUID bankAccountId,
        LocalDate transactionDate,
        String description,
        BigDecimal amount,
        TransactionType type,
        String documentNumber,
        String bankIdentifier,
        TransactionStatus status,
        String notes
) {}
