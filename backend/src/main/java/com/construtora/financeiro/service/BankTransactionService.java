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

    public List<BankTransactionResponse> findByAccount(UUID bankAccountId) {
        return repository.findByBankAccountIdOrderByTransactionDateDesc(bankAccountId)
                .stream().map(mapper::toResponse).toList();
    }

    public List<BankTransactionResponse> findByStatus(TransactionStatus status) {
        return repository.findByStatus(status).stream().map(mapper::toResponse).toList();
    }
}
