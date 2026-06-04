package com.construtora.financeiro.repository;

import com.construtora.financeiro.model.Module;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ModuleRepository extends JpaRepository<Module, UUID> {
    List<Module> findAllByOrderBySortOrder();
    Optional<Module> findByCode(String code);
}
