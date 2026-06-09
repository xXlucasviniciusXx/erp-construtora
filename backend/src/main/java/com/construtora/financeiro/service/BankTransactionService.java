package com.construtora.financeiro.service;

import com.construtora.financeiro.dto.bank.BankTransactionResponse;
import com.construtora.financeiro.mapper.BankTransactionMapper;
import com.construtora.financeiro.model.enums.TransactionStatus;
import com.construtora.financeiro.repository.BankTransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class BankTransactionService {

    private final BankTransactionRepository repository;
    private final BankTransactionMapper mapper;

    public BankTransactionService(BankTransactionRepository repository, BankTransactionMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    public org.springframework.data.domain.Page<BankTransactionResponse> findByAccount(
            UUID bankAccountId, org.springframework.data.domain.Pageable pageable) {
        return repository.findByBankAccountIdOrderByTransactionDateDesc(bankAccountId, pageable).map(mapper::toResponse);
    }

    public org.springframework.data.domain.Page<BankTransactionResponse> findByStatus(
            TransactionStatus status, org.springframework.data.domain.Pageable pageable) {
        return repository.findByStatusOrderByTransactionDateDesc(status, pageable).map(mapper::toResponse);
    }

    public org.springframework.data.domain.Page<BankTransactionResponse> findByAccountAndStatus(
            UUID bankAccountId, TransactionStatus status, org.springframework.data.domain.Pageable pageable) {
        return repository.findByBankAccountIdAndStatusOrderByTransactionDateDesc(bankAccountId, status, pageable)
                .map(mapper::toResponse);
    }
}
