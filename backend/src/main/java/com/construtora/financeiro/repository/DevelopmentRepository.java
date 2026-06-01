package com.construtora.financeiro.repository;

import com.construtora.financeiro.model.Development;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DevelopmentRepository extends JpaRepository<Development, UUID> {
    List<Development> findAllByOrderByName();
    long count();
}
