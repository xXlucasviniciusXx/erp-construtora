package com.construtora.financeiro.mapper;

import com.construtora.financeiro.dto.sale.InstallmentDetailResponse;
import com.construtora.financeiro.dto.sale.InstallmentResponse;
import com.construtora.financeiro.dto.sale.SaleResponse;
import com.construtora.financeiro.model.Client;
import com.construtora.financeiro.model.Installment;
import com.construtora.financeiro.model.Property;
import com.construtora.financeiro.model.PropertySale;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class SaleMapper {

    public InstallmentResponse toInstallmentResponse(Installment i) {
        return new InstallmentResponse(
                i.getId(), i.getSale().getId(), i.getNumber(), i.getAmount(), i.getDueDate(),
                i.getPaymentDate(), i.getStatus(), i.getPaymentMethod(), i.getReceiptUrl(), i.getNotes());
    }

    public InstallmentDetailResponse toDetailResponse(Installment i) {
        PropertySale s = i.getSale();
        Client c = s.getClient();
        return new InstallmentDetailResponse(
                i.getId(), s.getId(), i.getNumber(), i.getAmount(), i.getDueDate(),
                i.getPaymentDate(), i.getStatus(),
                c.getId(), c.getName(), c.getDocument(), c.getPhone(),
                s.getProperty().getDevelopment(), propertyLabel(s.getProperty()));
    }

    public SaleResponse toResponse(PropertySale s) {
        List<InstallmentResponse> installments = s.getInstallments().stream()
                .map(this::toInstallmentResponse)
                .collect(Collectors.toList());
        return new SaleResponse(
                s.getId(),
                s.getClient().getId(), s.getClient().getName(),
                s.getProperty().getId(), propertyLabel(s.getProperty()),
                s.getTotalValue(), s.getDownPayment(), s.getInstallmentsCount(), s.getFirstDueDate(),
                s.getPaymentMethod(), s.getCorrectionIndex(), s.getInterestRate(), s.getPenaltyRate(),
                s.getStatus(), s.getSaleDate(), s.getNotes(), installments);
    }

    private String propertyLabel(Property p) {
        StringBuilder sb = new StringBuilder(p.getDevelopment());
        if (p.getBlock() != null) sb.append(" / Q").append(p.getBlock());
        if (p.getLot() != null) sb.append(" / L").append(p.getLot());
        if (p.getUnit() != null) sb.append(" / Un.").append(p.getUnit());
        return sb.toString();
    }
}
