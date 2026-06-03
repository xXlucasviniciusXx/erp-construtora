package com.construtora.financeiro.repository;

import com.construtora.financeiro.model.BankTransaction;
import com.construtora.financeiro.model.enums.TransactionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BankTransactionRepository extends JpaRepository<BankTransaction, UUID> {

    List<BankTransaction> findByStatus(TransactionStatus status);

    List<BankTransaction> findByBankAccountIdOrderByTransactionDateDesc(UUID bankAccountId);

    // Variantes paginadas (listagem na tela de conciliação)
    Page<BankTransaction> findByStatusOrderByTransactionDateDesc(TransactionStatus status, Pageable pageable);

    Page<BankTransaction> findByBankAccountIdOrderByTransactionDateDesc(UUID bankAccountId, Pageable pageable);

    long countByStatus(TransactionStatus status);

    boolean existsByBankAccountIdAndBankIdentifier(UUID bankAccountId, String bankIdentifier);
}
