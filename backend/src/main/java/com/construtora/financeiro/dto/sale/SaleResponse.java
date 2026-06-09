package com.construtora.financeiro.dto.sale;

import com.construtora.financeiro.model.enums.SaleStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record SaleResponse(
        UUID id,
        String contractNumber,
        UUID clientId,
        String clientName,
        UUID lotId,
        String propertyLabel,
        BigDecimal expectedValue,     // valor previsto do lote (read-only)
        BigDecimal totalValue,        // valor que foi vendido
        BigDecimal downPayment,
        Integer installmentsCount,
        LocalDate firstDueDate,
        String purchaseType,
        String paymentMethod,
        String correctionIndex,
        BigDecimal interestRate,
        BigDecimal penaltyRate,
        SaleStatus status,
        LocalDate saleDate,
        String notes,
        // ---- Distrato (preenchido quando a venda é distratada) ----
        LocalDate distratoDate,
        String distratoReason,
        BigDecimal distratoRefundAmount,
        BigDecimal distratoRetainedAmount,
        String distratoRule,
        String distratoRuleDetail,
        // Indicadores derivados das parcelas
        Integer paidInstallments,
        BigDecimal paidAmount,
        BigDecimal openAmount,
        List<InstallmentResponse> installments
) {}
