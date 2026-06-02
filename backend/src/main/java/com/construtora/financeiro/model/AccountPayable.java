package com.construtora.financeiro.model;

import com.construtora.financeiro.model.enums.PayableStatus;
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
@Table(name = "accounts_payable")
@Getter
@Setter
public class AccountPayable {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private String supplier;

    private String category;
    private String description;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "payment_date")
    private LocalDate paymentDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PayableStatus status = PayableStatus.OPEN;

    @Column(name = "payment_method")
    private String paymentMethod;

    @Column(name = "cost_center")
    private String costCenter;

    /** Empreendimento vinculado (opcional). Nulo = despesa geral/administrativa. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "development_id")
    private Development development;

    @Column(name = "attachment_url")
    private String attachmentUrl;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
