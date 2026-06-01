package com.construtora.financeiro.repository;

import com.construtora.financeiro.model.Block;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BlockRepository extends JpaRepository<Block, UUID> {
    List<Block> findByDevelopmentIdOrderByInternalCode(UUID developmentId);
    long countByDevelopmentId(UUID developmentId);
}
