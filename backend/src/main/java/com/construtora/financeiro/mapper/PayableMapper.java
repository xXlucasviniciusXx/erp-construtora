package com.construtora.financeiro.mapper;

import com.construtora.financeiro.dto.payable.PayableRequest;
import com.construtora.financeiro.dto.payable.PayableResponse;
import com.construtora.financeiro.model.AccountPayable;
import com.construtora.financeiro.model.Development;
import com.construtora.financeiro.model.enums.PayableStatus;
import com.construtora.financeiro.repository.DevelopmentRepository;
import org.springframework.stereotype.Component;

@Component
public class PayableMapper {

    private final DevelopmentRepository developmentRepository;

    public PayableMapper(DevelopmentRepository developmentRepository) {
        this.developmentRepository = developmentRepository;
    }

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
        // Empreendimento é opcional; nulo = despesa geral/administrativa.
        a.setDevelopment(r.developmentId() != null
                ? developmentRepository.findById(r.developmentId()).orElse(null)
                : null);
        a.setAttachmentUrl(r.attachmentUrl());
        return a;
    }

    public PayableResponse toResponse(AccountPayable a) {
        Development dev = a.getDevelopment();
        return new PayableResponse(
                a.getId(), a.getSupplier(), a.getCategory(), a.getDescription(), a.getAmount(),
                a.getDueDate(), a.getPaymentDate(), a.getStatus(), a.getPaymentMethod(),
                a.getCostCenter(),
                dev != null ? dev.getId() : null,
                dev != null ? dev.getName() : null,
                a.getAttachmentUrl());
    }
}
