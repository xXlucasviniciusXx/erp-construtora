package com.construtora.financeiro.service;

import com.construtora.financeiro.dto.bank.BankAccountRequest;
import com.construtora.financeiro.dto.bank.BankAccountResponse;
import com.construtora.financeiro.exception.ResourceNotFoundException;
import com.construtora.financeiro.model.BankAccount;
import com.construtora.financeiro.repository.BankAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class BankAccountService {

    private final BankAccountRepository repository;

    public BankAccountService(BankAccountRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<BankAccountResponse> findAll() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    public BankAccountResponse create(BankAccountRequest request) {
        BankAccount a = new BankAccount();
        apply(request, a);
        return toResponse(repository.save(a));
    }

    public BankAccountResponse update(UUID id, BankAccountRequest request) {
        BankAccount a = getEntity(id);
        apply(request, a);
        return toResponse(repository.save(a));
    }

    public void delete(UUID id) {
        repository.delete(getEntity(id));
    }

    public BankAccount getEntity(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Conta bancária", id));
    }

    private void apply(BankAccountRequest r, BankAccount a) {
        a.setName(r.name());
        a.setBankCode(r.bankCode());
        a.setBankName(r.bankName());
        a.setAgency(r.agency());
        a.setAccountNumber(r.accountNumber());
        a.setInitialBalance(r.initialBalance() != null ? r.initialBalance() : BigDecimal.ZERO);
        a.setActive(r.active() == null || r.active());
    }

    private BankAccountResponse toResponse(BankAccount a) {
        return new BankAccountResponse(a.getId(), a.getName(), a.getBankCode(), a.getBankName(),
                a.getAgency(), a.getAccountNumber(), a.getInitialBalance(), a.isActive());
    }
}
