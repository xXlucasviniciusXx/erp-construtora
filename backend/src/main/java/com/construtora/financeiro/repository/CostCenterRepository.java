package com.construtora.financeiro.repository;

import com.construtora.financeiro.model.CostCenter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CostCenterRepository extends JpaRepository<CostCenter, UUID> {
    List<CostCenter> findAllByOrderByName();
    boolean existsByNameIgnoreCase(String name);
}
