package com.construtora.financeiro.repository;

import com.construtora.financeiro.model.AccountPayable;
import com.construtora.financeiro.model.enums.PayableStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    /**
     * Busca paginada com filtros opcionais: texto (fornecedor/descrição), status e
     * empreendimento. {@code onlyGeral=true} traz só as despesas sem empreendimento.
     */
    @Query("""
            select a from AccountPayable a
              left join a.development d
            where (:q = ''
                   or lower(a.supplier) like lower(concat('%', :q, '%'))
                   or lower(coalesce(a.description, '')) like lower(concat('%', :q, '%')))
              and (:status is null or a.status = :status)
              and ((:onlyGeral = true and d is null)
                   or (:onlyGeral = false and (:devId is null or d.id = :devId)))
            """)
    Page<AccountPayable> search(@Param("q") String q,
                                @Param("status") PayableStatus status,
                                @Param("devId") UUID devId,
                                @Param("onlyGeral") boolean onlyGeral,
                                Pageable pageable);

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

    /** Despesas PAGAS agrupadas por empreendimento (nulo = "Geral / Administrativo"). */
    @Query("""
            select coalesce(d.name, 'Geral / Administrativo') as development,
                   count(a) as total,
                   coalesce(sum(a.amount), 0) as amount
            from AccountPayable a
              left join a.development d
            where a.status = com.construtora.financeiro.model.enums.PayableStatus.PAID
            group by d.name
            order by amount desc
            """)
    List<Object[]> expensesByDevelopmentPaid();

    /** Despesas PAGAS agrupadas por categoria (grupo / item). */
    @Query("""
            select coalesce(c.grupo, '—') as grupo,
                   coalesce(c.name, 'Sem categoria') as categoria,
                   count(a) as total,
                   coalesce(sum(a.amount), 0) as amount
            from AccountPayable a
              left join a.category c
            where a.status = com.construtora.financeiro.model.enums.PayableStatus.PAID
            group by c.grupo, c.name
            order by amount desc
            """)
    List<Object[]> expensesByCategoryPaid();

    /** Despesas PAGAS agrupadas por centro de custo. */
    @Query("""
            select coalesce(cc.name, 'Sem centro de custo') as centro,
                   count(a) as total,
                   coalesce(sum(a.amount), 0) as amount
            from AccountPayable a
              left join a.costCenter cc
            where a.status = com.construtora.financeiro.model.enums.PayableStatus.PAID
            group by cc.name
            order by amount desc
            """)
    List<Object[]> expensesByCostCenterPaid();
}
