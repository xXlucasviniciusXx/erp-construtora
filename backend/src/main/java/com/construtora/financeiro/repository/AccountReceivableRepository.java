package com.construtora.financeiro.repository;

import com.construtora.financeiro.model.AccountReceivable;
import com.construtora.financeiro.model.enums.ReceivableStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface AccountReceivableRepository extends JpaRepository<AccountReceivable, UUID> {

    /** Busca paginada por cliente/descrição + filtro opcional de status. */
    @Query("""
            select a from AccountReceivable a
              left join a.client c
            where (:q = ''
                   or lower(coalesce(c.name, '')) like lower(concat('%', :q, '%'))
                   or lower(coalesce(a.description, '')) like lower(concat('%', :q, '%')))
              and (:status is null or a.status = :status)
            """)
    Page<AccountReceivable> search(@Param("q") String q,
                                   @Param("status") ReceivableStatus status,
                                   Pageable pageable);

    List<AccountReceivable> findByDueDateBetween(LocalDate start, LocalDate end);

    List<AccountReceivable> findByStatus(ReceivableStatus status);

    boolean existsByClientIdAndStatus(UUID clientId, ReceivableStatus status);

    @Query("""
            select coalesce(sum(a.amount), 0) from AccountReceivable a
            where a.status = :status and a.dueDate between :start and :end
            """)
    BigDecimal sumByStatusAndDueDateBetween(@Param("status") ReceivableStatus status,
                                            @Param("start") LocalDate start,
                                            @Param("end") LocalDate end);

    @Query("""
            select a from AccountReceivable a
            where a.status = com.construtora.financeiro.model.enums.ReceivableStatus.OPEN
              and a.amount = :amount
            """)
    List<AccountReceivable> findReconcilableByAmount(@Param("amount") BigDecimal amount);
}
