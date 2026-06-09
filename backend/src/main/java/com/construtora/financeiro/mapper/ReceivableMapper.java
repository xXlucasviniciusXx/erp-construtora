package com.construtora.financeiro.mapper;

import com.construtora.financeiro.dto.receivable.ReceivableResponse;
import com.construtora.financeiro.model.AccountReceivable;
import com.construtora.financeiro.model.Category;
import org.springframework.stereotype.Component;

@Component
public class ReceivableMapper {

    public ReceivableResponse toResponse(AccountReceivable a) {
        Category cat = a.getCategory();
        return new ReceivableResponse(
                a.getId(),
                a.getClient() != null ? a.getClient().getId() : null,
                a.getClient() != null ? a.getClient().getName() : null,
                a.getSale() != null ? a.getSale().getId() : null,
                a.getInstallment() != null ? a.getInstallment().getId() : null,
                cat != null ? cat.getId() : null,
                cat != null ? cat.getName() : null,
                cat != null ? cat.getGrupo() : null,
                a.getDescription(), a.getAmount(), a.getDueDate(), a.getReceiveDate(),
                a.getStatus(), a.getPaymentMethod(), a.getNotes(),
                a.getBankCreditDate());
    }
}
