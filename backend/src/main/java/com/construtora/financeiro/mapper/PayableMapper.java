package com.construtora.financeiro.mapper;

import com.construtora.financeiro.dto.payable.PayableRequest;
import com.construtora.financeiro.dto.payable.PayableResponse;
import com.construtora.financeiro.model.AccountPayable;
import com.construtora.financeiro.model.Category;
import com.construtora.financeiro.model.CostCenter;
import com.construtora.financeiro.model.Development;
import com.construtora.financeiro.model.enums.PayableStatus;
import com.construtora.financeiro.repository.CategoryRepository;
import com.construtora.financeiro.repository.CostCenterRepository;
import com.construtora.financeiro.repository.DevelopmentRepository;
import org.springframework.stereotype.Component;

@Component
public class PayableMapper {

    private final CategoryRepository categoryRepository;
    private final CostCenterRepository costCenterRepository;
    private final DevelopmentRepository developmentRepository;

    public PayableMapper(CategoryRepository categoryRepository,
                         CostCenterRepository costCenterRepository,
                         DevelopmentRepository developmentRepository) {
        this.categoryRepository = categoryRepository;
        this.costCenterRepository = costCenterRepository;
        this.developmentRepository = developmentRepository;
    }

    public AccountPayable toEntity(PayableRequest r, AccountPayable target) {
        AccountPayable a = target != null ? target : new AccountPayable();
        a.setSupplier(r.supplier());
        a.setCategory(r.categoryId() != null
                ? categoryRepository.findById(r.categoryId()).orElse(null) : null);
        a.setDescription(r.description());
        a.setAmount(r.amount());
        a.setDueDate(r.dueDate());
        a.setPaymentDate(r.paymentDate());
        a.setStatus(r.status() != null ? r.status() : PayableStatus.OPEN);
        a.setPaymentMethod(r.paymentMethod());
        a.setCostCenter(r.costCenterId() != null
                ? costCenterRepository.findById(r.costCenterId()).orElse(null) : null);
        // Empreendimento é opcional; nulo = despesa geral/administrativa.
        a.setDevelopment(r.developmentId() != null
                ? developmentRepository.findById(r.developmentId()).orElse(null) : null);
        a.setAttachmentUrl(r.attachmentUrl());
        return a;
    }

    public PayableResponse toResponse(AccountPayable a) {
        Category cat = a.getCategory();
        CostCenter cc = a.getCostCenter();
        Development dev = a.getDevelopment();
        return new PayableResponse(
                a.getId(), a.getSupplier(),
                cat != null ? cat.getId() : null,
                cat != null ? cat.getName() : null,
                cat != null ? cat.getGrupo() : null,
                a.getDescription(), a.getAmount(),
                a.getDueDate(), a.getPaymentDate(), a.getStatus(), a.getPaymentMethod(),
                cc != null ? cc.getId() : null,
                cc != null ? cc.getName() : null,
                dev != null ? dev.getId() : null,
                dev != null ? dev.getName() : null,
                a.getAttachmentUrl());
    }
}
