package com.construtora.financeiro.mapper;

import com.construtora.financeiro.dto.payable.PayableRequest;
import com.construtora.financeiro.dto.payable.PayableResponse;
import com.construtora.financeiro.model.AccountPayable;
import com.construtora.financeiro.model.enums.PayableStatus;
import org.springframework.stereotype.Component;

@Component
public class PayableMapper {

    public AccountPayable toEntity(PayableRequest r, AccountPayable target) {
        AccountPayable a = target != null ? target : new AccountPayable();
        a.setSupplier(r.supplier());
        a.setCategory(r.category());
        a.setDescription(r.description());
        a.setAmount(r.amount());
        a.setDueDate(r.dueDate());
        a.setPaymentDate(r.paymentDate());
        a.setStatus(r.status() != null ? r.status() : PayableStatus.OPEN);
        a.setPaymentMethod(r.paymentMethod());
        a.setCostCenter(r.costCenter());
        a.setAttachmentUrl(r.attachmentUrl());
        return a;
    }

    public PayableResponse toResponse(AccountPayable a) {
        return new PayableResponse(
                a.getId(), a.getSupplier(), a.getCategory(), a.getDescription(), a.getAmount(),
                a.getDueDate(), a.getPaymentDate(), a.getStatus(), a.getPaymentMethod(),
                a.getCostCenter(), a.getAttachmentUrl());
    }
}
