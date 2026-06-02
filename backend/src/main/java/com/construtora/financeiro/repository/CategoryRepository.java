package com.construtora.financeiro.repository;

import com.construtora.financeiro.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CategoryRepository extends JpaRepository<Category, UUID> {
    List<Category> findAllByOrderByGrupoAscNameAsc();
}
