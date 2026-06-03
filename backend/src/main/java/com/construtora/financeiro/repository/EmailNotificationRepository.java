package com.construtora.financeiro.repository;

import com.construtora.financeiro.model.EmailNotification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface EmailNotificationRepository extends JpaRepository<EmailNotification, UUID> {

    /** Histórico paginado com filtros opcionais de status e tipo de evento. */
    @Query("""
            select n from EmailNotification n
            where (:status = '' or n.status = :status)
              and (:eventType = '' or n.eventType = :eventType)
            order by n.createdAt desc
            """)
    Page<EmailNotification> search(@Param("status") String status,
                                   @Param("eventType") String eventType,
                                   Pageable pageable);
}
