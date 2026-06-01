package com.construtora.financeiro.repository;

import com.construtora.financeiro.model.AccountPayable;
import com.construtora.financeiro.model.enums.PayableStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface AccountPayableRepository extends JpaRepository<AccountPayable, UUID> {

    List<AccountPayable> findByDueDateBetween(LocalDate start, LocalDate end);

    List<AccountPayable> findByStatus(PayableStatus status);

    @Query("""
            select coalesce(sum(a.amount), 0) from AccountPayable a
            where a.status = :status and a.dueDate between :start and :end
            """)
    BigDecimal sumByStatusAndDueDateBetween(@Param("status") PayableStatus status,
                                            @Param("start") LocalDate start,
                                            @Param("end") LocalDate end);

    @Query("""
            select a from AccountPayable a
            where a.status = com.construtora.financeiro.model.enums.PayableStatus.OPEN
              and a.amount = :amount
            """)
    List<AccountPayable> findReconcilableByAmount(@Param("amount") BigDecimal amount);
}
