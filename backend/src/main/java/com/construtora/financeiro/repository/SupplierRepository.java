package com.construtora.financeiro.repository;

import com.construtora.financeiro.model.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SupplierRepository extends JpaRepository<Supplier, UUID> {
    List<Supplier> findByNameContainingIgnoreCaseOrderByName(String name);
    List<Supplier> findAllByOrderByName();
}
