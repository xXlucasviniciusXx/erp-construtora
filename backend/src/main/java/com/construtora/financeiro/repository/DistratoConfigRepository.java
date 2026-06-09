package com.construtora.financeiro.repository;

import com.construtora.financeiro.model.DistratoConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DistratoConfigRepository extends JpaRepository<DistratoConfig, UUID> {

    /** Configuração global (development == null). */
    Optional<DistratoConfig> findByDevelopmentIsNull();

    Optional<DistratoConfig> findByDevelopmentId(UUID developmentId);

    List<DistratoConfig> findAllByOrderByDevelopmentIdAsc();
}
