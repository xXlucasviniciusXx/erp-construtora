package com.construtora.financeiro.repository;

import com.construtora.financeiro.model.Supplier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface SupplierRepository extends JpaRepository<Supplier, UUID> {
    List<Supplier> findByNameContainingIgnoreCaseOrderByName(String name);
    List<Supplier> findAllByOrderByName();

    /** Busca paginada por nome. */
    @Query("""
            select s from Supplier s
            where :q = '' or lower(s.name) like lower(concat('%', :q, '%'))
            order by s.name
            """)
    Page<Supplier> search(@Param("q") String q, Pageable pageable);
}
