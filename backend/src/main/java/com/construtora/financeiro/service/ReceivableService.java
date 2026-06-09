package com.construtora.financeiro.service;

import com.construtora.financeiro.dto.receivable.ReceivableRequest;
import com.construtora.financeiro.dto.receivable.ReceivableResponse;
import com.construtora.financeiro.exception.ResourceNotFoundException;
import com.construtora.financeiro.mapper.ReceivableMapper;
import com.construtora.financeiro.model.AccountReceivable;
import com.construtora.financeiro.model.enums.ReceivableStatus;
import com.construtora.financeiro.annotation.Auditable;
import com.construtora.financeiro.repository.AccountReceivableRepository;
import com.construtora.financeiro.repository.CategoryRepository;
import com.construtora.financeiro.repository.ClientRepository;
import com.construtora.financeiro.repository.InstallmentRepository;
import com.construtora.financeiro.repository.PropertySaleRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Service
@Transactional
public class ReceivableService {

    private final AccountReceivableRepository repository;
    private final ClientRepository clientRepository;
    private final PropertySaleRepository saleRepository;
    private final InstallmentRepository installmentRepository;
    private final CategoryRepository categoryRepository;
    private final ReceivableMapper mapper;

    public ReceivableService(AccountReceivableRepository repository, ClientRepository clientRepository,
                             PropertySaleRepository saleRepository, InstallmentRepository installmentRepository,
                             CategoryRepository categoryRepository, ReceivableMapper mapper) {
        this.repository = repository;
        this.clientRepository = clientRepository;
        this.saleRepository = saleRepository;
        this.installmentRepository = installmentRepository;
        this.categoryRepository = categoryRepository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public Page<ReceivableResponse> search(String q, com.construtora.financeiro.model.enums.ReceivableStatus status, Pageable pageable) {
        String query = (q != null && !q.isBlank()) ? q.trim() : "";
        return repository.search(query, status, pageable).map(mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public ReceivableResponse findById(UUID id) {
        return mapper.toResponse(getEntity(id));
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
        a.setStatus(ReceivableStatus.RECEIVED);
        a.setReceiveDate(receiveDate != null ? receiveDate : LocalDate.now());
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
        return a;
    }
}
