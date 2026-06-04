package com.construtora.financeiro.repository;

import com.construtora.financeiro.model.CorrectionIndex;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CorrectionIndexRepository extends JpaRepository<CorrectionIndex, UUID> {
    List<CorrectionIndex> findByActiveTrueOrderBySortOrderAscNameAsc();
    boolean existsByNameIgnoreCaseAndIdNot(String name, UUID id);
}
