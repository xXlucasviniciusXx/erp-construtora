package com.construtora.financeiro.repository;

import com.construtora.financeiro.model.Reconciliation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReconciliationRepository extends JpaRepository<Reconciliation, UUID> {

    Optional<Reconciliation> findByBankTransactionIdAndUndoneFalse(UUID bankTransactionId);

    List<Reconciliation> findByUndoneFalseOrderByReconciledAtDesc();
}
