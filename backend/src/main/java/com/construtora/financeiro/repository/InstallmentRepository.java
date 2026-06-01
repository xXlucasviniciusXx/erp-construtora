package com.construtora.financeiro.repository;

import com.construtora.financeiro.model.Installment;
import com.construtora.financeiro.model.enums.InstallmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface InstallmentRepository extends JpaRepository<Installment, UUID> {

    List<Installment> findBySaleId(UUID saleId);

    long countByStatus(InstallmentStatus status);

    List<Installment> findByStatusAndDueDateBefore(InstallmentStatus status, LocalDate date);

    @Query("""
            select coalesce(sum(i.amount), 0) from Installment i
            where i.status = :status and i.dueDate between :start and :end
            """)
    BigDecimal sumByStatusAndDueDateBetween(@Param("status") InstallmentStatus status,
                                            @Param("start") LocalDate start,
                                            @Param("end") LocalDate end);

    /** Candidatas para conciliação: parcelas em aberto/atrasadas com mesmo valor. */
    @Query("""
            select i from Installment i
            where i.status in (com.construtora.financeiro.model.enums.InstallmentStatus.OPEN,
                               com.construtora.financeiro.model.enums.InstallmentStatus.OVERDUE)
              and i.amount = :amount
            """)
    List<Installment> findReconcilableByAmount(@Param("amount") BigDecimal amount);

    /** Todas as parcelas em aberto/atrasadas (para o seletor de conciliação manual). */
    @Query("""
            select i from Installment i
            where i.status in (com.construtora.financeiro.model.enums.InstallmentStatus.OPEN,
                               com.construtora.financeiro.model.enums.InstallmentStatus.OVERDUE)
            order by i.dueDate asc
            """)
    List<Installment> findAllReconcilable();

    /** Conta débitos em aberto do cliente (parcelas em aberto/atrasadas das vendas dele). */
    @Query("""
            select count(i) from Installment i
            where i.sale.client.id = :clientId
              and i.status in (com.construtora.financeiro.model.enums.InstallmentStatus.OPEN,
                               com.construtora.financeiro.model.enums.InstallmentStatus.OVERDUE)
            """)
    long countOpenDebtsByClient(@Param("clientId") UUID clientId);

    /** Clientes inadimplentes: parcelas vencidas e ainda não pagas. */
    @Query("""
            select i from Installment i
            where i.status in (com.construtora.financeiro.model.enums.InstallmentStatus.OPEN,
                               com.construtora.financeiro.model.enums.InstallmentStatus.OVERDUE)
              and i.dueDate < :today
            order by i.dueDate asc
            """)
    List<Installment> findOverdueUnpaid(@Param("today") LocalDate today);
}
