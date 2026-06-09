package com.construtora.financeiro.repository;

import com.construtora.financeiro.model.Distrato;
import com.construtora.financeiro.model.enums.DistratoStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DistratoRepository extends JpaRepository<Distrato, UUID> {

    List<Distrato> findAllByOrderByCreatedAtDesc();

    List<Distrato> findByStatusOrderByCreatedAtDesc(DistratoStatus status);

    /** Distrato em andamento (não concluído/cancelado) de uma venda. */
    Optional<Distrato> findFirstBySaleIdAndStatusNotInOrderByCreatedAtDesc(
            UUID saleId, List<DistratoStatus> excluded);

    boolean existsBySaleIdAndStatusIn(UUID saleId, List<DistratoStatus> statuses);

    Optional<Distrato> findByPayableId(UUID payableId);

    Optional<Distrato> findByReceivableId(UUID receivableId);
}
