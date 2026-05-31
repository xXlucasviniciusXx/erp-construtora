package com.construtora.financeiro.model;

import com.construtora.financeiro.model.enums.ReceivableStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "accounts_receivable")
@Getter
@Setter
public class AccountReceivable {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "client_id")
    private Client client;

    @ManyToOne
    @JoinColumn(name = "sale_id")
    private PropertySale sale;

    @ManyToOne
    @JoinColumn(name = "installment_id")
    private Installment installment;

    private String description;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "receive_date")
    private LocalDate receiveDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReceivableStatus status = ReceivableStatus.OPEN;

    @Column(name = "payment_method")
    private String paymentMethod;

    @Column(columnDefinition = "text")
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
