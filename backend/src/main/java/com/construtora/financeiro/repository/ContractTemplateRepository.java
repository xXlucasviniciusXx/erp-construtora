package com.construtora.financeiro.repository;

import com.construtora.financeiro.model.ContractTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ContractTemplateRepository extends JpaRepository<ContractTemplate, UUID> {

    List<ContractTemplate> findByKindOrderByNameAsc(String kind);

    Optional<ContractTemplate> findFirstByKindAndIsDefaultTrue(String kind);

    /**
     * Zera o flag de padrão de todos os modelos do tipo informado, exceto o id
     * preservado (que pode ser nulo na criação). Executado como UPDATE em massa
     * ANTES de marcar o novo padrão, evitando violar o índice único parcial.
     */
    @Modifying
    @Query("update ContractTemplate t set t.isDefault = false "
            + "where t.kind = :kind and (:keepId is null or t.id <> :keepId)")
    void clearDefaults(@Param("kind") String kind, @Param("keepId") UUID keepId);
}
