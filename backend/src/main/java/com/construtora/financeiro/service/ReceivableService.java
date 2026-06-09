package com.construtora.financeiro.service;

import com.construtora.financeiro.dto.receivable.ReceivableRequest;
import com.construtora.financeiro.dto.receivable.ReceivableResponse;
import com.construtora.financeiro.exception.BusinessException;
import com.construtora.financeiro.exception.ResourceNotFoundException;
import com.construtora.financeiro.mapper.ReceivableMapper;
import com.construtora.financeiro.model.AccountReceivable;
import com.construtora.financeiro.model.enums.ReceivableApprovalStatus;
import com.construtora.financeiro.model.enums.ReceivableStatus;
import com.construtora.financeiro.annotation.Auditable;
import com.construtora.financeiro.repository.AccountReceivableRepository;
import com.construtora.financeiro.repository.CategoryRepository;
import com.construtora.financeiro.repository.ClientRepository;
import com.construtora.financeiro.repository.InstallmentRepository;
import com.construtora.financeiro.repository.PropertySaleRepository;
import com.construtora.financeiro.security.SecurityUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Service
@Transactional
public class ReceivableService {

    private final AccountReceivableRepository repository;
    private final ClientRepository clientRepository;
    private final PropertySaleRepository saleRepository;
    private final InstallmentRepository installmentRepository;
    private final CategoryRepository categoryRepository;
    private final com.construtora.financeiro.security.DevelopmentScopeService scope;
    private final ReceivableMapper mapper;

    public ReceivableService(AccountReceivableRepository repository, ClientRepository clientRepository,
                             PropertySaleRepository saleRepository, InstallmentRepository installmentRepository,
                             CategoryRepository categoryRepository,
                             com.construtora.financeiro.security.DevelopmentScopeService scope, ReceivableMapper mapper) {
        this.repository = repository;
        this.clientRepository = clientRepository;
        this.saleRepository = saleRepository;
        this.installmentRepository = installmentRepository;
        this.categoryRepository = categoryRepository;
        this.scope = scope;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public Page<ReceivableResponse> search(String q, com.construtora.financeiro.model.enums.ReceivableStatus status, Pageable pageable) {
        String query = (q != null && !q.isBlank()) ? q.trim() : "";
        var qs = scope.queryScope();
        return repository.search(query, status, qs.unrestricted(), qs.devIds(), pageable).map(mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public ReceivableResponse findById(UUID id) {
        com.construtora.financeiro.model.AccountReceivable a = getEntity(id);
        if (scope.isRestricted()) {
            UUID dev = a.getSale() != null
                    ? a.getSale().getLot().getBlock().getDevelopment().getId() : null;
            scope.requireAccess(dev, "Conta a receber", id);
        }
        return mapper.toResponse(a);
    }

    @Auditable(action = "RECEIVABLE_CREATE", entity = "accounts_receivable")
    public ReceivableResponse create(ReceivableRequest request) {
        return mapper.toResponse(repository.save(apply(request, new AccountReceivable())));
    }

    @Auditable(action = "RECEIVABLE_UPDATE", entity = "accounts_receivable")
    public ReceivableResponse update(UUID id, ReceivableRequest request) {
        return mapper.toResponse(repository.save(apply(request, getEntity(id))));
    }

    @Auditable(action = "RECEIVABLE_CONFIRM", entity = "accounts_receivable")
    public ReceivableResponse confirmReceive(UUID id, LocalDate receiveDate) {
        AccountReceivable a = getEntity(id);
        if (a.getApprovalStatus() != ReceivableApprovalStatus.APPROVED) {
            throw new BusinessException(
                    "Conta a receber não aprovada. Apenas contas APROVADAS podem ser baixadas. Aprovação atual: "
                            + a.getApprovalStatus());
        }
        a.setStatus(ReceivableStatus.RECEIVED);
        a.setReceiveDate(receiveDate != null ? receiveDate : LocalDate.now());
        return mapper.toResponse(repository.save(a));
    }

    @Auditable(action = "RECEIVABLE_APPROVE", entity = "accounts_receivable")
    public ReceivableResponse approve(UUID id) {
        AccountReceivable a = getEntity(id);
        if (a.getApprovalStatus() != ReceivableApprovalStatus.PENDING) {
            throw new BusinessException(
                    "Apenas contas a receber PENDENTES podem ser aprovadas. Aprovação atual: "
                            + a.getApprovalStatus());
        }
        a.setApprovalStatus(ReceivableApprovalStatus.APPROVED);
        a.setApprovedBy(SecurityUtils.currentUserId().orElse(null));
        a.setApprovedAt(OffsetDateTime.now(ZoneOffset.UTC));
        a.setRejectionReason(null);
        return mapper.toResponse(repository.save(a));
    }

    @Auditable(action = "RECEIVABLE_REJECT", entity = "accounts_receivable")
    public ReceivableResponse reject(UUID id, String reason) {
        AccountReceivable a = getEntity(id);
        if (a.getApprovalStatus() != ReceivableApprovalStatus.PENDING) {
            throw new BusinessException(
                    "Apenas contas a receber PENDENTES podem ser rejeitadas. Aprovação atual: "
                            + a.getApprovalStatus());
        }
        a.setApprovalStatus(ReceivableApprovalStatus.REJECTED);
        a.setApprovedBy(SecurityUtils.currentUserId().orElse(null));
        a.setApprovedAt(OffsetDateTime.now(ZoneOffset.UTC));
        a.setRejectionReason(reason);
        return mapper.toResponse(repository.save(a));
    }

    @Auditable(action = "RECEIVABLE_CANCEL", entity = "accounts_receivable")
    public ReceivableResponse cancel(UUID id) {
        AccountReceivable a = getEntity(id);
        a.setStatus(ReceivableStatus.CANCELLED);
        a.setReceiveDate(null);
        return mapper.toResponse(repository.save(a));
    }

    @Auditable(action = "RECEIVABLE_DELETE", entity = "accounts_receivable")
    public void delete(UUID id) {
        repository.delete(getEntity(id));
    }

    public AccountReceivable getEntity(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Conta a receber", id));
    }

    private AccountReceivable apply(ReceivableRequest r, AccountReceivable a) {
        a.setClient(r.clientId() != null
                ? clientRepository.findById(r.clientId()).orElse(null) : null);
        a.setSale(r.saleId() != null
                ? saleRepository.findById(r.saleId()).orElse(null) : null);
        a.setInstallment(r.installmentId() != null
                ? installmentRepository.findById(r.installmentId()).orElse(null) : null);
        a.setCategory(r.categoryId() != null
                ? categoryRepository.findById(r.categoryId()).orElse(null) : null);
        a.setDescription(r.description());
        a.setAmount(r.amount());
        a.setDueDate(r.dueDate());
        a.setReceiveDate(r.receiveDate());
        a.setStatus(r.status() != null ? r.status() : ReceivableStatus.OPEN);
        a.setPaymentMethod(r.paymentMethod());
        a.setNotes(r.notes());
        // Entidade nova nasce PENDENTE de aprovação; no update o approvalStatus não é alterado.
        if (a.getId() == null) {
            a.setApprovalStatus(ReceivableApprovalStatus.PENDING);
        }
        return a;
    }
}
