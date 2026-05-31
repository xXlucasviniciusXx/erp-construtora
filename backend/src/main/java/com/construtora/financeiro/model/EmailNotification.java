package com.construtora.financeiro.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "email_notifications")
@Getter
@Setter
public class EmailNotification {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private String recipient;

    @Column(nullable = false)
    private String subject;

    @Column(columnDefinition = "text")
    private String body;

    /** PAYMENT_OVERDUE, PAYMENT_CONFIRMED, SALE_CREATED, CONTRACT_GENERATED. */
    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(nullable = false)
    private String status = "PENDING";

    @Column(columnDefinition = "text")
    private String error;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "sent_at")
    private OffsetDateTime sentAt;
}
