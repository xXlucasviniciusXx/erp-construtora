package com.construtora.financeiro.repository;

import com.construtora.financeiro.model.Property;
import com.construtora.financeiro.model.enums.PropertyStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PropertyRepository extends JpaRepository<Property, UUID> {
    long countByStatus(PropertyStatus status);
}
