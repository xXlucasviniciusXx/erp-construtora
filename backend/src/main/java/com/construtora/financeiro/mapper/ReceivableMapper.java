package com.construtora.financeiro.mapper;

import com.construtora.financeiro.dto.receivable.ReceivableResponse;
import com.construtora.financeiro.model.AccountReceivable;
import org.springframework.stereotype.Component;

@Component
public class ReceivableMapper {

    public ReceivableResponse toResponse(AccountReceivable a) {
        return new ReceivableResponse(
                a.getId(),
                a.getClient() != null ? a.getClient().getId() : null,
                a.getClient() != null ? a.getClient().getName() : null,
                a.getSale() != null ? a.getSale().getId() : null,
                a.getInstallment() != null ? a.getInstallment().getId() : null,
                a.getDescription(), a.getAmount(), a.getDueDate(), a.getReceiveDate(),
                a.getStatus(), a.getPaymentMethod(), a.getNotes());
    }
}
