package com.construtora.financeiro.dto.sale;

import com.construtora.financeiro.model.enums.InstallmentStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record InstallmentResponse(
        UUID id,
        UUID saleId,
        Integer number,
        BigDecimal amount,
        LocalDate dueDate,
        LocalDate paymentDate,
        InstallmentStatus status,
        String paymentMethod,
        String receiptUrl,
        String notes,
        long daysLate,
        BigDecimal penaltyAmount,
        BigDecimal interestAmount,
        BigDecimal updatedAmount,
        // Data do crédito no extrato bancário, capturada na conciliação (informativo, uso futuro).
        LocalDate bankCreditDate,
        // Composição do valor recebido na baixa (null enquanto não paga).
        BigDecimal paidPrincipal,
        BigDecimal paidInterest,
        BigDecimal paidPenalty,
        // Correção monetária (aniversário anual) — saldo corrigido das parcelas em aberto.
        BigDecimal correctedAmount,
        BigDecimal monetaryCorrection,
        String correctionIndex,
        boolean correctionAvailable
) {}
