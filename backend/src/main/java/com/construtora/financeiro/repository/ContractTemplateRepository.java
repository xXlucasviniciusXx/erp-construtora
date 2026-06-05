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

    /** Padrão específico de um empreendimento. */
    Optional<ContractTemplate> findFirstByKindAndDevelopmentIdAndIsDefaultTrue(String kind, UUID developmentId);

    /** Padrão global (sem empreendimento). */
    Optional<ContractTemplate> findFirstByKindAndDevelopmentIdIsNullAndIsDefaultTrue(String kind);

    /**
     * Zera o flag de padrão dos modelos do MESMO escopo (kind + empreendimento),
     * exceto o id preservado. Executado como UPDATE em massa ANTES de marcar o
     * novo padrão, evitando violar o índice único parcial.
     */
    @Modifying
    @Query("update ContractTemplate t set t.isDefault = false "
            + "where t.kind = :kind "
            + "and ((:devId is null and t.developmentId is null) or t.developmentId = :devId) "
            + "and (:keepId is null or t.id <> :keepId)")
    void clearDefaults(@Param("kind") String kind,
                       @Param("devId") UUID developmentId,
                       @Param("keepId") UUID keepId);
}
