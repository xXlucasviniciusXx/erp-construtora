package com.construtora.financeiro.mapper;

import com.construtora.financeiro.dto.sale.InstallmentDetailResponse;
import com.construtora.financeiro.dto.sale.InstallmentResponse;
import com.construtora.financeiro.dto.sale.SaleResponse;
import com.construtora.financeiro.model.Client;
import com.construtora.financeiro.model.Installment;
import com.construtora.financeiro.model.Lot;
import com.construtora.financeiro.model.PropertySale;
import com.construtora.financeiro.model.enums.InstallmentStatus;
import com.construtora.financeiro.service.LateFeeCalculator;
import com.construtora.financeiro.service.LateFeeCalculator.LateFees;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class SaleMapper {

    private final LateFeeCalculator lateFeeCalculator;

    public SaleMapper(LateFeeCalculator lateFeeCalculator) {
        this.lateFeeCalculator = lateFeeCalculator;
    }

    public InstallmentResponse toInstallmentResponse(Installment i) {
        LateFees f = lateFeeCalculator.compute(i);
        return new InstallmentResponse(
                i.getId(), i.getSale().getId(), i.getNumber(), i.getAmount(), i.getDueDate(),
                i.getPaymentDate(), i.getStatus(), i.getPaymentMethod(), i.getReceiptUrl(), i.getNotes(),
                f.daysLate(), f.penaltyAmount(), f.interestAmount(), f.updatedAmount());
    }

    public InstallmentDetailResponse toDetailResponse(Installment i) {
        PropertySale s = i.getSale();
        Client c = s.getClient();
        Lot lot = s.getLot();
        LateFees f = lateFeeCalculator.compute(i);
        return new InstallmentDetailResponse(
                i.getId(), s.getId(), i.getNumber(), i.getAmount(), i.getDueDate(),
                i.getPaymentDate(), i.getStatus(),
                c.getId(), c.getName(), c.getDocument(), c.getPhone(),
                lot.getBlock().getDevelopment().getName(), LotMapper.label(lot),
                f.daysLate(), f.penaltyAmount(), f.interestAmount(), f.updatedAmount());
    }

    public SaleResponse toResponse(PropertySale s) {
        List<InstallmentResponse> installments = s.getInstallments().stream()
                .map(this::toInstallmentResponse)
                .collect(Collectors.toList());

        int paidCount = 0;
        BigDecimal paidAmount = BigDecimal.ZERO;
        BigDecimal openAmount = BigDecimal.ZERO;
        for (var i : s.getInstallments()) {
            if (i.getStatus() == InstallmentStatus.PAID) {
                paidCount++;
                paidAmount = paidAmount.add(i.getAmount());
            } else if (i.getStatus() != InstallmentStatus.CANCELLED) {
                openAmount = openAmount.add(i.getAmount());
            }
        }

        Lot lot = s.getLot();
        return new SaleResponse(
                s.getId(),
                s.getClient().getId(), s.getClient().getName(),
                lot.getId(), LotMapper.label(lot), lot.getPlannedValue(),
                s.getTotalValue(), s.getDownPayment(), s.getInstallmentsCount(), s.getFirstDueDate(),
                s.getPurchaseType(), s.getPaymentMethod(), s.getCorrectionIndex(),
                s.getInterestRate(), s.getPenaltyRate(),
                s.getStatus(), s.getSaleDate(), s.getNotes(),
                paidCount, paidAmount, openAmount, installments);
    }
}
