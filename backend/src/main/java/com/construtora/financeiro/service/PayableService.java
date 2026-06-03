package com.construtora.financeiro.service;

import com.construtora.financeiro.dto.payable.PayableRequest;
import com.construtora.financeiro.dto.payable.PayableResponse;
import com.construtora.financeiro.exception.ResourceNotFoundException;
import com.construtora.financeiro.mapper.PayableMapper;
import com.construtora.financeiro.model.AccountPayable;
import com.construtora.financeiro.model.enums.PayableStatus;
import com.construtora.financeiro.repository.AccountPayableRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Service
@Transactional
public class PayableService {

    private final AccountPayableRepository repository;
    private final AuditService auditService;
    private final PayableMapper mapper;

    public PayableService(AccountPayableRepository repository, AuditService auditService, PayableMapper mapper) {
        this.repository = repository;
        this.auditService = auditService;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public Page<PayableResponse> search(String q, PayableStatus status, String developmentId, Pageable pageable) {
        String query = (q != null && !q.isBlank()) ? q.trim() : "";
        boolean onlyGeral = "none".equalsIgnoreCase(developmentId);
        UUID devId = null;
        if (!onlyGeral && developmentId != null && !developmentId.isBlank()) {
            try { devId = UUID.fromString(developmentId); } catch (IllegalArgumentException ignored) { }
        }
        return repository.search(query, status, devId, onlyGeral, pageable).map(mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public PayableResponse findById(UUID id) {
        return mapper.toResponse(getEntity(id));
    }

    public PayableResponse create(PayableRequest request) {
        return mapper.toResponse(repository.save(mapper.toEntity(request, null)));
    }

    public PayableResponse update(UUID id, PayableRequest request) {
        return mapper.toResponse(repository.save(mapper.toEntity(request, getEntity(id))));
    }

    public PayableResponse confirmPayment(UUID id, LocalDate paymentDate) {
        AccountPayable a = getEntity(id);
        a.setStatus(PayableStatus.PAID);
        a.setPaymentDate(paymentDate != null ? paymentDate : LocalDate.now());
        PayableResponse resp = mapper.toResponse(repository.save(a));
        auditService.log("PAYABLE_PAID", "accounts_payable", id, a.getSupplier() + " R$ " + a.getAmount());
        return resp;
    }

    public PayableResponse cancel(UUID id) {
        AccountPayable a = getEntity(id);
        a.setStatus(PayableStatus.CANCELLED);
        a.setPaymentDate(null);
        PayableResponse resp = mapper.toResponse(repository.save(a));
        auditService.log("PAYABLE_CANCELLED", "accounts_payable", id, a.getSupplier() + " R$ " + a.getAmount());
        return resp;
    }

    public void delete(UUID id) {
        repository.delete(getEntity(id));
    }

    public AccountPayable getEntity(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Conta a pagar", id));
    }
}
