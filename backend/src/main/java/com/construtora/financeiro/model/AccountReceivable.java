package com.construtora.financeiro.model;

import com.construtora.financeiro.model.enums.ReceivableApprovalStatus;
import com.construtora.financeiro.model.enums.ReceivableStatus;
import jakarta.persistence.*;
import jakarta.persistence.FetchType;
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

    /** Categoria de receita (opcional — detalha "Outras Receitas" no DRE). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    private String description;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "receive_date")
    private LocalDate receiveDate;

    /**
     * Data do crédito no extrato bancário, capturada na conciliação (uso futuro).
     * É um registro informativo; a referência financeira principal continua sendo
     * a data da baixa ({@link #receiveDate}).
     */
    @Column(name = "bank_credit_date")
    private LocalDate bankCreditDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReceivableStatus status = ReceivableStatus.OPEN;

    @Column(name = "payment_method")
    private String paymentMethod;

    @Column(columnDefinition = "text")
    private String notes;

    /** Ciclo de aprovação (separado do status operacional). */
    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status", nullable = false, length = 20)
    private ReceivableApprovalStatus approvalStatus = ReceivableApprovalStatus.PENDING;

    @Column(name = "approved_by")
    private UUID approvedBy;

    @Column(name = "approved_at")
    private OffsetDateTime approvedAt;

    @Column(name = "rejection_reason", columnDefinition = "text")
    private String rejectionReason;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
