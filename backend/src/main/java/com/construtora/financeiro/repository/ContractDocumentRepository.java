package com.construtora.financeiro.repository;

import com.construtora.financeiro.model.ContractDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ContractDocumentRepository extends JpaRepository<ContractDocument, UUID> {

    List<ContractDocument> findBySaleIdOrderByGeneratedAtDesc(UUID saleId);

    int countBySaleIdAndType(UUID saleId, String type);
}
