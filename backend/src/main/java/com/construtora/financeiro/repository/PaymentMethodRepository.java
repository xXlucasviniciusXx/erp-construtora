package com.construtora.financeiro.repository;

import com.construtora.financeiro.model.PaymentMethod;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, UUID> {
    List<PaymentMethod> findByActiveTrueOrderBySortOrderAscNameAsc();
    boolean existsByNameIgnoreCaseAndIdNot(String name, UUID id);
}
