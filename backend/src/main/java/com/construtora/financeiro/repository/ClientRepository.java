package com.construtora.financeiro.repository;

import com.construtora.financeiro.model.Client;
import com.construtora.financeiro.model.enums.ClientStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ClientRepository extends JpaRepository<Client, UUID> {
    Optional<Client> findByDocument(String document);
    boolean existsByDocument(String document);

    /** Busca paginada por nome/documento + filtro opcional de status. */
    @Query("""
            select c from Client c
            where (:status is null or c.status = :status)
              and (:q = ''
                   or lower(c.name) like lower(concat('%', :q, '%'))
                   or c.document like concat('%', :q, '%'))
            """)
    Page<Client> search(@Param("q") String q,
                        @Param("status") ClientStatus status,
                        Pageable pageable);
}
