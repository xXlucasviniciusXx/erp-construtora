package com.construtora.financeiro.mapper;

import com.construtora.financeiro.dto.distrato.DistratoConfigResponse;
import com.construtora.financeiro.dto.distrato.DistratoResponse;
import com.construtora.financeiro.model.Distrato;
import com.construtora.financeiro.model.DistratoConfig;
import com.construtora.financeiro.model.enums.DistratoFinancialOutcome;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class DistratoMapper {

    public DistratoResponse toResponse(Distrato d) {
        BigDecimal entry = d.getFinancialOutcome() == DistratoFinancialOutcome.ZERO
                ? BigDecimal.ZERO
                : d.getFinalBalance() != null ? d.getFinalBalance().abs() : BigDecimal.ZERO;
        return new DistratoResponse(
                d.getId(),
                d.getSale().getId(),
                d.getSale().getContractNumber(),
                d.getClient().getId(),
                d.getClient().getName(),
                d.getLot().getId(),
                d.getDevelopmentName(),
                d.getBlockName(),
                d.getLotName(),
                d.getStatus(),
                d.getFinancialRule(),
                d.getReason(),
                d.getContractTotal(),
                d.getPaidAmount(),
                d.getDefaultRetentionPercent(),
                d.getUsedRetentionPercent(),
                d.getRetentionChangeReason(),
                d.getRetentionAmount(),
                d.getOverdueAmount(),
                d.getChargesAmount(),
                d.getTotalDebtAmount(),
                d.getFinalBalance(),
                d.getFinancialOutcome(),
                entry,
                d.getPayableId(),
                d.getReceivableId(),
                d.getRequestedBy(),
                d.getApprovedBy(),
                d.getSettledBy(),
                d.getRequestedAt(),
                d.getApprovedAt(),
                d.getConcludedAt(),
                d.getCalculationMemory(),
                d.getCreatedAt());
    }

    public DistratoConfigResponse toResponse(DistratoConfig c) {
        return new DistratoConfigResponse(
                c.getId(),
                c.getDevelopment() != null ? c.getDevelopment().getId() : null,
                c.getDevelopment() != null ? c.getDevelopment().getName() : null,
                c.getFinancialRule(),
                c.isActive());
    }
}
