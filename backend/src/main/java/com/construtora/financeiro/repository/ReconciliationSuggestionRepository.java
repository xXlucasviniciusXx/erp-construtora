package com.construtora.financeiro.repository;

import com.construtora.financeiro.model.ReconciliationSuggestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReconciliationSuggestionRepository extends JpaRepository<ReconciliationSuggestion, UUID> {
    List<ReconciliationSuggestion> findByBankTransactionId(UUID bankTransactionId);
    void deleteByBankTransactionId(UUID bankTransactionId);
}
