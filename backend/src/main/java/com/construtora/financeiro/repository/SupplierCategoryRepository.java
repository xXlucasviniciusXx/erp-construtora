package com.construtora.financeiro.repository;

import com.construtora.financeiro.model.SupplierCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SupplierCategoryRepository extends JpaRepository<SupplierCategory, UUID> {
    List<SupplierCategory> findByActiveTrueOrderBySortOrderAscNameAsc();
    boolean existsByNameIgnoreCaseAndIdNot(String name, UUID id);
}
