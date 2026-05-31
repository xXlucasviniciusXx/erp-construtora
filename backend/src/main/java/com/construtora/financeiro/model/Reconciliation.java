package com.construtora.financeiro.model;

import com.construtora.financeiro.model.enums.ReconciliationMode;
import com.construtora.financeiro.model.enums.TargetType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "reconciliations")
@Getter
@Setter
public class Reconciliation {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "bank_transaction_id")
    private BankTransaction bankTransaction;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false)
    private TargetType targetType;

    /** Id polimórfico do alvo (receivable / payable / installment). */
    @Column(name = "target_id", nullable = false)
    private UUID targetId;

    @Column(name = "matched_amount", nullable = false)
    private BigDecimal matchedAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReconciliationMode mode = ReconciliationMode.MANUAL;

    private BigDecimal confidence;

    @Column(name = "reconciled_by")
    private UUID reconciledBy;

    @Column(name = "reconciled_at", nullable = false)
    private OffsetDateTime reconciledAt = OffsetDateTime.now();

    @Column(nullable = false)
    private boolean undone = false;

    @Column(name = "undone_at")
    private OffsetDateTime undoneAt;

    @Column(columnDefinition = "text")
    private String notes;
}
