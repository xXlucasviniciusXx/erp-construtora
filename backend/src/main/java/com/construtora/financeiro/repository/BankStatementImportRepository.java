package com.construtora.financeiro.repository;

import com.construtora.financeiro.model.BankStatementImport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BankStatementImportRepository extends JpaRepository<BankStatementImport, UUID> {
    List<BankStatementImport> findByBankAccountIdOrderByCreatedAtDesc(UUID bankAccountId);
}
